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

  /** Tags / optional keywords */
  tags?: string[];

  /** Limit on the number of posts to return (first 5, latest 10, etc.) */
  limit?: number;

  textQuery?: string;     // free-text to send as `query`
  categoryHint?: string;  // what the user said: "problem set", "series 5", etc.
  assignmentIndex?: number; // assignment number if detected (1, 2, 3, ...)
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
 * Extracts course query, status, limit, category hints, and text query.
 */
export function parseEdSearchQuery(text: string): EdSearchParsedQuery {
  const lower = text.toLowerCase();

  // Status
  let status: EdPostStatusFilter | undefined = "all";

  // --- new filters first (slightly more specific expressions) ---
  if (
    lower.includes("new replies") ||
    lower.includes("nouvelles reponses") ||
    lower.includes("nouvelles réponses")
  ) {
    status = "new_replies";
  } else if (
    lower.includes("favorite") ||
    lower.includes("favourites") ||
    lower.includes("favoris") ||
    lower.includes("favori")
  ) {
    status = "favorites";
  } else if (
    lower.includes("approved") ||
    lower.includes("approuve") ||
    lower.includes("approuvé") ||
    lower.includes("approuves") ||
    lower.includes("approuvés") ||
    lower.includes("endorsed")
  ) {
    status = "approved";
  } else if (
    lower.includes("instructors") ||
    lower.includes("instructor") ||
    lower.includes("staff") ||
    lower.includes("enseignant") ||
    lower.includes("enseignants") ||
    lower.includes("prof ")
  ) {
    status = "instructors";

    // --- old filters (already present) ---
  } else if (
    lower.includes("unanswered") ||
    lower.includes("sans reponse") ||
    lower.includes("sans réponse")
  ) {
    status = "unanswered";
  } else if (
    lower.includes("unread") ||
    lower.includes("non lus") ||
    lower.includes("non lu")
  ) {
    status = "unread";
  } else if (
    lower.includes("resolved") ||
    lower.includes("resolus") ||
    lower.includes("résolus")
  ) {
    status = "resolved";
  }


  // Limite ("first 5", "latest 10")
  let limit: number | undefined;
  const limitMatch =
    lower.match(/(first|latest|top)\s+(\d{1,3})/) ||
    lower.match(/(\d{1,3})\s+(posts?|questions?)/);
  if (limitMatch) {
    const value = parseInt(limitMatch[2] ?? limitMatch[1], 10);
    if (!Number.isNaN(value)) {
      limit = value;
    }
  }

  // ===== Course =====
  // 1) First, try a code like COM-301, CS-202, etc.
  let courseQuery: string | undefined;
  const courseCodeMatch = text.match(/[A-Z]{2,4}-\d{2,3}/);

  if (courseCodeMatch) {
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

  const tags: string[] = [];

  return {
    originalQuery: text,
    courseQuery,
    status,
    dateFrom,
    dateTo,
    tags,
    limit,
    // textQuery, categoryHint, assignmentIndex are now handled by LLM router
    textQuery: undefined,
    categoryHint: undefined,
    assignmentIndex: undefined,
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

const COURSE_SYNONYMS: Record<string, string[]> = {
  // Software Enterprise (Swent)
  "CS-311": ["swent", "the software enterprise", "software enterprise"],

  // Computer Security
  "COM-301": ["compsec", "computer security", "security"],

  // Stochastic models / Modstock
  "COM-300": ["modstock", "modeles stochastiques", "modèles stochastiques", "stochastics"],

  // Signal Processing
  "COM-302": ["sigproc", "signal processing", "signal proc"],

  // Probability & Statistics
  "MATH-232": ["probastat", "proba stat", "probability and statistics", "probability & statistics"],

  // Analysis 3
  "MATH-203": ["analysis 3", "analys 3", "ana3", "analysis iii", "analyse 3", "analyse iii"],

  // Time & Discrete Systems
  "CS-234": ["tds", "time and discrete systems", "time & discrete systems"],

  // Algebra
  "MATH-310": ["algebre", "algèbre", "algebra"],

  // You can add more aliases here if needed.
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
 * Resolves courseQuery → ED courseId and builds options
 * for EdDiscussionClient.fetchThreads.
 * Uses the LLM router to refine search parameters (textQuery, limit, status, category).
 */
export async function buildEdSearchRequest(
  edClient: EdDiscussionClient,
  parsed: EdSearchParsedQuery,
  availableCourses: EdCourse[]
): Promise<EdSearchRequestParams | EdBrainError> {
  // Manual course resolution (always used)
  if (!parsed.courseQuery) {
    return {
      type: "INVALID_QUERY",
      message: "No course detected in the query",
    };
  }

  const normalizedCourseQuery = parsed.courseQuery.toLowerCase();

  let resolvedCourse =
    availableCourses.find((c) =>
      c.code?.toLowerCase().includes(normalizedCourseQuery)
    ) ||
    availableCourses.find((c) =>
      c.name?.toLowerCase().includes(normalizedCourseQuery)
    );

  // Fallback to synonym-based resolution if direct resolution failed
  if (!resolvedCourse) {
    resolvedCourse = resolveCourseFromSynonyms(
      parsed.originalQuery,
      availableCourses
    );
  }

  if (!resolvedCourse) {
    return {
      type: "INVALID_QUERY",
      message: `Could not match course "${parsed.courseQuery}" to any of your ED courses`,
    };
  }

  // Initialize fetchOptions from manual resolution and parsed fields
  const fetchOptions: FetchThreadsOptions = {
    courseId: resolvedCourse.id,
    limit: parsed.limit ?? 5, // Default to 5 if not specified
    statusFilter: parsed.status ?? "all",
  };

  // LLM router refines search parameters (textQuery, limit, status, category)
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
  });

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
      userQuery: parsed.originalQuery,
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
