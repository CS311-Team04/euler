// functions/src/features/ed.ts

import * as logger from "firebase-functions/logger";
import { db } from "../core/firebase";
import { EdConnectorRepository } from "../connectors/ed/EdConnectorRepository";
import { EdConnectorService } from "../connectors/ed/EdConnectorService";
import { encryptSecret, decryptSecret } from "../security/secretCrypto";
import { EdDiscussionClient } from "../connectors/ed/EdDiscussionClient";
import {
  parseEdSearchQuery,
  buildEdSearchRequest,
  normalizeEdThreads,
  EdBrainError,
  NormalizedEdPost,
} from "../edSearchDomain";

// ED Discussion API base URL
const ED_DEFAULT_BASE_URL = "https://eu.edstem.org/api";

type EdBrainSearchInput = {
  query: string;
  limit?: number;
};

export type EdBrainSearchOutput = {
  ok: boolean;
  posts: NormalizedEdPost[];
  filters: {
    course?: string;
    status?: string;
    limit?: number;
  };
  error?: EdBrainError;
};

function isEdBrainError(result: any): result is EdBrainError {
  return result && typeof result === "object" && "type" in result;
}

export async function edBrainSearchCore(
  uid: string,
  input: EdBrainSearchInput
): Promise<EdBrainSearchOutput> {
  const query = (input.query || "").trim();
  if (!query) {
    return {
      ok: false,
      posts: [],
      filters: {},
      error: { type: "INVALID_QUERY", message: "Missing 'query'" },
    };
  }

  // 1) Get the user's ED config
  const edConnectorRepository = new EdConnectorRepository(db);
  const config = await edConnectorRepository.getConfig(uid);
  if (!config || !config.apiKeyEncrypted || !config.baseUrl) {
    return {
      ok: false,
      posts: [],
      filters: {},
      error: {
        type: "AUTH_ERROR",
        message: "ED connector not configured for this user",
      },
    };
  }

  // 2) Decrypt the token
  let apiToken: string;
  try {
    apiToken = decryptSecret(config.apiKeyEncrypted);
  } catch (e) {
    logger.error("edBrainSearch.decryptFailed", { uid, error: String(e) });
    return {
      ok: false,
      posts: [],
      filters: {},
      error: {
        type: "AUTH_ERROR",
        message: "Failed to decrypt ED token",
      },
    };
  }

  const client = new EdDiscussionClient(config.baseUrl, apiToken);

  // 3) Get the user's ED courses
  let courses;
  try {
    const userInfo = await client.getUser();
    courses = userInfo.courses.map((c) => c.course);
  } catch (e: any) {
    const msg = String(e?.message || e);
    logger.error("edBrainSearch.fetchUserFailed", { uid, error: msg });

    if (msg.includes("401") || msg.includes("403")) {
      return {
        ok: false,
        posts: [],
        filters: {},
        error: {
          type: "AUTH_ERROR",
          message: "ED authentication failed",
        },
      };
    }
    if (msg.includes("429")) {
      return {
        ok: false,
        posts: [],
        filters: {},
        error: {
          type: "RATE_LIMIT",
          message: "ED API rate limit reached",
        },
      };
    }
    return {
      ok: false,
      posts: [],
      filters: {},
      error: {
        type: "NETWORK_ERROR",
        message: "Failed to contact ED API",
      },
    };
  }

  // 4) Parse the natural language query
  const parsed = parseEdSearchQuery(query);

  // 5) Resolve the course + fetch options
  const req = await buildEdSearchRequest(client, parsed, courses);
  if (isEdBrainError(req)) {
    return {
      ok: false,
      posts: [],
      filters: {
        status: "all", // Default, LLM would have set this if successful
        limit: input.limit ?? 5, // Use input limit or default
      },
      error: req,
    };
  }

  // Override limit if explicitly provided in input
  if (input.limit != null && Number.isFinite(input.limit)) {
    req.fetchOptions.limit = input.limit;
  }

  // 6) Fetch threads from ED
  let threads;
  try {
    threads = await client.fetchThreads(req.fetchOptions);
  } catch (e: any) {
    const msg = String(e?.message || e);
    logger.error("edBrainSearch.fetchThreadsFailed", { uid, error: msg });

    const filters = {
      course: req.resolvedCourse.code || req.resolvedCourse.name,
      status: req.fetchOptions.statusFilter || "all",
      limit: req.fetchOptions.limit ?? 5,
    };

    if (msg.includes("401") || msg.includes("403")) {
      return {
        ok: false,
        posts: [],
        filters,
        error: {
          type: "AUTH_ERROR",
          message: "ED authentication failed",
        },
      };
    }
    if (msg.includes("429")) {
      return {
        ok: false,
        posts: [],
        filters,
        error: {
          type: "RATE_LIMIT",
          message: "ED API rate limit reached",
        },
      };
    }
    return {
      ok: false,
      posts: [],
      filters,
      error: {
        type: "NETWORK_ERROR",
        message: "Failed to contact ED API",
      },
    };
  }

  if (!threads.length) {
    return {
      ok: false,
      posts: [],
      filters: {
        course: req.resolvedCourse.code || req.resolvedCourse.name,
        status: req.fetchOptions.statusFilter || "all",
        limit: req.fetchOptions.limit ?? 5,
      },
      error: {
        type: "NO_RESULTS",
        message: "No posts found for these filters",
      },
    };
  }

  // 7) Normalize and return
  const normalized = normalizeEdThreads(threads, req.resolvedCourse);

  return {
    ok: true,
    posts: normalized,
    filters: {
      course: req.resolvedCourse.code || req.resolvedCourse.name,
      status: req.fetchOptions.statusFilter || "all",
      limit: req.fetchOptions.limit ?? 5,
    },
  };
}

