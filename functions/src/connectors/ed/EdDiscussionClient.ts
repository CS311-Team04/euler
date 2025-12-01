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
}
