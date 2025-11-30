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
 */
const ED_BLOCK_PATTERNS: RegExp[] = [
  /\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?)\b.*\bed\b/i,
  /\bcomment\s+(marche|fonctionne|utiliser?)\b.*\bed\b/i,
  /\b(où|ou)\s+(trouver?|est)\b.*\bed\b/i,
  /\bexplique[rz]?\b.*\bed\b/i,
];

/**
 * Registry of all ED intents
 * ADD NEW FUNCTIONS HERE
 */
export const ED_INTENT_CONFIGS: IntentConfig[] = [
  // ===== POST TO ED =====
  {
    id: "post_question",
    matchPatterns: [
      /\b(post[eé]?[rz]?|publi[eé]?[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(met[st]?[rz]?|mettre|ajoute[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(envoie[rz]?|envoyer)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(pose[rz]?|demande[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\bpartage[rz]?\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
      /\b(cr[ée]{1,2}[rz]?|faire?|fais)\b.*\b(post|thread|discussion|sujet)\b.*\b(sur|à|a)\s+(ed|edstem)/i,
      /\b(je\s+(veux|voudrais|souhaite)|peux[- ]tu|tu\s+peux)\b.*\b(post[eé]?[rz]?|publi[eé]?[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem)/i,
    ],
    blockPatterns: ED_BLOCK_PATTERNS,
  },
    // ===== PLACEHOLDER: FETCH ED POSTS =====

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
