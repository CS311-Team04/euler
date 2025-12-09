

export interface EdCourse {
  id: number;
  code: string;
  name: string;
  // ED returns many other fields, we keep the rest as "unknown" for now.
  [key: string]: unknown;
}

// Internal statusFilter -> ED API `filter` parameter
const ED_STATUS_FILTER_PARAM: Record<string, string> = {
  unread: "unread",
  unanswered: "unanswered",
  resolved: "resolved",

  // new filters
  new_replies: "new",      // "New replies"
  approved: "endorsed",    // "Approved"
  favorites: "starred",    // "Favorites"
  instructors: "staff",    // "Instructors"
};

export interface EdUserResponse {
  courses: Array<{
    course: EdCourse;
    role: string;
  }>;
  user: {
    email: string;
    name: string;
    [key: string]: unknown;
  };
  realms?: unknown[];
  [key: string]: unknown;
}

export interface EdThread {
  id: number;
  title: string;
  created_at: string;
  updated_at?: string;
  type?: string;
  is_unread?: boolean;
  is_answered?: boolean;
  is_resolved?: boolean;
  tags?: string[];
  content?: string;
  category?: string;
  subcategory?: string;
  // The rest of the fields are not yet used by our backend.
  [key: string]: unknown;
}

export interface FetchThreadsOptions {
  courseId: number;
  limit?: number;
  /**
   * High-level filter, we'll adapt it to the exact ED API parameters
   * (e.g., "unanswered", "unread", "resolved", etc.).
   */
   statusFilter?:
    | "all"
    | "unanswered"
    | "unread"
    | "resolved"
    | "new_replies"
    | "approved"
    | "favorites"
    | "instructors";

  query?: string;     // free-text like "exo 3"
  category?: string;  // ED category/tag name like "Problem sets"
  subcategory?: string; // ED subcategory name, e.g. "Assignment 4 - Sniffing Traffic"
}

interface EdThreadsResponse {
  threads: EdThread[];
  [key: string]: unknown;
}

export type EdCategoryTree = {
  categories: string[]; // same as before
  subcategoriesByCategory: Record<string, string[]>; // e.g. { "Programming Assignments": ["Assignment 1 - ...", "Assignment 2 - ...", ...] }
};

/**
 * Minimal HTTP client around the ED Discussion API.
 *
 * For now we:
 * - test whether a given token is valid (testConnection)
 * - fetch user info + enrolled courses (getUser / getCourses)
 * - fetch discussion threads for a given course (fetchThreads)
 */
export class EdDiscussionClient {
  constructor(
    private readonly baseUrl: string,
    private readonly apiToken: string
  ) {}

  private buildUrl(path: string): string {
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const trimmedPath = path.replace(/^\/+/, "");
    return `${trimmedBase}/${trimmedPath}`;
  }

  private async getJson<T>(path: string, init?: RequestInit): Promise<T> {
    const url = this.buildUrl(path);

    const response = await fetch(url, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${this.apiToken}`,
        ...(init?.headers ?? {}),
      },
    });

    if (!response.ok) {
      throw new Error(
        `ED API GET ${path} failed with status ${response.status}`
      );
    }

    return (await response.json()) as T;
  }

  /**
   * Returns true if the token is valid (2xx status),
   * false if ED returns a non-2xx status or if the request fails.
   */
  async testConnection(): Promise<boolean> {
    try {
      // If getUser() succeeds, the token is valid
      await this.getUser();
      return true;
    } catch (error) {
      console.error("EDDiscussionClient.testConnection failed", error);
      return false;
    }
  }

  /**
   * Fetches the /user endpoint, which includes user info and enrolled courses.
   */
  async getUser(): Promise<EdUserResponse> {
    return this.getJson<EdUserResponse>("user");
  }

  /**
   * Convenience helper that returns only the list of courses.
   */
  async getCourses(): Promise<EdCourse[]> {
    const user = await this.getUser();
    return user.courses.map((c) => c.course);
  }

  /**
   * Fetches discussion threads for a given course.
   * This is the base primitive we'll use later in the "brain" to get posts.
   */
  async fetchThreads(options: FetchThreadsOptions): Promise<EdThread[]> {
    const params = new URLSearchParams();

    if (options.limit != null) {
      params.set("limit", String(options.limit));
    }

    if (options.statusFilter && options.statusFilter !== "all") {
      const filterParam = ED_STATUS_FILTER_PARAM[options.statusFilter];
      if (filterParam) {
        params.set("filter", filterParam);
      }
    }

    // NEW: search-specific params
    const hasSearch = Boolean(options.query || options.category);
    if (options.query) params.set("query", options.query);
    if (options.category) params.set("category", options.category);
    if (options.subcategory) params.set("subcategory", options.subcategory);

    if (hasSearch) {
      // mimic the UI behaviour
      if (!params.has("limit")) params.set("limit", "5");
      params.set("sort", "relevance");
    }

    const basePath = hasSearch
      ? `courses/${options.courseId}/threads/search`
      : `courses/${options.courseId}/threads`;

    const path =
      params.toString().length > 0
        ? `${basePath}?${params.toString()}`
        : basePath;

    const res = await this.getJson<EdThreadsResponse>(path);
    return res.threads ?? [];
  }

  /**
   * Fetches all unique categories from threads in a course.
   * This is done by fetching a large batch of threads and extracting their categories.
   */
  async fetchCategoriesFromThreads(courseId: number): Promise<string[]> {
    const threads = await this.fetchThreads({
      courseId,
      limit: 200,
      statusFilter: "all",
    });

    const set = new Set<string>();
    for (const t of threads) {
      if (typeof t.category === "string" && t.category.trim().length > 0) {
        set.add(t.category);
      }
    }
    return Array.from(set);
  }

  /**
   * Fetches the category tree (categories and their subcategories) from threads in a course.
   */
  async fetchCategoryTreeFromThreads(courseId: number): Promise<EdCategoryTree> {
    const threads = await this.fetchThreads({
      courseId,
      limit: 200,
      statusFilter: "all",
    });

    const categorySet = new Set<string>();
    const subMap = new Map<string, Set<string>>();

    for (const t of threads) {
      const category = typeof t.category === "string" && t.category.trim().length > 0
        ? t.category
        : null;
      const subcategory = typeof t.subcategory === "string" && t.subcategory.trim().length > 0
        ? t.subcategory
        : null;

      if (category) {
        categorySet.add(category);
      }

      if (category && subcategory) {
        if (!subMap.has(category)) {
          subMap.set(category, new Set<string>());
        }
        subMap.get(category)!.add(subcategory);
      }
    }

    return {
      categories: Array.from(categorySet),
      subcategoriesByCategory: Object.fromEntries(
        [...subMap.entries()].map(([cat, subs]) => [cat, Array.from(subs)])
      ),
    };
  }
  * Creates a new thread in a course.
   *
   * Mirrors the Python edapi `post_thread` call:
   *   POST /courses/{courseId}/threads
   *   body: { thread: { ...fields } }
   */
  async postThread(
    courseId: number,
    params: {
      title: string;
      content: string;
      type?: string;
      category?: string;
      subcategory?: string;
      subsubcategory?: string;
      isPinned?: boolean;
      isPrivate?: boolean;
      isAnonymous?: boolean;
      isMegathread?: boolean;
      anonymousComments?: boolean;
    }
  ): Promise<{
    id?: number;
    course_id?: number;
    number?: number;
    title?: string;
    document?: string;
  }> {
    const threadPayload = {
      type: params.type ?? "question",
      title: params.title,
      category: params.category ?? "General",
      subcategory: params.subcategory ?? "",
      subsubcategory: params.subsubcategory ?? "",
      content: params.content,
      is_pinned: params.isPinned ?? false,
      is_private: params.isPrivate ?? false,
      is_anonymous: params.isAnonymous ?? false,
      is_megathread: params.isMegathread ?? false,
      anonymous_comments: params.anonymousComments ?? false,
    };

    const url = this.buildUrl(`courses/${courseId}/threads`);
    const response = await fetch(url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.apiToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ thread: threadPayload }),
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(
        `ED postThread failed: ${response.status} ${response.statusText} - ${text}`
      );
    }

    const body = await response.json();
    return body?.thread ?? {};
  }
}
