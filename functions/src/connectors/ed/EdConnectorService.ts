import { EdConnectorConfig } from "./EdConnectorModel";
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
        lastError: "invalid_credentials",
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
      lastError: ok ? null : "test_failed",
    };

    await this.repo.saveConfig(userId, updated);
    return updated;
  }
}
