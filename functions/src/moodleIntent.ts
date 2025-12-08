// functions/src/moodleIntent.ts
// Moodle intent detection and handling

import {
  detectMoodleIntent,
  MOODLE_INTENT_CONFIGS,
} from "./intentDetection";

/**
 * Supported Moodle intent types that can be detected.
 * Maps to intentId in the generic intent detection system.
 *
 * @see intentDetection.ts for the detection engine
 */
export type MoodleIntentType = "fetch_file";

/**
 * Result of Moodle intent detection.
 */
export interface MoodleIntentDetectionResult {
  /** Whether a Moodle file fetch intent was detected */
  moodle_intent_detected: boolean;
  /** The type of intent detected (null if none) */
  moodle_intent: MoodleIntentType | null;
  /** The matched pattern (for debugging, optional) */
  matched_pattern?: string;
}

/**
 * Core detection function for Moodle file fetch intent.
 * Uses the generic detection system under the hood.
 *
 * @param question - The user's question/message
 * @returns Detection result with intent type if detected
 */
export function detectMoodleFileFetchIntentCore(question: string): MoodleIntentDetectionResult {
  const result = detectMoodleIntent(question);

  return {
    moodle_intent_detected: result.detected,
    moodle_intent: result.intentId as MoodleIntentType | null,
    matched_pattern: result.matchedPattern,
  };
}

/**
 * Extracts file type, number, course name, and week from the user's message.
 * This is the second layer detection - determines what type of file (lecture/homework/solution),
 * extracts the file number, course name, and week if mentioned.
 *
 * @param question - The user's message
 * @returns Object with fileType, fileNumber, courseName (optional), and week (optional), or null
 */
export function extractFileInfo(question: string): {
  fileType: "lecture" | "homework" | "homework_solution";
  fileNumber: string | null;
  courseName: string | null;
  week: number | null;
} | null {
  const trimmed = question.trim();

  // 1) Always extract a number if present (first numeric token)
  let number: string | null = null;
  const numMatch = trimmed.match(/\b(\d+)\b/);
  if (numMatch) number = numMatch[1];

  // 2) Detect file type by keywords (priority: solution > homework > lecture)
  const isSolution = /\b(solution|correction|corrigé)\b/i.test(trimmed);
  const isHomework =
    /\b(devoir|homework|home\s*work|travail|serie|série)\b/i.test(trimmed);
  const isLecture = /\b(lecture|leçon|lesson|cours)\b/i.test(trimmed);

  let fileType: "lecture" | "homework" | "homework_solution" = "lecture";
  if (isSolution) fileType = "homework_solution";
  else if (isHomework) fileType = "homework";
  else if (isLecture) fileType = "lecture";

  // 3) Extract course name (simple heuristics; keep existing broad patterns)
  let courseName: string | null = null;

  // Pattern A: "from/of/de/du ... <name>"
  const fromOfPattern =
    /\b(from|of|du|de|dans|in|sur)\s+(?:the\s+)?([a-z][a-z\s-]{2,}?)(?:\s+(?:course|class|matiere|matériel|moodle))?/i;
  const fromOfMatch = trimmed.match(fromOfPattern);
  if (fromOfMatch) {
    courseName = fromOfMatch[fromOfMatch.length - 1]?.trim() || null;
    if (courseName) {
      courseName = courseName
        .replace(/\s+(course|class|matiere|matériel|moodle|sur|on)$/i, "")
        .trim();
    }
  }

  // Pattern B: "cours <name>" / "course <name>"
  if (!courseName) {
    const courseClassPattern =
      /\b(?:le\s+|la\s+)?(cours|course|class|matiere|matériel)\s+(?:de\s+|du\s+)?([a-z][a-z\s-]+?)(?:\s+sur|\s+from|\s+of|\s+du|\s+de|$)/i;
    const courseClassMatch = trimmed.match(courseClassPattern);
    if (courseClassMatch) {
      courseName = courseClassMatch[courseClassMatch.length - 1]?.trim() || null;
      if (courseName) {
        courseName = courseName.replace(/\s+(sur\s+)?moodle$/i, "").trim();
      }
    }
  }

  // Pattern C: after file keywords
  if (!courseName) {
    const afterFilePattern =
      /\b(?:lecture|leçon|lesson|devoir|homework|home\s*work|travail|solution|correction|corrigé|cours)(?:\s+\d+)?\s+(?:de\s+|du\s+)?([a-z][a-z\s-]{2,})(?:\s+(?:sur|on|moodle)|$)/i;
    const afterFileMatch = trimmed.match(afterFilePattern);
    if (afterFileMatch) {
      const potentialCourse = afterFileMatch[afterFileMatch.length - 1]?.trim();
      const commonWords =
        /\b(from|of|the|me|my|get|fetch|show|week|semaine|sur|on|moodle|donne|donner)\b/i;
      if (potentialCourse && !commonWords.test(potentialCourse) && potentialCourse.length > 2) {
        courseName = potentialCourse.replace(/\s+(sur\s+)?moodle$/i, "").trim();
      }
    }
  }

  // Pattern D: standalone capitalized phrase
  if (!courseName) {
    const standalonePattern = /\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*(?:\s*-\s*[a-z\s]+)?)\b/;
    const standaloneMatch = trimmed.match(standalonePattern);
    if (standaloneMatch) {
      const potential = standaloneMatch[1]?.trim();
      const excludeWords =
        /^(Fetch|Get|Show|Display|Download|I|Want|Need|Would|Like|The|From|Of|Week|Semaine|Donne|Donner)/i;
      if (potential && !excludeWords.test(potential)) {
        courseName = potential;
      }
    }
  }

  // Week not used for matching anymore; keep null
  const week: number | null = null;

  return {
    fileType,
    fileNumber: number,
    courseName,
    week,
  };
}

// Re-export for convenience
export { MOODLE_INTENT_CONFIGS, detectMoodleIntent };
