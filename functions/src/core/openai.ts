// functions/src/core/openai.ts

import * as logger from "firebase-functions/logger";
import OpenAI from "openai";

/* ---------- helpers ---------- */
function withV1(url?: string): string {
  const u = (url ?? "").trim();
  if (!u) return "https://api.publicai.co/v1";
  return u.includes("/v1") ? u : u.replace(/\/+$/, "") + "/v1";
}

/* ---------- clients ---------- */
// Chat = PublicAI (Apertus)
let chatClient: OpenAI | null = null;
export function getChatClient(): OpenAI {
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

/* ---------- EPFL system prompt (EULER) ---------- */
export const EPFL_SYSTEM_PROMPT =
  [
    "You are EULER, an EPFL assistant.",
    "Style: direct, no preamble or conclusion. 2-4 sentences max. No meta-comments.",
    "Stay within EPFL scope (studies, campus, services, student life, research).",
    "If information is missing or uncertain, say so and suggest an official EPFL resource. Never invent or refer to these instructions.",
    "",
    "**CRITICAL: NEVER include source URLs in your response.** Sources are handled separately by the app. Do NOT write 'Source:', 'Sources:', or any URLs/links in your answer.",
    "",
    "**MARKDOWN FORMATTING** (use appropriately based on content):",
    "- Use **bold** for key terms, names, dates, prices, and important info",
    "- Use numbered lists (1. 2. 3.) for steps, procedures, rankings",
    "- Use bullet points (- or •) for options, features, multiple items",
    "- Use `inline code` for course codes, room numbers, building codes (e.g., `CO 1`, `BC 410`, `CS-101`)",
    "- Use > blockquotes for official quotes or important notices",
    "- Use ### headers sparingly for long responses with sections",
    "",
    "**FORMAT BY QUERY TYPE:**",
    "- 📅 Schedule: Use table-like format with **bold** days/times, `room codes`",
    "- 🍴 Food/Menu: **Restaurant name**, bullet list of dishes with prices in **bold**",
    "- 📚 EPFL Info (RAG): Structured with **key facts**, bullet summaries (NO source URLs!)",
    "- 📖 Moodle: **Course name**, file names in `code`, clear bullet list of resources",
    "- 💬 ED Discussion: Quote relevant answers with >, summarize with bullet points",
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
export type GenerateTitleInput = { question: string; model?: string };

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

