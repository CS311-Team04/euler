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

