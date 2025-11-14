// functions/src/index.ts
import path from "node:path";
import dotenv from "dotenv";
dotenv.config({ path: path.join(__dirname, "..", ".env") });

import * as logger from "firebase-functions/logger";
import OpenAI from "openai";
import { randomUUID } from "node:crypto";
import * as functions from "firebase-functions/v1";

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
const chatClient = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY!,
  baseURL: withV1(process.env.OPENAI_BASE_URL),
  defaultHeaders: { "User-Agent": "euler-mvp/1.0 (genkit-functions)" },
});

// Embeddings = Jina
const EMBED_URL = withV1(process.env.EMBED_BASE_URL) + "/embeddings";
const EMBED_KEY = process.env.EMBED_API_KEY!;
const EMBED_MODEL = process.env.EMBED_MODEL_ID!; // e.g. "jina-embeddings-v3"

/* =========================================================
 *                         CHAT (optional)
 * ======================================================= */
export async function apertusChatFnCore({
  messages, model, temperature,
}: {
  messages: Array<{ role: "user" | "assistant" | "system"; content: string }>;
  model?: string; temperature?: number;
}) {
  const resp = await chatClient.chat.completions.create({
    model: model ?? process.env.APERTUS_MODEL_ID!,
    messages,
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

export async function generateTitleCore({ question, model }: GenerateTitleInput) {
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
  });

  const title = (reply || "")
    .replace(/\s+/g, " ")
    .replace(/^["'“”]+|["'“”]+$/g, "")
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
type AnswerWithRagInput = { question: string; topK?: number; model?: string };

export async function answerWithRagCore({ question, topK, model }: AnswerWithRagInput) {
  const q = question.trim();

  // Gate 1: skip retrieval on small talk
  const isSmallTalk = SMALL_TALK.test(q) && q.length <= 30;

  let chosen: Array<{ title?: string; url?: string; text: string; score: number }> = [];
  let bestScore = 0;

  if (!isSmallTalk) {
    const [queryVec] = await embedInBatches([q], 1, 0);
    const raw = await qdrantSearchHybrid(queryVec, q, Math.max(topK ?? 0, MAX_CANDIDATES));

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
        .slice(0, MAX_DOCS);

      let budget = CONTEXT_BUDGET;
      for (const g of orderedGroups) {
        for (const h of g) {
          const title = h.payload?.title as string | undefined;
          const url   = h.payload?.url as string | undefined;
          const txt   = String(h.payload?.text ?? "").slice(0, SNIPPET_LIMIT);
          if (txt.length + 50 > budget) continue;
          chosen.push({ title, url, text: txt, score: h.score ?? 0 });
          budget -= (txt.length + 50);
        }
      }
    }
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

  const prompt = [
    "Tu es un assistant concis. Utilise le contexte fourni (plusieurs extraits) et cite les indices [1], [2], etc.",
    context ? `\nContexte:\n${context}\n` : "\n(Contexte vide)\n",
    `Question: ${q}`,
    "Si l'information n'est pas dans le contexte, dis que tu ne sais pas.",
    "À la fin, écris EXACTEMENT: USED_CONTEXT=YES si tu as utilisé un extrait du contexte, sinon USED_CONTEXT=NO."
  ].join("\n");

  const chat = await chatClient.chat.completions.create({
    model: model ?? process.env.APERTUS_MODEL_ID!,
    messages: [{ role: "user", content: prompt }],
    temperature: 0.2,
    max_tokens: 400,
  });

  const rawReply = chat.choices?.[0]?.message?.content ?? "";
  const marker = rawReply.match(/USED_CONTEXT=(YES|NO)\s*$/i);
  const usedContextByModel = marker ? marker[1].toUpperCase() === "YES" : false;
  const reply = rawReply.replace(/\s*USED_CONTEXT=(YES|NO)\s*$/i, "").trim();

  // Only expose a URL when BOTH gates pass: we have chosen context and the model says it used it
  const primary_url = (chosen.length > 0 && usedContextByModel) ? (chosen[0]?.url ?? null) : null;

  return {
    reply,
    primary_url,
    best_score: bestScore,
    sources: chosen.map((c, i) => ({ idx: i + 1, title: c.title, url: c.url, score: c.score })),
  };
}

/* =========================================================
 *                EXPORTS: callable + HTTP twins
 * ======================================================= */

// ping
export const ping = functions.https.onRequest((_req, res) => {
  res.status(200).send("pong");
});

// callable (for Kotlin via Firebase SDK)
export const indexChunksFn = functions.https.onCall(async (data: IndexChunksInput) => {
  const { chunks } = data || ({} as any);
  if (!Array.isArray(chunks) || chunks.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "Missing 'chunks'");
  }
  return await indexChunksCore({ chunks });
});

export const answerWithRagFn = functions.https.onCall(async (data: AnswerWithRagInput) => {
  const question = String(data?.question || "").trim();
  const topK = Number(data?.topK ?? 5);
  const model = data?.model;
  if (!question) throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");
  return await answerWithRagCore({ question, topK, model });
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

export const indexChunksHttp = functions.https.onRequest(async (req, res) => {
  try {
    if (req.method !== "POST") { res.status(405).end(); return; }
    checkKey(req);
    const out = await indexChunksCore(req.body as IndexChunksInput);
    res.status(200).json(out);
  } catch (e: any) {
    res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
  }
});

export const answerWithRagHttp = functions.https.onRequest(async (req, res) => {
  try {
    if (req.method !== "POST") { res.status(405).end(); return; }
    checkKey(req);
    const out = await answerWithRagCore(req.body as AnswerWithRagInput);
    res.status(200).json(out);
  } catch (e: any) {
    res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
  }
});

export const generateTitleFn = functions.https.onCall(async (data: GenerateTitleInput) => {
  const q = String(data?.question || "").trim();
  const model = data?.model;
  if (!q) throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");
  return await generateTitleCore({ question: q, model });
});
