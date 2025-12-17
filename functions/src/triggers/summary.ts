// functions/src/triggers/summary.ts

import * as logger from "firebase-functions/logger";
import admin from "firebase-admin";
import { db } from "../core/firebase";
import { getChatClient } from "../core/llm";
import OpenAI from "openai";

export type ChatTurn = { role: "user" | "assistant"; content: string };

export function clampText(s: string | undefined | null, max = 1200): string {
  const t = (s ?? "").toString();
  if (t.length <= max) return t;
  return t.slice(0, max);
}

export async function buildRollingSummary({
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

/**
 * Handler for onMessageCreate Firestore trigger
 * This function is called by the trigger in index.ts
 */
export async function onMessageCreateHandler(
  snap: admin.firestore.DocumentSnapshot,
  ctx: { params: Record<string, string> }
) {
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
}

