// functions/src/edIntentPrompts.ts
// Prompt templates and configuration for ED intent handling

import { EdIntentType } from "./edIntent";

/**
 * Prompt template for generating formatted ED posts.
 * This prompt instructs Apertus to format questions with proper context and politeness.
 */
export function buildEdPostPrompt(question: string): string {
  return [
    "The user wants to post a question on ED Discussion.",
    "",
    "IMPORTANT:",
    "- DO NOT say that the feature is not available",
    "- DO NOT say it's under development",
    "- DO NOT say they need to wait",
    "- Respond as if you can help them formulate their question NOW",
    "",
    "You need to do TWO things:",
    "",
    "1. Respond naturally and INCLUDE the optimized formulation directly in your response.",
    "   Present it as a clear reformulation proposal for ED.",
    "",
    "2. Also extract the question and title in the technical format below.",
    "",
    "Instructions for the formulation:",
    "- Extract the content of their question (without 'post', 'on ed', etc.)",
    "- Formulate it clearly, concisely, and professionally",
    "- Make sure it's a real question",
    "- ADD appropriate polite phrases:",
    "  * Beginning: 'Hello', 'Hi', or 'Hey'",
    "  * End: 'Thanks in advance', 'Thanks for your help', or 'Thanks'",
    "- The question should be polite and respectful for ED Discussion",
    "- Match the language of the user's original message (French → French, English → English)",
    "",
    "Instructions for the TITLE:",
    "- The title should be a short CONTEXT (3-4 words MAX) that situates the question",
    "- Examples: 'Modstoch S7 Q5', 'Question 5 Modstoch', 'Algebra Ex 3'",
    "- It's just to quickly identify what the question is about",
    "- NOT a summary of the question, just the context (course, exercise, week, etc.)",
    "",
    "MANDATORY response format (follow this format EXACTLY):",
    "REPONSE: [your response that INCLUDES the optimized formulation]",
    "",
    "FORMATTED_QUESTION: [the formatted question - technical version, on a single line]",
    "FORMATTED_TITLE: [the title - technical version, on a single line]",
    "",
    "Examples:",
    "",
    "Example 1:",
    "If the user says: 'post on ed about question 5 of modstoch from week 7'",
    "REPONSE: Here's an optimized formulation of your question for ED Discussion:",
    "",
    "**Question 5 Modstoch S7**",
    "",
    "Hello,",
    "",
    "How do I solve question 5 from the Modstoch exercise for week 7?",
    "",
    "Thanks in advance!",
    "",
    "FORMATTED_QUESTION: Hello,\\n\\nHow do I solve question 5 from the Modstoch exercise for week 7?\\n\\nThanks in advance!",
    "FORMATTED_TITLE: Modstoch S7 Q5",
    "",
    "Example 2:",
    "If the user says: 'poste sur ed comment résoudre une équation différentielle'",
    "REPONSE: Voici une formulation optimisée de votre question pour ED Discussion:",
    "",
    "**Équations différentielles**",
    "",
    "Bonjour,",
    "",
    "Comment résoudre une équation différentielle du premier ordre ?",
    "",
    "Merci pour votre aide !",
    "",
    "FORMATTED_QUESTION: Bonjour,\\n\\nComment résoudre une équation différentielle du premier ordre ?\\n\\nMerci pour votre aide !",
    "FORMATTED_TITLE: Équations différentielles",
    "",
    `User's message: "${question}"`,
  ].join("\n");
}

/**
 * Maps intent types to their prompt builders.
 * Add new prompt builders here when adding new intent types.
 */
export const ED_INTENT_PROMPT_BUILDERS: Record<EdIntentType, (question: string) => string> = {
  post_question: buildEdPostPrompt,
};

