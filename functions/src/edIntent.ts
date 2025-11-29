// functions/src/edIntent.ts
// Detection logic for ED Discussion posting intents

/**
 * Supported ED intent types that Apertus can detect.
 */
export type EdIntentType = "post_question" | "post_answer" | "post_comment";

/**
 * Result of ED intent detection.
 */
export interface EdIntentDetectionResult {
  /** Whether an ED posting intent was detected */
  ed_intent_detected: boolean;
  /** The type of intent detected (null if none) */
  ed_intent: EdIntentType | null;
  /** The matched pattern (for debugging, optional) */
  matched_pattern?: string;
}

/**
 * Regex patterns to detect intent to post on ED Discussion.
 * 
 * These patterns match phrases like:
 * - "post sur ed" / "poste sur ed"
 * - "poste ça sur ED"
 * - "publie sur ed"
 * - "mets ça sur ed" / "met ça sur ed"
 * - "envoie sur ed"
 * - "pose cette question sur ed"
 * - "ajoute sur ed"
 * - "partage sur ed"
 * 
 * These patterns should NOT match:
 * - "c'est quoi ED?" (asking about ED)
 * - "comment marche ED?" (asking how ED works)
 * - "où trouver ED?" (asking where to find ED)
 */
const ED_POST_PATTERNS: Array<{ pattern: RegExp; intent: EdIntentType }> = [
  // Direct "post/poste/poster" patterns
  {
    pattern: /\b(post[eé]?[rz]?|publi[eé]?[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "mets/met/mettre ça sur ed"
  {
    pattern: /\b(met[st]?[rz]?|mettre|ajoute[rz]?)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "envoie/envoyer sur ed"
  {
    pattern: /\b(envoie[rz]?|envoyer)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "pose cette question sur ed"
  {
    pattern: /\b(pose[rz]?|demande[rz]?)\b.*\b(question|message)?\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "partage sur ed"
  {
    pattern: /\bpartage[rz]?\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "créer un post/thread sur ed"
  {
    pattern: /\b(cr[ée]{1,2}[rz]?|faire?|fais)\b.*\b(post|thread|discussion|sujet|fil)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // Imperative with "sur ed" at the end: "poste-le sur ed"
  {
    pattern: /\b(post[eé]|publi[eé]|met[st]|ajoute|envoie|partage)[- ]?(le|la|les|ça|cela|ceci)?\s+(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
  // "je veux poster sur ed"
  {
    pattern: /\b(je\s+(veux|voudrais|souhaite|aimerais)|peux[- ]tu|tu\s+peux)\b.*\b(post[eé]?[rz]?|publi[eé]?[rz]?|mettre|envoyer)\b.*\b(sur|à|a)\s+(ed|edstem|ed\s*discussion)/i,
    intent: "post_question",
  },
];

/**
 * Patterns that indicate the user is asking ABOUT ED, not trying to post.
 * These should block detection even if a post pattern matches.
 */
const ED_QUESTION_PATTERNS: RegExp[] = [
  /\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?|qu['']?est[- ]ce\s+qu['']?|que\s+signifie|que\s+veut\s+dire)\b.*\bed\b/i,
  /\bcomment\s+(marche|fonctionne|utiliser?|accéder)\b.*\bed\b/i,
  /\b(où|ou)\s+(trouver?|est|se\s+trouve)\b.*\bed\b/i,
  /\bed\b.*\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?)\b/i,
  /\bexplique[rz]?\b.*\bed\b/i,
];

/**
 * Core detection function for ED posting intent.
 * Uses REGEX patterns for instant (~50ms) detection without LLM calls.
 * 
 * @param question - The user's question/message
 * @returns Detection result with intent type if detected
 */
export function detectPostToEdIntentCore(question: string): EdIntentDetectionResult {
  const trimmedQuestion = question.trim();

  // First, check if this is a question ABOUT ED (not a posting intent)
  for (const blockPattern of ED_QUESTION_PATTERNS) {
    if (blockPattern.test(trimmedQuestion)) {
      return {
        ed_intent_detected: false,
        ed_intent: null,
        matched_pattern: undefined,
      };
    }
  }

  // Check for posting intent patterns
  for (const { pattern, intent } of ED_POST_PATTERNS) {
    if (pattern.test(trimmedQuestion)) {
      return {
        ed_intent_detected: true,
        ed_intent: intent,
        matched_pattern: pattern.source,
      };
    }
  }

  // No intent detected
  return {
    ed_intent_detected: false,
    ed_intent: null,
    matched_pattern: undefined,
  };
}

/**
 * Generates an appropriate response when an ED intent is detected.
 * 
 * @param intent - The detected intent type
 * @param originalQuestion - The user's original message
 * @returns A user-friendly response message
 */
export function generateEdIntentResponse(
  intent: EdIntentType,
  _originalQuestion: string
): string {
  switch (intent) {
    case "post_question":
      return [
        "J'ai détecté que vous souhaitez poster une question sur ED Discussion.",
        "",
        "Pour poster sur ED, assurez-vous que :",
        "1. Votre connecteur ED est configuré dans les paramètres",
        "2. Vous avez sélectionné le cours approprié",
        "",
        "Voulez-vous que je vous aide à formuler votre question pour ED ?",
      ].join("\n");

    case "post_answer":
      return [
        "J'ai détecté que vous souhaitez répondre à une discussion sur ED.",
        "",
        "Pour répondre sur ED, assurez-vous que votre connecteur ED est configuré.",
      ].join("\n");

    case "post_comment":
      return [
        "J'ai détecté que vous souhaitez commenter sur ED Discussion.",
        "",
        "Pour commenter sur ED, assurez-vous que votre connecteur ED est configuré.",
      ].join("\n");

    default:
      return "J'ai détecté une intention liée à ED Discussion.";
  }
}

/**
 * Full ED intent detection result with response, ready for answerWithRagFn integration.
 */
export interface EdIntentFullResult extends EdIntentDetectionResult {
  /** Pre-generated reply if intent was detected */
  reply?: string;
}

/**
 * Combined detection and response generation.
 * Call this from answerWithRagFn for seamless integration.
 * 
 * @param question - The user's question/message
 * @returns Full result with detection and optional reply
 */
export function detectAndRespondToEdIntent(question: string): EdIntentFullResult {
  const detection = detectPostToEdIntentCore(question);

  if (detection.ed_intent_detected && detection.ed_intent) {
    return {
      ...detection,
      reply: generateEdIntentResponse(detection.ed_intent, question),
    };
  }

  return detection;
}

/**
 * Builds the prompt for Apertus when an ED intent is detected.
 * Used in the hybrid approach: REGEX detects, Apertus responds naturally.
 * 
 * @param question - The user's original message
 * @returns Prompt string for Apertus
 */
export function buildEdIntentPromptForApertus(question: string): string {
  return [
    "L'utilisateur souhaite poster sur ED Discussion.",
    "Tu as détecté cette intention dans son message.",
    "Pour l'instant, tu ne peux PAS encore publier sur ED - cette fonctionnalité est en cours de développement.",
    "Réponds naturellement pour lui expliquer que tu as compris son intention mais que tu ne peux pas encore le faire.",
    "Sois encourageant et dis-lui que cette fonctionnalité arrive bientôt.",
    "",
    `Message de l'utilisateur: "${question}"`,
  ].join("\n");
}
