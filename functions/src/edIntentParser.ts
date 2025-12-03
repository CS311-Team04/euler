// functions/src/edIntentParser.ts
// Parser for extracting formatted questions and titles from Apertus responses

/**
 * Result of parsing an ED post response from Apertus.
 */
export interface ParsedEdPostResponse {
  /** Clean reply text without technical markers */
  reply: string;
  /** Formatted question ready for ED post (with politeness formulas) */
  formattedQuestion: string | null;
  /** Contextual title (3-4 words max) */
  formattedTitle: string | null;
}

/**
 * Regex patterns for extracting formatted content from Apertus responses.
 */
const FORMATTED_QUESTION_PATTERN = /FORMATTED_QUESTION:\s*([^\n]+(?:\n(?!FORMATTED_|REPONSE:)[^\n]+)*)/i;
const FORMATTED_TITLE_PATTERN = /FORMATTED_TITLE:\s*([^\n]+)/i;
const REPONSE_PREFIX_PATTERN = /^REPONSE:\s*/im;

/**
 * Parses Apertus response to extract formatted question and title.
 * Handles multiple formats and edge cases.
 * 
 * @param rawReply - The full reply from Apertus
 * @returns Parsed response with clean reply, formatted question, and title
 */
export function parseEdPostResponse(rawReply: string): ParsedEdPostResponse {
  const formattedQuestionMatch = rawReply.match(FORMATTED_QUESTION_PATTERN);
  const formattedTitleMatch = rawReply.match(FORMATTED_TITLE_PATTERN);
  
  const formattedQuestion = formattedQuestionMatch?.[1]?.trim() || null;
  const formattedTitle = formattedTitleMatch?.[1]?.trim() || null;
  
  // Clean reply by removing technical markers
  let cleanReply = rawReply;
  if (formattedQuestionMatch) {
    cleanReply = cleanReply.replace(/FORMATTED_QUESTION:.*?(?=\nFORMATTED_TITLE:|\nREPONSE:|$)/gis, "");
  }
  if (formattedTitleMatch) {
    cleanReply = cleanReply.replace(/FORMATTED_TITLE:.*?(?:\n|$)/gi, "");
  }
  cleanReply = cleanReply.replace(REPONSE_PREFIX_PATTERN, "").trim();
  
  // Fallback: use cleanReply as formattedQuestion if reasonable length
  const finalFormattedQuestion = formattedQuestion || (cleanReply.length > 0 && cleanReply.length < 500 ? cleanReply : null);
  
  return {
    reply: cleanReply || rawReply,
    formattedQuestion: finalFormattedQuestion,
    formattedTitle: formattedTitle,
  };
}
