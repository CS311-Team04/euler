// functions/src/__tests__/edIntentParser.test.ts

import { parseEdPostResponse } from "../edIntentParser";

describe("parseEdPostResponse", () => {
  describe("should parse correctly formatted responses", () => {
    it("should extract formatted question and title from standard format", () => {
      const rawReply = [
        "REPONSE: Voici une formulation optimisée de votre question pour ED Discussion :",
        "",
        "**Question 5 Modstoch S7**",
        "",
        "Bonjour,",
        "",
        "Comment résoudre la question 5 ?",
        "",
        "Merci d'avance !",
        "",
        "FORMATTED_QUESTION: Bonjour,\\n\\nComment résoudre la question 5 ?\\n\\nMerci d'avance !",
        "FORMATTED_TITLE: Modstoch S7 Q5",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.formattedQuestion).toBe(
        "Bonjour,\\n\\nComment résoudre la question 5 ?\\n\\nMerci d'avance !"
      );
      expect(result.formattedTitle).toBe("Modstoch S7 Q5");
      expect(result.reply).toContain("Voici une formulation optimisée");
      expect(result.reply).not.toContain("FORMATTED_QUESTION");
      expect(result.reply).not.toContain("FORMATTED_TITLE");
    });

    it("should handle multiline formatted question", () => {
      const rawReply = [
        "REPONSE: Here is your question:",
        "",
        "FORMATTED_QUESTION: Bonjour,\\n\\nQuestion multiline\\navec plusieurs lignes\\n\\nMerci",
        "FORMATTED_TITLE: Test Title",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.formattedQuestion).toContain("Bonjour");
      expect(result.formattedQuestion).toContain("Question multiline");
      expect(result.formattedTitle).toBe("Test Title");
    });

    it("should clean reply by removing technical markers", () => {
      const rawReply = [
        "REPONSE: Natural response text here",
        "FORMATTED_QUESTION: Question text",
        "FORMATTED_TITLE: Title text",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.reply).toBe("Natural response text here");
      expect(result.reply).not.toContain("FORMATTED_QUESTION");
      expect(result.reply).not.toContain("FORMATTED_TITLE");
      expect(result.reply).not.toContain("REPONSE:");
    });
  });

  describe("should handle edge cases", () => {
    it("should handle response without FORMATTED_QUESTION", () => {
      const rawReply = "Just a normal response without format markers";

      const result = parseEdPostResponse(rawReply);

      // Fallback: uses cleanReply if length < 500
      expect(result.formattedQuestion).toBe(rawReply);
      expect(result.formattedTitle).toBeNull();
      expect(result.reply).toBe(rawReply);
    });

    it("should handle response without FORMATTED_TITLE", () => {
      const rawReply = [
        "REPONSE: Some response",
        "FORMATTED_QUESTION: Question only",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.formattedQuestion).toBe("Question only");
      expect(result.formattedTitle).toBeNull();
    });

    it("should handle empty reply", () => {
      const result = parseEdPostResponse("");

      expect(result.reply).toBe("");
      expect(result.formattedQuestion).toBeNull();
      expect(result.formattedTitle).toBeNull();
    });

    it("should handle whitespace-only reply", () => {
      const result = parseEdPostResponse("   \n\n   ");

      expect(result.reply.trim()).toBe("");
      expect(result.formattedQuestion).toBeNull();
      expect(result.formattedTitle).toBeNull();
    });

    it("should use clean reply as fallback for formattedQuestion if reasonable length", () => {
      const shortReply = "Short clean question text";

      const result = parseEdPostResponse(shortReply);

      expect(result.formattedQuestion).toBe(shortReply);
      expect(result.reply).toBe(shortReply);
    });

    it("should not use very long reply as fallback for formattedQuestion", () => {
      const longReply = "A".repeat(600); // > 500 chars

      const result = parseEdPostResponse(longReply);

      expect(result.formattedQuestion).toBeNull();
      expect(result.reply).toBe(longReply);
    });

    it("should handle REPONSE: prefix without formatted sections", () => {
      const rawReply = "REPONSE: Just a response with prefix";

      const result = parseEdPostResponse(rawReply);

      expect(result.reply).toBe("Just a response with prefix");
      expect(result.reply).not.toContain("REPONSE:");
    });

    it("should handle case-insensitive markers", () => {
      const rawReply = [
        "reponse: Lowercase response",
        "formatted_question: Lowercase question",
        "formatted_title: Lowercase title",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.formattedQuestion).toBe("Lowercase question");
      expect(result.formattedTitle).toBe("Lowercase title");
      expect(result.reply).not.toContain("reponse:");
    });
  });

  describe("should preserve clean reply structure", () => {
    it("should preserve markdown formatting in clean reply", () => {
      const rawReply = [
        "REPONSE: Here is your question:",
        "",
        "**Question 5 Modstoch S7**",
        "",
        "Bonjour,",
        "",
        "FORMATTED_QUESTION: Formatted question",
        "FORMATTED_TITLE: Title",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.reply).toContain("**Question 5 Modstoch S7**");
      expect(result.reply).toContain("Bonjour,");
      expect(result.reply).not.toContain("FORMATTED_QUESTION");
    });

    it("should handle reply with only natural text", () => {
      const rawReply = [
        "Voici une formulation optimisée de votre question pour ED Discussion :",
        "",
        "**Question 5 Modstoch S7**",
        "",
        "Bonjour,",
        "",
        "Comment résoudre la question 5 ?",
        "",
        "Merci d'avance !",
      ].join("\n");

      const result = parseEdPostResponse(rawReply);

      expect(result.reply).toBe(rawReply);
      expect(result.formattedQuestion).toBe(rawReply); // Used as fallback
    });
  });
});

