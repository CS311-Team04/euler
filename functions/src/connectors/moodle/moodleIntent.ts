// functions/src/connectors/moodle/moodleIntent.ts
// Intent detection for Moodle file retrieval queries

import { IntentConfig, detectIntent } from "../../intentDetection";

/**
 * Supported Moodle intent types
 */
export type MoodleIntentType = "get_file" | "list_courses";

/**
 * Result of Moodle intent detection with extracted parameters
 */
export interface MoodleIntentDetectionResult {
  /** Whether a Moodle intent was detected */
  moodle_intent_detected: boolean;
  /** The type of intent detected */
  moodle_intent: MoodleIntentType | null;
  /** Extracted course name/query */
  course_query?: string;
  /** Extracted file descriptor (e.g., "1st lecture", "week 2 slides") */
  file_query?: string;
  /** The matched pattern (for debugging) */
  matched_pattern?: string;
}

/**
 * Block patterns - questions ABOUT Moodle (not action intents)
 */
const MOODLE_BLOCK_PATTERNS: RegExp[] = [
  /\b(c['']?est\s+quoi|qu['']?est[- ]ce\s+que?)\b.*\bmoodle\b/i,
  /\bcomment\s+(marche|fonctionne|utiliser?|connecter?)\b.*\bmoodle\b/i,
  /\b(où|ou)\s+(trouver?|est)\b.*\bmoodle\b/i,
  /\bexplique[rz]?\b.*\bmoodle\b/i,
];

/**
 * Patterns for file retrieval intents
 * 
 * Examples of matching queries:
 * - "Retrieve the 1st lecture for the probability and statistics course"
 * - "Get me the slides from analysis 1"
 * - "Download the PDF from week 2 of linear algebra"
 * - "Trouve-moi le cours de physique semaine 3"
 * - "Récupère le fichier du cours de programmation"
 */
const FILE_RETRIEVAL_PATTERNS: RegExp[] = [
  // English patterns
  /\b(retrieve|get|download|fetch|find|give\s+me|show\s+me)\b.+\b(lecture|cours|slide|pdf|file|document|exercise|td|tp).+\b(from|for|of|in)\s+(.+?)\s*(course|class|module)?$/i,
  /\b(retrieve|get|download|fetch|find|give\s+me|show\s+me)\b.+\b(course|class|module)\b/i,
  /\b(retrieve|get|download|fetch|find)\b.+\b(1st|2nd|3rd|4th|5th|first|second|third|fourth|fifth)\b.+\b(lecture|cours|slide|file)/i,
  /\b(lecture|slide|pdf|file|document)\b.+\b(from|for|of|in)\s+\w+.*(course|class|moodle)?/i,
  /\b(get|retrieve|download|fetch)\s+(the\s+)?(pdf|file|lecture|slides?)\b/i,
  
  // French patterns
  /\b(récupère|récupérer|trouve|trouver|donne|donner|montre|montrer|télécharge|télécharger)\b.+\b(cours|lecture|fichier|document|exercice|td|tp|slides?|pdf)\b/i,
  /\b(récupère|récupérer|trouve|trouver|donne|donner)\b.+\bde\s+(.+?)\s*(cours)?$/i,
  /\b(le|la|les)\s+(cours|fichier|pdf|document|lecture)\b.+\bde\s+/i,
  
  // Direct file requests
  /\bpdf\s+(du|de\s+la|from|of)\s+/i,
  /\bfichier\s+(du|de\s+la|from|of)\s+/i,
  
  // Week/Lecture number patterns
  /\b(week|semaine)\s*\d+\b.+\b(course|cours|class)/i,
  /\b(lecture|cours)\s*\d+\b/i,
];

/**
 * Patterns for listing courses
 */
const LIST_COURSES_PATTERNS: RegExp[] = [
  /\b(list|show|display|what\s+are)\b.+\b(my\s+)?(courses?|classes?|modules?)/i,
  /\b(liste|montre|affiche|quels?\s+sont)\b.+\b(mes\s+)?(cours|classes?|modules?)/i,
  /\bmy\s+moodle\s+courses?\b/i,
  /\bmes\s+cours\s+(sur\s+)?moodle\b/i,
];

/**
 * Registry of Moodle intents
 */
export const MOODLE_INTENT_CONFIGS: IntentConfig[] = [
  {
    id: "get_file",
    matchPatterns: FILE_RETRIEVAL_PATTERNS,
    blockPatterns: MOODLE_BLOCK_PATTERNS,
  },
  {
    id: "list_courses",
    matchPatterns: LIST_COURSES_PATTERNS,
    blockPatterns: MOODLE_BLOCK_PATTERNS,
  },
];

/**
 * Extracts course name and file descriptor from a natural language query.
 * 
 * @param query - The user's original message
 * @returns Extracted course and file information
 */
function extractQueryParameters(query: string): { courseQuery?: string; fileQuery?: string } {
  const queryLower = query.toLowerCase();
  
  // Common course name patterns
  const coursePatterns = [
    // "for the X course" / "from the X course"
    /(?:for|from|of|in)\s+(?:the\s+)?(.+?)\s*(?:course|class|module)(?:\s|$|\.)/i,
    // "du cours de X" / "de X"
    /(?:du\s+cours\s+de|de\s+la?\s*)?(\w[\w\s]+?)(?:\s+cours)?(?:\s*$|\.)/i,
    // "X course" at end
    /(.+?)\s+(?:course|class|module)(?:\s|$|\.)/i,
  ];

  let courseQuery: string | undefined;
  for (const pattern of coursePatterns) {
    const match = query.match(pattern);
    if (match && match[1]) {
      courseQuery = match[1].trim();
      // Clean up extracted course name
      courseQuery = courseQuery
        .replace(/^(the|le|la|les|du|de)\s+/i, "")
        .replace(/\s+(course|class|module|cours)$/i, "")
        .trim();
      if (courseQuery.length > 3) break;
    }
  }

  // Extract file descriptor (ordinal + type)
  const filePatterns = [
    // "1st lecture", "first slides", etc.
    /((?:1st|2nd|3rd|4th|5th|first|second|third|fourth|fifth|premier|première|deuxième|troisième)\s+(?:lecture|cours|slide|pdf|file|document|exercise|td|tp|fichier))/i,
    // "lecture 1", "week 2 slides"
    /((?:lecture|cours|slide|week|semaine)\s*\d+(?:\s+(?:slides?|pdf|file))?)/i,
    // "the lecture", "the slides", "le cours"
    /((?:the|le|la|les)\s+(?:lecture|cours|slide|pdf|file|document|fichier))/i,
  ];

  let fileQuery: string | undefined;
  for (const pattern of filePatterns) {
    const match = query.match(pattern);
    if (match && match[1]) {
      fileQuery = match[1].trim();
      break;
    }
  }

  // Default file query if we have a course but no specific file
  if (courseQuery && !fileQuery) {
    // Try to find any ordinal or type hint
    if (/\b(1st|first|premier|première)\b/i.test(queryLower)) {
      fileQuery = "1st lecture";
    } else if (/\b(2nd|second|deuxième)\b/i.test(queryLower)) {
      fileQuery = "2nd lecture";
    } else if (/\bslide/i.test(queryLower)) {
      fileQuery = "slides";
    } else if (/\bexercise|exercice|td|tp/i.test(queryLower)) {
      fileQuery = "exercise";
    } else {
      fileQuery = "lecture"; // default to lecture
    }
  }

  return { courseQuery, fileQuery };
}

/**
 * Detects a Moodle file retrieval or listing intent in the user's message.
 * 
 * @param question - The user's message
 * @returns Detection result with intent type and extracted parameters
 */
export function detectMoodleIntent(question: string): MoodleIntentDetectionResult {
  const result = detectIntent(question, MOODLE_INTENT_CONFIGS);

  if (!result.detected) {
    return {
      moodle_intent_detected: false,
      moodle_intent: null,
    };
  }

  const intentType = result.intentId as MoodleIntentType;

  // Extract parameters for file retrieval
  if (intentType === "get_file") {
    const { courseQuery, fileQuery } = extractQueryParameters(question);
    return {
      moodle_intent_detected: true,
      moodle_intent: intentType,
      course_query: courseQuery,
      file_query: fileQuery,
      matched_pattern: result.matchedPattern,
    };
  }

  return {
    moodle_intent_detected: true,
    moodle_intent: intentType,
    matched_pattern: result.matchedPattern,
  };
}

