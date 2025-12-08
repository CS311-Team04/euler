/**
 * Minimal HTTP client around the ED Discussion API.
 *
 * For now we only need to test whether a given token is valid,
 * by calling:
 *   GET https://eu.edstem.org/api/user
 * with:
 *   Authorization: Bearer <token>
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

  /**
   * Returns true if the token is valid (2xx status),
   * false if ED returns a non-2xx status or if the request fails.
   */
  async testConnection(): Promise<boolean> {
    try {
      const response = await fetch(this.buildUrl("user"), {
        method: "GET",
        headers: {
          Authorization: `Bearer ${this.apiToken}`,
        },
      });

      return response.ok;
    } catch (error) {
      // Network / DNS / timeout error → treat as failure
      console.error("EDDiscussionClient.testConnection failed", error);
      return false;
    }
  }

  /**
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
