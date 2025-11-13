"use strict";
// functions/src/index.ts
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.generateTitleFn = exports.answerWithRagHttp = exports.indexChunksHttp = exports.answerWithRagFn = exports.indexChunksFn = exports.ping = void 0;
exports.apertusChatFnCore = apertusChatFnCore;
exports.generateTitleCore = generateTitleCore;
exports.indexChunksCore = indexChunksCore;
exports.answerWithRagCore = answerWithRagCore;
const node_path_1 = __importDefault(require("node:path"));
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config({ path: node_path_1.default.join(__dirname, "..", ".env") });
const logger = __importStar(require("firebase-functions/logger"));
const openai_1 = __importDefault(require("openai"));
const node_crypto_1 = require("node:crypto");
const functions = __importStar(require("firebase-functions/v1"));
/* ---------- helpers ---------- */
function withV1(url) {
    const u = (url ?? "").trim();
    if (!u)
        return "https://api.publicai.co/v1";
    return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}
function isUuid(s) {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s);
}
function isUintString(s) {
    return /^\d+$/.test(s);
}
function normalizePointId(id) {
    if (typeof id === "number")
        return id;
    const s = String(id);
    if (isUintString(s))
        return Number(s);
    if (isUuid(s))
        return s;
    return (0, node_crypto_1.randomUUID)();
}
/* ---------- clients ---------- */
// Chat = PublicAI (Apertus)
const chatClient = new openai_1.default({
    apiKey: process.env.OPENAI_API_KEY,
    baseURL: withV1(process.env.OPENAI_BASE_URL),
    defaultHeaders: { "User-Agent": "euler-mvp/1.0 (genkit-functions)" },
});
// Embeddings = Jina
const EMBED_URL = withV1(process.env.EMBED_BASE_URL) + "/embeddings";
const EMBED_KEY = process.env.EMBED_API_KEY;
const EMBED_MODEL = process.env.EMBED_MODEL_ID; // e.g. "jina-embeddings-v3"
/* =========================================================
 *                         CHAT (optional)
 * ======================================================= */
async function apertusChatFnCore({ messages, model, temperature, }) {
    const resp = await chatClient.chat.completions.create({
        model: model ?? process.env.APERTUS_MODEL_ID,
        messages,
        temperature: temperature ?? 0.2,
    });
    const reply = resp.choices?.[0]?.message?.content ?? "";
    logger.info("apertusChat success", { length: reply.length });
    return { reply };
}
async function generateTitleCore({ question, model }) {
    const prompt = [
        "Generate a concise conversation title (4-5 words max, no trailing punctuation).",
        `User's first question: "${question.trim()}"`,
        "Return ONLY the title.",
    ].join("\n");
    // réutilise le même client Apertus
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
// collection is already created with hybrid schema; only verify existence
async function qdrantEnsureCollection(_) {
    const base = process.env.QDRANT_URL;
    const key = process.env.QDRANT_API_KEY;
    const coll = process.env.QDRANT_COLLECTION;
    const res = await fetch(`${base}/collections/${coll}`, { headers: { "api-key": key } });
    if (!res.ok) {
        const t = await res.text().catch(() => "");
        throw new Error(`Qdrant collection "${coll}" not found or not accessible: ${res.status} ${t}`);
    }
}
async function qdrantUpsert(points) {
    const res = await fetch(`${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points?wait=true`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY },
        body: JSON.stringify({ points }),
    });
    if (!res.ok) {
        const t = await res.text().catch(() => "");
        throw new Error(`Qdrant upsert failed: ${res.status} ${t}`);
    }
}
// retrieval knobs
const MAX_CANDIDATES = 24; // wider pool for better recall
const MAX_DOCS = 3; // distinct sources to include
const MAX_PER_DOC = 2; // chunks per source
const CONTEXT_BUDGET = 1600; // total chars across all chunks
const SNIPPET_LIMIT = 600; // per-chunk truncate
// --- RRF fusion helper ---
function rrfFuse(dense, sparse, k = 60) {
    const map = new Map();
    const add = (arr) => {
        arr.forEach((it) => {
            const key = String(it.id);
            const inc = 1.0 / (k + it._rank + 1);
            const cur = map.get(key);
            if (cur)
                cur.score += inc;
            else
                map.set(key, { item: it, score: inc });
        });
    };
    add(dense);
    add(sparse);
    return [...map.values()]
        .sort((a, b) => b.score - a.score)
        .map(v => ({ ...v.item, score: v.score }));
}
// --- dense-only search for NAMED vector schema ---
async function qdrantSearchDenseNamed(denseVec, topK = 24, filter) {
    const body = {
        vector: { name: "dense", vector: denseVec, params: { hnsw_ef: 48 } },
        limit: topK,
        with_payload: true,
        with_vector: false,
    };
    if (filter)
        body.filter = filter;
    const r = await fetch(`${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY },
        body: JSON.stringify(body),
    });
    if (!r.ok)
        throw new Error(`dense search failed: ${r.status} ${await r.text()}`);
    const j = await r.json();
    // annotate rank so RRF is stable
    return (j.result ?? []).map((h, i) => ({ ...h, _rank: i }));
}
// --- sparse-only search using query text (needs sparse_vectors in collection) ---
async function qdrantSearchSparseText(queryText, topK = 24, filter) {
    const body = {
        sparse: { name: "sparse", text: queryText },
        limit: topK,
        with_payload: true,
        with_vector: false,
    };
    if (filter)
        body.filter = filter;
    const r = await fetch(`${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY },
        body: JSON.stringify(body),
    });
    if (!r.ok)
        throw new Error(`sparse search failed: ${r.status} ${await r.text()}`);
    const j = await r.json();
    return (j.result ?? []).map((h, i) => ({ ...h, _rank: i }));
}
// --- hybrid with safe fallback (NO /points/query) ---
async function qdrantSearchHybrid(denseVec, queryText, topK = 24, filter) {
    // run dense always
    const dense = await qdrantSearchDenseNamed(denseVec, topK, filter);
    // try sparse; if it fails (older Qdrant or missing sparse_vectors), fall back to dense-only
    let sparse = [];
    try {
        sparse = await qdrantSearchSparseText(queryText, topK, filter);
    }
    catch {
        return dense; // dense-only fallback
    }
    const fused = rrfFuse(dense, sparse, 60);
    return fused.slice(0, topK);
}
/* =========================================================
 *            EMBEDDINGS UTIL
 * ======================================================= */
const MAX_CHARS = 8000;
function sanitizeTexts(arr) {
    return arr
        .map((s) => (typeof s === "string" ? s : String(s ?? "")))
        .map((s) => s.trim())
        .map((s) => (s.length > MAX_CHARS ? s.slice(0, MAX_CHARS) : s))
        .filter((s) => s.length > 0);
}
async function embedBatch(texts) {
    const clean = sanitizeTexts(texts);
    if (clean.length === 0)
        throw new Error("No valid texts to embed.");
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
    return (json.data ?? []).map((d) => d.embedding);
}
async function sleep(ms) { return new Promise((res) => setTimeout(res, ms)); }
async function embedInBatches(allTexts, batchSize = 16, pauseMs = 150) {
    const out = [];
    for (let i = 0; i < allTexts.length; i += batchSize) {
        const slice = allTexts.slice(i, i + batchSize);
        if (i > 0 && pauseMs > 0)
            await sleep(pauseMs);
        const vecs = await embedBatch(slice);
        out.push(...vecs);
    }
    return out;
}
async function indexChunksCore({ chunks }) {
    if (!chunks?.length)
        return { count: 0, dim: 0 };
    const texts = chunks.map((c) => {
        const parts = [];
        if (c.title)
            parts.push(`Title: ${c.title}`);
        const section = c.payload?.section ?? c.payload?.SECTION ?? c.payload?.Section;
        if (section)
            parts.push(`Section: ${section}`);
        parts.push(c.text);
        return parts.join("\n\n");
    });
    const vectors = await embedInBatches(texts, 16, 150);
    const dim = vectors[0].length;
    await qdrantEnsureCollection(dim);
    const points = vectors.map((v, i) => ({
        id: normalizePointId(chunks[i].id),
        vector: { dense: v }, // named dense vector
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
async function answerWithRagCore({ question, topK, model }) {
    const [queryVec] = await embedInBatches([question], 1, 0);
    // server-side hybrid search
    const raw = await qdrantSearchHybrid(queryVec, question, Math.max(topK ?? 0, MAX_CANDIDATES));
    // group by source (url first, fallback title)
    const groups = new Map();
    for (const h of raw) {
        const key = (h.payload?.url || h.payload?.title || String(h.id)).toString();
        if (!groups.has(key))
            groups.set(key, []);
        const arr = groups.get(key);
        if (arr.length < MAX_PER_DOC)
            arr.push(h);
    }
    // assemble diversified context under a strict budget
    const orderedGroups = Array.from(groups.values())
        .sort((a, b) => (b[0].score ?? 0) - (a[0].score ?? 0))
        .slice(0, MAX_DOCS);
    let budget = CONTEXT_BUDGET;
    const chosen = [];
    for (const g of orderedGroups) {
        for (const h of g) {
            const title = h.payload?.title;
            const url = h.payload?.url;
            const txt = String(h.payload?.text ?? "").slice(0, SNIPPET_LIMIT);
            const snippet = txt;
            if (snippet.length + 50 > budget)
                continue;
            chosen.push({ title, url, text: snippet, score: h.score ?? 0 });
            budget -= (snippet.length + 50);
        }
    }
    // numbered context
    const context = chosen.map((c, i) => {
        const head = c.title ? `[${i + 1}] ${c.title}` : `[${i + 1}]`;
        const src = c.url ? `\nSource: ${c.url}` : "";
        return `${head}\n${c.text}${src}`;
    }).join("\n\n");
    const prompt = [
        "Tu es un assistant concis. Utilise le contexte fourni (plusieurs extraits) et cite les indices [1], [2], etc.",
        context ? `\nContexte:\n${context}\n` : "\n(Contexte vide)\n",
        `Question: ${question}`,
        "Si l'information n'est pas dans le contexte, dis que tu ne sais pas.",
    ].join("\n");
    const chat = await chatClient.chat.completions.create({
        model: model ?? process.env.APERTUS_MODEL_ID,
        messages: [{ role: "user", content: prompt }],
        temperature: 0.2,
        max_tokens: 400,
    });
    return {
        reply: chat.choices?.[0]?.message?.content ?? "",
        sources: chosen.map((c, i) => ({ idx: i + 1, title: c.title, url: c.url, score: c.score })),
    };
}
/* =========================================================
 *                EXPORTS: callable + HTTP twins
 * ======================================================= */
// ping
exports.ping = functions.https.onRequest((_req, res) => {
    res.status(200).send("pong");
});
// callable (for Kotlin via Firebase SDK)
exports.indexChunksFn = functions.https.onCall(async (data) => {
    const { chunks } = data || {};
    if (!Array.isArray(chunks) || chunks.length === 0) {
        throw new functions.https.HttpsError("invalid-argument", "Missing 'chunks'");
    }
    return await indexChunksCore({ chunks });
});
exports.answerWithRagFn = functions.https.onCall(async (data) => {
    const question = String(data?.question || "").trim();
    const topK = Number(data?.topK ?? 5);
    const model = data?.model;
    if (!question)
        throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");
    return await answerWithRagCore({ question, topK, model });
});
// HTTP endpoints (for Python)
const INDEX_API_KEY = process.env.INDEX_API_KEY || ""; // set in functions/.env
function checkKey(req) {
    if (!INDEX_API_KEY)
        return; // no key configured -> open (dev only)
    if (req.get("x-api-key") !== INDEX_API_KEY) {
        const e = new Error("unauthorized");
        e.code = 401;
        throw e;
    }
}
exports.indexChunksHttp = functions.https.onRequest(async (req, res) => {
    try {
        if (req.method !== "POST") {
            res.status(405).end();
            return;
        }
        checkKey(req);
        const out = await indexChunksCore(req.body);
        res.status(200).json(out);
        return;
    }
    catch (e) {
        res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
        return;
    }
});
exports.answerWithRagHttp = functions.https.onRequest(async (req, res) => {
    try {
        if (req.method !== "POST") {
            res.status(405).end();
            return;
        }
        checkKey(req);
        const out = await answerWithRagCore(req.body);
        res.status(200).json(out);
        return;
    }
    catch (e) {
        res.status(e.code === 401 ? 401 : 400).json({ error: String(e) });
        return;
    }
});
exports.generateTitleFn = functions.https.onCall(async (data) => {
    const q = String(data?.question || "").trim();
    const model = data?.model;
    if (!q)
        throw new functions.https.HttpsError("invalid-argument", "Missing 'question'");
    return await generateTitleCore({ question: q, model });
});
//# sourceMappingURL=index.js.map