// functions/src/edSearchDomain.ts

import {
  EdCourse,
  EdThread,
  FetchThreadsOptions,
  EdDiscussionClient,
} from "./connectors/ed/EdDiscussionClient";

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
 * Représente ce qu'on comprend de la requête NL de l'utilisateur.
 */
export interface EdSearchParsedQuery {
  originalQuery: string;

  /** Exemple "CS-101" avant résolution en courseId ED */
  courseQuery?: string;

  /** Filtre de statut haut niveau (unanswered, unread, resolved, etc.) */
  status?: EdPostStatusFilter;

  /** Intervalle de dates (ISO) pour filtrer côté ED */
  dateFrom?: string;
  dateTo?: string;

  /** Tags / mots-clés éventuels */
  tags?: string[];

  /** Limite sur le nombre de posts à retourner (first 5, latest 10, etc.) */
  limit?: number;

  textQuery?: string;     // free-text to send as `query`
  categoryHint?: string;  // what the user said: "problem set", "series 5", etc.
  assignmentIndex?: number; // assignment number if detected (1, 2, 3, ...)
}

/**
 * Format standard d'un post que le frontend va consommer.
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
 * Erreurs structurées renvoyées par le brain.
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

const CATEGORY_SYNONYMS: Record<string, string[]> = {
  "problem sets": [
    "problem set",
    "problem sets",
    "ps",
    "ps1",
    "ps2",
    "exercise",
    "exercises",
    "exercice",
    "exercices",
    "exo",
    "exos",
    "sheet",
    "series"
  ],
  "lectures": ["lecture", "lectures", "cours", "slides"],
  "project": ["project", "projet"],
  "general": ["general", "général"],
  "assignments": [
    "assignment",
    "assignments",
    "assignement",
    "homework",
    "homeworks",
    "hw",
    "programming assignments",
    "programming assignment",
  ],
};

/**
 * Finds the best canonical key in CATEGORY_SYNONYMS for a given categoryHint
 * and returns the normalized synonyms for that key.
 */
function getCategorySynonyms(categoryHint: string): string[] {
  const nHint = normalize(categoryHint);

  // Find the best canonical key
  let bestCanonicalKey: string | undefined;
  let bestKeyScore = 0;

  for (const [canonicalKey, synonyms] of Object.entries(CATEGORY_SYNONYMS)) {
    const nKey = normalize(canonicalKey);
    let keyScore = 0;

    // Check exact match with canonical key
    if (nKey === nHint) {
      keyScore = 1.0;
    } else if (nKey.includes(nHint) || nHint.includes(nKey)) {
      keyScore = 0.8;
    } else {
      // Check synonyms
      for (const synonym of synonyms) {
        const nSyn = normalize(synonym);
        if (nSyn === nHint) {
          keyScore = 1.0;
          break;
        } else if (nSyn.includes(nHint) || nHint.includes(nSyn)) {
          keyScore = 0.8;
          break;
        }
      }
    }

    if (keyScore > bestKeyScore) {
      bestKeyScore = keyScore;
      bestCanonicalKey = canonicalKey;
    }
  }

  // If no good match found, use categoryHint itself as the only synonym
  const synonymsToUse = bestCanonicalKey
    ? CATEGORY_SYNONYMS[bestCanonicalKey]
    : [categoryHint];

  // Return normalized synonyms
  return synonymsToUse.map((s) => normalize(s));
}

/**
 * Extracts meaningful keywords from user input by removing course query,
 * category hints, and stop words.
 */
function extractTextQuery(
  input: string,
  courseQuery?: string,
  categoryHint?: string
): string | undefined {
  let s = input.toLowerCase();

  // Remove course name / code if present
  if (courseQuery) {
    s = s.replace(new RegExp(courseQuery.toLowerCase(), "g"), "");
  }

  // Remove category hint words and their synonyms
  if (categoryHint) {
    // First remove the categoryHint itself
    s = s.replace(new RegExp(categoryHint.toLowerCase(), "g"), "");
    
    // Get normalized synonyms for this categoryHint
    const normalizedSynonyms = getCategorySynonyms(categoryHint);
    
    // Remove each synonym as whole words
    for (const nSyn of normalizedSynonyms) {
      // Skip exo/exos - keep them in the query string
      if (nSyn === "exo" || nSyn === "exos") {
        continue;
      }
      
      // Match whole words only (word boundaries)
      s = s.replace(new RegExp(`\\b${nSyn.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, "g"), "");
    }
  }

  // Remove generic boilerplate words
  const stopWords = [
    "show",
    "find",
    "display",
    "list",
    "lookup",
    "look up",
    "search",
    "forum",
    "thread",
    "threads",
    "discussion",
    "discussions",
    "post",
    "posts",
    "question",
    "questions",
    "message",
    "messages",
    "about",
    "regarding",
    "please",
    "all",
    "the",
    "in",
    "on",
    "of"
  ];

  for (const w of stopWords) {
    s = s.replace(new RegExp(`\\b${w}\\b`, "g"), "");
  }

  // Collapse whitespace and trim
  s = s.replace(/\s+/g, " ").trim();

  return s.length > 0 ? s : undefined;
}

/**
 * MVP de parsing de la requête NL → EdSearchParsedQuery.
 * Ici on fait du rule-based simple pour commencer.
 */
export function parseEdSearchQuery(text: string): EdSearchParsedQuery {
  const lower = text.toLowerCase();

  // Statut
  let status: EdPostStatusFilter | undefined = "all";

  // --- nouveaux filtres d’abord (expressions un peu plus spécifiques) ---
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

    // --- anciens filtres (déjà présents) ---
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

  // ===== Category hint (MVP) =====
  let categoryHint: string | undefined;

  if (
    lower.includes("problem set") ||
    lower.includes("problemset") ||
    lower.includes("problem sets") ||
    lower.includes("ps ") ||
    lower.match(/\bps\d+/) ||     // ps5, ps6, ps12 etc.
    lower.includes("feuille") ||
    lower.includes("exo")
  ) {
    categoryHint = "problem sets";
  } else if (
    lower.includes("lecture") ||
    lower.includes("lectures") ||
    lower.includes("cours")
  ) {
    categoryHint = "lectures";
  } else if (
    lower.includes("project") ||
    lower.includes("projet")
  ) {
    categoryHint = "project";
  } else if (
    lower.includes("general") ||
    lower.includes("général")
  ) {
    categoryHint = "general";
  } else if (lower.includes("bootcamp")) {
    categoryHint = "bootcamp";
  }

  // ===== Assignment detection =====
  let assignmentIndex: number | undefined;

  // Detect if user is talking about assignments
  if (
    lower.includes("assignment") ||
    lower.includes("assignement") ||
    lower.includes("homework") ||
    lower.includes("hw ")
  ) {
    if (!categoryHint) {
      categoryHint = "assignments";
    }
  }

  // Extract assignment index if present
  const assignmentMatch =
    lower.match(/(\d+)(st|nd|rd|th)?\s+assignment/) ||
    lower.match(/assignment\s+(\d+)/) ||
    lower.match(/assignement\s+(\d+)/) ||
    lower.match(/homeworks?\s+(\d+)/) ||
    lower.match(/homework\s+(\d+)/) ||
    lower.match(/hw\s*(\d+)/) ||
    lower.match(/hw(\d+)/);

  if (assignmentMatch) {
    const idx = parseInt(assignmentMatch[1], 10);
    if (!Number.isNaN(idx)) {
      assignmentIndex = idx;
    }
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

  // ===== Cours =====
  // 1) D'abord un code type COM-301, CS-202, etc.
  let courseQuery: string | undefined;
  const courseCodeMatch = text.match(/[A-Z]{2,4}-\d{2,3}/);

  if (courseCodeMatch) {
    courseQuery = courseCodeMatch[0];
  } else {
    // 2) Sinon, essayer de récupérer le nom après "in" ou "from"
    // ex: "show instructors posts in Algebra"
    const nameMatch = text.match(/\b(?:in|from)\s+([A-Za-z0-9\-()\/ ]+)$/i);
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

  // Extract clean text query using helper function
  const textQuery = extractTextQuery(text, courseQuery, categoryHint);

  return {
    originalQuery: text,
    courseQuery,
    status,
    dateFrom,
    dateTo,
    tags,
    limit,
    textQuery,
    categoryHint,
    assignmentIndex,
  };
}



/**
 * Paramètres nécessaires pour appeler EdDiscussionClient.fetchThreads.
 */
export interface EdSearchRequestParams {
  courseId: number;
  fetchOptions: FetchThreadsOptions;
  resolvedCourse: EdCourse;
}

/**
 * Resolves a category hint (natural language) to an actual ED category name
 * by fetching available categories and matching them using synonyms.
 */
async function resolveCategoryHint(
  edClient: EdDiscussionClient,
  courseId: number,
  categoryHint?: string,
  assignmentIndex?: number
): Promise<string | undefined> {
  if (!categoryHint) return undefined;

  // Get normalized synonyms for this categoryHint
  const normalizedSynonyms = getCategorySynonyms(categoryHint);

  // Determine canonical key
  const nHint = normalize(categoryHint);
  let canonicalKey: string | undefined;
  let bestKeyScore = 0;

  for (const [key, synonyms] of Object.entries(CATEGORY_SYNONYMS)) {
    const nKey = normalize(key);
    let keyScore = 0;

    if (nKey === nHint) {
      keyScore = 1.0;
    } else if (nKey.includes(nHint) || nHint.includes(nKey)) {
      keyScore = 0.8;
    } else {
      for (const synonym of synonyms) {
        const nSyn = normalize(synonym);
        if (nSyn === nHint) {
          keyScore = 1.0;
          break;
        } else if (nSyn.includes(nHint) || nHint.includes(nSyn)) {
          keyScore = 0.8;
          break;
        }
      }
    }

    if (keyScore > bestKeyScore) {
      bestKeyScore = keyScore;
      canonicalKey = key;
    }
  }

  // Fetch course categories
  const categories = await edClient.fetchCategoriesFromThreads(courseId);

  // Find best matching category
  let bestName: string | undefined;
  let bestScore = 0;

  // Special handling for assignments
  if (canonicalKey === "assignments") {
    for (const name of categories) {
      const nCat = normalize(name);
      let score = 0;

      if (assignmentIndex != null) {
        // Score 1.0 if category contains both "assignment" and the assignment index
        if (
          (nCat.includes("assignment") || nCat.includes("assignement")) &&
          nCat.includes(String(assignmentIndex))
        ) {
          score = 1.0;
        } else if (
          nCat.includes("programming assignments") ||
          nCat.includes("assignments")
        ) {
          score = 0.8;
        }
      } else {
        // Score 0.8 for generic assignment categories
        if (
          nCat.includes("assignment") ||
          nCat.includes("assignement") ||
          nCat.includes("programming assignments") ||
          nCat.includes("homework") ||
          nCat.includes("homeworks") ||
          nCat.startsWith("hw")
        ) {
          score = 0.8;
        }
      }

      if (score > bestScore) {
        bestScore = score;
        bestName = name;
      }
    }
  } else {
    // Generic scoring for other categories
    for (const name of categories) {
      const nCat = normalize(name);
      let score = 0;

      // Check against each normalized synonym
      for (const nSyn of normalizedSynonyms) {
        if (nCat === nSyn) {
          score = Math.max(score, 1.0);
        } else if (nCat.includes(nSyn) || nSyn.includes(nCat)) {
          score = Math.max(score, 0.8);
        }
      }

      if (score > bestScore) {
        bestScore = score;
        bestName = name;
      }
    }
  }

  return bestScore >= 0.7 ? bestName : undefined;
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
  "PROBESTAT": ["probastat", "proba stat", "probability and statistics", "probability & statistics"],

  // Analysis 3
  "MATH-203": ["analysis 3", "analys 3", "ana3", "analysis iii", "analyse 3", "analyse iii"],

  // Time & Discrete Systems (adjust the code to your real course)
  "TDS-CODE": ["tds", "time and discrete systems", "time & discrete systems"],

  // Algebra (adjust the code to your real course)
  "ALGEBRE-CODE": ["algebre", "algèbre", "algebra"],

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
 * Escapes special regex characters in a string.
 */
function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Resolves a subcategory hint based on the resolved category and parsed query.
 */
async function resolveSubcategoryHint(
  edClient: EdDiscussionClient,
  courseId: number,
  resolvedCategory: string | undefined,
  parsed: EdSearchParsedQuery
): Promise<string | undefined> {
  if (!resolvedCategory) return undefined;

  const tree = await edClient.fetchCategoryTreeFromThreads(courseId);
  const subs = tree.subcategoriesByCategory?.[resolvedCategory];
  if (!subs || subs.length === 0) return undefined;

  const normalizedQuery = normalize(parsed.originalQuery);
  const idx = parsed.assignmentIndex;

  let bestName: string | undefined;
  let bestScore = 0;

  for (const name of subs) {
    const nName = normalize(name);
    let score = 0;

    // Special boost for assignments: "assignment N" or "HW N"
    if (
      parsed.categoryHint === "assignments" &&
      idx != null
    ) {
      if (
        nName.includes("assignment") &&
        nName.includes(String(idx))
      ) {
        score = 2.0; // strong match e.g. "Assignment 4 - Sniffing Traffic"
      } else if (
        (nName.includes("homework") || nName.includes("homeworks") || nName.startsWith("hw")) &&
        nName.includes(String(idx))
      ) {
        score = 2.0; // strong match e.g. "HW1", "HW2", "homework 1"
      } else if (
        nName.includes("assignment") ||
        nName.includes("homework") ||
        nName.includes("homeworks") ||
        nName.startsWith("hw")
      ) {
        score = 1.0; // generic assignment/homework subcategory
      }
    } else if (
      parsed.categoryHint === "assignments" &&
      nName.includes("assignment")
    ) {
      score = 1.0; // generic assignment subcategory
    }

    // Generic token-based boost: words from the original query appearing in the subcategory name
    // (use simple splitting, ignore very short tokens).
    const tokens = normalizedQuery.split(/\s+/).filter((t) => t.length > 2);
    for (const t of tokens) {
      if (nName.includes(t)) {
        score += 0.1;
      }
    }

    if (score > bestScore) {
      bestScore = score;
      bestName = name;
    }
  }

  // Only keep a subcategory if we have a reasonably good score
  if (bestScore >= 1.0 && bestName) {
    return bestName;
  }
  return undefined;
}

/**
 * Résout courseQuery → courseId ED et construit les options
 * pour EdDiscussionClient.fetchThreads.
 * Now async to support category resolution.
 */
export async function buildEdSearchRequest(
  edClient: EdDiscussionClient,
  parsed: EdSearchParsedQuery,
  availableCourses: EdCourse[]
): Promise<EdSearchRequestParams | EdBrainError> {
  let resolvedCourse: EdCourse | undefined;

  // First, try to resolve from parsed.courseQuery (code or name)
  if (parsed.courseQuery) {
    const normalizedCourseQuery = parsed.courseQuery.toLowerCase();

    resolvedCourse =
      availableCourses.find((c) =>
        c.code?.toLowerCase().includes(normalizedCourseQuery)
      ) ||
      availableCourses.find((c) =>
        c.name?.toLowerCase().includes(normalizedCourseQuery)
      );
  }

  // Fallback to synonym-based resolution if direct resolution failed
  if (!resolvedCourse) {
    resolvedCourse = resolveCourseFromSynonyms(
      parsed.originalQuery,
      availableCourses
    );
  }

  // If still no course found, return error
  if (!resolvedCourse) {
    return {
      type: "INVALID_QUERY",
      message: parsed.courseQuery
        ? `Could not match course "${parsed.courseQuery}" to any of your ED courses`
        : "No course detected in the query",
    };
  }

  const fetchOptions: FetchThreadsOptions = {
    courseId: resolvedCourse.id,
    limit: parsed.limit,
    statusFilter: parsed.status,
  };

  // Clean up textQuery by removing course aliases
  let cleanedQuery = parsed.textQuery;

  if (resolvedCourse.code && cleanedQuery) {
    const aliases = COURSE_SYNONYMS[resolvedCourse.code] ?? [];
    for (const alias of aliases) {
      if (!alias) continue;
      const pattern = new RegExp(`\\b${escapeRegex(alias)}\\b`, "gi");
      cleanedQuery = cleanedQuery.replace(pattern, "");
    }
    // Normalize spaces
    cleanedQuery = cleanedQuery.replace(/\s+/g, " ").trim();
  }

  // Ensure assignment/homework phrase is present when we have an index
  if (
    parsed.categoryHint === "assignments" &&
    parsed.assignmentIndex != null
  ) {
    const idx = parsed.assignmentIndex;
    const originalLower = parsed.originalQuery.toLowerCase();
    const cleanedLower = cleanedQuery?.toLowerCase() || "";

    // Decide label: homework / hw / assignment depending on what the user wrote
    let label = "assignment";
    if (originalLower.includes("homework") || originalLower.includes("homeworks")) {
      label = "homework";
    } else if (originalLower.includes("hw")) {
      label = "hw";
    }

    const phrase = `${label} ${idx}`;

    // If cleanedQuery doesn't already clearly contain this phrase, prefix it
    if (!cleanedLower.includes(label) || !cleanedLower.includes(String(idx))) {
      cleanedQuery = cleanedQuery
        ? `${phrase} ${cleanedQuery}`
        : phrase;
    }
  }

  // Assign cleaned query if it has content, otherwise fallback to original
  if (cleanedQuery && cleanedQuery.length > 0) {
    fetchOptions.query = cleanedQuery;
  } else if (parsed.textQuery) {
    // Fallback to original if for some reason everything was removed
    fetchOptions.query = parsed.textQuery;
  }

  // Resolve category hint if present
  let resolvedCategory: string | undefined = undefined;
  if (parsed.categoryHint) {
    resolvedCategory = await resolveCategoryHint(
      edClient,
      resolvedCourse.id,
      parsed.categoryHint,
      parsed.assignmentIndex
    );
    if (resolvedCategory) {
      fetchOptions.category = resolvedCategory;

      const resolvedSubcategory = await resolveSubcategoryHint(
        edClient,
        resolvedCourse.id,
        resolvedCategory,
        parsed
      );

      if (resolvedSubcategory) {
        fetchOptions.subcategory = resolvedSubcategory;
      }
    }
  }

  return {
    courseId: resolvedCourse.id,
    fetchOptions,
    resolvedCourse,
  };
}


/**
 * Construit le deep link ED vers un thread donné.
 * Pour l'instant on suppose la région "eu" (EPFL).
 */
function buildEdThreadUrl(course: EdCourse, thread: EdThread): string {
  const courseId = course.id;
  const threadId = thread.id;

  if (!courseId || !threadId) {
    return "";
  }

  // Exemple de format d'URL :
  // https://edstem.org/eu/courses/1807/discussion/189994
  return `https://edstem.org/eu/courses/${courseId}/discussion/${threadId}`;
}

/**
 * Normalise une liste de threads ED en format frontend.
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
      author: "", // on remplira quand on aura les champs auteur dans EdThread
      createdAt: String(t.created_at ?? ""),
      status,
      course: course.code || course.name || String(course.id),
      tags: (t.tags as string[]) ?? [],
      url: buildEdThreadUrl(course, t),
    };
  });
}
