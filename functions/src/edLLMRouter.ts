// functions/src/edLLMRouter.ts

import OpenAI from "openai";
import * as logger from "firebase-functions/logger";

/* ---------- helpers ---------- */
function withV1(url?: string): string {
  const u = (url ?? "").trim();
  if (!u) return "https://api.publicai.co/v1";
  return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}

/* ---------- types ---------- */
export type EdLLMRouterStatus =
  | "all"
  | "unread"
  | "unanswered"
  | "resolved"
  | "instructors"
  | "approved"
  | "new_replies"
  | "favorites";

export interface EdLLMRouterInput {
  // Raw user query
  userQuery: string;

  // Available ED courses (you can pass userInfo.courses.map(c => c.course))
  courses: Array<{
    id: number;
    code?: string;
    name?: string;
  }>;

  // Optional: later we can add categories per course
  // so the LLM can also choose a category / subcategory.
  // categoriesByCourse?: {
  //   [courseId: number]: { name: string; subcategories?: string[] }[];
  // };
}

export interface EdLLMRouterOutput {
  // Course chosen by the LLM
  courseId?: number;
  courseCode?: string;

  // ED status filter
  status?: EdLLMRouterStatus;

  // Text to send in `query=` parameter of /threads/search
  textQuery?: string;

  // Limit to apply (e.g., 10, 20…)
  limit?: number;

  // Hints for categories (will be used in edSearchDomain.ts)
  categoryHint?: string;
  subcategoryHint?: string;
}

export interface EdCategoryLLMInput {
  userQuery: string;
  course: { id: number; code?: string | null; name?: string | null };
  categoryTree: {
    categories: string[];
    subcategoriesByCategory: Record<string, string[]>;
  };
}

export interface EdCategoryLLMOutput {
  chosenCategory: string | null; // must be one of categories[] or "none" / null
  chosenSubcategory: string | null; // must be one of subcategories[] under chosenCategory, or "none" / null
}

/* ---------- system prompt ---------- */
const ED_ROUTER_SYSTEM_PROMPT = [
  "You are an ED Discussion search router for the EPFL Euler app.",
  "Your job is to translate a natural language query into structured filters for the ED API.",
  "",
  "You receive:",
  "- userQuery: the raw question from the user",
  "- courses: the list of ED courses available to this user (id, code, name)",
  "",
  "You MUST output ONLY a valid JSON object of type EdLLMRouterOutput:",
  "{",
  '  "courseId"?: number,          // id from the provided courses list',
  '  "courseCode"?: string,        // code like "COM-301"',
  '  "status"?: "all" | "unread" | "unanswered" | "resolved" | "instructors" | "approved" | "new_replies" | "favorites",',
  '  "textQuery"?: string,         // short search text for ED /threads/search',
  '  "limit"?: number,             // reasonable number of posts to fetch (e.g. 5, 10, 20)',
  '  "categoryHint"?: string,      // OPTIONAL: high-level category hint (e.g. "assignments", "exercises", "lectures")',
  '  "subcategoryHint"?: string    // OPTIONAL: more precise subcategory hint (e.g. "assignment 4", "HW2")',
  "}",
  "",
  "STATUS FILTER RULES (CRITICAL):",
  "",
  "The 'status' field MUST be EXACTLY one of: \"all\", \"unanswered\", \"unread\", \"resolved\", \"new_replies\", \"favorites\", \"instructors\", \"approved\".",
  "",
  "1. Default behavior:",
  "   - If no specific filter is mentioned in userQuery, return \"status\": \"all\".",
  "",
  "2. Specific filter detection (English + French):",
  "   - \"unanswered posts\", \"questions sans réponse\", \"sans réponse\" -> \"unanswered\"",
  "   - \"unread posts\", \"messages non lus\", \"non lus\" -> \"unread\"",
  "   - \"resolved questions\", \"posts résolus\", \"résolus\", \"answered\", \"répondu\" -> \"resolved\"",
  "   - \"new replies\", \"nouvelles réponses\" -> \"new_replies\"",
  "   - \"favorite posts\", \"posts favoris\", \"favoris\" -> \"favorites\"",
  "   - \"instructor posts\", \"posts des enseignants\", \"posts du staff\", \"posts des profs\" -> \"instructors\"",
  "   - \"approved posts\", \"posts approuvés\", \"approuvés\" -> \"approved\"",
  "",
  "3. Special rule with \"all\":",
  "   - If userQuery contains ONLY \"all\" (no specific filter) -> \"status\": \"all\"",
  "   - If userQuery contains \"all\" AND a specific filter (e.g., \"all answered posts\", \"all unread posts\") ->",
  "     IGNORE \"all\" and use the specific filter mentioned.",
  "   - Examples:",
  "     * \"show all answered posts in COM-300\" -> \"status\": \"resolved\" (NOT \"all\")",
  "     * \"list all unread posts for CS-311\" -> \"status\": \"unread\"",
  "     * \"show all posts about homework 2\" -> \"status\": \"all\" (no specific filter mentioned)",
  "",
  "4. When in doubt:",
  "   - If multiple specific filters are mentioned, choose the one that seems most important for the search,",
  "     but NEVER return \"all\" in this case.",
  "   - If no clear filter is mentioned, return \"all\".",
  "",
  "OTHER RULES:",
  "- Always pick the SINGLE most likely course from the given list.",
  "- If several match, choose the one whose code or name best matches userQuery.",
  "- textQuery must be short and focused, e.g. \"assignment 4 grading\" or \"module 2 exo 4\".",
  "- textQuery MUST NOT be the full sentence; keep only the useful search terms.",
  "- If the user clearly mentions an assignment/homework number, set categoryHint=\"assignments\" and subcategoryHint like \"assignment 4\" or \"homework 2\".",
  "- If the user asks for only a few posts (\"first 5\", \"latest 10\"), set limit accordingly; otherwise 5 is a good default.",
  "",
  "CRITICAL: Your response MUST be ONLY raw JSON, with no explanation, no markdown, no extra text."
].join("\n");

/* ---------- client ---------- */
let routerClient: OpenAI | null = null;
function getRouterChatClient(): OpenAI {
  if (!routerClient) {
    routerClient = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY!,
      baseURL: withV1(process.env.OPENAI_BASE_URL),
      defaultHeaders: { "User-Agent": "euler-mvp/1.0 (ed-llm-router)" },
    });
  }
  return routerClient;
}

/* ---------- main function ---------- */
/**
 * Call the LLM router (Apertus) to decide course / status / textQuery / hints.
 * This does NOT change any behaviour by itself; edSearchDomain.ts will decide when to use it.
 */
export async function callEdRouterLLM(
  input: EdLLMRouterInput,
  client?: OpenAI
): Promise<EdLLMRouterOutput> {
  const activeClient = client ?? getRouterChatClient();

  const userMessage = JSON.stringify(
    {
      userQuery: input.userQuery,
      courses: input.courses,
      // categoriesByCourse: input.categoriesByCourse ?? undefined, // later if needed
    },
    null,
    2
  );

  const resp = await activeClient.chat.completions.create({
    model: process.env.APERTUS_ROUTER_MODEL_ID || process.env.APERTUS_MODEL_ID!,
    messages: [
      { role: "system", content: ED_ROUTER_SYSTEM_PROMPT },
      {
        role: "user",
        content: `Here is the input as JSON:\n\n${userMessage}\n\nReturn ONLY the JSON EdLLMRouterOutput object.`,
      },
    ],
    temperature: 0.0,
    max_tokens: 300,
  });

  const raw = resp.choices?.[0]?.message?.content ?? "{}";
  logger.info("callEdRouterLLM.raw", { raw: raw.slice(0, 500) });

  // Try to extract the JSON object even if there is some noise
  const firstBrace = raw.indexOf("{");
  const lastBrace = raw.lastIndexOf("}");
  const jsonSlice =
    firstBrace >= 0 && lastBrace > firstBrace ? raw.slice(firstBrace, lastBrace + 1) : raw;

  try {
    const parsed = JSON.parse(jsonSlice);

    const out: EdLLMRouterOutput = {
      courseId:
        typeof parsed.courseId === "number" && Number.isFinite(parsed.courseId)
          ? parsed.courseId
          : undefined,
      courseCode: typeof parsed.courseCode === "string" ? parsed.courseCode : undefined,
      status: typeof parsed.status === "string" ? parsed.status : undefined,
      textQuery: typeof parsed.textQuery === "string" ? parsed.textQuery : undefined,
      limit:
        typeof parsed.limit === "number" && Number.isFinite(parsed.limit)
          ? parsed.limit
          : undefined,
      categoryHint: typeof parsed.categoryHint === "string" ? parsed.categoryHint : undefined,
      subcategoryHint: typeof parsed.subcategoryHint === "string" ? parsed.subcategoryHint : undefined,
    };

    logger.info("callEdRouterLLM.parsed", out);
    return out;
  } catch (e: any) {
    logger.error("callEdRouterLLM.parseError", {
      error: String(e),
      rawPreview: raw.slice(0, 500),
    });
    // Neutral fallback: doesn't break anything, will let the rule-based brain decide
    return {};
  }
}

/* ---------- category selection ---------- */
const ED_CATEGORY_SYSTEM_PROMPT = [
  "You are an assistant that selects the most relevant ED Discussion category and optional subcategory for a user query.",
  "You MUST respond with strict JSON only.",
  "",
  "You receive:",
  "- userQuery: the raw question from the user",
  "- course: the ED course (id, code, name) that was selected",
  "- categoryTree: an object with categories (array of main category names) and subcategoriesByCategory (object mapping each category to its children/subcategories)",
  "",
  "Rules:",
  "- Pick the single best main category (chosenCategory) from the categories array.",
  "- If one of its children (subcategories) matches clearly (e.g. specific module number, assignment number, etc.), set chosenSubcategory to that child's exact name.",
  "- If no main category matches well, use \"none\" for both chosenCategory and chosenSubcategory.",
  "- If a main category fits but no child is clearly correct, set chosenSubcategory to \"none\".",
  "- The chosenCategory must match EXACTLY one of the category names in the input (case-sensitive).",
  "- The chosenSubcategory must match EXACTLY one of the subcategory names under the chosen category (case-sensitive), or \"none\".",
  "- You are NOT answering the user; you are only routing to the best category and subcategory.",
  "",
  "Return JSON of the form:",
  "{",
  '  "chosenCategory": "<exact category name>" | "none",',
  '  "chosenSubcategory": "<exact subcategory name>" | "none"',
  "}",
  "",
  "CRITICAL: Your response MUST be ONLY raw JSON, with no explanation, no markdown, no extra text."
].join("\n");

/**
 * Call the LLM to choose a category and optional subcategory from the real ED categories of a course.
 */
export async function callEdCategoryLLM(
  input: EdCategoryLLMInput,
  clientOverride?: OpenAI
): Promise<EdCategoryLLMOutput> {
  const activeClient = clientOverride ?? getRouterChatClient();

  if (!input.categoryTree || !input.categoryTree.categories || input.categoryTree.categories.length === 0) {
    return { chosenCategory: null, chosenSubcategory: null };
  }

  const userMessage = JSON.stringify(
    {
      userQuery: input.userQuery,
      course: {
        id: input.course.id,
        code: input.course.code,
        name: input.course.name,
      },
      categoryTree: input.categoryTree,
    },
    null,
    2
  );

  const resp = await activeClient.chat.completions.create({
    model: process.env.APERTUS_ROUTER_MODEL_ID || process.env.APERTUS_MODEL_ID!,
    messages: [
      { role: "system", content: ED_CATEGORY_SYSTEM_PROMPT },
      {
        role: "user",
        content: `Here is the input as JSON:\n\n${userMessage}\n\nReturn ONLY the JSON object with chosenCategory and chosenSubcategory.`,
      },
    ],
    temperature: 0.0,
    max_tokens: 200,
  });

  const raw = resp.choices?.[0]?.message?.content ?? "{}";
  logger.info("callEdCategoryLLM.raw", { raw: raw.slice(0, 200) });

  // Try to extract the JSON object even if there is some noise
  const firstBrace = raw.indexOf("{");
  const lastBrace = raw.lastIndexOf("}");
  const jsonSlice =
    firstBrace >= 0 && lastBrace > firstBrace ? raw.slice(firstBrace, lastBrace + 1) : raw;

  try {
    const parsed = JSON.parse(jsonSlice);
    let chosenCategory: string | null = null;
    let chosenSubcategory: string | null = null;

    // Parse chosenCategory
    if (typeof parsed.chosenCategory === "string") {
      const value = parsed.chosenCategory.trim();
      const valueLower = value.toLowerCase();

      // Normalize "none" to null
      if (valueLower === "none" || value === "") {
        chosenCategory = null;
      } else {
        // Check if it matches one of the input categories (case-sensitive match)
        if (input.categoryTree.categories.includes(value)) {
          chosenCategory = value;
        } else {
          // Try case-insensitive match as fallback
          const matched = input.categoryTree.categories.find((cat) => cat.toLowerCase() === valueLower);
          if (matched) {
            chosenCategory = matched;
          } else {
            // Not in the list, return null
            chosenCategory = null;
          }
        }
      }
    }

    // Parse chosenSubcategory (only if we have a valid category)
    if (chosenCategory && typeof parsed.chosenSubcategory === "string") {
      const value = parsed.chosenSubcategory.trim();
      const valueLower = value.toLowerCase();

      // Normalize "none" to null
      if (valueLower === "none" || value === "") {
        chosenSubcategory = null;
      } else {
        // Get subcategories for the chosen category
        const subcategories = input.categoryTree.subcategoriesByCategory[chosenCategory] || [];
        
        // Check if it matches one of the subcategories (case-sensitive match)
        if (subcategories.includes(value)) {
          chosenSubcategory = value;
        } else {
          // Try case-insensitive match as fallback
          const matched = subcategories.find((sub) => sub.toLowerCase() === valueLower);
          if (matched) {
            chosenSubcategory = matched;
          } else {
            // Not in the list, return null
            chosenSubcategory = null;
          }
        }
      }
    }

    logger.info("callEdCategoryLLM.parsed", {
      chosenCategory,
      chosenSubcategory,
      availableCategories: input.categoryTree.categories.length,
    });
    return { chosenCategory, chosenSubcategory };
  } catch (e: any) {
    logger.error("callEdCategoryLLM.parseError", {
      error: String(e),
      rawPreview: raw.slice(0, 200),
    });
    // Fallback: no category or subcategory
    return { chosenCategory: null, chosenSubcategory: null };
  }
}

