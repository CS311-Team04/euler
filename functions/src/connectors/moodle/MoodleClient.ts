export class MoodleClient {
  constructor(
    private readonly baseUrl: string,
    private readonly token: string
  ) {}

  /**
    * Builds the full URL for a given Moodle web service function.
   */
  private buildUrl(wsFunction: string): string {
    const trimmedBase = this.baseUrl.replace(/\/+$/, "");
    const url = new URL(trimmedBase + "/webservice/rest/server.php");
    url.searchParams.set("wstoken", this.token);
    url.searchParams.set("wsfunction", wsFunction);
    url.searchParams.set("moodlewsrestformat", "json");
    return url.toString();
  }

  /**
   * Calls core_webservice_get_site_info to validate connectivity & token.
   * Returns true if the call succeeds and does not contain an error.
   */
  async testConnection(): Promise<boolean> {
    const url = this.buildUrl("core_webservice_get_site_info");

    let res: Response;
    try {
      res = await fetch(url, { method: "GET" });
    } catch (e) {
      console.error("MoodleClient.testConnection network error", e);
      return false;
    }

    if (!res.ok) {
      console.error("MoodleClient.testConnection bad status", res.status);
      return false;
    }

    try {
      const body = (await res.json()) as any;

      // Moodle errors often come back with 'exception' or 'errorcode'
      if (body && (body.exception || body.errorcode)) {
        console.error("MoodleClient.testConnection Moodle error", body);
        return false;
      }

      return true;
    } catch (e) {
      console.error("MoodleClient.testConnection JSON parse error", e);
      return false;
    }
  }
}