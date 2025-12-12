// functions/src/edIntent.ts
// Backwards-compatible wrapper around the new generic intent detection system

import {
  detectEdIntent,
  ED_INTENT_CONFIGS,
} from "./intentDetection";
import { ED_INTENT_PROMPT_BUILDERS } from "./edIntentPrompts";

/**
 * Supported ED intent types that can be detected.
 * Maps to intentId in the generic intent detection system.
 * 
 * @see intentDetection.ts for the detection engine
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
 * Uses the generic detection system under the hood.
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
 * Builds the prompt for Apertus when an ED intent is detected.
 * Uses the prompt builder for the specific intent type.
 *
 * @param question - The user's original message
 * @param intentType - The detected intent type (optional, defaults to post_question)
 * @returns Prompt string for Apertus
 */
export function buildEdIntentPromptForApertus(
  question: string,
  intentType: EdIntentType = "post_question"
): string {
  const promptBuilder = ED_INTENT_PROMPT_BUILDERS[intentType] || ED_INTENT_PROMPT_BUILDERS.post_question;
  return promptBuilder(question);
}

/**
 * Result of ED fetch intent detection.
 */
export interface EdFetchIntentDetectionResult {
  /** Whether an ED fetch intent was detected */
  ed_fetch_intent_detected: boolean;
  /** The query string to use for fetching (null if not detected) */
  ed_fetch_query: string | null;
}

/**
 * Core detection function for ED fetch intent.
 * Uses the generic detection system under the hood.
 *
 * @param question - The user's question/message
 * @returns Detection result with fetch query if detected
 */
export function detectFetchFromEdIntentCore(
  question: string,
): EdFetchIntentDetectionResult {
  const result = detectEdIntent(question);

  // Check if the detected intent is "fetch_question"
  if (result.detected && result.intentId === "fetch_question") {
    return {
      ed_fetch_intent_detected: true,
      ed_fetch_query: question.trim(),
    };
  }

  return {
    ed_fetch_intent_detected: false,
    ed_fetch_query: null,
  };
}

// Re-export for convenience
export { ED_INTENT_CONFIGS, detectEdIntent };
export { parseEdPostResponse } from "./edIntentParser";
