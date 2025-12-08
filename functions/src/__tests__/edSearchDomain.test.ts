import { describe, it, expect } from '@jest/globals';

import {
  parseEdSearchQuery,
  buildEdSearchRequest,
  normalizeEdThreads,
  EdSearchParsedQuery,
  EdBrainError,
} from "../edSearchDomain";

import {
  EdCourse,
  EdThread,
  EdDiscussionClient,
} from "../connectors/ed/EdDiscussionClient";

function isBrainError(result: unknown): result is EdBrainError {
  return (
    typeof result === "object" &&
    result !== null &&
    "type" in result
  );
}

describe("parseEdSearchQuery", () => {
  it("devrait extraire status, cours, limite et date pour une requête typique", () => {
    const query = "show me unanswered posts from CS-101 yesterday first 5";

    const parsed = parseEdSearchQuery(query);

    expect(parsed.originalQuery).toBe(query);
    expect(parsed.status).toBe("unanswered");
    expect(parsed.courseQuery).toBe("CS-101");
    expect(parsed.limit).toBe(5);

    // On vérifie juste que des dates sont bien présentes et valides
    expect(parsed.dateFrom).toBeDefined();
    expect(parsed.dateTo).toBeDefined();

    if (parsed.dateFrom) {
      expect(() => new Date(parsed.dateFrom!)).not.toThrow();
    }
    if (parsed.dateTo) {
      expect(() => new Date(parsed.dateTo!)).not.toThrow();
    }
  });

  it("devrait gérer une requête sans status explicite ni limite", () => {
    const query = "show posts from COM-301 today";

    const parsed = parseEdSearchQuery(query);

    // Selon ton impl, status peut être "all" ou undefined
    expect(["all", undefined]).toContain(parsed.status);
    expect(parsed.courseQuery).toBe("COM-301");
    expect(parsed.limit).toBeUndefined();
    expect(parsed.tags).toEqual([]);
  });
});

describe("buildEdSearchRequest", () => {
  const courses: EdCourse[] = [
    {
      id: 1,
      code: "CS-101",
      name: "Computer Security",
    },
    {
      id: 2,
      code: "COM-301",
      name: "Communication Systems",
    },
  ];

  // Mock EdDiscussionClient that doesn't actually fetch categories
  const mockClient = {
    fetchCategoriesFromThreads: async () => Promise.resolve([]),
  } as unknown as EdDiscussionClient;

  it("devrait résoudre le cours et construire les options pour fetchThreads", async () => {
    const parsed: EdSearchParsedQuery = {
      originalQuery: "show unanswered posts from CS-101",
      courseQuery: "CS-101",
      status: "unanswered",
      limit: 10,
      tags: [],
    };

    const result = await buildEdSearchRequest(mockClient, parsed, courses);

    expect(isBrainError(result)).toBe(false);

    if (!isBrainError(result)) {
      expect(result.courseId).toBe(1);
      expect(result.resolvedCourse.code).toBe("CS-101");
      expect(result.fetchOptions.courseId).toBe(1);
      expect(result.fetchOptions.limit).toBe(10);
      expect(result.fetchOptions.statusFilter).toBe("unanswered");
    }
  });

  it("devrait retourner une erreur INVALID_QUERY si aucun cours ne match", async () => {
    const parsed: EdSearchParsedQuery = {
      originalQuery: "show posts from MATH-999",
      courseQuery: "MATH-999",
      status: "all",
      tags: [],
    };

    const result = await buildEdSearchRequest(mockClient, parsed, courses);

    expect(isBrainError(result)).toBe(true);

    if (isBrainError(result)) {
      expect(result.type).toBe("INVALID_QUERY");
      expect(result.message).toContain("MATH-999");
    }
  });
});

describe("normalizeEdThreads", () => {
  const course: EdCourse = {
    id: 1,
    code: "CS-101",
    name: "Computer Security",
  };

  it("devrait normaliser un thread ED en NormalizedEdPost", () => {
    const threads: EdThread[] = [
      {
        id: 123,
        title: "Exam 1 clarification",
        created_at: "2025-01-01T12:00:00Z",
        is_unread: true,
        is_answered: false,
        is_resolved: false,
        tags: ["exam", "clarification"],
      },
    ];

    const normalized = normalizeEdThreads(threads, course);

    expect(normalized).toHaveLength(1);
    const post = normalized[0];

    expect(post.postId).toBe("123");
    expect(post.title).toBe("Exam 1 clarification");
    expect(post.snippet).toBe("Exam 1 clarification");
    expect(post.createdAt).toBe("2025-01-01T12:00:00Z");
    expect(post.course).toBe("CS-101");
    expect(post.tags).toEqual(["exam", "clarification"]);
    expect(post.status).toContain("unread");
  });
});
