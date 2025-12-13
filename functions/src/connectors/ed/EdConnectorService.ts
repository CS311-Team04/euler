import {
  EdConnectorConfig,
  ED_CONNECTOR_ERROR_CODES,
} from "./EdConnectorModel";
import { EdConnectorRepository } from "./EdConnectorRepository";
import { EdDiscussionClient } from "./EdDiscussionClient";

export interface EdConnectorConnectParams {
  /** Raw ED API token provided by the user. */
  apiToken: string;
  /** Optional override for the ED API base URL. */
  baseUrl?: string;
}

/**
 * Core domain logic for the ED Discussion connector.
 */
export class EdConnectorService {
  constructor(
    private readonly repo: EdConnectorRepository,
    private readonly encrypt: (plain: string) => string,
    private readonly decrypt: (cipher: string) => string,
    private readonly defaultBaseUrl: string = "https://eu.edstem.org/api"
  ) {}

  private resolveBaseUrl(baseUrl?: string): string {
    return baseUrl?.trim() || this.defaultBaseUrl;
  }

  private escapeXml(text: string): string {
    return text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&apos;");
  }

  private buildContentXml(body: string): string {
    const trimmed = body.trim();
    if (!trimmed) {
      return '<document version="2.0"></document>';
    }
    // Split into paragraphs on blank lines, keep line breaks as <br/> within a paragraph.
    const paragraphs = trimmed
      .split(/\n{2,}/)
      .map((p) => p.trim())
      .filter(Boolean)
      .map((p) => {
        const withBreaks = this.escapeXml(p).replace(/\n+/g, "<br/>");
        return `<paragraph>${withBreaks}</paragraph>`;
      })
      .join("");

    return `<document version="2.0">${paragraphs}</document>`;
  }

  /**
   * Returns the stored connector config for this user.
   * If none exists, we consider the connector "not_connected".
   */
  async getStatus(userId: string): Promise<EdConnectorConfig> {
    const config = await this.repo.getConfig(userId);
    if (!config) {
      return { status: "not_connected" };
    }
    return config;
  }

  /**
   * Attempts to connect the ED connector using the provided token (and optional base URL).
   * - Valid token => save encrypted token + mark as "connected"
   * - Invalid / failing => mark as "error" with lastError = "invalid_credentials"
   */
  async connect(
    userId: string,
    params: EdConnectorConnectParams
  ): Promise<EdConnectorConfig> {
    const baseUrl = this.resolveBaseUrl(params.baseUrl);
    const client = new EdDiscussionClient(baseUrl, params.apiToken);
    const now = new Date().toISOString();

    const ok = await client.testConnection();

    if (!ok) {
      const errorConfig: EdConnectorConfig = {
        status: "error",
        baseUrl,
        lastTestAt: now,
        lastError: ED_CONNECTOR_ERROR_CODES.INVALID_CREDENTIALS,
      };

      // We save this error state so the UI can display feedback to the user.
      await this.repo.saveConfig(userId, errorConfig);
      return errorConfig;
    }

    const config: EdConnectorConfig = {
      status: "connected",
      baseUrl,
      apiKeyEncrypted: this.encrypt(params.apiToken),
      lastTestAt: now,
      lastError: null,
    };

    await this.repo.saveConfig(userId, config);
    return config;
  }

  /**
   * Disconnect the connector for this user by deleting its config.
   */
  async disconnect(userId: string): Promise<void> {
    await this.repo.deleteConfig(userId);
  }

  /**
   * Re-test connectivity using the stored (encrypted) token.
   * - If no config/token => "not_connected"
   * - If test OK       => update status to "connected"
   * - If test fails    => update status to "error" with lastError = "test_failed"
   */
  async test(userId: string): Promise<EdConnectorConfig> {
    const existing = await this.repo.getConfig(userId);

    if (!existing || !existing.apiKeyEncrypted || !existing.baseUrl) {
      return { status: "not_connected" };
    }

    const apiToken = this.decrypt(existing.apiKeyEncrypted);
    const client = new EdDiscussionClient(existing.baseUrl, apiToken);
    const now = new Date().toISOString();

    const ok = await client.testConnection();

    const updated: EdConnectorConfig = {
      ...existing,
      status: ok ? "connected" : "error",
      lastTestAt: now,
      lastError: ok ? null : ED_CONNECTOR_ERROR_CODES.TEST_FAILED,
    };

    await this.repo.saveConfig(userId, updated);
    return updated;
  }

  /**
   * Creates a thread on ED using the stored connector credentials.
   */
  async postThread(
    userId: string,
    params: { title: string; body: string; courseId?: number; isAnonymous?: boolean }
  ): Promise<{
    threadId?: number;
    courseId?: number;
    threadNumber?: number;
    title?: string;
  }> {
    const existing = await this.repo.getConfig(userId);
    if (!existing || !existing.apiKeyEncrypted) {
      throw new Error("ED connector is not connected");
    }

    const apiToken = this.decrypt(existing.apiKeyEncrypted);
    const baseUrl = this.resolveBaseUrl(existing.baseUrl);
    if (!params.courseId) {
      throw new Error("courseId is required");
    }
    const courseId = params.courseId;
    const client = new EdDiscussionClient(baseUrl, apiToken);

    const thread = await client.postThread(courseId, {
      title: params.title,
      content: this.buildContentXml(params.body),
      isAnonymous: params.isAnonymous ?? false,
    });

    return {
      threadId: thread.id,
      courseId: thread.course_id,
      threadNumber: thread.number,
      title: thread.title,
    };
  }
}
