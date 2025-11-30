// functions/src/edIntent.ts
// Backwards-compatible wrapper around the new generic intent detection system

import {
  detectEdIntent,
  ED_INTENT_CONFIGS,
} from "./intentDetection";

/**
 * Supported ED intent types that Apertus can detect.
 * Maps to intentId in the new system.
 * 
 * TODO: Add here when implemented:
 * - "fetch_posts"
 */
export type EdIntentType = "post_question";

/**
 * Result of ED intent detection.
 * Backwards-compatible with existing code.
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
 * Core detection function for ED posting intent.
 * Uses the new generic detection system under the hood.
 *
 * @param question - The user's question/message
 * @returns Detection result with intent type if detected
 */
export function detectPostToEdIntentCore(question: string): EdIntentDetectionResult {
  const result = detectEdIntent(question);

  return {
    ed_intent_detected: result.detected,
    ed_intent: result.intentId as EdIntentType | null,
    matched_pattern: result.matchedPattern,
  };
}

/**
 * Response templates for each intent type.
 * TODO: Add here when implemented:
 * - fetch_posts
 */
const INTENT_RESPONSES: Record<EdIntentType, string[]> = {
  post_question: [
    "J'ai détecté que vous souhaitez poster une question sur ED Discussion.",
    "",
    "Pour poster sur ED, assurez-vous que :",
    "1. Votre connecteur ED est configuré dans les paramètres",
    "2. Vous avez sélectionné le cours approprié",
    "",
    "Voulez-vous que je vous aide à formuler votre question pour ED ?",
  ],
};

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
  const response = INTENT_RESPONSES[intent];
  if (response) {
    return response.join("\n");
  }
  return "J'ai détecté une intention liée à ED Discussion.";
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
 * Prompt templates for Apertus based on intent type.
 * TODO: Add here when implemented:
 * - fetch_posts
*/
const INTENT_PROMPTS: Record<EdIntentType, string[]> = {
  post_question: [
    "L'utilisateur souhaite poster sur ED Discussion.",
    "Tu as détecté cette intention dans son message.",
    "Pour l'instant, tu ne peux PAS encore publier sur ED - cette fonctionnalité est en cours de développement.",
    "Réponds naturellement pour lui expliquer que tu as compris son intention mais que tu ne peux pas encore le faire.",
    "Sois encourageant et dis-lui que cette fonctionnalité arrive bientôt.",
  ],
};

/**
 * Builds the prompt for Apertus when an ED intent is detected.
 * Used in the hybrid approach: REGEX detects, Apertus responds naturally.
 *
 * @param question - The user's original message
 * @param intentType - The detected intent type (optional, defaults to post_question)
 * @returns Prompt string for Apertus
 */
export function buildEdIntentPromptForApertus(
  question: string,
  intentType: EdIntentType = "post_question"
): string {
  const basePrompt = INTENT_PROMPTS[intentType] || INTENT_PROMPTS.post_question;
  return [...basePrompt, "", `Message de l'utilisateur: "${question}"`].join("\n");
}

// Re-export for convenience
export { ED_INTENT_CONFIGS, detectEdIntent };
