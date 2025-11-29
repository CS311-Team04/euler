// functions/src/__tests__/edIntent.test.ts

import {
  detectPostToEdIntentCore,
  generateEdIntentResponse,
  detectAndRespondToEdIntent,
  buildEdIntentPromptForApertus,
  EdIntentType,
} from "../edIntent";

describe("detectPostToEdIntentCore", () => {
  describe("should detect ED posting intent", () => {
    const positiveTestCases: Array<{ input: string; expectedIntent: EdIntentType }> = [
      // Basic "post sur ed" patterns
      { input: "post sur ed", expectedIntent: "post_question" },
      { input: "poste sur ed", expectedIntent: "post_question" },
      { input: "poste ça sur ED", expectedIntent: "post_question" },
      { input: "poster sur ed", expectedIntent: "post_question" },
      { input: "postez sur ed", expectedIntent: "post_question" },
      
      // "publie" patterns
      { input: "publie sur ed", expectedIntent: "post_question" },
      { input: "publier sur ed", expectedIntent: "post_question" },
      { input: "publie ça sur ED Discussion", expectedIntent: "post_question" },
      
      // "met/mets" patterns
      { input: "mets ça sur ed", expectedIntent: "post_question" },
      { input: "met ça sur ed", expectedIntent: "post_question" },
      { input: "mettre sur ed", expectedIntent: "post_question" },
      
      // "ajoute" patterns
      { input: "ajoute sur ed", expectedIntent: "post_question" },
      { input: "ajouter sur ed", expectedIntent: "post_question" },
      
      // "envoie" patterns
      { input: "envoie sur ed", expectedIntent: "post_question" },
      { input: "envoyer sur ed", expectedIntent: "post_question" },
      { input: "envoie ce message sur ed", expectedIntent: "post_question" },
      
      // "pose" patterns
      { input: "pose cette question sur ed", expectedIntent: "post_question" },
      { input: "poser la question sur ed", expectedIntent: "post_question" },
      { input: "demande sur ed", expectedIntent: "post_question" },
      
      // "partage" patterns
      { input: "partage sur ed", expectedIntent: "post_question" },
      { input: "partager sur ed", expectedIntent: "post_question" },
      
      // "créer" patterns
      { input: "crée un post sur ed", expectedIntent: "post_question" },
      { input: "créer un thread sur ed", expectedIntent: "post_question" },
      { input: "faire une discussion sur ed", expectedIntent: "post_question" },
      { input: "fais un sujet sur ed", expectedIntent: "post_question" },
      
      // "je veux" patterns
      { input: "je veux poster sur ed", expectedIntent: "post_question" },
      { input: "je voudrais poster sur ed", expectedIntent: "post_question" },
      { input: "je souhaite publier sur ed", expectedIntent: "post_question" },
      { input: "peux-tu poster sur ed", expectedIntent: "post_question" },
      { input: "tu peux poster ça sur ed", expectedIntent: "post_question" },
      
      // With context/sentences
      { input: "J'ai une question sur les maths, poste-la sur ed", expectedIntent: "post_question" },
      { input: "S'il te plaît, publie ma question sur ED", expectedIntent: "post_question" },
      { input: "Pourrais-tu poster ce problème sur ed stp?", expectedIntent: "post_question" },
      
      // "edstem" variations
      { input: "post sur edstem", expectedIntent: "post_question" },
      { input: "publie sur ed discussion", expectedIntent: "post_question" },
    ];

    test.each(positiveTestCases)(
      'should detect intent for: "$input"',
      ({ input, expectedIntent }) => {
        const result = detectPostToEdIntentCore(input);
        expect(result.ed_intent_detected).toBe(true);
        expect(result.ed_intent).toBe(expectedIntent);
      }
    );
  });

  describe("should NOT detect ED posting intent for questions about ED", () => {
    const negativeTestCases: string[] = [
      // Asking ABOUT ED
      "c'est quoi ED?",
      "c'est quoi ed",
      "qu'est-ce que ED?",
      "qu'est ce qu'ed",
      "que signifie ed?",
      "comment marche ED?",
      "comment fonctionne ed?",
      "comment utiliser ed?",
      "comment accéder à ed?",
      "où trouver ED?",
      "où est ED?",
      "où se trouve ed?",
      "explique-moi ED",
      "explique ed",
      
      // Unrelated questions
      "Quelle est la date limite d'inscription?",
      "Comment contacter le secrétariat?",
      "J'ai un problème avec mon compte EPFL",
      "Quels sont les horaires de la bibliothèque?",
      
      // Partial matches that shouldn't trigger
      "poster une affiche",
      "publier un article",
      "poste de travail",
    ];

    test.each(negativeTestCases)(
      'should NOT detect intent for: "%s"',
      (input) => {
        const result = detectPostToEdIntentCore(input);
        expect(result.ed_intent_detected).toBe(false);
        expect(result.ed_intent).toBeNull();
      }
    );
  });

  describe("edge cases", () => {
    it("should handle empty string", () => {
      const result = detectPostToEdIntentCore("");
      expect(result.ed_intent_detected).toBe(false);
      expect(result.ed_intent).toBeNull();
    });

    it("should handle whitespace-only string", () => {
      const result = detectPostToEdIntentCore("   ");
      expect(result.ed_intent_detected).toBe(false);
      expect(result.ed_intent).toBeNull();
    });

    it("should be case-insensitive", () => {
      const lower = detectPostToEdIntentCore("poste sur ed");
      const upper = detectPostToEdIntentCore("POSTE SUR ED");
      const mixed = detectPostToEdIntentCore("Poste Sur ED");
      
      expect(lower.ed_intent_detected).toBe(true);
      expect(upper.ed_intent_detected).toBe(true);
      expect(mixed.ed_intent_detected).toBe(true);
    });
  });
});

describe("generateEdIntentResponse", () => {
  it("should generate response for post_question intent", () => {
    const response = generateEdIntentResponse("post_question", "test question");
    expect(response).toContain("poster une question sur ED Discussion");
    expect(response).toContain("connecteur ED");
  });

  it("should generate response for post_answer intent", () => {
    const response = generateEdIntentResponse("post_answer", "test");
    expect(response).toContain("répondre");
  });

  it("should generate response for post_comment intent", () => {
    const response = generateEdIntentResponse("post_comment", "test");
    expect(response).toContain("commenter");
  });
});

describe("detectAndRespondToEdIntent", () => {
  it("should return full result with reply when intent detected", () => {
    const result = detectAndRespondToEdIntent("poste sur ed");
    expect(result.ed_intent_detected).toBe(true);
    expect(result.ed_intent).toBe("post_question");
    expect(result.reply).toBeDefined();
    expect(result.reply).toContain("ED Discussion");
  });

  it("should return detection-only result when no intent", () => {
    const result = detectAndRespondToEdIntent("c'est quoi ED?");
    expect(result.ed_intent_detected).toBe(false);
    expect(result.ed_intent).toBeNull();
    expect(result.reply).toBeUndefined();
  });
});

describe("buildEdIntentPromptForApertus", () => {
  it("should build prompt with user question", () => {
    const prompt = buildEdIntentPromptForApertus("poste ma question sur ed");
    expect(prompt).toContain("L'utilisateur souhaite poster sur ED Discussion");
    expect(prompt).toContain("poste ma question sur ed");
    expect(prompt).toContain("fonctionnalité est en cours de développement");
  });

  it("should include encouragement", () => {
    const prompt = buildEdIntentPromptForApertus("test");
    expect(prompt).toContain("encourageant");
    expect(prompt).toContain("bientôt");
  });
});

