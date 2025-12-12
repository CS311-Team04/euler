// functions/src/index.ts

import path from "node:path";
import dotenv from "dotenv";
dotenv.config({ path: path.join(__dirname, "..", ".env") });

import * as logger from "firebase-functions/logger";
import OpenAI from "openai";
import { randomUUID } from "node:crypto";
import * as functions from "firebase-functions/v1";
import admin from "firebase-admin";
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
import {
  parseEdSearchQuery,
  buildEdSearchRequest,
  normalizeEdThreads,
  EdBrainError,
  NormalizedEdPost,
} from "./edSearchDomain";
import { 
  scrapeWeeklyMenu, 
  formatMenuForContext,
  findCheapestMeal,
  findVeggieMeals,
  findMealsByRestaurant,
  type DailyMenu,
  type Meal,
  TARGET_RESTAURANTS,
} from "./food/epflFoodScraper";
import { detectMoodleFileFetchIntentCore, extractFileInfo } from "./moodleIntent";
import { MoodleClient } from "./connectors/moodle/MoodleClient";


const europeFunctions = functions.region("europe-west6");


/* ---------- helpers ---------- */
function withV1(url?: string): string {
  const u = (url ?? "").trim();
  if (!u) return "https://api.publicai.co/v1";
  return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}
function isUuid(s: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s);
}
function isUintString(s: string): boolean {
  return /^\d+$/.test(s);
}
function normalizePointId(id: string | number): string | number {
  if (typeof id === "number") return id;
  const s = String(id);
  if (isUintString(s)) return Number(s);
  if (isUuid(s)) return s;
  return randomUUID();
}

/* ---------- clients ---------- */
// Chat = PublicAI (Apertus)
let chatClient: OpenAI | null = null;
function getChatClient(): OpenAI {
  if (!chatClient) {
    chatClient = new OpenAI({
      apiKey: process.env.OPENAI_API_KEY!,
      baseURL: withV1(process.env.OPENAI_BASE_URL),
      defaultHeaders: { "User-Agent": "euler-mvp/1.0 (genkit-functions)" },
    });
  }
  return chatClient;
}

// For testing: allow overriding the client
export function setChatClient(client: OpenAI | null) {
  chatClient = client;
}

// Firebase Admin (Firestore)
if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

const ED_DEFAULT_COURSE_ID = Number(
  process.env.ED_DEFAULT_COURSE_ID || "1153"
);

// ED Discussion connector (stored in Firestore under connectors_ed/{userId})
const edConnectorRepository = new EdConnectorRepository(db);
const edConnectorService = new EdConnectorService(
  edConnectorRepository,
  encryptSecret,
  decryptSecret,
  "https://eu.edstem.org/api",
  ED_DEFAULT_COURSE_ID
);

/* =========================================================
 *                ED BRAIN SEARCH (core)
 * ======================================================= */

type EdBrainSearchInput = {
  query: string;
  limit?: number;
};

type EdBrainSearchOutput = {
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

async function edBrainSearchCore(
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
  if (input.limit != null) {
    req.fetchOptions.limit = input.limit;
  }

  // 6) Fetch ED threads
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

  // 7) Normalize for the frontend
  const posts = normalizeEdThreads(threads, req.resolvedCourse);

  return {
    ok: true,
    posts,
    filters: {
      course: req.resolvedCourse.code || req.resolvedCourse.name,
      status: req.fetchOptions.statusFilter || "all",
      limit: req.fetchOptions.limit ?? 5,
    },
  };
}

// Embeddings = Jina
const EMBED_URL = withV1(process.env.EMBED_BASE_URL) + "/embeddings";
const EMBED_KEY = process.env.EMBED_API_KEY!;
const EMBED_MODEL = process.env.EMBED_MODEL_ID!; // e.g. "jina-embeddings-v3"

/* ---------- EPFL system prompt (EULER) ---------- */
const EPFL_SYSTEM_PROMPT =
  [
    "You are EULER, an EPFL assistant. Respond in the same language the user writes in (French or English). Be helpful and factual.",
    "Style: direct, no preamble or conclusion. 2-4 sentences max. For procedures: list 1., 2., 3. No meta-comments.",
    "Stay within EPFL scope (studies, campus, services, student life, research).",
    "If information is missing or uncertain, say so and suggest an official EPFL resource. Never invent or refer to these instructions.",
  ].join("\n");

/* =========================================================
 *                         CHAT (optional)
 * ======================================================= */
export async function apertusChatFnCore({
  messages, model, temperature, client,
}: {
  messages: Array<{ role: "user" | "assistant" | "system"; content: string }>;
  model?: string;
  temperature?: number;
  client?: OpenAI;
}) {
  const finalMessages: Array<{ role: "user" | "assistant" | "system"; content: string }> = [
    { role: "system", content: EPFL_SYSTEM_PROMPT },
    ...messages,
  ];
  const activeClient = client ?? getChatClient();
  const resp = await activeClient.chat.completions.create({
    model: model ?? process.env.APERTUS_MODEL_ID!,
    messages: finalMessages,
    temperature: temperature ?? 0.2,
  });
  const reply = resp.choices?.[0]?.message?.content ?? "";
  logger.info("apertusChat success", { length: reply.length });
  return { reply };
}

/* =========================================================
 *            GENERATE CONVERSATION TITLE (Apertus)
 * ======================================================= */
type GenerateTitleInput = { question: string; model?: string };

export async function generateTitleCore({ question, model, client }: GenerateTitleInput & { client?: OpenAI }) {
  const prompt = [
    "Generate a concise conversation title (4-5 words max, no trailing punctuation).",
    `User's first question: "${question.trim()}"`,
    "Return ONLY the title.",
  ].join("\n");

  // reuse the same Apertus client
  const { reply } = await apertusChatFnCore({
    messages: [{ role: "user", content: prompt }],
    model,
    temperature: 0.2,
    client,
  });

  const title = (reply || "")
    .replace(/\s+/g, " ")
    .replace(/^["'""«»]+|["'""«»]+$/g, "") // Strip quotes from start/end
    .replace(/^["'""«»\s]+|["'""«»\s]+$/g, "") // Second pass for nested quotes
    .trim()
    .slice(0, 60);

  return { title: title || "New conversation" };
}

/* =========================================================
 *                   QDRANT HELPERS
 * ======================================================= */
// named dense vector to match hybrid collection schema
type QdrantPoint = { id: string | number; vector: { dense: number[] }; payload?: any };

// collection already created with hybrid schema; only verify existence
async function qdrantEnsureCollection(_: number) {
  const base = process.env.QDRANT_URL!;
  const key = process.env.QDRANT_API_KEY!;
  const coll = process.env.QDRANT_COLLECTION!;
  const res = await fetch(`${base}/collections/${coll}`, { headers: { "api-key": key } });
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`Qdrant collection "${coll}" not found: ${res.status} ${t}`);
  }
}

async function qdrantUpsert(points: QdrantPoint[]) {
  const res = await fetch(
    `${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points?wait=true`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY! },
      body: JSON.stringify({ points }),
    }
  );
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`Qdrant upsert failed: ${res.status} ${t}`);
  }
}

// retrieval knobs
const MAX_CANDIDATES = 24;
const MAX_DOCS       = 3;
const MAX_PER_DOC    = 2;
const CONTEXT_BUDGET = 1600;
const SNIPPET_LIMIT  = 600;

// gates
const SCORE_GATE = 0.35; // top score must be >= to use context
const SMALL_TALK = /^(hi|hello|salut|yo|coucou|hey|who are you|ça va|tu vas bien|bonjour|bonsoir)\b/i;

// --- RRF fusion ---
function rrfFuse<T extends { id: any }>(
  dense: Array<T & { _rank: number }>,
  sparse: Array<T & { _rank: number }>,
  k = 60
) {
  const map = new Map<string, { item: T; score: number }>();
  const add = (arr: Array<T & { _rank: number }>) => {
    arr.forEach((it) => {
      const key = String(it.id);
      const inc = 1.0 / (k + it._rank + 1);
      const cur = map.get(key);
      if (cur) cur.score += inc; else map.set(key, { item: it, score: inc });
    });
  };
  add(dense); add(sparse);
  return [...map.values()]
    .sort((a, b) => b.score - a.score)
    .map(v => ({ ...v.item, score: v.score }));
}

// dense-only search for NAMED vector schema
async function qdrantSearchDenseNamed(
  denseVec: number[],
  topK = 24,
  filter?: any
) {
  const body: any = {
    vector: { name: "dense", vector: denseVec, params: { hnsw_ef: 48 } },
    limit: topK,
    with_payload: true,
    with_vector: false,
    score_threshold: 0.25,
  };
  if (filter) body.filter = filter;

  const r = await fetch(
    `${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY! },
      body: JSON.stringify(body),
    }
  );
  if (!r.ok) throw new Error(`dense search failed: ${r.status} ${await r.text()}`);
  const j = await r.json();
  return (j.result ?? []).map((h: any, i: number) => ({ ...h, _rank: i })) as Array<any>;
}

// sparse-only search using query text
async function qdrantSearchSparseText(
  queryText: string,
  topK = 24,
  filter?: any
) {
  const body: any = {
    sparse: { name: "sparse", text: queryText },
    limit: topK,
    with_payload: true,
    with_vector: false,
    score_threshold: 0.25,
  };
  if (filter) body.filter = filter;

  const r = await fetch(
    `${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY! },
      body: JSON.stringify(body),
    }
  );
  if (!r.ok) throw new Error(`sparse search failed: ${r.status} ${await r.text()}`);
  const j = await r.json();
  return (j.result ?? []).map((h: any, i: number) => ({ ...h, _rank: i })) as Array<any>;
}

// hybrid with safe fallback
async function qdrantSearchHybrid(
  denseVec: number[],
  queryText: string,
  topK = 24,
  filter?: any
) {
  const dense = await qdrantSearchDenseNamed(denseVec, topK, filter);
  let sparse: Array<any> = [];
  try {
    sparse = await qdrantSearchSparseText(queryText, topK, filter);
  } catch {
    return dense; // fallback
  }
  const fused = rrfFuse(dense, sparse, 60);
  return fused.slice(0, topK);
}

/* =========================================================
 *            EMBEDDINGS UTIL
 * ======================================================= */
const MAX_CHARS = 8000;

function sanitizeTexts(arr: string[]): string[] {
  return arr
    .map((s) => (typeof s === "string" ? s : String(s ?? "")))
    .map((s) => s.trim())
    .map((s) => (s.length > MAX_CHARS ? s.slice(0, MAX_CHARS) : s))
    .filter((s) => s.length > 0);
}

async function embedBatch(texts: string[]): Promise<number[][]> {
  const clean = sanitizeTexts(texts);
  if (clean.length === 0) throw new Error("No valid texts to embed.");

  const body = JSON.stringify({ model: EMBED_MODEL, input: clean });
  const res = await fetch(EMBED_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${EMBED_KEY}`,
      "Content-Type": "application/json",
      Accept: "application/json",
      "User-Agent": "euler-mvp/1.0 (contact: you@example.com)",
    },
    body,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    logger.error("Embed error", {
      status: res.status, url: EMBED_URL, model: EMBED_MODEL,
      sampleInputLen: clean[0]?.length, batchSize: clean.length,
      bodyPreview: body.slice(0, 2000), responseTextPreview: text.slice(0, 2000),
    });
    throw new Error(`${res.status} ${text || "(no body)"}`);
  }

  const json = await res.json();
  return (json.data ?? []).map((d: any) => d.embedding as number[]);
}

async function sleep(ms: number) { return new Promise((res) => setTimeout(res, ms)); }

async function embedInBatches(allTexts: string[], batchSize = 16, pauseMs = 150): Promise<number[][]> {
  const out: number[][] = [];
  for (let i = 0; i < allTexts.length; i += batchSize) {
    const slice = allTexts.slice(i, i + batchSize);
    if (i > 0 && pauseMs > 0) await sleep(pauseMs);
    const vecs = await embedBatch(slice);
    out.push(...vecs);
  }
  return out;
}

/* =========================================================
 *            INDEX CHUNKS
 * ======================================================= */
type IndexChunk = { id: string; text: string; title?: string; url?: string; payload?: any };
type IndexChunksInput = { chunks: IndexChunk[] };

export async function indexChunksCore({ chunks }: IndexChunksInput) {
  if (!chunks?.length) return { count: 0, dim: 0 };
  const texts = chunks.map((c) => {
    const parts: string[] = [];
    if (c.title) parts.push(`Title: ${c.title}`);
    const section = c.payload?.section ?? c.payload?.SECTION ?? c.payload?.Section;
    if (section) parts.push(`Section: ${section}`);
    parts.push(c.text);
    return parts.join("\n\n");
  });
  const vectors = await embedInBatches(texts, 16, 150);
  const dim = vectors[0].length;
  await qdrantEnsureCollection(dim);
  const points = vectors.map((v, i) => ({
    id: normalizePointId(chunks[i].id),
    vector: { dense: v },
    payload: {
      original_id: chunks[i].id,
      text: chunks[i].text,
      title: chunks[i].title,
      url: chunks[i].url,
      ...(chunks[i].payload ?? {}),
    },
  }));
  await qdrantUpsert(points);
  logger.info("indexChunks", { count: chunks.length, dim });
  return { count: chunks.length, dim };
}

/* =========================================================
 *                ANSWER WITH RAG
 * ======================================================= */
type AnswerWithRagInput = {
  question: string;
  topK?: number;
  model?: string;
  summary?: string;
  recentTranscript?: string;
  profileContext?: string;
  uid?: string; // Optional: for schedule context
};

// Schedule-related question patterns (French + English)
const SCHEDULE_PATTERNS = /\b(horaire|schedule|timetable|cours|class|lecture|planning|agenda|calendrier|calendar|demain|tomorrow|aujourd'hui|today|cette semaine|this week|prochaine|next|quand|when|heure|time|salle|room|où|where|leçon|lesson|what['']?s\s+on|my\s+classes|my\s+lectures|my\s+schedule)\b/i;

// Food-related question patterns - split into strong (always trigger) and weak (need context)
// Strong indicators: EPFL restaurant names, explicit eating phrases (French + English)
const FOOD_STRONG_PATTERNS = /\b(alpine|arcadie|esplanade|espla|native|ornithorynque|orni|piano|qu'est-ce qu'on mange|il y a quoi à manger|ya quoi|y'a quoi|où manger|quoi manger|on mange quoi|what['']?s\s+for\s+(lunch|dinner|breakfast)|what\s+can\s+i\s+eat|where\s+to\s+eat|food\s+today|menus?\s+today|what['']?s\s+on\s+the\s+menu|cafeteria|canteen)\b/i;

// Diet queries phrased as "y'a quoi de veggie ..." (captures apostrophes) + English equivalents
const FOOD_DIET_QUERY = /\b(y['']?\s*a\s+quoi|ya\s+quoi|qu'est-ce qu'on|on\s+mange\s+quoi|quoi\s+(de|d')|what['']?s|is\s+there|are\s+there|any)[^.]{0,50}\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b/i;

// Weak indicators: only trigger if combined with food context words (French + English)
const FOOD_WEAK_PATTERNS = /\b(manger|menu|plat|repas|resto|restaurant|nourriture|déjeuner|dîner|cantine|cafétéria|eat|eating|lunch|dinner|breakfast|meal|meals|food|cafeteria|canteen|dish|dishes)\b/i;

// Diet-related patterns: trigger only with eating/restaurant context (French + English)
const FOOD_DIET_PATTERNS = /\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b.*\b(manger|plat|resto|restaurant|menu|repas|cantine|midi|soir|déjeuner|dîner|eat|lunch|dinner|meal|food|cafeteria|canteen)\b|\b(manger|plat|resto|restaurant|menu|repas|cantine|midi|soir|déjeuner|dîner|eat|lunch|dinner|meal|food|cafeteria|canteen)\b.*\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b/i;

// Price patterns: only for food when combined with eating context (French + English)
const FOOD_PRICE_PATTERNS = /\b(moins cher|cheapest|pas cher|prix|cheap|under|less\s+than|affordable|budget)\b.*\b(manger|plat|resto|restaurant|menu|repas|midi|soir|déjeuner|dîner|cantine|eat|lunch|dinner|meal|food|cafeteria)\b|\b(manger|plat|resto|restaurant|menu|repas|midi|soir|déjeuner|dîner|cantine|eat|lunch|dinner|meal|food|cafeteria)\b.*\b(moins cher|cheapest|pas cher|prix|cheap|under|less\s+than|affordable|budget)\b/i;

/** Check if question is food-related using refined patterns */
function isFoodRelatedQuestion(q: string): boolean {
  // Strong patterns always trigger
  if (FOOD_STRONG_PATTERNS.test(q)) return true;
  // "y'a quoi de veggie ..." style queries
  if (FOOD_DIET_QUERY.test(q)) return true;
  // Diet patterns with eating context
  if (FOOD_DIET_PATTERNS.test(q)) return true;
  // Price patterns with eating context  
  if (FOOD_PRICE_PATTERNS.test(q)) return true;
  // Weak patterns only if the question is clearly about eating (not just contains "menu" or "restaurant")
  // Must have at least 2 food-related words or a clear eating phrase (French + English)
  if (FOOD_WEAK_PATTERNS.test(q)) {
    const foodWords = q.toLowerCase().match(/\b(manger|menu|plat|repas|resto|restaurant|nourriture|déjeuner|dîner|cantine|cafétéria|midi|soir|aujourd'hui|demain|eat|eating|lunch|dinner|breakfast|meal|food|cafeteria|canteen|today|tomorrow)\b/gi);
    return (foodWords?.length ?? 0) >= 2;
  }
  return false;
}

export async function answerWithRagCore({
  question, topK, model, summary, recentTranscript, profileContext, uid, client,
}: AnswerWithRagInput & { client?: OpenAI }) {
  const q = question.trim();
  const tStart = Date.now();
  let timeScheduleMs = 0;
  let timeRetrievalMs = 0;
  let timePromptMs = 0;
  let timeLLMMs = 0;

  const shortQuestion = q.length <= 80;
  const tinyQuestion = q.length <= 40;
  const envTopK = process.env.APERTUS_TOPK ? Number(process.env.APERTUS_TOPK) : undefined;
  const effectiveTopK = topK ?? envTopK ?? (tinyQuestion ? 8 : shortQuestion ? 12 : 18);
  const effectiveMaxDocs = shortQuestion ? Math.max(1, MAX_DOCS - 1) : MAX_DOCS;
  const contextBudget = shortQuestion ? Math.min(CONTEXT_BUDGET, 1100) : CONTEXT_BUDGET;
  const snippetLimit = shortQuestion ? Math.min(SNIPPET_LIMIT, 450) : SNIPPET_LIMIT;
  const summaryBudget = shortQuestion ? 0 : 1200;
  const transcriptBudget = shortQuestion ? 0 : 800;
  const scheduleBudget = 900;
  const profileBudget = 900;
  const finalModel = model ?? process.env.APERTUS_MODEL_ID!;
  const finalTemperature = process.env.APERTUS_TEMPERATURE ? Number(process.env.APERTUS_TEMPERATURE) : 0.0;
  const finalMaxTokens = process.env.APERTUS_MAX_TOKENS ? Number(process.env.APERTUS_MAX_TOKENS) : 280;
  // optional rolling summary (kept concise)
  const trimmedSummary =
    summaryBudget > 0 ? (summary ?? "").toString().trim().slice(0, summaryBudget) : "";
  
  // Check if this is a food-related question FIRST (before schedule/RAG)
  const isFoodQuestion = isFoodRelatedQuestion(q);
  
  if (isFoodQuestion) {
    try {
      logger.info("answerWithRagCore.foodQuestion", { questionLen: q.length });
      const foodResult = await answerFoodQuestionCore(q);
      logger.info("answerWithRagCore.timing", {
        path: "food",
        totalMs: Date.now() - tStart,
        scheduleMs: timeScheduleMs,
        retrievalMs: timeRetrievalMs,
        promptMs: timePromptMs,
        llmMs: timeLLMMs,
      });
      return {
        reply: foodResult.reply,
        primary_url: "https://www.epfl.ch/campus/restaurants-shops-hotels/fr/offre-du-jour-de-tous-les-points-de-restauration/",
        best_score: 1.0,
        sources: [],
        source_type: "food" as const,
      };
    } catch (e) {
      logger.warn("answerWithRagCore.foodQuestionFailed", { error: String(e) });
      // Fall through to regular RAG if food query fails
    }
  }
  

  // Check if this is a schedule-related question and fetch schedule context
  let scheduleContext = "";
  const isScheduleQuestion = SCHEDULE_PATTERNS.test(q);

  if (isScheduleQuestion && uid) {
    try {
      const tScheduleStart = Date.now();
      scheduleContext = await getScheduleContextCore(uid);
      timeScheduleMs = Date.now() - tScheduleStart;
      if (scheduleContext) {
        const originalScheduleLen = scheduleContext.length;
        if (scheduleContext.length > scheduleBudget) {
          scheduleContext = scheduleContext.slice(0, scheduleBudget);
        }
        logger.info("answerWithRagCore.scheduleContext", {
          uid,
          hasSchedule: true,
          contextLen: scheduleContext.length,
          trimmed: originalScheduleLen > scheduleContext.length,
        });
      }
    } catch (e) {
      logger.warn("answerWithRagCore.scheduleContextFailed", { error: String(e) });
    }
  }

  // Gate 1: skip retrieval on small talk
  const isSmallTalk = SMALL_TALK.test(q) && q.length <= 30;
  const skipRetrieval = (isScheduleQuestion && Boolean(scheduleContext));

  let routedQuestion = q;
  const routerModel = process.env.APERTUS_ROUTER_MODEL_ID;
  if (!isSmallTalk && !skipRetrieval && routerModel) {
    try {
      const routerClient = client ?? getChatClient();
      const routerResp = await routerClient.chat.completions.create({
        model: routerModel,
        messages: [
          { role: "system", content: "Rewrite the following question into a short, explicit version for EPFL document search. Do not answer; return only the reformulated question. No pleasantries or meta." },
          { role: "user", content: q },
        ],
        temperature: 0,
        max_tokens: 64,
      });
      const routed = routerResp.choices?.[0]?.message?.content?.trim();
      if (routed) {
        routedQuestion = routed;
      }
    } catch (e) {
      logger.warn("answerWithRagCore.routerFailed", { error: String(e) });
    }
  }

  let chosen: Array<{ title?: string; url?: string; text: string; score: number }> = [];
  let bestScore = 0;

  if (!isSmallTalk && !skipRetrieval) {
    const tRetrievalStart = Date.now();
    const [queryVec] = await embedInBatches([routedQuestion], 1, 0);
    const searchK = Math.min(Math.max(effectiveTopK, 4), MAX_CANDIDATES);
    const raw = await qdrantSearchHybrid(queryVec, routedQuestion, searchK);

    bestScore = raw[0]?.score ?? 0;

    // Gate 2: drop weak matches
    if (bestScore >= SCORE_GATE) {
      // group by source (url first, fallback title)
      const groups = new Map<string, Array<typeof raw[number]>>();
      for (const h of raw) {
        const key = (h.payload?.url || h.payload?.title || String(h.id)).toString();
        if (!groups.has(key)) groups.set(key, []);
        const arr = groups.get(key)!;
        if (arr.length < MAX_PER_DOC) arr.push(h);
      }

      // assemble diversified context under a budget
      const orderedGroups = Array.from(groups.values())
        .sort((a, b) => (b[0].score ?? 0) - (a[0].score ?? 0))
        .slice(0, effectiveMaxDocs);

      let budget = contextBudget;
      const seenChunkKeys = new Set<string>();
      for (const g of orderedGroups) {
        for (const h of g) {
          const title = h.payload?.title as string | undefined;
          const url   = h.payload?.url as string | undefined;
          const txt   = String(h.payload?.text ?? "").slice(0, snippetLimit);
          const chunkKey = `${title ?? ""}|${txt.slice(0, 160)}`;
          if (seenChunkKeys.has(chunkKey)) continue;
          if (txt.length + 50 > budget) continue;
          chosen.push({ title, url, text: txt, score: h.score ?? 0 });
          seenChunkKeys.add(chunkKey);
          budget -= (txt.length + 50);
        }
      }
    }
    timeRetrievalMs = Date.now() - tRetrievalStart;
  }

  // build context only if we kept chunks
  const context =
    chosen.length === 0
      ? ""
      : chosen
          .map((c, i) => {
            const head = c.title ? `[${i + 1}] ${c.title}` : `[${i + 1}]`;
            const src  = c.url ? `\nSource: ${c.url}` : "";
            return `${head}\n${c.text}${src}`;
          })
          .join("\n\n");

  // optional transcript (kept concise)
  const trimmedTranscript =
    transcriptBudget > 0 ? (recentTranscript ?? "").toString().trim().slice(0, transcriptBudget) : "";

  if (profileContext) {
    profileContext = profileContext.toString().slice(0, profileBudget);
  }

  // Fast path: schedule questions with schedule context -> minimal prompt, no RAG
  if (isScheduleQuestion && scheduleContext) {
    const tFastPromptStart = Date.now();
    
    // Extract today's day info for explicit prompting
    const nowForPrompt = new Date();
    const zurichDayStr = nowForPrompt.toLocaleString('en-US', { timeZone: 'Europe/Zurich', weekday: 'long' });
    const zurichDateStr = nowForPrompt.toLocaleDateString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      weekday: 'long', 
      day: 'numeric', 
      month: 'long',
      year: 'numeric'
    });
    
    const fastPrompt = [
      `CRITICAL: Today is ${zurichDateStr}. When the user asks about "today", answer with ${zurichDayStr}'s schedule ONLY.`,
      "Answer the schedule question using only the provided timetable. Brief response, list if needed.",
      "IMPORTANT: For exam/final questions, ALWAYS include the FULL DATE (day, date, month, year) and time in your answer.",
      `Timetable:\n${scheduleContext}`,
      `Question: ${question}`,
    ].join("\n\n");
    timePromptMs = Date.now() - tFastPromptStart;

    const fastClient = client ?? getChatClient();
    const tLLMStartFast = Date.now();
    const fastChat = await fastClient.chat.completions.create({
      model: finalModel,
      messages: [
        { role: "system", content: `You answer schedule questions. TODAY IS ${zurichDayStr.toUpperCase()}. When user asks about "today", look for ${zurichDayStr} in the timetable. For EXAMS/FINALS: ALWAYS include the FULL DATE (weekday + day + month + year) and time. Match the user's language (French or English). Be concise but complete for dates.` },
        { role: "user", content: fastPrompt },
      ],
      temperature: finalTemperature,
      max_tokens: Math.min(finalMaxTokens, 220),
    });
    timeLLMMs = Date.now() - tLLMStartFast;
    const reply = (fastChat.choices?.[0]?.message?.content ?? "").trim();

    logger.info("answerWithRagCore.timing", {
      path: "schedule-fast",
      totalMs: Date.now() - tStart,
      scheduleMs: timeScheduleMs,
      retrievalMs: timeRetrievalMs,
      promptMs: timePromptMs,
      llmMs: timeLLMMs,
      chosenCount: 0,
      contextLen: 0,
      source_type: "schedule",
    });

    return {
      reply,
      primary_url: null,
      best_score: 1.0,
      sources: [],
      source_type: "schedule",
    };
  }

  let finalRagsContext = context;
  // Build schedule-specific instructions if schedule context is present
  const scheduleInstructions = scheduleContext
    ? "Schedule: answer only using the provided timetable. Concise list response."
    : "";

  const sourcePriorityLine =
    "Sources priority: Summary > Recent window > RAG context (ignore RAG for schedule questions).";

  const tPromptStart = Date.now();
  const prompt = [
    "Respond briefly, no intro or conclusion. Match the user's language (French or English).",
    "Format: steps -> short numbered list; otherwise 2-4 short sentences with line breaks. No meta-comments.",
    sourcePriorityLine,
    scheduleInstructions,
    trimmedSummary ? `Conversation summary (do not display as-is):\n${trimmedSummary}\n` : "",
    trimmedTranscript ? `Recent window (do not display):\n${trimmedTranscript}\n` : "",
    scheduleContext ? `User timetable (use only for schedule questions):\n${scheduleContext}\n` : "",
    (finalRagsContext && !scheduleContext) ? `RAG context:\n${finalRagsContext}\n` : "",
    `Question: ${question}`,
    !scheduleContext ? "If information is not provided, say you don't know." : "",
  ].filter(Boolean).join("\n\n");

  logger.info("answerWithRagCore.context", {
    chosenCount: chosen.length,
    contextLen: context.length,
    trimmedSummaryLen: trimmedSummary.length,
    hasTranscript: Boolean(trimmedTranscript),
    transcriptLen: trimmedTranscript.length,
    hasProfileContext: Boolean(profileContext),
    profileContextLen: profileContext ? profileContext.length : 0,
    titles: chosen.map(c => c.title).filter(Boolean).slice(0, 5),
    summaryHead: trimmedSummary.slice(0, 120),
    profileContextHead: profileContext ? profileContext.slice(0, 120) : null,
    routerUsed: routedQuestion !== q,
    routedQuestionHead: routedQuestion.slice(0, 120),
  });

  const summaryUsageRules =
    [
      "Sources (priority): 1) Summary, 2) Recent window, 3) RAG context.",
      "Schedule questions: use only the provided timetable.",
      "If info is missing or uncertain: say so and suggest a reliable EPFL source.",
    ].join("\n");

  const profileContextSection = ""; // personal/profile handling removed

  const systemContent = [
    EPFL_SYSTEM_PROMPT,
    summaryUsageRules,
    trimmedSummary
      ? "Conversation summary to consider (do not display as-is):\n" + trimmedSummary
      : "",
    trimmedTranscript
      ? "Recent window (do not display; useful for immediate references):\n" + trimmedTranscript
      : "",
    scheduleContext
      ? "User EPFL schedule (use for questions about schedule, courses, rooms):\n" + scheduleContext
      : "",
    profileContextSection,
  ]
    .filter(Boolean)
    .join("\n\n");

  timePromptMs = Date.now() - tPromptStart;

  // Log system content to verify profile context is included (structured logging only)
  logger.info("answerWithRagCore.systemContent", {
    systemContentLen: systemContent.length,
    containsPriorityContext: systemContent.includes("PRIORITY USER CONTEXT"),
    containsProfileData: profileContext ? systemContent.includes(profileContext.slice(0, 50)) : false,
  });

  const activeClient = client ?? getChatClient();
  const tLLMStart = Date.now();
  const chat = await activeClient.chat.completions.create({
    model: finalModel,
    messages: [
      { role: "system", content: systemContent },
      { role: "user", content: prompt },
    ],
    temperature: finalTemperature,
    max_tokens: finalMaxTokens,
  });
  timeLLMMs = Date.now() - tLLMStart;

  const rawReply = chat.choices?.[0]?.message?.content ?? "";
  const reply = rawReply.replace(/\s*USED_CONTEXT=(YES|NO)\s*$/i, "").trim();

  // Determine source type: schedule takes priority over RAG for schedule questions
  const usedSchedule = isScheduleQuestion && scheduleContext.length > 0;
  const usedRag = chosen.length > 0 && !usedSchedule;

  // source_type: "schedule" | "rag" | "none"
  let source_type: "schedule" | "rag" | "none" = "none";
  if (usedSchedule) {
    source_type = "schedule";
  } else if (usedRag) {
    source_type = "rag";
  }

  // Only expose a URL when using RAG (not schedule)
  const primary_url = usedRag ? (chosen[0]?.url ?? null) : null;

  logger.info("answerWithRagCore.timing", {
    totalMs: Date.now() - tStart,
    scheduleMs: timeScheduleMs,
    retrievalMs: timeRetrievalMs,
    promptMs: timePromptMs,
    llmMs: timeLLMMs,
    chosenCount: chosen.length,
    contextLen: context.length,
    source_type,
  });

  return {
    reply,
    primary_url,
    best_score: bestScore,
    sources: usedRag ? chosen.map((c, i) => ({ idx: i + 1, title: c.title, url: c.url, score: c.score })) : [],
    source_type, // "schedule", "rag", or "none"
  };
}

/* =========================================================
 *                ROLLING SUMMARY (Firestore)
 * ======================================================= */

type ChatTurn = { role: "user" | "assistant"; content: string };

function clampText(s: string | undefined | null, max = 1200): string {
  const t = (s ?? "").toString();
  if (t.length <= max) return t;
  return t.slice(0, max);
}

async function buildRollingSummary({
  priorSummary,
  recentTurns,
  client,
}: {
  priorSummary?: string;
  recentTurns: ChatTurn[];
  client?: OpenAI;
}): Promise<string> {
  // Keep inputs short and deterministic
  const sys =
    [
      "You maintain a cumulative, actionable summary of a conversation (user ↔ assistant).",
      "Objective: capture only what helps continue the exchange (topic, intentions, constraints, decisions).",
      "Forbidden: no general statements outside the conversation, no sources, no URLs, no pleasantries.",
      "Requirements:",
      "- Write concisely and factually in English.",
      "- Start with: 'So far, we have discussed:' then 2-5 brief bullet points.",
      "- Add if present: 'Intentions/expectations: ...', 'Constraints/preferences: ...', 'Open questions: ...'.",
      "- Length: ≤ 10 lines. No verbatim quotes; rephrase.",
    ].join("\n");

  const parts: Array<{ role: "system" | "user" | "assistant"; content: string }> = [];
  parts.push({ role: "system", content: sys });
  if (priorSummary) {
    parts.push({
      role: "user",
      content: `Previous summary:\n${clampText(priorSummary, 800)}`,
    });
  }
  // Include the last few turns for recency; cap to ~8 turns to be safe
  const turns = recentTurns.slice(-8);
  const recentStr = turns
    .map((t) => (t.role === "user" ? `User: ${t.content}` : `Assistant: ${t.content}`))
    .join("\n");
  parts.push({
    role: "user",
    content: `New exchanges (integrate without rewriting everything):\n${clampText(recentStr, 1500)}`,
  });
  parts.push({
    role: "user",
    content: [
      "Produce the new cumulative summary in the requested format.",
      "Use bullet points for the first section, then short lines for the rest.",
      "Do not invent. Avoid trivial details and any method explanations.",
    ].join("\n"),
  });

  const modelId = process.env.APERTUS_SUMMARY_MODEL_ID || process.env.APERTUS_MODEL_ID!;
  const activeClient = client ?? getChatClient();
  const resp = await activeClient.chat.completions.create({
    model: modelId,
    messages: parts,
    temperature: 0.1,
    max_tokens: 220,
  });
  const updated = (resp.choices?.[0]?.message?.content ?? "").trim();
  return clampText(updated, 1200);
}

export const onMessageCreate = europeFunctions.firestore
  .document("users/{uid}/conversations/{cid}/messages/{mid}")
  .onCreate(async (snap: functions.firestore.DocumentSnapshot, ctx: functions.EventContext) => {
    try {
      const data = snap.data() as any;
      const content = (data?.content ?? "").toString().trim();
      const role = (data?.role ?? "").toString();
      if (!content || !role || data?.summary) {
        return;
      }

      const { uid, cid, mid } = ctx.params as Record<string, string>;
      const messagesCol = db
        .collection("users").doc(uid)
        .collection("conversations").doc(cid)
        .collection("messages");

      // Load recent window in ascending order
      const recentSnap = await messagesCol.orderBy("createdAt", "asc").limitToLast(20).get();
      const recentDocs = recentSnap.docs;

      // Build prior summary from the most recent message BEFORE current that has one
      let priorSummary: string | undefined;
      for (let i = recentDocs.length - 1; i >= 0; i--) {
        const d: admin.firestore.QueryDocumentSnapshot = recentDocs[i] as any;
        if (d.id === mid) {
          // skip current; continue scanning earlier docs
          continue;
        }
        const sd = d.data() as any;
        if (typeof sd?.summary === "string" && sd.summary.trim()) {
          priorSummary = sd.summary;
          break;
        }
      }

      // Extract recent turns (role/content) from the window
      const recentTurns: ChatTurn[] = recentDocs.map((d: admin.firestore.QueryDocumentSnapshot) => {
        const x = d.data() as any;
        const r = (x?.role ?? "").toString() as "user" | "assistant";
        const c = (x?.content ?? "").toString();
        return { role: r === "assistant" ? "assistant" : "user", content: c };
      });

      const summary = await buildRollingSummary({ priorSummary, recentTurns });
      await snap.ref.update({ summary });
      logger.info("summary.updated", { uid, cid, mid, len: summary.length });
    } catch (e: any) {
      logger.error("summary.failed", { error: String(e) });
    }
  });

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

      return {
        reply,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: true,
        ed_intent: edIntentResult.ed_intent,
        ed_formatted_question: formattedQuestion,
        ed_formatted_title: formattedTitle,
        ed_fetch_intent_detected,
        ed_fetch_query,
      };
    }
    // === End ED Intent Detection ===

    // === Moodle Intent Detection (fast, regex-based) ===
    // First layer: General detection - determines if this is a Moodle fetch request
    const moodleIntentResult = detectMoodleFileFetchIntentCore(question);
    // Detect if user is writing in English for response language (moved outside try block for catch access)
    const isEnglishMoodle = /\b(get|fetch|download|show|display|retrieve|homework|lecture|solution|from|the|my|please|can\s+you|i\s+want|i\s+need)\b/i.test(question);
    
    if (moodleIntentResult.moodle_intent_detected && moodleIntentResult.moodle_intent && uid) {
      logger.info("answerWithRagFn.moodleIntentDetected", {
        intent: moodleIntentResult.moodle_intent,
        questionLen: question.length,
        uid,
      });

      try {
        // Get Moodle config
        const moodleConfig = await moodleService.getStatus(uid);
        if (moodleConfig.status !== "connected" || !moodleConfig.baseUrl || !moodleConfig.tokenEncrypted) {
          return {
            reply: isEnglishMoodle
              ? "You're not connected to Moodle. Please connect in the settings."
              : "Vous n'êtes pas connecté à Moodle. Veuillez vous connecter dans les paramètres.",
            primary_url: null,
            best_score: 0,
            sources: [],
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        // Second layer: Extract specific file type, number, course name, and week
        const fileInfo = extractFileInfo(question);
        if (!fileInfo) {
          return {
            reply: isEnglishMoodle
              ? "I couldn't identify the requested file type. Please specify (e.g., \"Lecture 5\", \"Homework 3\", \"Homework solution 2\")."
              : "Je n'ai pas pu identifier le type de fichier demandé. Veuillez préciser (ex: \"Lecture 5\", \"Homework 3\", \"Solution devoir 2\").",
            primary_url: null,
            best_score: 0,
            sources: [],
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        // Decrypt the token and trim any whitespace
        let token: string;
        try {
          token = decryptSecret(moodleConfig.tokenEncrypted).trim();
          if (!token) {
            throw new Error("Decrypted token is empty");
          }
          logger.info("answerWithRagFn.tokenDecrypted", {
            uid,
            tokenLength: token.length,
          });
        } catch (decryptError: any) {
          logger.error("answerWithRagFn.tokenDecryptionFailed", {
            uid,
            error: String(decryptError),
            errorMessage: decryptError.message,
            hasTokenEncrypted: !!moodleConfig.tokenEncrypted,
            tokenEncryptedLength: moodleConfig.tokenEncrypted?.length || 0,
          });
          return {
            reply: isEnglishMoodle
              ? "Error decrypting Moodle token. Please reconnect to Moodle in the settings."
              : "Erreur lors du décryptage du token Moodle. Veuillez vous reconnecter à Moodle dans les paramètres.",
            primary_url: null,
            best_score: 0,
            sources: [],
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        const client = new MoodleClient(moodleConfig.baseUrl, token);

        logger.info("answerWithRagFn.moodleFileFetch", {
          uid,
          baseUrl: moodleConfig.baseUrl,
          fileType: fileInfo.fileType,
          fileNumber: fileInfo.fileNumber,
          courseName: fileInfo.courseName,
          week: fileInfo.week,
        });

        let foundFile: { fileurl: string; filename: string; mimetype: string } | null = null;
        let foundInCourse: any = null; // Track which course the file was found in

        // If course name is specified, search only in that course
        if (fileInfo.courseName) {
          const targetCourse = await client.findCourseByName(fileInfo.courseName);
          if (!targetCourse) {
            return {
              reply: isEnglishMoodle
                ? `I couldn't find the course "${fileInfo.courseName}" in your Moodle courses.`
                : `Je n'ai pas trouvé le cours "${fileInfo.courseName}" dans vos cours Moodle.`,
              primary_url: null,
              best_score: 0,
              sources: [],
              moodle_intent_detected: true,
              moodle_intent: moodleIntentResult.moodle_intent,
              moodle_file: null,
            };
          }

          // Validate course ID - Moodle returns 'id' field for courses
          const courseId = targetCourse.id;
          if (!courseId) {
            logger.error("answerWithRagFn.missingCourseId", {
              courseName: fileInfo.courseName,
              course: targetCourse,
              availableFields: Object.keys(targetCourse),
            });
            return {
              reply: isEnglishMoodle
                ? `Error: Could not find the course ID for "${fileInfo.courseName}".`
                : `Erreur: Impossible de trouver l'ID du cours "${fileInfo.courseName}".`,
              primary_url: null,
              best_score: 0,
              sources: [],
              moodle_intent_detected: true,
              moodle_intent: moodleIntentResult.moodle_intent,
              moodle_file: null,
            };
          }

          const courseIdNum = typeof courseId === "number" ? courseId : parseInt(String(courseId), 10);
          if (isNaN(courseIdNum) || courseIdNum <= 0) {
            logger.error("answerWithRagFn.invalidCourseId", {
              courseName: fileInfo.courseName,
              course: targetCourse,
              courseId,
              courseIdNum,
            });
            return {
              reply: isEnglishMoodle
                ? `Error: Invalid course ID for "${fileInfo.courseName}".`
                : `Erreur: ID de cours invalide pour "${fileInfo.courseName}".`,
              primary_url: null,
              best_score: 0,
              sources: [],
              moodle_intent_detected: true,
              moodle_intent: moodleIntentResult.moodle_intent,
              moodle_file: null,
            };
          }

          logger.info("answerWithRagFn.searchingCourse", {
            courseName: fileInfo.courseName,
            courseId: courseIdNum,
            courseFullname: targetCourse.fullname,
            courseShortname: targetCourse.shortname,
            fileType: fileInfo.fileType,
            fileNumber: fileInfo.fileNumber,
            week: fileInfo.week,
          });

          // Search in the specific course
          foundFile = await client.findFile(
            courseIdNum,
            fileInfo.fileType,
            fileInfo.fileNumber,
            fileInfo.week
          );
          if (foundFile) {
            foundInCourse = targetCourse;
          }
        } else {
          // No course specified - search in all courses
          const courses = await client.getEnrolledCourses();
          if (courses.length === 0) {
            return {
              reply: isEnglishMoodle
                ? "No courses found in your Moodle."
                : "Aucun cours trouvé dans votre Moodle.",
              primary_url: null,
              best_score: 0,
              sources: [],
              moodle_intent_detected: true,
              moodle_intent: moodleIntentResult.moodle_intent,
              moodle_file: null,
            };
          }

          // Search for file in all courses in parallel for better performance
          // We use Promise.allSettled to search all courses simultaneously, then
          // check results in order and take the first match (maintains deterministic behavior)
          let foundInCourse: any = null;
          
          const searchPromises = courses.map(async (course) => {
            const courseId = course.id; // Moodle returns 'id' field
            if (!courseId) {
              logger.warn("answerWithRagFn.courseWithoutId", {
                course,
                availableFields: Object.keys(course),
              });
              return { course, file: null };
            }

            const courseIdNum = typeof courseId === "number" ? courseId : parseInt(String(courseId), 10);
            if (isNaN(courseIdNum) || courseIdNum <= 0) {
              logger.warn("answerWithRagFn.invalidCourseIdInList", {
                course,
                courseId,
                courseIdNum,
              });
              return { course, file: null };
            }

            logger.info("answerWithRagFn.searchingInCourse", {
              courseId: courseIdNum,
              courseFullname: course.fullname,
              fileType: fileInfo.fileType,
              fileNumber: fileInfo.fileNumber,
              week: fileInfo.week,
            });

            const file = await client.findFile(
              courseIdNum,
              fileInfo.fileType,
              fileInfo.fileNumber,
              fileInfo.week
            );
            return { course, file };
          });

          // Wait for all searches to complete (in parallel), then find first match
          const results = await Promise.allSettled(searchPromises);
          for (const result of results) {
            if (result.status === "fulfilled" && result.value.file) {
              foundFile = result.value.file;
              foundInCourse = result.value.course;
              break; // Take first match (maintains original behavior)
            }
          }

          // Store the course for later use in response
          if (foundInCourse) {
            fileInfo.courseName = foundInCourse.fullname || foundInCourse.shortname || foundInCourse.displayname;
          }
        }

        if (!foundFile) {
          const fileTypeNamesFr: Record<string, string> = {
            "lecture": "cours",
            "homework": "devoir",
            "homework_solution": "solution de devoir",
          };
          const fileTypeNamesEn: Record<string, string> = {
            "lecture": "lecture",
            "homework": "homework",
            "homework_solution": "homework solution",
          };
          return {
            reply: isEnglishMoodle
              ? `I couldn't find ${fileTypeNamesEn[fileInfo.fileType]} ${fileInfo.fileNumber} in your Moodle courses.`
              : `Je n'ai pas trouvé ${fileTypeNamesFr[fileInfo.fileType]} ${fileInfo.fileNumber} dans vos cours Moodle.`,
            primary_url: null,
            best_score: 0,
            sources: [],
            moodle_intent_detected: true,
            moodle_intent: moodleIntentResult.moodle_intent,
            moodle_file: null,
          };
        }

        // Get direct download URL (follows redirects if needed)
        const downloadUrl = await client.getDirectFileUrl(foundFile.fileurl);

        // Get course name for the response
        let courseDisplayName = "Moodle";
        if (foundInCourse) {
          courseDisplayName = foundInCourse.fullname || foundInCourse.shortname || foundInCourse.displayname || "Moodle";
        } else if (fileInfo.courseName) {
          // If course name was specified but we don't have the full course object, use the name
          courseDisplayName = fileInfo.courseName;
        }

        // Reply text (keep short; card shows details)
        let replyText = isEnglishMoodle
          ? "Here is the requested file."
          : "Voici le document demandé.";

        const moodleFileResponse = {
          url: downloadUrl,
          filename: foundFile.filename,
          mimetype: foundFile.mimetype,
          courseName: courseDisplayName,
          fileType: fileInfo.fileType,
          fileNumber: fileInfo.fileNumber,
          week: fileInfo.week,
        };

        logger.info("answerWithRagFn.moodleFileFound", {
          uid,
          filename: foundFile.filename,
          url: downloadUrl.substring(0, 100) + "...",
          mimetype: foundFile.mimetype,
          courseName: courseDisplayName,
          moodleFileResponse,
        });

        return {
          reply: replyText,
          primary_url: null,
          best_score: 0,
          sources: [],
          moodle_intent_detected: true,
          moodle_intent: moodleIntentResult.moodle_intent,
          moodle_file: moodleFileResponse,
        };
      } catch (e: any) {
        const fileInfoForLog = extractFileInfo(question);
        logger.error("answerWithRagFn.moodleFileFetchFailed", {
          error: String(e),
          errorMessage: e.message,
          errorStack: e.stack,
          uid,
          intent: moodleIntentResult.moodle_intent,
          fileType: fileInfoForLog?.fileType,
          fileNumber: fileInfoForLog?.fileNumber,
          courseName: fileInfoForLog?.courseName,
          week: fileInfoForLog?.week,
        });
        return {
          reply: isEnglishMoodle
            ? `Error retrieving file from Moodle: ${e.message || "unknown error"}`
            : `Erreur lors de la récupération du fichier Moodle : ${e.message || "erreur inconnue"}`,
          primary_url: null,
          best_score: 0,
          sources: [],
          moodle_intent_detected: true,
          moodle_intent: moodleIntentResult.moodle_intent,
          moodle_file: null,
        };
      }
    }
    // === End Moodle Intent Detection ===

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
const INDEX_API_KEY = process.env.INDEX_API_KEY || "";

function checkKey(req: functions.https.Request) {
  if (!INDEX_API_KEY) return;
  if (req.get("x-api-key") !== INDEX_API_KEY) {
    const e = new Error("unauthorized");
    (e as any).code = 401;
    throw e;
  }
}

function requireAuth(context: functions.https.CallableContext): string {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication is required"
    );
  }
  return uid;
}

export const indexChunksHttp = europeFunctions.https.onRequest(async (req: functions.https.Request, res: functions.Response<any>) => {
  try {
    if (req.method !== "POST") { res.status(405).end(); return; }
    checkKey(req);
    const out = await indexChunksCore(req.body as IndexChunksInput);
    res.status(200).json(out);
  } catch (e: any) {
    res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
  }
});

export const answerWithRagHttp = europeFunctions.https.onRequest(async (req: functions.https.Request, res: functions.Response<any>) => {
  try {
    if (req.method !== "POST") { res.status(405).end(); return; }
    checkKey(req);

    const body = req.body as AnswerWithRagInput;
    const question = String(body?.question || "").trim();

    const fetchIntentResult = detectFetchFromEdIntentCore(question);
    const { ed_fetch_intent_detected, ed_fetch_query } = fetchIntentResult;

    // === ED Intent Detection (fast, regex-based) ===
    const edIntentResult = detectPostToEdIntentCore(question);
    if (edIntentResult.ed_intent_detected && edIntentResult.ed_intent) {
      logger.info("answerWithRagHttp.edIntentDetected", {
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

      res.status(200).json({
        reply,
        primary_url: null,
        best_score: 0,
        sources: [],
        ed_intent_detected: true,
        ed_intent: edIntentResult.ed_intent,
        ed_formatted_question: formattedQuestion,
        ed_formatted_title: formattedTitle,
        ed_fetch_intent_detected,
        ed_fetch_query,
      });
      return;
    }
    // === End ED Intent Detection ===

    const ragResult = await answerWithRagCore(body);
    res.status(200).json({
      ...ragResult,
      ed_intent_detected: false,
      ed_intent: null,
      ed_formatted_question: null,
      ed_formatted_title: null,
      ed_fetch_intent_detected,
      ed_fetch_query,
      moodle_intent_detected: false,
      moodle_intent: null,
      moodle_file: null,
    });
  } catch (e: any) {
    res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
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
};

export const edConnectorPostFn = europeFunctions.https.onCall(
  async (data: EdConnectorPostCallableInput, context) => {
    const uid = requireAuth(context);

    const title = String(data?.title || "").trim();
    const body = String(data?.body || "").trim();
    const courseId =
      typeof data?.courseId === "number" ? data.courseId : undefined;

    if (!title && !body) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "title or body is required"
      );
    }

    try {
      const result = await edConnectorService.postThread(uid, {
        title,
        body,
        courseId,
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

/* =========================================================
 *                MOODLE CONNECTOR
 * ======================================================= */
import { MoodleConnectorRepository } from "./connectors/moodle/MoodleConnectorRepository";
import { MoodleConnectorService } from "./connectors/moodle/MoodleConnectorService";

// Use the same encryption as ED connector
const moodleRepo = new MoodleConnectorRepository(db);
const moodleService = new MoodleConnectorService(moodleRepo, encryptSecret, decryptSecret);
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

/** Parsed calendar event from ICS */
interface ScheduleEvent {
  uid: string;
  summary: string;
  location?: string;
  description?: string;
  dtstart: string; // ISO string
  dtend: string;   // ISO string
  rrule?: string;  // recurrence rule if any
}

/** Weekly slot (template) - day of week + time */
interface WeeklySlot {
  dayOfWeek: number; // 0=Sunday, 1=Monday, ..., 6=Saturday
  dayName: string;   // "Monday", "Tuesday", etc.
  startTime: string; // "15:15"
  endTime: string;   // "16:00"
  summary: string;
  location?: string;
  courseCode?: string;
}

/** Final exam event */
interface FinalExam {
  date: string;      // "2026-01-17"
  startTime: string; // "08:15"
  endTime: string;   // "11:15"
  summary: string;
  location?: string;
  courseCode?: string;
}

/** Optimized schedule structure */
interface OptimizedSchedule {
  weeklySlots: WeeklySlot[];
  finalExams: FinalExam[];
  semesterStart?: string;
  semesterEnd?: string;
}

/** Pattern to detect final exams (written or oral) */
const EXAM_PATTERN = /\b(written|oral|exam|examen|écrit|final|midterm|test)\b/i;

/** Extract course code from summary (e.g., "COM-300" from description) */
function extractCourseCode(description?: string): string | undefined {
  if (!description) return undefined;
  const match = description.match(/Course Code\n([A-Z]+-\d+)/);
  return match ? match[1] : undefined;
}

/** Parse ICS and extract optimized schedule (one week + exams) */
function parseICSOptimized(icsText: string): OptimizedSchedule {
  const allEvents: ScheduleEvent[] = [];
  const lines = icsText.replace(/\r\n /g, '').replace(/\r\n\t/g, '').split(/\r?\n/);

  let currentEvent: Partial<ScheduleEvent> | null = null;

  for (const line of lines) {
    if (line === 'BEGIN:VEVENT') {
      currentEvent = {};
    } else if (line === 'END:VEVENT' && currentEvent) {
      if (currentEvent.uid && currentEvent.summary && currentEvent.dtstart && currentEvent.dtend) {
        allEvents.push(currentEvent as ScheduleEvent);
      }
      currentEvent = null;
    } else if (currentEvent) {
      const colonIdx = line.indexOf(':');
      if (colonIdx === -1) continue;

      const fullKey = line.slice(0, colonIdx);
      const value = line.slice(colonIdx + 1);

      // Check if the key has parameters (like TZID)
      const semicolonIdx = fullKey.indexOf(';');
      const key = semicolonIdx !== -1 ? fullKey.slice(0, semicolonIdx) : fullKey;
      const hasSwissTZ = fullKey.includes('TZID=Europe/Zurich');

      switch (key) {
        case 'UID':
          currentEvent.uid = value;
          break;
        case 'SUMMARY':
          currentEvent.summary = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'LOCATION':
          currentEvent.location = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'DESCRIPTION':
          currentEvent.description = value.replace(/\\,/g, ',').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
          break;
        case 'DTSTART':
          currentEvent.dtstart = parseICSDate(value, hasSwissTZ);
          break;
        case 'DTEND':
          currentEvent.dtend = parseICSDate(value, hasSwissTZ);
          break;
        case 'RRULE':
          currentEvent.rrule = value;
          break;
      }
    }
  }

  // Separate regular classes from exams
  const regularClasses: ScheduleEvent[] = [];
  const examEvents: ScheduleEvent[] = [];
  
  const now = new Date();
  const fourWeeksFromNow = new Date(now.getTime() + 28 * 24 * 60 * 60 * 1000);

  for (const event of allEvents) {
    const eventDate = new Date(event.dtstart);
    const eventEnd = new Date(event.dtend);
    const durationHours = (eventEnd.getTime() - eventDate.getTime()) / (1000 * 60 * 60);
    
    // Treat as exam if:
    // 1. Summary contains exam-related words (written, oral, exam, etc.)
    // 2. OR event is far in the future (>4 weeks) with long duration (>3 hours) - likely an exam slot
    const isExamByPattern = EXAM_PATTERN.test(event.summary);
    const isExamByDateAndDuration = eventDate > fourWeeksFromNow && durationHours > 3;
    
    if (isExamByPattern || isExamByDateAndDuration) {
      examEvents.push(event);
      logger.info("parseICSOptimized.examDetected", {
        summary: event.summary,
        date: event.dtstart,
        byPattern: isExamByPattern,
        byDateDuration: isExamByDateAndDuration,
        durationHours,
      });
    } else {
      regularClasses.push(event);
    }
  }

  // Extract ONE canonical week from regular classes
  // Use a Map to dedupe by: dayOfWeek + startTime + summary
  const weeklyMap = new Map<string, WeeklySlot>();
  const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  for (const event of regularClasses) {
    const start = new Date(event.dtstart);
    const end = new Date(event.dtend);
    
    // Get day of week in Swiss timezone
    const dayOfWeek = getZurichDayOfWeek(start);
    
    // Get times in Swiss timezone (HH:MM format)
    const startTime = start.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const endTime = end.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });

    // Create unique key for deduplication
    const key = `${dayOfWeek}-${startTime}-${event.summary}`;

    if (!weeklyMap.has(key)) {
      // Only include defined fields (Firestore doesn't accept undefined)
      const slot: WeeklySlot = {
        dayOfWeek,
        dayName: dayNames[dayOfWeek],
        startTime,
        endTime,
        summary: event.summary,
      };
      if (event.location) slot.location = event.location;
      const code = extractCourseCode(event.description);
      if (code) slot.courseCode = code;
      weeklyMap.set(key, slot);
    }
  }

  // Convert to sorted array (Monday first, then by time)
  const weeklySlots = Array.from(weeklyMap.values())
    .sort((a, b) => {
      // Monday=1 should come first, Sunday=0 last
      const dayA = a.dayOfWeek === 0 ? 7 : a.dayOfWeek;
      const dayB = b.dayOfWeek === 0 ? 7 : b.dayOfWeek;
      if (dayA !== dayB) return dayA - dayB;
      return a.startTime.localeCompare(b.startTime);
    });

  // Extract final exams (dedupe by course)
  const examMap = new Map<string, FinalExam>();

  for (const event of examEvents) {
    const start = new Date(event.dtstart);
    const end = new Date(event.dtend);
    
    // Get date in Swiss timezone (YYYY-MM-DD format)
    const dateStr = start.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
    
    // Get times in Swiss timezone (HH:MM format)
    const startTime = start.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const endTime = end.toLocaleTimeString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      hour: '2-digit', 
      minute: '2-digit', 
      hour12: false 
    });
    const courseCode = extractCourseCode(event.description);

    // Dedupe by summary (one exam per course)
    const key = event.summary;
    if (!examMap.has(key)) {
      // Only include defined fields (Firestore doesn't accept undefined)
      const exam: FinalExam = {
        date: dateStr,
        startTime,
        endTime,
        summary: event.summary,
      };
      if (event.location) exam.location = event.location;
      if (courseCode) exam.courseCode = courseCode;
      examMap.set(key, exam);
    }
  }

  const finalExams = Array.from(examMap.values())
    .sort((a, b) => a.date.localeCompare(b.date));

  logger.info("parseICSOptimized.result", {
    totalEvents: allEvents.length,
    uniqueWeeklySlots: weeklySlots.length,
    finalExams: finalExams.length,
  });

  return { weeklySlots, finalExams };
}

/** Convert ICS date format to ISO string with proper timezone handling */
function parseICSDate(icsDate: string, isSwissTime: boolean = false): string {
  // Handle formats: 20251127T081500Z or 20251127T081500 or 20251127
  const isUTC = icsDate.endsWith('Z');
  const clean = icsDate.replace('Z', '');
  
  if (clean.length === 8) {
    // Date only: YYYYMMDD
    const dateStr = `${clean.slice(0, 4)}-${clean.slice(4, 6)}-${clean.slice(6, 8)}T00:00:00`;
    // If Swiss time, add timezone offset; if UTC, add Z; otherwise no suffix
    return isSwissTime ? dateStr + '+01:00' : (isUTC ? dateStr + 'Z' : dateStr);
  }
  
  // DateTime: YYYYMMDDTHHMMSS
  const datePart = clean.slice(0, 8);
  const timePart = clean.slice(9, 15);
  const isoString = `${datePart.slice(0, 4)}-${datePart.slice(4, 6)}-${datePart.slice(6, 8)}T${timePart.slice(0, 2)}:${timePart.slice(2, 4)}:${timePart.slice(4, 6)}`;
  
  // If already in Swiss time (TZID=Europe/Zurich), add timezone offset
  // CET (+01:00) in winter, CEST (+02:00) in summer
  if (isSwissTime) {
    const month = parseInt(datePart.slice(4, 6));
    // DST in Switzerland: approximately April to September
    const isDST = month >= 4 && month <= 9;
    return isoString + (isDST ? '+02:00' : '+01:00');
  }
  
  // If originally UTC, add Z suffix
  return isUTC ? isoString + 'Z' : isoString;
}

/** Format optimized schedule for LLM context */
function formatOptimizedScheduleForContext(schedule: OptimizedSchedule): string {
  const lines: string[] = [];

  // Weekly schedule template
  if (schedule.weeklySlots.length > 0) {
    lines.push("📚 WEEKLY TIMETABLE (every week):");

    let currentDay = '';
    for (const slot of schedule.weeklySlots) {
      if (slot.dayName !== currentDay) {
        currentDay = slot.dayName;
        lines.push(`\n${slot.dayName}:`);
      }
      const loc = slot.location ? ` @ ${slot.location}` : '';
      const code = slot.courseCode ? ` (${slot.courseCode})` : '';
      lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${code}${loc}`);
    }
  }

  // Final exams - format with COURSE NAME FIRST and FULL DATE for better model matching
  if (schedule.finalExams.length > 0) {
    lines.push("\n\n📝 FINAL EXAMS (written exam dates):");
    lines.push("IMPORTANT: Always include the FULL DATE when answering about exams.\n");
    for (const exam of schedule.finalExams) {
      // Parse date carefully - exam.date is in YYYY-MM-DD format
      // Add time to avoid timezone issues: treat as noon in Zurich
      const dateObj = new Date(exam.date + 'T12:00:00');
      const dateStr = dateObj.toLocaleDateString('en-GB', {
        timeZone: 'Europe/Zurich',
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
      const loc = exam.location ? ` in room ${exam.location}` : '';
      // Put course name FIRST, then FULL DATE with time
      lines.push(`  • ${exam.summary}: ${dateStr}, ${exam.startTime}–${exam.endTime}${loc}`);
    }
  }

  if (lines.length === 0) {
    return "No classes or exams found.";
  }

  return lines.join('\n');
}

/** Get current date in Zurich timezone */
function getZurichDate(): Date {
  // Create a date string in Zurich timezone and parse it back
  const zurichStr = new Date().toLocaleString('en-US', { timeZone: 'Europe/Zurich' });
  return new Date(zurichStr);
}

/** Get day of week in Zurich timezone (0=Sunday, 6=Saturday) */
function getZurichDayOfWeek(date: Date): number {
  const zurichStr = date.toLocaleString('en-US', { timeZone: 'Europe/Zurich', weekday: 'short' });
  const dayMap: Record<string, number> = { 'Sun': 0, 'Mon': 1, 'Tue': 2, 'Wed': 3, 'Thu': 4, 'Fri': 5, 'Sat': 6 };
  const dayAbbr = zurichStr.split(',')[0];
  return dayMap[dayAbbr] ?? date.getDay();
}

/** Generate schedule for a specific date range using the weekly template */
function generateScheduleForDateRange(schedule: OptimizedSchedule, startDate: Date, endDate: Date): string {
  const lines: string[] = [];

  // Group weekly slots by day
  const slotsByDay = new Map<number, WeeklySlot[]>();
  for (const slot of schedule.weeklySlots) {
    if (!slotsByDay.has(slot.dayOfWeek)) {
      slotsByDay.set(slot.dayOfWeek, []);
    }
    slotsByDay.get(slot.dayOfWeek)!.push(slot);
  }

  // Iterate through each day in range (using Zurich timezone)
  const current = new Date(startDate);
  while (current <= endDate) {
    // Use Zurich timezone for day of week calculation
    const dayOfWeek = getZurichDayOfWeek(current);
    const slots = slotsByDay.get(dayOfWeek) || [];

    // Check for exams on this date (use Zurich date string)
    const zurichDateParts = current.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' }); // YYYY-MM-DD format
    const examsToday = schedule.finalExams.filter(e => e.date === zurichDateParts);

    if (slots.length > 0 || examsToday.length > 0) {
      const dayLabel = current.toLocaleDateString('en-GB', {
        timeZone: 'Europe/Zurich',
        weekday: 'long',
        day: 'numeric',
        month: 'long'
      });
      lines.push(`\n📅 ${dayLabel.charAt(0).toUpperCase() + dayLabel.slice(1)}`);

      // Add regular classes
      for (const slot of slots) {
        const loc = slot.location ? ` @ ${slot.location}` : '';
        lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${loc}`);
      }

      // Add exams
      for (const exam of examsToday) {
        const loc = exam.location ? ` @ ${exam.location}` : '';
        lines.push(`  • ${exam.startTime}–${exam.endTime}: 📝 ${exam.summary}${loc}`);
      }
    }

    current.setDate(current.getDate() + 1);
  }

  return lines.length > 0 ? lines.join('\n') : "No classes scheduled for this period.";
}

/** Sync user's EPFL schedule from ICS URL */
type SyncScheduleInput = { icsUrl: string };

// Allowlist of hosts that are permitted for ICS URL fetching (SSRF protection)
const ALLOWED_ICS_HOSTS = [
  'campus.epfl.ch',
  'isa.epfl.ch',
  'isacademia.epfl.ch',
  'edu.epfl.ch',
  'moodle.epfl.ch',
];

function isAllowedIcsHost(urlString: string): boolean {
  try {
    const parsed = new URL(urlString);
    const host = parsed.hostname.toLowerCase();
    return ALLOWED_ICS_HOSTS.some(allowed =>
      host === allowed || host.endsWith('.' + allowed)
    );
  } catch {
    return false;
  }
}

export async function syncEpflScheduleCore(uid: string, { icsUrl }: SyncScheduleInput) {
  // Validate URL
  if (!icsUrl || typeof icsUrl !== 'string') {
    throw new Error("Missing or invalid 'icsUrl'");
  }

  // Basic URL validation - should look like an EPFL/IS-Academia ICS URL
  const url = icsUrl.trim();
  if (!url.startsWith('http://') && !url.startsWith('https://')) {
    throw new Error("Invalid URL format");
  }

  // SSRF protection: Only allow fetching from known EPFL hosts
  if (!isAllowedIcsHost(url)) {
    logger.warn("syncEpflSchedule.rejectedHost", { uid, urlPreview: url.slice(0, 60) });
    throw new Error("URL must be from an EPFL domain (campus.epfl.ch, isa.epfl.ch, etc.)");
  }

  // Fetch the ICS file
  logger.info("syncEpflSchedule.fetching", { uid, urlPreview: url.slice(0, 60) });

  const response = await fetch(url, {
    headers: {
      'User-Agent': 'EULER-App/1.0',
      'Accept': 'text/calendar, */*',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch ICS: ${response.status} ${response.statusText}`);
  }

  const icsText = await response.text();

  if (!icsText.includes('BEGIN:VCALENDAR')) {
    throw new Error("Invalid ICS format: not a valid calendar file");
  }

  // Parse with OPTIMIZED parser (one week + exams only)
  const schedule = parseICSOptimized(icsText);

  logger.info("syncEpflSchedule.parsed", {
    uid,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
    // Log each slot's day for debugging
    slotDetails: schedule.weeklySlots.map(s => ({
      dayOfWeek: s.dayOfWeek,
      dayName: s.dayName,
      time: s.startTime,
      summary: s.summary.substring(0, 30),
    })),
    examDetails: schedule.finalExams.map(e => ({
      date: e.date,
      summary: e.summary.substring(0, 30),
    })),
  });

  // Store OPTIMIZED structure in Firestore (much smaller!)
  const userRef = db.collection('users').doc(uid);
  await userRef.set({
    epflSchedule: {
      icsUrl: url,
      weeklySlots: schedule.weeklySlots,
      finalExams: schedule.finalExams,
      lastSync: admin.firestore.FieldValue.serverTimestamp(),
      slotCount: schedule.weeklySlots.length,
      examCount: schedule.finalExams.length,
    }
  }, { merge: true });

  logger.info("syncEpflSchedule.stored", {
    uid,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
  });

  return {
    success: true,
    eventCount: schedule.weeklySlots.length + schedule.finalExams.length,
    weeklySlots: schedule.weeklySlots.length,
    finalExams: schedule.finalExams.length,
    message: `Synced ${schedule.weeklySlots.length} weekly classes and ${schedule.finalExams.length} final exams.`,
  };
}

/** Get user's schedule context for LLM */
export async function getScheduleContextCore(uid: string): Promise<string> {
  const userDoc = await db.collection('users').doc(uid).get();
  const data = userDoc.data();

  if (!data?.epflSchedule) {
    return "";
  }

  // Check for new optimized format
  if (data.epflSchedule.weeklySlots) {
    const schedule: OptimizedSchedule = {
      weeklySlots: data.epflSchedule.weeklySlots || [],
      finalExams: data.epflSchedule.finalExams || [],
    };

    // For context, show the weekly template + upcoming week + exams
    // Use Zurich timezone for "now"
    const now = getZurichDate();
    const nextWeek = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    
    // Get today's info in Swiss timezone
    const todayStr = now.toLocaleDateString('en-US', {
      timeZone: 'Europe/Zurich',
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
    const todayDayOfWeek = getZurichDayOfWeek(now);
    const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

    const lines: string[] = [];
    
    // CRITICAL: Put today's schedule FIRST and prominently
    lines.push(`🚨 TODAY IS: ${todayStr} (${dayNames[todayDayOfWeek]})`);
    lines.push(`When the user asks about "today", answer with ${dayNames[todayDayOfWeek]}'s classes ONLY.\n`);
    
    // Show TODAY's classes first (most important)
    const todaySlots = schedule.weeklySlots.filter(s => s.dayOfWeek === todayDayOfWeek);
    if (todaySlots.length > 0) {
      lines.push(`📅 TODAY'S CLASSES (${dayNames[todayDayOfWeek]}):`);
      for (const slot of todaySlots) {
        const loc = slot.location ? ` @ ${slot.location}` : '';
        const code = slot.courseCode ? ` (${slot.courseCode})` : '';
        lines.push(`  • ${slot.startTime}–${slot.endTime}: ${slot.summary}${code}${loc}`);
      }
      lines.push("");
    } else {
      lines.push(`📅 TODAY (${dayNames[todayDayOfWeek]}): No classes scheduled.\n`);
    }

    // Show EXAMS prominently with FULL DATES (critical for exam questions)
    if (schedule.finalExams.length > 0) {
      lines.push(`\n🎓 YOUR FINAL EXAMS (ALWAYS include full date in answers):`);
      for (const exam of schedule.finalExams) {
        // Parse date carefully - exam.date is in YYYY-MM-DD format
        const dateObj = new Date(exam.date + 'T12:00:00');
        const fullDateStr = dateObj.toLocaleDateString('en-GB', {
          timeZone: 'Europe/Zurich',
          weekday: 'long',
          day: 'numeric',
          month: 'long',
          year: 'numeric'
        });
        const loc = exam.location ? ` in room ${exam.location}` : '';
        lines.push(`  • ${exam.summary}: ${fullDateStr}, ${exam.startTime}–${exam.endTime}${loc}`);
      }
      lines.push("");
    }

    // Then show the full weekly template
    lines.push(formatOptimizedScheduleForContext(schedule));

    // Also show specific dates for the next 7 days
    lines.push("\n\n📆 THIS WEEK (specific dates):");
    lines.push(generateScheduleForDateRange(schedule, now, nextWeek));
    
    // Log for debugging
    logger.info("getScheduleContextCore.todayInfo", {
      uid,
      todayStr,
      todayDayOfWeek,
      dayName: dayNames[todayDayOfWeek],
      weeklySlotDays: schedule.weeklySlots.map(s => ({ day: s.dayOfWeek, name: s.dayName, summary: s.summary })),
    });

    return lines.join('\n');
  }

  // Fallback for old format (legacy)
  if (data.epflSchedule.events && data.epflSchedule.events.length > 0) {
    // Old format - just show a message to re-sync
    return "Schedule available. For better performance, please reconnect your EPFL Campus calendar.";
  }

  return "";
}

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

/** 
 * Sync weekly food menus from EPFL website
 * Stores in a global collection (not per-user since menus are the same for everyone)
 */
export async function syncEpflFoodMenusCore(): Promise<{ success: boolean; mealCount: number; days: number }> {
  logger.info("syncEpflFoodMenus.starting");
  
  const weeklyMenu = await scrapeWeeklyMenu();
  
  const totalMeals = weeklyMenu.days.reduce((sum, day) => sum + day.meals.length, 0);
  
  // Store in Firestore - global collection
  const menuRef = db.collection('epflMenus').doc(weeklyMenu.weekStart);
  await menuRef.set({
    weekStart: weeklyMenu.weekStart,
    days: weeklyMenu.days.map(day => ({
      date: day.date,
      dayName: day.dayName,
      meals: day.meals.map(meal => ({
        name: meal.name,
        restaurant: meal.restaurant,
        studentPrice: meal.studentPrice,
        isVegetarian: meal.isVegetarian,
        isVegan: meal.isVegan,
        allergens: meal.allergens,
      })),
    })),
    lastSync: admin.firestore.FieldValue.serverTimestamp(),
    mealCount: totalMeals,
  });
  
  logger.info("syncEpflFoodMenus.success", { 
    weekStart: weeklyMenu.weekStart,
    mealCount: totalMeals,
    days: weeklyMenu.days.length,
  });
  
  return {
    success: true,
    mealCount: totalMeals,
    days: weeklyMenu.days.length,
  };
}

/**
 * Get the current week's Monday date string (Zurich timezone)
 */
function getCurrentWeekStart(): string {
  const zurichStr = new Date().toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const now = new Date(zurichStr);
  now.setHours(12, 0, 0, 0); // Avoid timezone edge cases
  const day = now.getDay();
  const diff = now.getDate() - day + (day === 0 ? -6 : 1);
  const monday = new Date(now);
  monday.setDate(diff);
  return monday.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' }); // YYYY-MM-DD
}

/**
 * Get today's date string in YYYY-MM-DD (Zurich timezone)
 */
function getTodayDateStr(): string {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
}

/**
 * Get tomorrow's date string in YYYY-MM-DD (Zurich timezone)
 */
function getTomorrowDateStr(): string {
  const now = new Date();
  const zurichStr = now.toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const tomorrow = new Date(zurichStr);
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toLocaleDateString('en-CA', { timeZone: 'Europe/Zurich' });
}

/**
 * Retrieve food menu context for LLM
 * Returns formatted string for the current week
 */
export async function getFoodContextCore(): Promise<string> {
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    // Try to sync if no data exists
    try {
      await syncEpflFoodMenusCore();
      const newDoc = await db.collection('epflMenus').doc(weekStart).get();
      if (newDoc.exists) {
        const data = newDoc.data() as any;
        return formatMenuForContext(data);
      }
    } catch (e) {
      logger.warn("getFoodContext.syncFailed", { error: String(e) });
    }
    return "No menu available for this week. Data has not been synced yet.";
  }
  
  const data = menuDoc.data() as any;
  return formatMenuForContext(data);
}

/**
 * Get meals for a specific day (today, tomorrow, or a date string)
 */
export async function getMealsForDay(daySpecifier: "today" | "tomorrow" | string): Promise<DailyMenu | null> {
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    // Try to sync
    try {
      await syncEpflFoodMenusCore();
    } catch (e) {
      return null;
    }
    const newDoc = await db.collection('epflMenus').doc(weekStart).get();
    if (!newDoc.exists) return null;
  }
  
  const data = menuDoc.data() as any;
  
  let targetDate: string;
  if (daySpecifier === "today") {
    targetDate = getTodayDateStr();
  } else if (daySpecifier === "tomorrow") {
    targetDate = getTomorrowDateStr();
  } else {
    targetDate = daySpecifier;
  }
  
  const dayData = data.days?.find((d: any) => d.date === targetDate);
  if (!dayData) return null;
  
  return {
    date: dayData.date,
    dayName: dayData.dayName,
    meals: dayData.meals || [],
  };
}

/**
 * Detect which day the user is asking about from their question
 * Returns: { dayIndex: 0-4 for Mon-Fri, dayLabel: localized label, isEnglish: language flag }
 */
function detectDayFromQuestion(q: string): { dayIndex: number | null; dayLabel: string; isEnglish: boolean } {
  const today = new Date();
  const zurichStr = today.toLocaleDateString('en-US', { timeZone: 'Europe/Zurich' });
  const zurichToday = new Date(zurichStr);
  const todayDayOfWeek = zurichToday.getDay(); // 0=Sun, 1=Mon, ..., 6=Sat
  
  // Detect language - check for English patterns
  const isEnglish = /\b(what['']?s|lunch|dinner|breakfast|today|tomorrow|monday|tuesday|wednesday|thursday|friday|meal|food|cafeteria|canteen|eat|cheap|vegetarian|vegan)\b/i.test(q);
  
  // Day name mappings (French and English)
  const dayMappings: Array<{ patterns: string[]; dayOfWeek: number; labelFr: string; labelEn: string }> = [
    { patterns: ["lundi", "monday"], dayOfWeek: 1, labelFr: "lundi", labelEn: "Monday" },
    { patterns: ["mardi", "tuesday"], dayOfWeek: 2, labelFr: "mardi", labelEn: "Tuesday" },
    { patterns: ["mercredi", "wednesday"], dayOfWeek: 3, labelFr: "mercredi", labelEn: "Wednesday" },
    { patterns: ["jeudi", "thursday"], dayOfWeek: 4, labelFr: "jeudi", labelEn: "Thursday" },
    { patterns: ["vendredi", "friday"], dayOfWeek: 5, labelFr: "vendredi", labelEn: "Friday" },
  ];
  
  // Check for explicit day names first
  for (const { patterns, dayOfWeek, labelFr, labelEn } of dayMappings) {
    if (patterns.some(p => q.includes(p))) {
      // Calculate index (0=Mon, 4=Fri)
      return { dayIndex: dayOfWeek - 1, dayLabel: isEnglish ? labelEn : labelFr, isEnglish };
    }
  }
  
  // Check for "demain" / "tomorrow"
  if (q.includes("demain") || q.includes("tomorrow")) {
    const tomorrowDayOfWeek = (todayDayOfWeek + 1) % 7;
    if (tomorrowDayOfWeek >= 1 && tomorrowDayOfWeek <= 5) {
      return { dayIndex: tomorrowDayOfWeek - 1, dayLabel: isEnglish ? "tomorrow" : "demain", isEnglish };
    }
    return { dayIndex: null, dayLabel: isEnglish ? "tomorrow" : "demain", isEnglish }; // Weekend
  }
  
  // Default to today
  if (todayDayOfWeek >= 1 && todayDayOfWeek <= 5) {
    return { dayIndex: todayDayOfWeek - 1, dayLabel: isEnglish ? "today" : "aujourd'hui", isEnglish };
  }
  return { dayIndex: null, dayLabel: isEnglish ? "today" : "aujourd'hui", isEnglish }; // Weekend
}

/**
 * Extract max price from question like "moins de 10 chf" or "under 15 francs"
 */
function extractMaxPrice(q: string): number | null {
  // Match patterns like "moins de 10", "moins de 10 chf", "under 15", "< 12"
  const patterns = [
    /moins\s+de\s+(\d+(?:[.,]\d+)?)/i,
    /under\s+(\d+(?:[.,]\d+)?)/i,
    /en\s+dessous\s+de\s+(\d+(?:[.,]\d+)?)/i,
    /<\s*(\d+(?:[.,]\d+)?)/,
    /max(?:imum)?\s+(\d+(?:[.,]\d+)?)/i,
  ];
  
  for (const pattern of patterns) {
    const match = q.match(pattern);
    if (match) {
      return parseFloat(match[1].replace(",", "."));
    }
  }
  return null;
}

/**
 * Filter meals by max price
 */
function filterMealsByPrice(meals: Meal[], maxPrice: number): Meal[] {
  return meals.filter(m => m.studentPrice !== null && m.studentPrice <= maxPrice);
}

/**
 * Answer a food-related question using the food data
 * Responds in the same language the user uses (French or English)
 */
export async function answerFoodQuestionCore(question: string): Promise<{
  reply: string;
  source_type: "food";
  meals?: Meal[];
}> {
  const q = question.toLowerCase();
  
  // Determine which day the user is asking about (includes language detection)
  const { dayIndex, dayLabel, isEnglish } = detectDayFromQuestion(q);
  
  // Get the weekly menu
  const weekStart = getCurrentWeekStart();
  const menuDoc = await db.collection('epflMenus').doc(weekStart).get();
  
  if (!menuDoc.exists) {
    try {
      await syncEpflFoodMenusCore();
    } catch (e) {
      return {
        reply: isEnglish 
          ? `I don't have menu data. Please try again later.`
          : `Je n'ai pas de données sur les menus. Veuillez réessayer plus tard.`,
        source_type: "food",
      };
    }
  }
  
  const data = (await db.collection('epflMenus').doc(weekStart).get()).data() as any;
  
  if (!data?.days || dayIndex === null || dayIndex >= data.days.length) {
    return {
      reply: isEnglish
        ? `I don't have menu data for ${dayLabel}. Menus are available Monday through Friday.`
        : `Je n'ai pas de données sur les menus pour ${dayLabel}. Les menus sont disponibles du lundi au vendredi.`,
      source_type: "food",
    };
  }
  
  const dayData = data.days[dayIndex];
  const dailyMenu: DailyMenu = {
    date: dayData.date,
    dayName: dayData.dayName,
    meals: dayData.meals || [],
  };
  
  if (dailyMenu.meals.length === 0) {
    return {
      reply: isEnglish
        ? `No menu available for ${dayLabel} (${dailyMenu.date}).`
        : `Aucun menu disponible pour ${dayLabel} (${dailyMenu.date}).`,
      source_type: "food",
    };
  }
  
  // Extract price filter if present
  const maxPrice = extractMaxPrice(q);
  
  // Format day label with capitalization
  const formattedDayLabel = dayLabel.charAt(0).toUpperCase() + dayLabel.slice(1);
  
  // Check for specific restaurant query
  const restaurantMatch = TARGET_RESTAURANTS.find(r => 
    q.includes(r.toLowerCase()) || 
    q.includes(r.toLowerCase().split("-")[0].trim()) ||
    (r === "Ornithorynque" && q.includes("orni")) ||
    (r === "Native-Restauration végétale - Bar à café" && q.includes("native"))
  );
  
  if (restaurantMatch) {
    let meals = findMealsByRestaurant(dailyMenu, restaurantMatch);
    if (maxPrice !== null) {
      meals = filterMealsByPrice(meals, maxPrice);
    }
    
    if (meals.length === 0) {
      const priceNote = maxPrice !== null 
        ? (isEnglish ? ` under ${maxPrice} CHF` : ` à moins de ${maxPrice} CHF`)
        : "";
      return {
        reply: isEnglish
          ? `No dishes found${priceNote} at ${restaurantMatch} ${dayLabel}.`
          : `Aucun plat trouvé${priceNote} pour ${restaurantMatch} ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const mealsList = meals.map(m => {
      const tags: string[] = [];
      if (m.isVegan) tags.push(isEnglish ? "🌱 vegan" : "🌱 vegan");
      else if (m.isVegetarian) tags.push(isEnglish ? "🥬 vegetarian" : "🥬 végétarien");
      const price = m.studentPrice ? ` — ${m.studentPrice.toFixed(2)} CHF` : "";
      const tagStr = tags.length > 0 ? ` [${tags.join(", ")}]` : "";
      return `• ${m.name}${tagStr}${price}`;
    }).join("\n");
    
    const priceNote = maxPrice !== null ? ` (< ${maxPrice} CHF)` : "";
    return {
      reply: isEnglish
        ? `${formattedDayLabel} at ${restaurantMatch}${priceNote}:\n${mealsList}`
        : `${formattedDayLabel} à ${restaurantMatch}${priceNote}:\n${mealsList}`,
      source_type: "food",
      meals,
    };
  }
  
  // Check for vegetarian/vegan query
  const isVeganQuery = q.includes("vegan");
  const isVeggieQuery = q.includes("végé") || q.includes("veggie") || q.includes("végétarien") || q.includes("vegetarian") || isVeganQuery;
  
  if (isVeggieQuery) {
    let veggieMeals = findVeggieMeals(dailyMenu, isVeganQuery);
    if (maxPrice !== null) {
      veggieMeals = filterMealsByPrice(veggieMeals, maxPrice);
    }
    
    if (veggieMeals.length === 0) {
      const type = isVeganQuery ? "vegan" : (isEnglish ? "vegetarian" : "végétarien");
      const priceNote = maxPrice !== null 
        ? (isEnglish ? ` under ${maxPrice} CHF` : ` à moins de ${maxPrice} CHF`)
        : "";
      return {
        reply: isEnglish
          ? `No ${type} dishes${priceNote} found for ${dayLabel}.`
          : `Aucun plat ${type}${priceNote} trouvé pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const type = isVeganQuery ? "vegan" : (isEnglish ? "vegetarian" : "végétarien");
    const mealsList = veggieMeals.map(m => {
      const tag = m.isVegan ? "🌱" : "🥬";
      const price = m.studentPrice ? ` — ${m.studentPrice.toFixed(2)} CHF` : "";
      return `• ${tag} ${m.name} (${m.restaurant})${price}`;
    }).join("\n");
    
    const priceNote = maxPrice !== null ? ` (< ${maxPrice} CHF)` : "";
    return {
      reply: isEnglish
        ? `${type.charAt(0).toUpperCase() + type.slice(1)} dishes ${dayLabel}${priceNote}:\n${mealsList}`
        : `Plats ${type}s ${dayLabel}${priceNote}:\n${mealsList}`,
      source_type: "food",
      meals: veggieMeals,
    };
  }
  
  // Check for price-based query (meals under X CHF)
  if (maxPrice !== null) {
    const affordableMeals = filterMealsByPrice(dailyMenu.meals, maxPrice);
    
    if (affordableMeals.length === 0) {
      return {
        reply: isEnglish
          ? `No dishes under ${maxPrice} CHF found for ${dayLabel}.`
          : `Aucun plat à moins de ${maxPrice} CHF trouvé pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    // Sort by price
    affordableMeals.sort((a, b) => (a.studentPrice ?? 999) - (b.studentPrice ?? 999));
    
    const mealsList = affordableMeals.map(m => {
      const tags: string[] = [];
      if (m.isVegan) tags.push("🌱");
      else if (m.isVegetarian) tags.push("🥬");
      const tagStr = tags.length > 0 ? ` ${tags.join(" ")}` : "";
      return `• ${m.name}${tagStr} (${m.restaurant}) — ${m.studentPrice?.toFixed(2)} CHF`;
    }).join("\n");
    
    return {
      reply: isEnglish
        ? `Dishes under ${maxPrice} CHF ${dayLabel}:\n${mealsList}`
        : `Plats à moins de ${maxPrice} CHF ${dayLabel}:\n${mealsList}`,
      source_type: "food",
      meals: affordableMeals,
    };
  }
  
  // Check for cheapest meal query (le moins cher)
  if (q.includes("moins cher") || q.includes("cheapest") || q.includes("cheap")) {
    const cheapest = findCheapestMeal(dailyMenu);
    if (!cheapest) {
      return {
        reply: isEnglish
          ? `No price data available for ${dayLabel}.`
          : `Pas de données de prix disponibles pour ${dayLabel}.`,
        source_type: "food",
      };
    }
    
    const tags: string[] = [];
    if (cheapest.isVegan) tags.push(isEnglish ? "🌱 vegan" : "🌱 vegan");
    else if (cheapest.isVegetarian) tags.push(isEnglish ? "🥬 vegetarian" : "🥬 végétarien");
    const tagStr = tags.length > 0 ? ` [${tags.join(", ")}]` : "";
    
    return {
      reply: isEnglish
        ? `The cheapest dish ${dayLabel} is "${cheapest.name}"${tagStr} at ${cheapest.restaurant} for ${cheapest.studentPrice?.toFixed(2)} CHF (student price).`
        : `Le plat le moins cher ${dayLabel} est "${cheapest.name}"${tagStr} à ${cheapest.restaurant} pour ${cheapest.studentPrice?.toFixed(2)} CHF (prix étudiant).`,
      source_type: "food",
      meals: [cheapest],
    };
  }
  
  // General "what's to eat" query - summarize the day
  const byRestaurant = new Map<string, Meal[]>();
  for (const meal of dailyMenu.meals) {
    if (!byRestaurant.has(meal.restaurant)) byRestaurant.set(meal.restaurant, []);
    byRestaurant.get(meal.restaurant)!.push(meal);
  }
  
  const lines: string[] = [`${formattedDayLabel} (${dailyMenu.date}):`];
  for (const [restaurant, meals] of byRestaurant) {
    lines.push(`\n🏪 ${restaurant}:`);
    for (const meal of meals) {
      const tags: string[] = [];
      if (meal.isVegan) tags.push("🌱");
      else if (meal.isVegetarian) tags.push("🥬");
      const price = meal.studentPrice ? ` — ${meal.studentPrice.toFixed(2)} CHF` : "";
      const tagStr = tags.length > 0 ? ` ${tags.join(" ")}` : "";
      lines.push(`  • ${meal.name}${tagStr}${price}`);
    }
  }
  
  return {
    reply: lines.join("\n"),
    source_type: "food",
    meals: dailyMenu.meals,
  };
}

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
