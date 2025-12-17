// functions/src/rag/qdrant.ts

// named dense vector to match hybrid collection schema
export type QdrantPoint = { id: string | number; vector: { dense: number[] }; payload?: any };

// collection already created with hybrid schema; only verify existence
export async function qdrantEnsureCollection(_: number) {
  const base = process.env.QDRANT_URL!;
  const key = process.env.QDRANT_API_KEY!;
  const coll = process.env.QDRANT_COLLECTION!;
  const res = await fetch(`${base}/collections/${coll}`, { headers: { "api-key": key } });
  if (!res.ok) {
    const t = await res.text().catch(() => "");
    throw new Error(`Qdrant collection "${coll}" not found: ${res.status} ${t}`);
  }
}

export async function qdrantUpsert(points: QdrantPoint[]) {
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
export const MAX_CANDIDATES = 24;
export const MAX_DOCS = 3;
export const MAX_PER_DOC = 2;
export const CONTEXT_BUDGET = 1600;
export const SNIPPET_LIMIT = 600;

// gates
export const SCORE_GATE = 0.35; // top score must be >= to use context
export const SMALL_TALK = /^(hi|hello|salut|yo|coucou|hey|who are you|ça va|tu vas bien|bonjour|bonsoir)\b/i;

// --- RRF fusion ---
export function rrfFuse<T extends { id: any }>(
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
export async function qdrantSearchDenseNamed(
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
export async function qdrantSearchSparseText(
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
export async function qdrantSearchHybrid(
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

