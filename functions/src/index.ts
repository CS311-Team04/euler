// functions/src/index.ts

import path from "node:path";
import dotenv from "dotenv";
dotenv.config({ path: path.join(__dirname, "..", ".env") });

import * as logger from "firebase-functions/logger";
import * as functions from "firebase-functions/v1";
import admin from "firebase-admin";
import { db, europeFunctions, requireAuth, checkKey } from "./core/firebase";
import { generateTitleCore, apertusChatFnCore, type GenerateTitleInput } from "./core/llm";
import { indexChunksCore, type IndexChunksInput } from "./rag/indexChunks";
import { answerWithRagCore, type AnswerWithRagInput } from "./rag/rag";
import { edBrainSearchCore } from "./features/ed";
import { syncEpflScheduleCore, type SyncScheduleInput } from "./features/schedule";
import { syncEpflFoodMenusCore, getFoodContextCore } from "./features/food";
import { handleMoodleIntent } from "./features/moodle";
import { onMessageCreateHandler } from "./triggers/summary";

// ED connector imports
import { EdConnectorRepository } from "./connectors/ed/EdConnectorRepository";
import { EdConnectorService } from "./connectors/ed/EdConnectorService";
import { encryptSecret, decryptSecret } from "./security/secretCrypto";
import {
  detectPostToEdIntentCore,
  detectFetchFromEdIntentCore,
  buildEdIntentPromptForApertus,
} from "./edIntent";
import { parseEdPostResponse } from "./edIntentParser";
import { EdDiscussionClient } from "./connectors/ed/EdDiscussionClient";
import { callEdRouterLLM, EdLLMRouterInput } from "./edLLMRouter";

// Moodle connector imports
import { MoodleConnectorRepository } from "./connectors/moodle/MoodleConnectorRepository";
import { MoodleConnectorService } from "./connectors/moodle/MoodleConnectorService";

// ED Discussion API base URL
const ED_DEFAULT_BASE_URL = "https://eu.edstem.org/api";

// Initialize connector services
const edConnectorRepository = new EdConnectorRepository(db);
const edConnectorService = new EdConnectorService(
  edConnectorRepository,
  encryptSecret,
  decryptSecret,
  ED_DEFAULT_BASE_URL
);

const moodleRepo = new MoodleConnectorRepository(db);
const moodleService = new MoodleConnectorService(moodleRepo, encryptSecret, decryptSecret);

/* =========================================================
 *                EXPORTS: callable + HTTP twins
 * ======================================================= */

// ping
export const ping = europeFunctions.https.onRequest((_req, res) => {
  res.status(200).send("pong");
});

// callable (for Kotlin via Firebase SDK)
export const indexChunksFn = europeFunctions.https.onCall(async (data: IndexChunksInput) => {
  try {
    const { chunks } = data || ({} as any);
    if (!Array.isArray(chunks) || chunks.length === 0) {
      throw new functions.https.HttpsError("invalid-argument", "Missing 'chunks'");
    }
    return await indexChunksCore({ chunks });
  } catch (e: any) {
    logger.error("indexChunksFn.failed", { error: String(e) });
    // Surface a useful message to the client
    throw new functions.https.HttpsError(
      "internal",
      "indexChunks failed",
      String(e?.message || e)
    );
  }
});

export const answerWithRagFn = europeFunctions.https.onCall(async (data: AnswerWithRagInput, context) => {
  try {
    const question = String(data?.question || "").trim();
    const topK = Number(data?.topK ?? 2);
    const model = data?.model;
    const summary = typeof data?.summary === "string" ? data.summary : undefined;
    const recentTranscript =
      typeof (data as any)?.recentTranscript === "string" ? (data as any).recentTranscript : undefined;
    // Get uid from auth context for schedule integration
    const uid = context.auth?.uid;

    if (!question) throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");

    // Handle Moodle intent (course overview or file fetch)
    const moodleResult = await handleMoodleIntent(question, uid, {
          moodleRepo,
      moodleService,
    });
    if (moodleResult) {
      return moodleResult;
    }

    const fetchIntentResult = detectFetchFromEdIntentCore(question);
    const { ed_fetch_intent_detected, ed_fetch_query } = fetchIntentResult;

    // === ED Intent Detection (fast, regex-based) ===
    const edIntentResult = detectPostToEdIntentCore(question);
    if (edIntentResult.ed_intent_detected && edIntentResult.ed_intent) {
      logger.info("answerWithRagFn.edIntentDetected", {
        intent: edIntentResult.ed_intent,
        questionLen: question.length,
      });

      // Hybrid approach: REGEX detects, Apertus responds naturally and formats the question
      const { reply: rawReply } = await apertusChatFnCore({
        messages: [{ role: "user", content: buildEdIntentPromptForApertus(question) }],
        temperature: 0.3,
      });

      // Parse the response to extract formatted question and title
      const { reply, formattedQuestion, formattedTitle } = parseEdPostResponse(rawReply);

      // Try to detect the suggested course from the prompt
      let suggestedCourseId: number | null = null;
      if (uid) {
        try {
          const existing = await edConnectorRepository.getConfig(uid);
          if (existing && existing.apiKeyEncrypted) {
            const apiToken = decryptSecret(existing.apiKeyEncrypted);
            const baseUrl = existing.baseUrl || ED_DEFAULT_BASE_URL;
            const client = new EdDiscussionClient(baseUrl, apiToken);
            const courses = await client.getCourses();

            if (courses.length > 0) {
              const llmInput: EdLLMRouterInput = {
                userQuery: question,
                courses: courses.map((c) => ({
                  id: c.id,
                  code: c.code || undefined,
                  name: c.name || undefined,
                })),
              };

              const llmOutput = await callEdRouterLLM(llmInput);
              if (llmOutput.courseId) {
                suggestedCourseId = llmOutput.courseId;
                logger.info("answerWithRagFn.courseDetected", {
                  suggestedCourseId,
                  courseCode: llmOutput.courseCode,
                });
              }
            }
          }
        } catch (e: any) {
          // If course detection fails, continue without it (non-blocking)
          logger.warn("answerWithRagFn.courseDetectionFailed", {
            error: String(e),
            message: e?.message,
          });
        }
      }

      return {
        reply,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: true,
        ed_intent: edIntentResult.ed_intent,
        ed_formatted_question: formattedQuestion,
        ed_formatted_title: formattedTitle,
        ed_suggested_course_id: suggestedCourseId,
        ed_fetch_intent_detected,
        ed_fetch_query,
        moodle_intent_detected: false,
        moodle_intent: null,
        moodle_file: null,
      };
    }
    // === End ED Intent Detection ===

    const profileContext =
      typeof (data as any)?.profileContext === "string" ? (data as any).profileContext : undefined;

    // Log received profile context (structured logging only)
    logger.info("answerWithRagFn.profileContext", {
      received: Boolean(profileContext),
      length: profileContext ? profileContext.length : 0,
      preview: profileContext ? profileContext.slice(0, 200) : null,
      fullContent: profileContext || null, // Log full content for debugging
    });

    logger.info("answerWithRagFn.input", {
      questionLen: question.length,
      hasSummary: Boolean(summary),
      summaryLen: summary ? summary.length : 0,
      hasTranscript: Boolean(recentTranscript),
      transcriptLen: recentTranscript ? recentTranscript.length : 0,
      hasProfileContext: Boolean(profileContext),
      profileContextLen: profileContext ? profileContext.length : 0,
      hasUid: Boolean(uid),
      topK,
      model: model ?? process.env.APERTUS_MODEL_ID!,
    });

    const ragResult = await answerWithRagCore({
      question,
      topK,
      model,
      summary,
      recentTranscript,
      profileContext,
      uid,
    });
    return {
      ...ragResult,
      ed_intent_detected: false,
      ed_intent: null,
      ed_formatted_question: null,
      ed_formatted_title: null,
      ed_suggested_course_id: null,
      ed_fetch_intent_detected,
      ed_fetch_query,
      moodle_intent_detected: false,
      moodle_intent: null,
      moodle_file: null,
    };
  } catch (e: any) {
    logger.error("answerWithRagFn.failed", { error: String(e) });
    throw new functions.https.HttpsError(
      "internal",
      "answerWithRag failed",
      String(e?.message || e)
    );
  }
});

// HTTP endpoints (for Python)
export const indexChunksHttp = europeFunctions.https.onRequest(async (req: functions.https.Request, res: functions.Response<any>) => {
  try {
    checkKey(req);
    if (req.method !== "POST") { res.status(405).end(); return; }
    const body = req.body || {};
    const { chunks } = body;
    if (!Array.isArray(chunks) || chunks.length === 0) {
      res.status(400).json({ error: "Missing 'chunks'" });
      return;
    }
    const result = await indexChunksCore({ chunks });
    res.status(200).json(result);
  } catch (e: any) {
    const code = (e as any).code;
    res.status(code === 401 ? 401 : 400).json({ error: String(e) });
  }
});

export const answerWithRagHttp = europeFunctions.https.onRequest(async (req: functions.https.Request, res: functions.Response<any>) => {
  try {
    checkKey(req);
    if (req.method !== "POST") { res.status(405).end(); return; }
    const body = req.body || {};
    const question = String(body.question || "").trim();
    if (!question) {
      res.status(400).json({ error: "Missing 'question'" });
      return;
    }
    const result = await answerWithRagCore({
      question,
      topK: typeof body.topK === "number" ? body.topK : undefined,
      model: typeof body.model === "string" ? body.model : undefined,
    });
    res.status(200).json(result);
  } catch (e: any) {
    res.status((e as any).code === 401 ? 401 : 400).json({ error: String(e) });
  }
});

export const generateTitleFn = europeFunctions.https.onCall(async (data: GenerateTitleInput) => {
  const q = String(data?.question || "").trim();
  const model = data?.model;
  if (!q) throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");
  return await generateTitleCore({ question: q, model });
});

type EdConnectorConnectCallableInput = {
  apiToken?: string;
  baseUrl?: string;
};

export const edConnectorStatusFn = europeFunctions.https.onCall(
  async (_data, context) => {
    const uid = requireAuth(context);

    try {
      const config = await edConnectorService.getStatus(uid);
      return config;
    } catch (e: any) {
      logger.error("edConnectorStatusFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to get ED connector status",
        String(e?.message || e)
      );
    }
  }
);

export const edConnectorConnectFn = europeFunctions.https.onCall(
  async (data: EdConnectorConnectCallableInput, context) => {
    const uid = requireAuth(context);

    const apiToken = String(data?.apiToken || "").trim();
    const baseUrl =
      typeof data?.baseUrl === "string" ? data.baseUrl : undefined;

    if (!apiToken) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing 'apiToken'"
      );
    }

    try {
      const config = await edConnectorService.connect(uid, {
        apiToken,
        baseUrl,
      });
      return config;
    } catch (e: any) {
      logger.error("edConnectorConnectFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to connect ED connector",
        String(e?.message || e)
      );
    }
  }
);

export const edConnectorDisconnectFn = europeFunctions.https.onCall(
  async (_data, context) => {
    const uid = requireAuth(context);

    try {
      await edConnectorService.disconnect(uid);
      // For convenience, return a simple status the UI can use.
      return { status: "not_connected" };
    } catch (e: any) {
      logger.error("edConnectorDisconnectFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to disconnect ED connector",
        String(e?.message || e)
      );
    }
  }
);

export const edConnectorTestFn = europeFunctions.https.onCall(
  async (_data, context) => {
    const uid = requireAuth(context);

    try {
      const config = await edConnectorService.test(uid);
      return config;
    } catch (e: any) {
      logger.error("edConnectorTestFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to test ED connector",
        String(e?.message || e)
      );
    }
  }
);

type EdBrainSearchCallableInput = {
  query?: string;
  limit?: number;
};

export const edBrainSearchFn = europeFunctions.https.onCall(
  async (data: EdBrainSearchCallableInput, context) => {
    const uid = requireAuth(context);

    const query = String(data?.query || "").trim();
    const limit =
      typeof data?.limit === "number" && Number.isFinite(data.limit)
        ? data.limit
        : undefined;

    try {
      const result = await edBrainSearchCore(uid, { query, limit });
      return result;
    } catch (e: any) {
      logger.error("edBrainSearchFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "edBrainSearch failed",
        String(e?.message || e)
      );
    }
  }
);

type EdConnectorPostCallableInput = {
  title?: string;
  body?: string;
  courseId?: number;
  isAnonymous?: boolean;
};

export const edConnectorPostFn = europeFunctions.https.onCall(
  async (data: EdConnectorPostCallableInput, context) => {
    const uid = requireAuth(context);

    const title = String(data?.title || "").trim();
    const body = String(data?.body || "").trim();
    const courseId =
      typeof data?.courseId === "number" ? data.courseId : undefined;
    const isAnonymous =
      typeof data?.isAnonymous === "boolean" ? data.isAnonymous : false;

    if (!title && !body) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "title or body is required"
      );
    }

    if (!courseId) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "courseId is required"
      );
    }

    try {
      const result = await edConnectorService.postThread(uid, {
        title,
        body,
        courseId,
        isAnonymous,
      });
      return result;
    } catch (e: any) {
      if (e instanceof functions.https.HttpsError) throw e;
      logger.error("edConnectorPostFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to post ED thread",
        String(e?.message || e)
      );
    }
  }
);

export const edConnectorGetCoursesFn = europeFunctions.https.onCall(
  async (_data, context) => {
    const uid = requireAuth(context);

    try {
      const existing = await edConnectorRepository.getConfig(uid);
      if (!existing || !existing.apiKeyEncrypted) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "ED connector is not connected"
        );
      }

      const apiToken = decryptSecret(existing.apiKeyEncrypted);
      const baseUrl = existing.baseUrl || ED_DEFAULT_BASE_URL;
      const client = new EdDiscussionClient(baseUrl, apiToken);

      const courses = await client.getCourses();
      return {
        courses: courses.map((c) => ({
          id: c.id,
          code: c.code,
          name: c.name,
        })),
      };
    } catch (e: any) {
      if (e instanceof functions.https.HttpsError) throw e;
      logger.error("edConnectorGetCoursesFn.failed", {
        uid,
        error: String(e),
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to get ED courses",
        String(e?.message || e)
      );
    }
  }
);

/* =========================================================
 *                MOODLE CONNECTOR
 * ======================================================= */

// callable functions
export const connectorsMoodleStatusFn = europeFunctions.https.onCall(
  async (data, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const config = await moodleService.getStatus(context.auth.uid);
    return {
      status: config.status,
      lastTestAt: config.lastTestAt ?? null,
      lastError: config.lastError ?? null,
    };
  }
);

// Fetches a Moodle token using username and password (server-side for security)
export const connectorsMoodleFetchTokenFn = europeFunctions.https.onCall(
  async (
    data: { baseUrl: string; username: string; password: string },
    context
  ) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { baseUrl, username, password } = data;

    // Validate inputs
    if (!baseUrl || !username || !password) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "baseUrl, username, and password are required"
      );
    }

    try {
      // Normalize baseUrl (remove trailing slash)
      const normalizedBaseUrl = baseUrl.trim().replace(/\/$/, "");

      // Build token endpoint URL - credentials sent server-side only
      const tokenUrl = `${normalizedBaseUrl}/login/token.php?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}&service=moodle_mobile_app`;

      logger.info("Fetching Moodle token", {
        baseUrl: normalizedBaseUrl,
        userId: context.auth.uid,
      });

      // Call Moodle API server-side (credentials never exposed to client)
      const response = await fetch(tokenUrl);
      const responseBody = await response.json();

      if (!response.ok || responseBody.error) {
        const errorMsg = responseBody.error || `HTTP ${response.status}`;
        logger.error("Failed to fetch Moodle token", {
          error: errorMsg,
          userId: context.auth.uid,
        });
        throw new functions.https.HttpsError(
          "internal",
          "Failed to fetch token from Moodle",
          errorMsg
        );
      }

      if (!responseBody.token) {
        logger.error("No token in Moodle response", {
          userId: context.auth.uid,
        });
        throw new functions.https.HttpsError(
          "internal",
          "No token received from Moodle"
        );
      }

      // Return only the token (credentials never returned to client)
      return { token: responseBody.token };
    } catch (e: any) {
      if (e instanceof functions.https.HttpsError) {
        throw e;
      }
      logger.error("Exception fetching Moodle token", {
        error: String(e),
        userId: context.auth.uid,
      });
      throw new functions.https.HttpsError(
        "internal",
        "Failed to fetch token from Moodle",
        String(e?.message || e)
      );
    }
  }
);
// attempts to connect to Moodle with provided baseUrl and token
export const connectorsMoodleConnectFn = europeFunctions.https.onCall(
  async (data, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const baseUrl = String(data?.baseUrl || "").trim();
    const token = String(data?.token || "").trim();
    if (!baseUrl || !token) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "baseUrl and token are required"
      );
    }

    const config = await moodleService.connect(context.auth.uid, { baseUrl, token });
    return {
      status: config.status,
      lastTestAt: config.lastTestAt ?? null,
      lastError: config.lastError ?? null,
    };
  }
);
// disconnects Moodle for the user
export const connectorsMoodleDisconnectFn = europeFunctions.https.onCall(
  async (data, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    await moodleService.disconnect(context.auth.uid);
    return { status: "not_connected" };
  }
);
// tests the Moodle connection for the user
export const connectorsMoodleTestFn = europeFunctions.https.onCall(
  async (data, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const config = await moodleService.test(context.auth.uid);
    return {
      status: config.status,
      lastTestAt: config.lastTestAt ?? null,
      lastError: config.lastError ?? null,
    };
  }
);

/* =========================================================
 *        EPFL CAMPUS SCHEDULE (ICS Integration)
 * ======================================================= */

/** Callable: sync EPFL schedule */
export const syncEpflScheduleFn = europeFunctions.https.onCall(async (data: SyncScheduleInput, context) => {
  if (!context.auth?.uid) {
    throw new functions.https.HttpsError("unauthenticated", "User must be signed in");
  }

  try {
    return await syncEpflScheduleCore(context.auth.uid, data);
  } catch (e: any) {
    logger.error("syncEpflScheduleFn.failed", { error: String(e), uid: context.auth.uid });
    throw new functions.https.HttpsError("internal", e.message || "Failed to sync schedule");
  }
});

/** Callable: disconnect EPFL schedule */
export const disconnectEpflScheduleFn = europeFunctions.https.onCall(async (_data, context) => {
  if (!context.auth?.uid) {
    throw new functions.https.HttpsError("unauthenticated", "User must be signed in");
  }

  try {
    const userRef = db.collection('users').doc(context.auth.uid);
    await userRef.update({
      epflSchedule: admin.firestore.FieldValue.delete(),
    });

    logger.info("disconnectEpflSchedule.success", { uid: context.auth.uid });
    return { success: true, message: "EPFL schedule disconnected" };
  } catch (e: any) {
    logger.error("disconnectEpflScheduleFn.failed", { error: String(e), uid: context.auth.uid });
    throw new functions.https.HttpsError("internal", e.message || "Failed to disconnect");
  }
});

/** Callable: get schedule status */
export const getEpflScheduleStatusFn = europeFunctions.https.onCall(async (_data, context) => {
  if (!context.auth?.uid) {
    throw new functions.https.HttpsError("unauthenticated", "User must be signed in");
  }

  try {
    const userDoc = await db.collection('users').doc(context.auth.uid).get();
    const data = userDoc.data();

    if (!data?.epflSchedule) {
      return { connected: false };
    }

    // New optimized format
    if (data.epflSchedule.weeklySlots) {
      return {
        connected: true,
        weeklySlots: data.epflSchedule.slotCount ?? data.epflSchedule.weeklySlots?.length ?? 0,
        finalExams: data.epflSchedule.examCount ?? data.epflSchedule.finalExams?.length ?? 0,
        eventCount: (data.epflSchedule.slotCount ?? 0) + (data.epflSchedule.examCount ?? 0),
        lastSync: data.epflSchedule.lastSync?.toDate?.()?.toISOString() ?? null,
        optimized: true,
      };
    }

    // Legacy format
    return {
      connected: true,
      eventCount: data.epflSchedule.eventCount ?? data.epflSchedule.events?.length ?? 0,
      lastSync: data.epflSchedule.lastSync?.toDate?.()?.toISOString() ?? null,
      optimized: false,
    };
  } catch (e: any) {
    logger.error("getEpflScheduleStatusFn.failed", { error: String(e), uid: context.auth.uid });
    throw new functions.https.HttpsError("internal", e.message || "Failed to get status");
  }
});

/* =========================================================
 *        EPFL FOOD MENUS (Web Scraping Integration)
 * ======================================================= */

/** Callable: sync EPFL food menus (admin/scheduled function) */
export const syncEpflFoodMenusFn = europeFunctions.https.onCall(async (_data, context) => {
  // This could be restricted to admin users or run as a scheduled function
  try {
    return await syncEpflFoodMenusCore();
  } catch (e: any) {
    logger.error("syncEpflFoodMenusFn.failed", { error: String(e) });
    throw new functions.https.HttpsError("internal", e.message || "Failed to sync food menus");
  }
});

/** Scheduled function: sync food menus every day at 6 AM */
export const scheduledFoodSync = functions
  .region("europe-west6")
  .pubsub.schedule("0 6 * * 1-5") // Every weekday at 6 AM
  .timeZone("Europe/Zurich")
  .onRun(async () => {
    logger.info("scheduledFoodSync.triggered");
    try {
      await syncEpflFoodMenusCore();
      logger.info("scheduledFoodSync.success");
    } catch (e) {
      logger.error("scheduledFoodSync.failed", { error: String(e) });
    }
  });

/** HTTP endpoint: get food context (for testing) */
export const getFoodContextHttp = europeFunctions.https.onRequest(async (req, res) => {
  try {
    if (req.method !== "GET") { res.status(405).end(); return; }
    const context = await getFoodContextCore();
    res.status(200).json({ context });
  } catch (e: any) {
    res.status(500).json({ error: String(e) });
  }
});

/* =========================================================
 *                ROLLING SUMMARY (Firestore)
 * ======================================================= */

export const onMessageCreate = europeFunctions.firestore
  .document("users/{uid}/conversations/{cid}/messages/{mid}")
  .onCreate(async (snap: functions.firestore.DocumentSnapshot, ctx: functions.EventContext) => {
    await onMessageCreateHandler(snap, ctx);
  });

// Re-export core functions for tests
export { answerWithRagCore } from "./rag/rag";
export { generateTitleCore } from "./core/llm";
export { indexChunksCore } from "./rag/indexChunks";
