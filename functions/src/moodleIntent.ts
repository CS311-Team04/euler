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
 * Maps ordinal words to numbers
 */
const ORDINAL_MAP: Record<string, string> = {
  first: "1", "1st": "1",
  second: "2", "2nd": "2",
  third: "3", "3rd": "3",
  fourth: "4", "4th": "4",
  fifth: "5", "5th": "5",
  sixth: "6", "6th": "6",
  seventh: "7", "7th": "7",
  eighth: "8", "8th": "8",
  ninth: "9", "9th": "9",
  tenth: "10", "10th": "10",
  eleventh: "11", "11th": "11",
  twelfth: "12", "12th": "12",
  // French ordinals
  premier: "1", première: "1", "1er": "1", "1ère": "1",
  deuxième: "2", "2ème": "2", "2e": "2",
  troisième: "3", "3ème": "3", "3e": "3",
  quatrième: "4", "4ème": "4", "4e": "4",
  cinquième: "5", "5ème": "5", "5e": "5",
  sixième: "6", "6ème": "6", "6e": "6",
  septième: "7", "7ème": "7", "7e": "7",
  huitième: "8", "8ème": "8", "8e": "8",
  neuvième: "9", "9ème": "9", "9e": "9",
  dixième: "10", "10ème": "10", "10e": "10",
};

/**
 * Converts ordinal word to number string
 */
function ordinalToNumber(word: string): string | null {
  return ORDINAL_MAP[word.toLowerCase()] || null;
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

  // 1) Extract number closest to file type keywords (lecture/homework/solution)
  // Supports both French and English keywords, including ordinals
  let number: string | null = null;
  
  // Ordinal patterns (first, second, 1st, 2nd, premier, deuxième, etc.)
  const ordinalPattern = /\b(first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth|1st|2nd|3rd|4th|5th|6th|7th|8th|9th|10th|11th|12th|premier|première|1er|1ère|deuxième|2ème|2e|troisième|3ème|3e|quatrième|4ème|4e|cinquième|5ème|5e|sixième|septième|huitième|neuvième|dixième)\b/i;
  
  // Try to find ordinal near file type keywords
  const ordinalMatch = trimmed.match(ordinalPattern);
  if (ordinalMatch) {
    number = ordinalToNumber(ordinalMatch[1]);
  }
  
  // Try to find number near file type keywords first (numeric)
  if (!number) {
    const lectureNumMatch = trimmed.match(/\b(lecture|lectures|leçon|lesson|cours|course|slide|slides|class)\s*(\d+)/i);
    const homeworkNumMatch = trimmed.match(/\b(devoir|homework|home\s*work|travail|serie|série|assignment|exercise)\s*(\d+)/i);
    const solutionNumMatch = trimmed.match(/\b(solution|solutions|correction|corrigé|answer|answers)\s*(\d+)/i);
    
    if (solutionNumMatch) {
      number = solutionNumMatch[2];
    } else if (homeworkNumMatch) {
      number = homeworkNumMatch[2];
    } else if (lectureNumMatch) {
      number = lectureNumMatch[2];
    }
  }
  
  // Try number before file type (e.g., "5 lecture", "first course")
  if (!number) {
    const numBeforeTypeMatch = trimmed.match(/\b(\d+)\s*(lecture|leçon|lesson|cours|course|slide|slides|class|devoir|homework|exercise)/i);
    if (numBeforeTypeMatch) {
      number = numBeforeTypeMatch[1];
    }
  }
  
  // Fallback: extract first standalone number
  if (!number) {
    const numMatch = trimmed.match(/\b(\d+)\b/);
    if (numMatch) number = numMatch[1];
  }

  // 2) Detect file type by keywords (priority: solution > homework > lecture)
  // Supports both French and English keywords
  const isSolution = /\b(solution|solutions|correction|corrigé|answer|answers)\b/i.test(trimmed);
  const isHomework =
    /\b(devoir|homework|home\s*work|travail|serie|série|assignment|exercise|exercises|exercice|exercices)\b/i.test(trimmed);
  const isLecture = /\b(lecture|lectures|leçon|lesson|cours|course|slide|slides|notes|class\s*notes|class)\b/i.test(trimmed);

  let fileType: "lecture" | "homework" | "homework_solution" = "lecture";
  if (isSolution) fileType = "homework_solution";
  else if (isHomework) fileType = "homework";
  else if (isLecture) fileType = "lecture";

  // 3) Extract course name - improved patterns
  let courseName: string | null = null;

  // Pattern NEW: "<verb> <course_name> <ordinal/number> <type>"
  // e.g., "retrieve algebra first course", "get calculus 5 lecture"
  const verbCourseOrdinalPattern = /\b(fetch|get|show|retrieve|download|find|open|donne|donner|récupérer)\s+([a-z][a-z\s-]{2,}?)\s+(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th|\d+)\s*(lecture|course|cours|lesson|class|homework|exercise|slides?)/i;
  const verbCourseOrdinalMatch = trimmed.match(verbCourseOrdinalPattern);
  if (verbCourseOrdinalMatch) {
    courseName = verbCourseOrdinalMatch[2]?.trim() || null;
  }

  // Pattern NEW2: "<verb> <ordinal/number> <course_name> <type>"
  // e.g., "retrieve first algebra lecture", "get 5 calculus course"
  if (!courseName) {
    const verbOrdinalCoursePattern = /\b(fetch|get|show|retrieve|download|find|open|donne|donner|récupérer)\s+(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th|\d+)\s+([a-z][a-z\s-]{2,}?)\s*(lecture|course|cours|lesson|class|homework|exercise|slides?)/i;
    const verbOrdinalCourseMatch = trimmed.match(verbOrdinalCoursePattern);
    if (verbOrdinalCourseMatch) {
      courseName = verbOrdinalCourseMatch[3]?.trim() || null;
    }
  }

  // Pattern NEW3: "<course_name> <type> <number>"
  // e.g., "algebra lecture 1", "calculus course 5"
  if (!courseName) {
    const courseTypeNumPattern = /\b([a-z][a-z\s-]{2,}?)\s+(lecture|course|cours|lesson|class|homework|exercise|slides?)\s*(\d+|first|second|third|fourth|fifth)/i;
    const courseTypeNumMatch = trimmed.match(courseTypeNumPattern);
    if (courseTypeNumMatch) {
      const potential = courseTypeNumMatch[1]?.trim();
      const excludeWords = /\b(fetch|get|show|retrieve|download|find|open|the|me|my|donne|donner|récupérer|moi|le|la|les)\b/i;
      if (potential && !excludeWords.test(potential) && potential.length > 2) {
        courseName = potential;
      }
    }
  }

  // Pattern A: "from/of/de/du ... <name>"
  if (!courseName) {
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
        /\b(from|of|the|me|my|get|fetch|show|week|semaine|sur|on|moodle|donne|donner|first|second|third|fourth|fifth)\b/i;
      if (potentialCourse && !commonWords.test(potentialCourse) && potentialCourse.length > 2) {
        courseName = potentialCourse.replace(/\s+(sur\s+)?moodle$/i, "").trim();
      }
    }
  }

  // Pattern D: standalone capitalized phrase (limited to prevent ReDoS)
  if (!courseName) {
    // Limit to max 5 words and 100 chars to prevent ReDoS attacks
    const maxLength = 100;
    if (trimmed.length <= maxLength) {
      const standalonePattern = /\b([A-Z][a-z]+(?:\s+[A-Z][a-z]+){0,4}(?:\s*-\s*[a-z\s]{0,30})?)\b/;
      const standaloneMatch = trimmed.match(standalonePattern);
      if (standaloneMatch) {
        const potential = standaloneMatch[1]?.trim();
        if (potential && potential.length > 2 && potential.length <= 50) {
          courseName = potential;
        }
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
