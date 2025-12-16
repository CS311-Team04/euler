// functions/src/rag/rag.ts

import * as logger from "firebase-functions/logger";
import OpenAI from "openai";
import { embedInBatches } from "./embeddings";
import { qdrantSearchHybrid, MAX_CANDIDATES, MAX_DOCS, MAX_PER_DOC, CONTEXT_BUDGET, SNIPPET_LIMIT, SCORE_GATE, SMALL_TALK } from "./qdrant";
import { getScheduleContextCore } from "../features/schedule";
import { answerFoodQuestionCore } from "../features/food";
import { getChatClient, EPFL_SYSTEM_PROMPT } from "../core/openai";

export type AnswerWithRagInput = {
  question: string;
  topK?: number;
  model?: string;
  summary?: string;
  recentTranscript?: string;
  profileContext?: string;
  uid?: string; // Optional: for schedule context
};

// Schedule-related question patterns (French + English)
const SCHEDULE_PATTERNS = /\b(horaire|schedule|timetable|cours|class|lecture|planning|agenda|calendrier|calendar|demain|tomorrow|aujourd'hui|today|cette semaine|this week|prochaine|next|quand|when|heure|time|salle|room|où|where|leçon|lesson|what['']?s\s+on|my\s+classes|my\s+lectures|my\s+schedule)\b/i;

// Food-related question patterns - split into strong (always trigger) and weak (need context)
// Strong indicators: EPFL restaurant names, explicit eating phrases (French + English)
const FOOD_STRONG_PATTERNS = /\b(alpine|arcadie|esplanade|espla|native|ornithorynque|orni|piano|qu'est-ce qu'on mange|il y a quoi à manger|ya quoi|y'a quoi|où manger|quoi manger|on mange quoi|what['']?s\s+for\s+(lunch|dinner|breakfast)|what\s+can\s+i\s+eat|where\s+to\s+eat|food\s+today|menus?\s+today|what['']?s\s+on\s+the\s+menu|cafeteria|canteen)\b/i;

// Diet queries phrased as "y'a quoi de veggie ..." (captures apostrophes) + English equivalents
const FOOD_DIET_QUERY = /\b(y['']?\s*a\s+quoi|ya\s+quoi|qu'est-ce qu'on|on\s+mange\s+quoi|quoi\s+(de|d')|what['']?s|is\s+there|are\s+there|any)[^.]{0,50}\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b/i;

// Weak indicators: only trigger if combined with food context words (French + English)
const FOOD_WEAK_PATTERNS = /\b(manger|menu|plat|repas|resto|restaurant|nourriture|déjeuner|dîner|cantine|cafétéria|eat|eating|lunch|dinner|breakfast|meal|meals|food|cafeteria|canteen|dish|dishes)\b/i;

// Diet-related patterns: trigger only with eating/restaurant context (French + English)
const FOOD_DIET_PATTERNS = /\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b.*\b(manger|plat|resto|restaurant|menu|repas|cantine|midi|soir|déjeuner|dîner|eat|lunch|dinner|meal|food|cafeteria|canteen)\b|\b(manger|plat|resto|restaurant|menu|repas|cantine|midi|soir|déjeuner|dîner|eat|lunch|dinner|meal|food|cafeteria|canteen)\b.*\b(végétarien|végé|veggie|vegan|vegetarian|plant[- ]based)\b/i;

// Price patterns: only for food when combined with eating context (French + English)
const FOOD_PRICE_PATTERNS = /\b(moins cher|cheapest|pas cher|prix|cheap|under|less\s+than|affordable|budget)\b.*\b(manger|plat|resto|restaurant|menu|repas|midi|soir|déjeuner|dîner|cantine|eat|lunch|dinner|meal|food|cafeteria)\b|\b(manger|plat|resto|restaurant|menu|repas|midi|soir|déjeuner|dîner|cantine|eat|lunch|dinner|meal|food|cafeteria)\b.*\b(moins cher|cheapest|pas cher|prix|cheap|under|less\s+than|affordable|budget)\b/i;

/** Check if question is food-related using refined patterns */
function isFoodRelatedQuestion(q: string): boolean {
  // Strong patterns always trigger
  if (FOOD_STRONG_PATTERNS.test(q)) return true;
  // "y'a quoi de veggie ..." style queries
  if (FOOD_DIET_QUERY.test(q)) return true;
  // Diet patterns with eating context
  if (FOOD_DIET_PATTERNS.test(q)) return true;
  // Price patterns with eating context  
  if (FOOD_PRICE_PATTERNS.test(q)) return true;
  // Weak patterns only if the question is clearly about eating (not just contains "menu" or "restaurant")
  // Must have at least 2 food-related words or a clear eating phrase (French + English)
  if (FOOD_WEAK_PATTERNS.test(q)) {
    const foodWords = q.toLowerCase().match(/\b(manger|menu|plat|repas|resto|restaurant|nourriture|déjeuner|dîner|cantine|cafétéria|midi|soir|aujourd'hui|demain|eat|eating|lunch|dinner|breakfast|meal|food|cafeteria|canteen|today|tomorrow)\b/gi);
    return (foodWords?.length ?? 0) >= 2;
  }
  return false;
}

export async function answerWithRagCore({
  question, topK, model, summary, recentTranscript, profileContext, uid, client,
}: AnswerWithRagInput & { client?: OpenAI }) {
  const q = question.trim();
  const tStart = Date.now();
  let timeScheduleMs = 0;
  let timeRetrievalMs = 0;
  let timePromptMs = 0;
  let timeLLMMs = 0;

  const shortQuestion = q.length <= 80;
  const tinyQuestion = q.length <= 40;
  const envTopK = process.env.APERTUS_TOPK ? Number(process.env.APERTUS_TOPK) : undefined;
  const effectiveTopK = topK ?? envTopK ?? (tinyQuestion ? 8 : shortQuestion ? 12 : 18);
  const effectiveMaxDocs = shortQuestion ? Math.max(1, MAX_DOCS - 1) : MAX_DOCS;
  const contextBudget = shortQuestion ? Math.min(CONTEXT_BUDGET, 1100) : CONTEXT_BUDGET;
  const snippetLimit = shortQuestion ? Math.min(SNIPPET_LIMIT, 450) : SNIPPET_LIMIT;
  const summaryBudget = shortQuestion ? 0 : 1200;
  const transcriptBudget = shortQuestion ? 0 : 800;
  const scheduleBudget = 900;
  const profileBudget = 900;
  const finalModel = model ?? process.env.APERTUS_MODEL_ID!;
  const finalTemperature = process.env.APERTUS_TEMPERATURE ? Number(process.env.APERTUS_TEMPERATURE) : 0.0;
  const finalMaxTokens = process.env.APERTUS_MAX_TOKENS ? Number(process.env.APERTUS_MAX_TOKENS) : 280;
  // optional rolling summary (kept concise)
  const trimmedSummary =
    summaryBudget > 0 ? (summary ?? "").toString().trim().slice(0, summaryBudget) : "";
  
  // Check if this is a food-related question FIRST (before schedule/RAG)
  const isFoodQuestion = isFoodRelatedQuestion(q);
  
  if (isFoodQuestion) {
    try {
      logger.info("answerWithRagCore.foodQuestion", { questionLen: q.length });
      const foodResult = await answerFoodQuestionCore(q);
      logger.info("answerWithRagCore.timing", {
        path: "food",
        totalMs: Date.now() - tStart,
        scheduleMs: timeScheduleMs,
        retrievalMs: timeRetrievalMs,
        promptMs: timePromptMs,
        llmMs: timeLLMMs,
      });
      return {
        reply: foodResult.reply,
        primary_url: "https://www.epfl.ch/campus/restaurants-shops-hotels/fr/offre-du-jour-de-tous-les-points-de-restauration/",
        best_score: 1.0,
        sources: [],
        source_type: "food" as const,
      };
    } catch (e) {
      logger.warn("answerWithRagCore.foodQuestionFailed", { error: String(e) });
      // Fall through to regular RAG if food query fails
    }
  }
  

  // Check if this is a schedule-related question and fetch schedule context
  let scheduleContext = "";
  const isScheduleQuestion = SCHEDULE_PATTERNS.test(q);

  if (isScheduleQuestion && uid) {
    try {
      const tScheduleStart = Date.now();
      scheduleContext = await getScheduleContextCore(uid);
      timeScheduleMs = Date.now() - tScheduleStart;
      if (scheduleContext) {
        const originalScheduleLen = scheduleContext.length;
        if (scheduleContext.length > scheduleBudget) {
          scheduleContext = scheduleContext.slice(0, scheduleBudget);
        }
        logger.info("answerWithRagCore.scheduleContext", {
          uid,
          hasSchedule: true,
          contextLen: scheduleContext.length,
          trimmed: originalScheduleLen > scheduleContext.length,
        });
      }
    } catch (e) {
      logger.warn("answerWithRagCore.scheduleContextFailed", { error: String(e) });
    }
  }

  // Gate 1: skip retrieval on small talk
  const isSmallTalk = SMALL_TALK.test(q) && q.length <= 30;
  const skipRetrieval = (isScheduleQuestion && Boolean(scheduleContext));

  let routedQuestion = q;
  const routerModel = process.env.APERTUS_ROUTER_MODEL_ID;
  if (!isSmallTalk && !skipRetrieval && routerModel) {
    try {
      const routerClient = client ?? getChatClient();
      const routerResp = await routerClient.chat.completions.create({
        model: routerModel,
        messages: [
          { role: "system", content: "Rewrite the following question into a short, explicit version for EPFL document search. Do not answer; return only the reformulated question. No pleasantries or meta." },
          { role: "user", content: q },
        ],
        temperature: 0,
        max_tokens: 64,
      });
      const routed = routerResp.choices?.[0]?.message?.content?.trim();
      if (routed) {
        routedQuestion = routed;
      }
    } catch (e) {
      logger.warn("answerWithRagCore.routerFailed", { error: String(e) });
    }
  }

  let chosen: Array<{ title?: string; url?: string; text: string; score: number }> = [];
  let bestScore = 0;

  if (!isSmallTalk && !skipRetrieval) {
    const tRetrievalStart = Date.now();
    const [queryVec] = await embedInBatches([routedQuestion], 1, 0);
    const searchK = Math.min(Math.max(effectiveTopK, 4), MAX_CANDIDATES);
    const raw = await qdrantSearchHybrid(queryVec, routedQuestion, searchK);

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
        .slice(0, effectiveMaxDocs);

      let budget = contextBudget;
      const seenChunkKeys = new Set<string>();
      for (const g of orderedGroups) {
        for (const h of g) {
          const title = h.payload?.title as string | undefined;
          const url   = h.payload?.url as string | undefined;
          const txt   = String(h.payload?.text ?? "").slice(0, snippetLimit);
          const chunkKey = `${title ?? ""}|${txt.slice(0, 160)}`;
          if (seenChunkKeys.has(chunkKey)) continue;
          if (txt.length + 50 > budget) continue;
          chosen.push({ title, url, text: txt, score: h.score ?? 0 });
          seenChunkKeys.add(chunkKey);
          budget -= (txt.length + 50);
        }
      }
    }
    timeRetrievalMs = Date.now() - tRetrievalStart;
  }

  // build context only if we kept chunks
  // Note: Source URLs are NOT included here - they're handled separately in the response
  // to prevent the LLM from embedding plain text URLs in its answer
  const context =
    chosen.length === 0
      ? ""
      : chosen
          .map((c, i) => {
            const head = c.title ? `[${i + 1}] ${c.title}` : `[${i + 1}]`;
            return `${head}\n${c.text}`;
          })
          .join("\n\n");

  // optional transcript (kept concise)
  const trimmedTranscript =
    transcriptBudget > 0 ? (recentTranscript ?? "").toString().trim().slice(0, transcriptBudget) : "";

  if (profileContext) {
    profileContext = profileContext.toString().slice(0, profileBudget);
  }

  // Fast path: schedule questions with schedule context -> minimal prompt, no RAG
  if (isScheduleQuestion && scheduleContext) {
    const tFastPromptStart = Date.now();
    
    // Extract today's day info for explicit prompting
    const nowForPrompt = new Date();
    const zurichDayStr = nowForPrompt.toLocaleString('en-US', { timeZone: 'Europe/Zurich', weekday: 'long' });
    const zurichDateStr = nowForPrompt.toLocaleDateString('en-GB', { 
      timeZone: 'Europe/Zurich', 
      weekday: 'long', 
      day: 'numeric', 
      month: 'long',
      year: 'numeric'
    });
    
    const fastPrompt = [
      `CRITICAL: Today is ${zurichDateStr}. When the user asks about "today", answer with ${zurichDayStr}'s schedule ONLY.`,
      "Answer the schedule question using only the provided timetable.",
      "IMPORTANT: For exam/final questions, ALWAYS include the FULL DATE (day, date, month, year) and time in your answer.",
      `Timetable:\n${scheduleContext}`,
      `Question: ${question}`,
    ].join("\n\n");
    timePromptMs = Date.now() - tFastPromptStart;

    const fastClient = client ?? getChatClient();
    const tLLMStartFast = Date.now();
    const scheduleSystemPrompt = [
      `You answer schedule questions. TODAY IS ${zurichDayStr.toUpperCase()}.`,
      `When user asks about "today", look for ${zurichDayStr} in the timetable.`,
      "For EXAMS/FINALS: ALWAYS include the FULL DATE (weekday + day + month + year) and time.",
      "",
      "**CRITICAL FORMATTING RULES:**",
      "- EACH class/lecture MUST be on its OWN LINE (use line breaks!)",
      "- Start each entry with a bullet point (- )",
      "- Format: - **HH:MM – HH:MM** · **Course Name** (`Room`)",
      "- Put room code in backticks at the END of each line",
      "- Add a blank line between sections if needed",
      "- End with a short summary line (e.g., 'You have 3 lectures today.')",
      "",
      "Example output:",
      "- **09:15 – 13:00** · **Software Enterprise** (`INF 019`)",
      "- **13:15 – 15:00** · **Stochastic Models** (`CE 1 2`)",
      "",
      "You have 2 lectures today.",
    ].join("\n");
    
    const fastChat = await fastClient.chat.completions.create({
      model: finalModel,
      messages: [
        { role: "system", content: scheduleSystemPrompt },
        { role: "user", content: fastPrompt },
      ],
      temperature: finalTemperature,
      max_tokens: Math.min(finalMaxTokens, 350),
    });
    timeLLMMs = Date.now() - tLLMStartFast;
    const reply = (fastChat.choices?.[0]?.message?.content ?? "").trim();

    logger.info("answerWithRagCore.timing", {
      path: "schedule-fast",
      totalMs: Date.now() - tStart,
      scheduleMs: timeScheduleMs,
      retrievalMs: timeRetrievalMs,
      promptMs: timePromptMs,
      llmMs: timeLLMMs,
      chosenCount: 0,
      contextLen: 0,
      source_type: "schedule",
    });

    return {
      reply,
      primary_url: null,
      best_score: 1.0,
      sources: [],
      source_type: "schedule",
    };
  }

  let finalRagsContext = context;
  // Build schedule-specific instructions if schedule context is present
  const scheduleInstructions = scheduleContext
    ? "Schedule: answer only using the provided timetable. Concise list response."
    : "";

  const sourcePriorityLine =
    "Sources priority: Summary > Recent window > RAG context (ignore RAG for schedule questions).";

  const tPromptStart = Date.now();
  const prompt = [
    "Respond briefly, no intro or conclusion.",
    "Format: steps -> short numbered list; otherwise 2-4 short sentences with line breaks. No meta-comments.",
    sourcePriorityLine,
    scheduleInstructions,
    trimmedSummary ? `Conversation summary (do not display as-is):\n${trimmedSummary}\n` : "",
    trimmedTranscript ? `Recent window (do not display):\n${trimmedTranscript}\n` : "",
    scheduleContext ? `User timetable (use only for schedule questions):\n${scheduleContext}\n` : "",
    (finalRagsContext && !scheduleContext) ? `RAG context:\n${finalRagsContext}\n` : "",
    `Question: ${question}`,
    !scheduleContext ? "If information is not provided, say you don't know." : "",
  ].filter(Boolean).join("\n\n");

  logger.info("answerWithRagCore.context", {
    chosenCount: chosen.length,
    contextLen: context.length,
    trimmedSummaryLen: trimmedSummary.length,
    hasTranscript: Boolean(trimmedTranscript),
    transcriptLen: trimmedTranscript.length,
    hasProfileContext: Boolean(profileContext),
    profileContextLen: profileContext ? profileContext.length : 0,
    titles: chosen.map(c => c.title).filter(Boolean).slice(0, 5),
    summaryHead: trimmedSummary.slice(0, 120),
    profileContextHead: profileContext ? profileContext.slice(0, 120) : null,
    routerUsed: routedQuestion !== q,
    routedQuestionHead: routedQuestion.slice(0, 120),
  });

  const summaryUsageRules =
    [
      "Sources (priority): 1) Summary, 2) Recent window, 3) RAG context.",
      "Schedule questions: use only the provided timetable.",
      "If info is missing or uncertain: say so and suggest a reliable EPFL source.",
    ].join("\n");

  const profileContextSection = ""; // personal/profile handling removed

  const systemContent = [
    EPFL_SYSTEM_PROMPT,
    summaryUsageRules,
    trimmedSummary
      ? "Conversation summary to consider (do not display as-is):\n" + trimmedSummary
      : "",
    trimmedTranscript
      ? "Recent window (do not display; useful for immediate references):\n" + trimmedTranscript
      : "",
    scheduleContext
      ? "User EPFL schedule (use for questions about schedule, courses, rooms):\n" + scheduleContext
      : "",
    profileContextSection,
  ]
    .filter(Boolean)
    .join("\n\n");

  timePromptMs = Date.now() - tPromptStart;

  // Log system content to verify profile context is included (structured logging only)
  logger.info("answerWithRagCore.systemContent", {
    systemContentLen: systemContent.length,
    containsPriorityContext: systemContent.includes("PRIORITY USER CONTEXT"),
    containsProfileData: profileContext ? systemContent.includes(profileContext.slice(0, 50)) : false,
  });

  const activeClient = client ?? getChatClient();
  const tLLMStart = Date.now();
  const chat = await activeClient.chat.completions.create({
    model: finalModel,
    messages: [
      { role: "system", content: systemContent },
      { role: "user", content: prompt },
    ],
    temperature: finalTemperature,
    max_tokens: finalMaxTokens,
  });
  timeLLMMs = Date.now() - tLLMStart;

  const rawReply = chat.choices?.[0]?.message?.content ?? "";
  const reply = rawReply.replace(/\s*USED_CONTEXT=(YES|NO)\s*$/i, "").trim();

  // Determine source type: schedule takes priority over RAG for schedule questions
  const usedSchedule = isScheduleQuestion && scheduleContext.length > 0;
  const usedRag = chosen.length > 0 && !usedSchedule;

  // source_type: "schedule" | "rag" | "none"
  let source_type: "schedule" | "rag" | "none" = "none";
  if (usedSchedule) {
    source_type = "schedule";
  } else if (usedRag) {
    source_type = "rag";
  }

  // Only expose a URL when using RAG (not schedule)
  const primary_url = usedRag ? (chosen[0]?.url ?? null) : null;

  logger.info("answerWithRagCore.timing", {
    totalMs: Date.now() - tStart,
    scheduleMs: timeScheduleMs,
    retrievalMs: timeRetrievalMs,
    promptMs: timePromptMs,
    llmMs: timeLLMMs,
    chosenCount: chosen.length,
    contextLen: context.length,
    source_type,
  });

  return {
    reply,
    primary_url,
    best_score: bestScore,
    sources: usedRag ? chosen.map((c, i) => ({ idx: i + 1, title: c.title, url: c.url, score: c.score })) : [],
    source_type, // "schedule", "rag", or "none"
  };
}

