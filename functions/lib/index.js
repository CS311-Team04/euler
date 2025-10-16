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
exports.answerWithRagHttp = exports.indexChunksHttp = exports.answerWithRagFn = exports.indexChunksFn = exports.ping = void 0;
exports.apertusChatFnCore = apertusChatFnCore;
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
async function qdrantEnsureCollection(dimension) {
    const base = process.env.QDRANT_URL;
    const key = process.env.QDRANT_API_KEY;
    const coll = process.env.QDRANT_COLLECTION;
    const res = await fetch(`${base}/collections/${coll}`, { headers: { "api-key": key } });
    if (res.ok)
        return;
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
async function qdrantSearch(vector, topK = 5) {
    const r = await fetch(`${process.env.QDRANT_URL}/collections/${process.env.QDRANT_COLLECTION}/points/search`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "api-key": process.env.QDRANT_API_KEY },
        body: JSON.stringify({ vector, limit: topK, with_payload: true }),
    });
    if (!r.ok) {
        const t = await r.text().catch(() => "");
        throw new Error(`Qdrant search failed: ${r.status} ${t}`);
    }
    const j = await r.json();
    return (j.result ?? []);
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
    const texts = chunks.map((c) => c.text);
    const vectors = await embedInBatches(texts, 16, 150);
    const dim = vectors[0].length;
    await qdrantEnsureCollection(dim);
    const points = vectors.map((v, i) => ({
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
async function answerWithRagCore({ question, topK, model }) {
    const [queryVec] = await embedInBatches([question], 1, 0);
    const hits = await qdrantSearch(queryVec, topK ?? 5);
    const context = hits
        .map((h) => `• ${h.payload?.title ? `[${h.payload.title}] ` : ""}${h.payload?.text ?? ""}`)
        .join("\n\n");
    const prompt = [
        "Tu es un assistant concis. Réponds uniquement en t'appuyant sur le contexte fourni et tu t'appelles euler.",
        context ? `\nContexte:\n${context}\n` : "\n(Contexte vide)\n",
        `Question: ${question}`,
        "\nSi l'information n'est pas dans le contexte, dis que tu ne sais pas.",
    ].join("\n");
    const chat = await chatClient.chat.completions.create({
        model: model ?? process.env.APERTUS_MODEL_ID,
        messages: [{ role: "user", content: prompt }],
        temperature: 0.2,
    });
    return {
        reply: chat.choices?.[0]?.message?.content ?? "",
        sources: hits.map((h) => ({ id: h.id, score: h.score, payload: h.payload })),
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
//# sourceMappingURL=index.js.map