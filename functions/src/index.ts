// functions/src/index.ts

import path from "node:path";
import dotenv from "dotenv";
dotenv.config({ path: path.join(__dirname, "..", ".env") });

import { setGlobalOptions } from "firebase-functions";
import * as logger from "firebase-functions/logger";
import { onCallGenkit } from "firebase-functions/https";
import { genkit, z } from "genkit";
import OpenAI from "openai";
import { randomUUID } from "node:crypto";

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
  // Qdrant n'accepte que "unsigned integer" ou "UUID" comme ID de point.
  if (typeof id === "number") return id;
  const s = String(id);
  if (isUintString(s)) return Number(s);
  if (isUuid(s)) return s;
  // fallback: on génère un UUID stable côté serveur
  return randomUUID();
}

/* ---------- clients ---------- */
setGlobalOptions({ maxInstances: 10 });
const ai = genkit({ plugins: [] });

// Chat = PublicAI (Apertus)
const chatClient = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY!,
  baseURL: withV1(process.env.OPENAI_BASE_URL),
  defaultHeaders: { "User-Agent": "euler-mvp/1.0 (genkit-functions)" },
});

// Embeddings = Jina 
const EMBED_URL = withV1(process.env.EMBED_BASE_URL) + "/embeddings";
const EMBED_KEY = process.env.EMBED_API_KEY!;
const EMBED_MODEL = process.env.EMBED_MODEL_ID!; // ex: "jina-embeddings-v3"

/* =========================================================
 *                         CHAT (optionnel)
 * ======================================================= */
export const apertusChat = ai.defineFlow(
  {
    name: "apertusChat",
    inputSchema: z.object({
      messages: z.array(z.object({
        role: z.enum(["user", "assistant", "system"]),
        content: z.string(),
      })),
      temperature: z.number().optional(),
      model: z.string().optional(),
    }),
    outputSchema: z.object({ reply: z.string() }),
  },
  async ({ messages, model, temperature }) => {
    const resp = await chatClient.chat.completions.create({
      model: model ?? process.env.APERTUS_MODEL_ID!,
      messages,
      temperature: temperature ?? 0.2,
    });
    const reply = resp.choices?.[0]?.message?.content ?? "";
    logger.info("apertusChat success", { length: reply.length });
    return { reply };
  }
);
export const apertusChatFn = onCallGenkit({ cors: true }, apertusChat);

/* =========================================================
 *                   QDRANT HELPERS
 * ======================================================= */
type QdrantPoint = { id: string | number; vector: number[]; payload?: any };

async function qdrantEnsureCollection(dimension: number) {
  const base = process.env.QDRANT_URL!;
  const key = process.env.QDRANT_API_KEY!;
  const coll = process.env.QDRANT_COLLECTION!;

  const res = await fetch(`${base}/collections/${coll}`, { headers: { "api-key": key } });
  if (res.ok) return;

  const createRes = await fetch(`${base}/collections/${coll}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", "api-key": key },
    body: JSON.stringify({ vectors: { size: dimension, distance: "Cosine" } }),
  });
  if (!createRes.ok) {
    const t = await createRes.text().catch(() => "");
    throw new Error(`Qdrant create collection failed: ${createRes.status} ${t}`);
  }
}

async function qdrantUpsert(points: QdrantPoint[]) {
  const res = await fetch(
    `${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points?wait=true`,
    {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "api-key": process.env.QDRANT_API_KEY!,
      },
      body: JSON.stringify({ points }),
    }
  );
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`Qdrant upsert failed: ${res.status} ${t}`);
  }
}

async function qdrantSearch(vector: number[], topK = 5) {
  const r = await fetch(
    `${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "api-key": process.env.QDRANT_API_KEY!,
      },
      body: JSON.stringify({ vector, limit: topK, with_payload: true }),
    }
  );
  if (!r.ok) {
    const t = await r.text().catch(() => "");
    throw new Error(`Qdrant search failed: ${r.status} ${t}`);
  }
  const j = await r.json();
  return (j.result ?? []) as Array<{ id: string | number; score: number; payload?: any }>;
}

/* =========================================================
 *            UTIL EMBEDDINGS (via fetch + logs)
 * ======================================================= */
const MAX_CHARS = 8000; // limite prudente

function sanitizeTexts(arr: string[]): string[] {
  return arr
    .map((s) => (typeof s === "string" ? s : String(s ?? "")))
    .map((s) => s.trim())
    .map((s) => (s.length > MAX_CHARS ? s.slice(0, MAX_CHARS) : s))
    .filter((s) => s.length > 0);
}

async function embedBatch(texts: string[]): Promise<number[][]> {
  const clean = sanitizeTexts(texts);
  if (clean.length === 0) throw new Error("No valid texts to embed (empty after sanitize).");

  const body = JSON.stringify({ model: EMBED_MODEL, input: clean });
  const res = await fetch(EMBED_URL, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${EMBED_KEY}`,
      "Content-Type": "application/json",
      "Accept": "application/json",
      "User-Agent": "euler-mvp/1.0 (contact: you@example.com)",
    },
    body,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    logger.error("Embed error", {
      status: res.status,
      url: EMBED_URL,
      model: EMBED_MODEL,
      sampleInputLen: clean[0]?.length,
      batchSize: clean.length,
      bodyPreview: body.slice(0, 2000),
      responseTextPreview: text.slice(0, 2000),
    });
    throw new Error(`${res.status} ${text || "(no body)"}`);
  }

  const json = await res.json();
  return (json.data ?? []).map((d: any) => d.embedding as number[]);
}

async function sleep(ms: number) {
  return new Promise((res) => setTimeout(res, ms));
}

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
 *            INGESTION DIRECTE DE CHUNKS: indexChunks
 * ======================================================= */
export const indexChunks = ai.defineFlow(
  {
    name: "indexChunks",
    inputSchema: z.object({
      chunks: z.array(z.object({
        id: z.string(),               // libre: on normalise en uint/UUID pour Qdrant
        text: z.string(),             // contenu du chunk
        title: z.string().optional(),
        url: z.string().optional(),
        payload: z.record(z.any()).optional(),
      })),
    }),
    outputSchema: z.object({ count: z.number(), dim: z.number() }),
  },
  async ({ chunks }) => {
    if (!chunks.length) return { count: 0, dim: 0 };

    const texts = chunks.map((c) => c.text);
    const vectors = await embedInBatches(texts, 16, 150);
    const dim = vectors[0].length;

    await qdrantEnsureCollection(dim);

    const points: QdrantPoint[] = vectors.map((v, i) => ({
      id: normalizePointId(chunks[i].id),
      vector: v,
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
);
export const indexChunksFn = onCallGenkit({ cors: true }, indexChunks);

/* =========================================================
 *                REQUÊTE: answerWithRag
 * ======================================================= */
export const answerWithRag = ai.defineFlow(
  {
    name: "answerWithRag",
    inputSchema: z.object({
      question: z.string(),
      topK: z.number().optional(),
      model: z.string().optional(),
    }),
    outputSchema: z.object({
      reply: z.string(),
      sources: z.array(z.object({
        id: z.union([z.string(), z.number()]),
        score: z.number(),
        payload: z.record(z.any()).optional(),
      })),
    }),
  },
  async ({ question, topK, model }) => {
    // 1) embed de la question
    const [queryVec] = await embedInBatches([question], 1, 0);

    // 2) recherche vectorielle
    const hits = await qdrantSearch(queryVec, topK ?? 5);
    const context = hits
      .map((h) => `• ${h.payload?.title ? `[${h.payload.title}] ` : ""}${h.payload?.text ?? ""}`)
      .join("\n\n");

    // 3) PROMPT: un seul message user
    const prompt = [
      "Tu es un assistant concis. Réponds uniquement en t'appuyant sur le contexte fourni et tu t'appelles euler.",
      context ? `\nContexte:\n${context}\n` : "\n(Contexte vide)\n",
      `Question: ${question}`,
      "\nSi l'information n'est pas dans le contexte, dis que tu ne sais pas."
    ].join("\n");

    const messages = [{ role: "user" as const, content: prompt }];

    const chat = await chatClient.chat.completions.create({
      model: model ?? process.env.APERTUS_MODEL_ID!,
      messages,
      temperature: 0.2,
    });

    return {
      reply: chat.choices?.[0]?.message?.content ?? "",
      sources: hits.map((h) => ({ id: h.id, score: h.score, payload: h.payload })),
    };
  }
);
export const answerWithRagFn = onCallGenkit({ cors: true }, answerWithRag);