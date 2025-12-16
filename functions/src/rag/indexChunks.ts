// functions/src/rag/indexChunks.ts

import { randomUUID } from "node:crypto";
import * as logger from "firebase-functions/logger";
import { embedInBatches } from "./embeddings";
import { qdrantEnsureCollection, qdrantUpsert } from "./qdrant";

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

export type IndexChunk = { id: string; text: string; title?: string; url?: string; payload?: any };
export type IndexChunksInput = { chunks: IndexChunk[] };

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

