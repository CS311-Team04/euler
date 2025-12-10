// functions/src/edSearchDomain.ts

import {
  EdCourse,
  EdThread,
  FetchThreadsOptions,
  EdDiscussionClient,
} from "./connectors/ed/EdDiscussionClient";
import {
  callEdRouterLLM,
  EdLLMRouterInput,
  EdLLMRouterOutput,
  callEdCategoryLLM,
  EdCategoryLLMInput,
} from "./edLLMRouter";
import * as logger from "firebase-functions/logger";

export type EdPostStatusFilter =
  | "all"
  | "unanswered"
  | "unread"
  | "resolved"
  | "new_replies" 
  | "approved"  
  | "favorites" 
  | "instructors";

/**
 * Represents what we understand from the user's natural language query.
 */
export interface EdSearchParsedQuery {
  originalQuery: string;

  /** Example "CS-101" before resolution to ED courseId */
  courseQuery?: string;

  /** High-level status filter (unanswered, unread, resolved, etc.) */
  status?: EdPostStatusFilter;

  /** Date range (ISO) to filter on ED side */
  dateFrom?: string;
  dateTo?: string;
}

/**
 * Standard format for a post that the frontend will consume.
 */
export interface NormalizedEdPost {
  postId: string;
  title: string;
  snippet: string;
  contentMarkdown: string;
  author: string;
  createdAt: string;
  status: string;
  course: string;
  tags: string[];
  url: string;
}

/**
 * Structured errors returned by the brain.
 */
export type EdBrainError =
  | { type: "AUTH_ERROR"; message: string }
  | { type: "NO_RESULTS"; message: string }
  | { type: "RATE_LIMIT"; message: string }
  | { type: "NETWORK_ERROR"; message: string }
  | { type: "INVALID_QUERY"; message: string };

/**
 * Normalizes a string for fuzzy matching (lowercase, remove accents, special chars).
 */
function normalize(str: string): string {
  return str
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9 ]/g, "")
    .trim();
}


/**
 * Parses natural language query into structured EdSearchParsedQuery.
 * Extracts course query and date range.
 * Note: status and limit are now handled by the LLM router.
 */
export function parseEdSearchQuery(text: string): EdSearchParsedQuery {
  const lower = text.toLowerCase();

  // ===== Course =====
  // 1) First, try a code like COM-301, CS-202, etc. (case-insensitive)
  let courseQuery: string | undefined;
  const courseCodeMatch = text.match(/[A-Za-z]{2,4}-\d{2,3}/i);

  if (courseCodeMatch) {
    // Preserve the original case from the match, but normalize for consistency
    courseQuery = courseCodeMatch[0];
  } else {
    // 2) Otherwise, try to extract the name after "in" or "from"
    // ex: "show instructors posts in Algebra" or "show posts in Algebra about homework"
    // Improvement: capture up to a following keyword (about, regarding, etc.) or until the end
    const nameMatch = text.match(/\b(?:in|from)\s+([A-Za-z0-9\-()\/ ]+?)(?:\s+(?:about|regarding|concerning|for|on)\s+|$)/i);
    if (nameMatch) {
      const rawName = nameMatch[1].trim().replace(/[.,!?]+$/, "");
      const lowerName = rawName.toLowerCase();

      const looksLikeExercise =
        lowerName.startsWith("module ") ||
        lowerName.startsWith("exo ") ||
        lowerName.startsWith("exercise ") ||
        lowerName.startsWith("exercice ") ||
        lowerName.startsWith("assignment ") ||
        lowerName.startsWith("assignement ") ||
        lowerName.startsWith("hw ");

      if (!looksLikeExercise) {
        courseQuery = rawName;
      }
    }

    // 3) If still nothing, search in synonyms directly in the phrase
    if (!courseQuery) {
      const normalizedText = normalize(text);

      for (const [code, aliases] of Object.entries(COURSE_SYNONYMS)) {
        for (const alias of aliases) {
          const normAlias = normalize(alias);
          if (normAlias && normalizedText.includes(normAlias)) {
            // Found an alias, use the corresponding code
            courseQuery = code;
            break;
          }
        }
        if (courseQuery) break;
      }
    }
  }

  // Dates (MVP)
  let dateFrom: string | undefined;
  let dateTo: string | undefined;
  const now = new Date();

  if (lower.includes("yesterday") || lower.includes("hier")) {
    const d = new Date(now);
    d.setDate(d.getDate() - 1);
    dateFrom = d.toISOString();
    dateTo = d.toISOString();
  } else if (lower.includes("today") || lower.includes("aujourd'hui")) {
    dateFrom = now.toISOString();
    dateTo = now.toISOString();
  }

  return {
    originalQuery: text,
    courseQuery,
    dateFrom,
    dateTo,
  };
}

/**
 * Parameters needed to call EdDiscussionClient.fetchThreads.
 */
export interface EdSearchRequestParams {
  courseId: number;
  fetchOptions: FetchThreadsOptions;
  resolvedCourse: EdCourse;
}

/**
 * Generates course code variants (lowercase, no space, with dash) from a base code.
 */
function generateCourseCodeVariants(code: string): string[] {
  const [prefix, number] = code.split("-");
  if (!prefix || !number) return [];
  const lowerPrefix = prefix.toLowerCase();
  return [
    `${lowerPrefix} ${number}`,
    `${lowerPrefix}${number}`,
    `${lowerPrefix}-${number}`,
  ];
}

/**
 * Course synonyms map. Base aliases are manually defined, code variants are auto-generated.
 */
const COURSE_SYNONYMS: Record<string, string[]> = {
  // Software Enterprise (Swent)
  "CS-311": ["swent", "the software enterprise", "software enterprise", "soft ent", ...generateCourseCodeVariants("CS-311")],

  // Computer Security
  "COM-301": ["compsec", "computer security", "security", "comp sec", "computer sec", "computer secu", ...generateCourseCodeVariants("COM-301")],

  // Stochastic models / Modstock
  "COM-300": ["modstock", "modeles stochastiques", "modèles stochastiques", "stochastics", "mod stoch", "stoch models", "stochastic models", ...generateCourseCodeVariants("COM-300")],

  // Signal Processing
  "COM-302": ["sigproc", "signal processing", "signal proc", ...generateCourseCodeVariants("COM-302")],

  // Probability & Statistics
  "MATH-232": ["probastat", "proba stat", "probability and statistics", "probability & statistics", ...generateCourseCodeVariants("MATH-232")],

  // Analysis 3
  "MATH-203": ["analysis 3", "analys 3", "ana3", "analysis iii", "analyse 3", "analyse iii", ...generateCourseCodeVariants("MATH-203")],

  // Time & Discrete Systems
  "CS-234": ["tds", "time and discrete systems", "time & discrete systems", ...generateCourseCodeVariants("CS-234")],

  // Algebra
  "MATH-310": ["algebra", "algèbre", "algebre", "algebra 3", ...generateCourseCodeVariants("MATH-310")],
};

/**
 * Resolves a course from the full text using course synonyms.
 */
function resolveCourseFromSynonyms(
  originalQuery: string,
  availableCourses: EdCourse[]
): EdCourse | undefined {
  const normalizedText = normalize(originalQuery);

  for (const course of availableCourses) {
    const code = course.code;
    if (!code) continue;

    const aliases = COURSE_SYNONYMS[code];
    if (!aliases) continue;

    for (const alias of aliases) {
      const normAlias = normalize(alias);
      if (normAlias && normalizedText.includes(normAlias)) {
        return course;
      }
    }
  }

  return undefined;
}

/**
 * Applies LLM router output to fetchOptions (textQuery, limit, status, category).
 * This is a helper to avoid code duplication.
 */
async function applyLLMParameters(
  edClient: EdDiscussionClient,
  fetchOptions: FetchThreadsOptions,
  resolvedCourse: EdCourse,
  llm: EdLLMRouterOutput,
  originalQuery: string
): Promise<void> {
  // Update only textQuery, limit, status from LLM (ignore courseId/courseCode)
  if (llm.textQuery && llm.textQuery.trim()) {
    fetchOptions.query = llm.textQuery.trim();
  }

  if (llm.limit != null && Number.isFinite(llm.limit)) {
    fetchOptions.limit = llm.limit;
  }

  // Update status from LLM (LLM chooses the status filter)
  if (llm.status) {
    fetchOptions.statusFilter = llm.status as EdPostStatusFilter;
  }

  // Category selection: fetch real ED categories and let LLM choose
  const categoryTree = await edClient.fetchCategoryTreeFromThreads(resolvedCourse.id);
  const categoryNames = categoryTree.categories || [];

  if (categoryNames.length > 0) {
    const catInput: EdCategoryLLMInput = {
      userQuery: originalQuery,
      course: {
        id: resolvedCourse.id,
        code: resolvedCourse.code,
        name: resolvedCourse.name,
      },
      categoryTree: categoryTree,
    };

    const catOutput = await callEdCategoryLLM(catInput);

    // Set main category if valid
    if (catOutput.chosenCategory && categoryNames.includes(catOutput.chosenCategory)) {
      fetchOptions.category = catOutput.chosenCategory;

      // Set subcategory if valid and available
      if (catOutput.chosenSubcategory) {
        const subcategories = categoryTree.subcategoriesByCategory[catOutput.chosenCategory] || [];
        // Case-insensitive match for subcategory
        const matchedSubcategory = subcategories.find(
          (sub) => sub.toLowerCase().trim() === catOutput.chosenSubcategory!.toLowerCase().trim()
        );
        if (matchedSubcategory) {
          fetchOptions.subcategory = matchedSubcategory;
        }
      }
    }

    logger.info("edCategoryLLM.result", {
      chosenCategory: catOutput.chosenCategory,
      chosenSubcategory: catOutput.chosenSubcategory,
      resolvedCategory: fetchOptions.category,
      resolvedSubcategory: fetchOptions.subcategory,
    });
  }
}

/**
 * Resolves courseQuery → ED courseId and builds options
 * for EdDiscussionClient.fetchThreads.
 * Uses the LLM router to refine search parameters (textQuery, limit, status, category).
 */
export async function buildEdSearchRequest(
  edClient: EdDiscussionClient,
  parsed: EdSearchParsedQuery,
  availableCourses: EdCourse[]
): Promise<EdSearchRequestParams | EdBrainError> {
  // Manual course resolution (rule-based only, no LLM fallback)
  if (!parsed.courseQuery) {
    return {
      type: "INVALID_QUERY",
      message: "No course detected in the query",
    };
  }

  const normalizedCourseQuery = parsed.courseQuery.toLowerCase();

  // 1) Try direct code match
  let resolvedCourse =
    availableCourses.find((c) =>
      c.code?.toLowerCase().includes(normalizedCourseQuery)
    ) ||
    availableCourses.find((c) =>
      c.name?.toLowerCase().includes(normalizedCourseQuery)
    );

  // 2) Fallback to synonym-based resolution if direct resolution failed
  if (!resolvedCourse) {
    resolvedCourse = resolveCourseFromSynonyms(
      parsed.originalQuery,
      availableCourses
    );
  }

  // If no course found after all rule-based attempts, return error
  if (!resolvedCourse) {
    return {
      type: "INVALID_QUERY",
      message: `Could not match course "${parsed.courseQuery}" to any of your ED courses`,
    };
  }

  // Manual resolution succeeded - use it and let LLM refine other parameters
  // Initialize fetchOptions with defaults (will be overridden by LLM if provided)
  const fetchOptions: FetchThreadsOptions = {
    courseId: resolvedCourse.id,
    limit: 5, // Default limit
    statusFilter: "all", // Default status
  };

  // LLM router refines search parameters (textQuery, limit, status, category)
  // Note: We ignore LLM's courseId/courseCode since manual resolution succeeded
  const llmInput: EdLLMRouterInput = {
    userQuery: parsed.originalQuery,
    courses: availableCourses.map((c) => ({
      id: c.id,
      code: c.code,
      name: c.name,
    })),
  };

  const llm: EdLLMRouterOutput = await callEdRouterLLM(llmInput);

  logger.info("edLLMRouter.output", {
    originalQuery: parsed.originalQuery,
    llm,
    manuallyResolvedCourse: resolvedCourse.code,
  });

  // Apply LLM parameters (textQuery, limit, status, category)
  await applyLLMParameters(edClient, fetchOptions, resolvedCourse, llm, parsed.originalQuery);

  return {
    courseId: resolvedCourse.id,
    resolvedCourse,
    fetchOptions,
  };
}

/**
 * Builds the ED deep link to a given thread.
 * For now we assume the "eu" region (EPFL).
 */
function buildEdThreadUrl(course: EdCourse, thread: EdThread): string {
  const courseId = course.id;
  const threadId = thread.id;

  if (!courseId || !threadId) {
    return "";
  }

  // Example URL format:
  // https://edstem.org/eu/courses/1807/discussion/189994
  return `https://edstem.org/eu/courses/${courseId}/discussion/${threadId}`;
}

/**
 * Normalizes a list of ED threads into frontend format.
 */
export function normalizeEdThreads(
  threads: EdThread[],
  course: EdCourse
): NormalizedEdPost[] {
  return threads.map((t) => {
    const contentMarkdown = String(t.content ?? "");
    const rawTitle = String(t.title ?? "");
    const baseForSnippet = contentMarkdown || rawTitle;
    const snippet =
      baseForSnippet.length > 200
        ? baseForSnippet.slice(0, 200) + "…"
        : baseForSnippet;

    const statusPieces: string[] = [];
    if (t.is_unread) statusPieces.push("unread");
    if (t.is_answered) statusPieces.push("answered");
    if (t.is_resolved) statusPieces.push("resolved");

    const status = statusPieces.join(", ") || "unknown";

    return {
      postId: String(t.id),
      title: rawTitle,
      snippet,
      contentMarkdown,
      author: "", // will be filled when we have author fields in EdThread
      createdAt: String(t.created_at ?? ""),
      status,
      course: course.code || course.name || String(course.id),
      tags: (t.tags as string[]) ?? [],
      url: buildEdThreadUrl(course, t),
    };
  });
}