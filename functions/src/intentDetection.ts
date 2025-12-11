// functions/src/intentDetection.ts
// Generic intent detection system - modular and extensible

/**
 * Configuration for a detectable intent
 */
export interface IntentConfig {
  /** Unique identifier for the intent (e.g., "post_question", "fetch_posts") */
  id: string;
  /** Regex patterns that trigger this intent */
  matchPatterns: RegExp[];
  /** Regex patterns that block this intent (e.g., questions about ED) */
  blockPatterns?: RegExp[];
}

/**
 * Result of intent detection
 */
export interface IntentDetectionResult {
  detected: boolean;
  intentId: string | null;
  matchedPattern?: string;
}

/**
 * Generic detection function - DO NOT MODIFY
 * Iterates through all configured intents and returns the first match
 *
 * @param input - The user's input string
 * @param configs - Array of intent configurations to check
 * @returns Detection result with intent ID if matched
 */
export function detectIntent(
  input: string,
  configs: IntentConfig[]
): IntentDetectionResult {
  const trimmed = input.trim();

  for (const config of configs) {
    // Skip if a blockPattern matches
    if (config.blockPatterns?.some((p) => p.test(trimmed))) {
      continue;
    }

    // Check matchPatterns
    const matched = config.matchPatterns.find((p) => p.test(trimmed));
    if (matched) {
      return {
        detected: true,
        intentId: config.id,
        matchedPattern: matched.source,
      };
    }
  }

  return {
    detected: false,
    intentId: null,
  };
}

// ============================================================
// INTENT REGISTRY - ADD NEW FUNCTIONS HERE
// ============================================================

/**
 * Block patterns - questions ABOUT ED (not action intents)
 * These patterns prevent false positives when users ask about ED itself
 * Supports both French and English
 */
const ED_BLOCK_PATTERNS: RegExp[] = [
  // French patterns
  /\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?)\b.*\bed\b/i,
  /\bcomment\s+(marche|fonctionne|utiliser?)\b.*\bed\b/i,
  /\b(où|ou)\s+(trouver?|est)\b.*\bed\b/i,
  /\bexplique[rz]?\b.*\bed\b/i,
  // English patterns
  /\bwhat\s+is\b.*\bed\b/i,
  /\bhow\s+(does|do|to)\s+(use|work)\b.*\bed\b/i,
  /\bwhere\s+(is|can\s+i\s+find)\b.*\bed\b/i,
  /\bexplain\b.*\bed\b/i,
  /\btell\s+me\s+about\b.*\bed\b/i,
];

/**
 * Block patterns - questions ABOUT Moodle (not action intents)
 * These patterns prevent false positives when users ask about Moodle itself
 * Supports both French and English
 */
const MOODLE_BLOCK_PATTERNS: RegExp[] = [
  // French patterns
  /\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?)\b.{0,50}\bmoodle\b/i,
  /\bcomment\s+(marche|fonctionne|utiliser?)\b.{0,50}\bmoodle\b/i,
  /\b(où|ou)\s+(trouver?|est)\b.{0,50}\bmoodle\b/i,
  /\bexplique[rz]?\b.{0,50}\bmoodle\b/i,
  // English patterns
  /\bwhat\s+is\b.{0,50}\bmoodle\b/i,
  /\bhow\s+(does|do|to)\s+(use|work)\b.{0,50}\bmoodle\b/i,
  /\bwhere\s+(is|can\s+i\s+find)\b.{0,50}\bmoodle\b/i,
  /\bexplain\b.{0,50}\bmoodle\b/i,
  /\btell\s+me\s+about\b.{0,50}\bmoodle\b/i,
];

/**
 * Registry of all ED intents
 * ADD NEW FUNCTIONS HERE
 * Supports both French and English patterns
 */
export const ED_INTENT_CONFIGS: IntentConfig[] = [
  // ===== POST TO ED =====
  {
    id: "post_question",
    matchPatterns: [
      // French patterns
      /\b(post[eé]?[rz]?|publi[eé]?[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(met[st]?[rz]?|mettre|ajoute[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(envoie[rz]?|envoyer)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(pose[rz]?|demande[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\bpartage[rz]?\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(cr[ée]{1,2}[rz]?|faire?|fais)\b.*\b(post|thread|discussion|sujet)\b.*\b(sur|à|a)\s+(ed|edstem)/i,
      /\b(je\s+(veux|voudrais|souhaite)|peux[- ]tu|tu\s+peux)\b.*\b(post[eé]?[rz]?|publi[eé]?[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem)/i,
      // English patterns
      /\b(post|publish|share|send)\b.*\b(on|to)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(put|add|submit)\b.*\b(on|to)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(ask|create)\b.*\b(on|to)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(make|create|start)\b.*\b(a\s+)?(post|thread|discussion|question)\b.*\b(on|to)\s+(ed|edstem)/i,
      /\b(can\s+you|could\s+you|please)\b.*\b(post|publish|share|send)\b.*\b(on|to)\s+(ed|edstem)/i,
      /\b(i\s+(want|need|would\s+like))\s+(to\s+)?(post|publish|share|send)\b.*\b(on|to)\s+(ed|edstem)/i,
      // Direct "post this on ED" style
      /\bpost\s+this\b.*\b(on|to)\s+(ed|edstem)/i,
      /\bshare\s+this\b.*\b(on|to)\s+(ed|edstem)/i,
    ],
    blockPatterns: ED_BLOCK_PATTERNS,
  },
    // ===== PLACEHOLDER: FETCH ED POSTS =====

];

/**
 * Registry of all Moodle intents
 *
 * First layer: General Moodle fetch detection - catches "fetch me the...", "get me the...", etc.
 * This determines if the request is for Moodle (vs RAG/schedule/ED) early on.
 */
export const MOODLE_INTENT_CONFIGS: IntentConfig[] = [
  // ===== GENERAL MOODLE FETCH INTENT =====
  // This is the first layer detection - catches any "fetch me the..." type request
  // The specific file type (lecture/homework/solution) is determined in the second layer
  // Note: These patterns are intentionally broad to catch various phrasings. False positives
  // are filtered out in the second layer (moodleIntent.ts) which requires Moodle-specific
  // keywords (lecture, homework, solution, cours, etc.) or explicit "moodle" mentions.
  {
    id: "fetch_file",
    matchPatterns: [
      // English patterns - require file-related context or explicit moodle mention
      /\b(fetch|get|show|display|download|retrieve|bring)\s+(me\s+)?(the\s+)?(lecture|homework|solution|file|document|cours|moodle)/i,
      /\b(fetch|get|show|display|download|retrieve|bring)\s+(me\s+)?(the\s+).*\bmoodle\b/i,
      /\b(i\s+)?(want|need|would\s+like)\s+(to\s+)?(fetch|get|see|view|download|retrieve)\s+(the\s+)?(lecture|homework|solution|file|document|cours|moodle)/i,
      // French patterns - require file-related context or explicit moodle mention
      /\b(donne|donner?|récupérer?|obtenir?|télécharger?|charger?|voir|afficher?|montrer?|ouvrir?|chercher?|trouver?)\s+(moi\s+)?(le\s+|la\s+|les\s+)?(cours|devoir|lecture|fichier|document|moodle)/i,
      /\b(donne|donner?|récupérer?|obtenir?|télécharger?|charger?|voir|afficher?|montrer?|ouvrir?|chercher?|trouver?)\s+(moi\s+)?(le\s+|la\s+|les\s+)?.*\bmoodle\b/i,
      /\b(je\s+)?(veux|voudrais|souhaite|ai\s+besoin)\s+(de\s+)?(récupérer?|obtenir?|voir|afficher?|télécharger?|donner?)\s+(le\s+|la\s+|les\s+)?(cours|devoir|lecture|fichier|document|moodle)/i,
      // Patterns that include file type keywords (lecture, homework, etc.)
      /\b(lecture|leçon|lesson|devoir|homework|home\s*work|travail|solution|correction|corrigé|cours)\s+(\d+|[a-z])/i,
      // Patterns with "cours" (course) and "semaine" (week) - common French patterns
      /\b(cours|leçon|lecture|devoir|homework)\s+(semaine|week)\s+\d+/i,
      /\b(semaine|week)\s+\d+\s+(de|du|of)\s+/i,
      // Pattern for "sur moodle" (on moodle) - explicit Moodle request
      /\b(sur|on|from)\s+moodle/i,
    ],
    blockPatterns: MOODLE_BLOCK_PATTERNS,
  },
];

// MAIN FUNCTION TO USE

/**
 * Detects an ED intent in the user's message
 *
 * @param question - The user's message
 * @returns Detection result with the detected intent
 */
export function detectEdIntent(question: string): IntentDetectionResult {
  return detectIntent(question, ED_INTENT_CONFIGS);
}

/**
 * Detects a Moodle intent in the user's message
 *
 * @param question - The user's message
 * @returns Detection result with the detected intent
 */
export function detectMoodleIntent(question: string): IntentDetectionResult {
  return detectIntent(question, MOODLE_INTENT_CONFIGS);
}
