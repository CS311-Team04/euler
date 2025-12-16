// functions/src/rag/embeddings.ts

import * as logger from "firebase-functions/logger";

// Helper function to normalize URLs (imported from core/openai.ts pattern)
function withV1(url?: string): string {
  const u = (url ?? "").trim();
  if (!u) return "https://api.publicai.co/v1";
  return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}

// Embeddings = Jina
const EMBED_URL = withV1(process.env.EMBED_BASE_URL) + "/embeddings";
const EMBED_KEY = process.env.EMBED_API_KEY!;
export const EMBED_MODEL = process.env.EMBED_MODEL_ID!; // e.g. "jina-embeddings-v3"

const MAX_CHARS = 8000;

export function sanitizeTexts(arr: string[]): string[] {
  return arr
    .map((s) => (typeof s === "string" ? s : String(s ?? "")))
    .map((s) => s.trim())
    .map((s) => (s.length > MAX_CHARS ? s.slice(0, MAX_CHARS) : s))
    .filter((s) => s.length > 0);
}

export async function embedBatch(texts: string[]): Promise<number[][]> {
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

export async function embedInBatches(allTexts: string[], batchSize = 16, pauseMs = 150): Promise<number[][]> {
  const out: number[][] = [];
  for (let i = 0; i < allTexts.length; i += batchSize) {
    const slice = allTexts.slice(i, i + batchSize);
    if (i > 0 && pauseMs > 0) await sleep(pauseMs);
    const vecs = await embedBatch(slice);
    out.push(...vecs);
  }
  return out;
}

