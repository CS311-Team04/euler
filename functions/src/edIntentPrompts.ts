// functions/src/edIntentPrompts.ts
// Prompt templates and configuration for ED intent handling

import { EdIntentType } from "./edIntent";

/**
 * Prompt template for generating formatted ED posts.
 * This prompt instructs Apertus to format questions with proper context and politeness.
 */
export function buildEdPostPrompt(question: string): string {
  return [
    "L'utilisateur souhaite poster une question sur ED Discussion.",
    "",
    " IMPORTANT :",
    "- NE DIS PAS que la fonctionnalité n'est pas disponible",
    "- NE DIS PAS que c'est en cours de développement",
    "- NE DIS PAS qu'il faut attendre",
    "- Réponds comme si tu pouvais l'aider à formuler sa question MAINTENANT",
    "",
    "Tu dois faire DEUX choses :",
    "",
    "1. Réponds-lui naturellement et INCLUS directement la formulation optimisée dans ta réponse.",
    "   Présente-la comme une proposition de reformulation claire pour ED.",
    "",
    "2. Extrais aussi la question et le titre dans le format technique ci-dessous.",
    "",
    "Instructions pour la formulation :",
    "- Extrais le contenu de sa question (sans 'poste', 'sur ed', etc.)",
    "- Formule-la de manière claire, concise et professionnelle",
    "- Assure-toi que c'est une vraie question",
    "- AJOUTE des formules de politesse appropriées :",
    "  * Début : 'Bonjour', 'Salut', ou 'Hello'",
    "  * Fin : 'Merci d'avance', 'Merci pour votre aide', ou 'Merci'",
    "- La question doit être polie et respectueuse pour ED Discussion",
    "",
    "Instructions pour le TITRE :",
    "- Le titre doit être un CONTEXTE court (3-4 mots MAX) qui situe la question",
    "- Exemples : 'Modstoch S7 Q5', 'Question 5 Modstoch', 'Algèbre Exo 3'",
    "- C'est juste pour identifier rapidement de quoi parle la question",
    "- PAS un résumé de la question, juste le contexte (cours, exercice, semaine, etc.)",
    "",
    "Format OBLIGATOIRE de ta réponse (respecte EXACTEMENT ce format) :",
    "REPONSE: [ta réponse qui INCLUT la formulation optimisée]",
    "",
    "FORMATTED_QUESTION: [la question formatée - version technique, sur une seule ligne]",
    "FORMATTED_TITLE: [le titre - version technique, sur une seule ligne]",
    "",
    "Exemples :",
    "",
    "Exemple 1 :",
    "Si l'utilisateur dit: 'poste sur ed par rapport à la question 5 de modstoch de la semaine 7'",
    "REPONSE: Voici une formulation optimisée de votre question pour ED Discussion :",
    "",
    "**Question 5 Modstoch S7**",
    "",
    "Bonjour,",
    "",
    "Comment résoudre la question 5 de l'exercice de modstoch de la semaine 7 ?",
    "",
    "Merci d'avance !",
    "",
    "FORMATTED_QUESTION: Bonjour,\\n\\nComment résoudre la question 5 de l'exercice de modstoch de la semaine 7 ?\\n\\nMerci d'avance !",
    "FORMATTED_TITLE: Modstoch S7 Q5",
    "",
    "Exemple 2 :",
    "Si l'utilisateur dit: 'poste sur ed comment résoudre une équation différentielle'",
    "REPONSE: Voici une formulation optimisée de votre question pour ED Discussion :",
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
    `Message de l'utilisateur: "${question}"`,
  ].join("\n");
}

/**
 * Maps intent types to their prompt builders.
 * Add new prompt builders here when adding new intent types.
 */
export const ED_INTENT_PROMPT_BUILDERS: Record<EdIntentType, (question: string) => string> = {
  post_question: buildEdPostPrompt,
};

