import {
  MoodleConnectorConfig,
} from "./MoodleConnectorModel";
import { MoodleConnectorRepository } from "./MoodleConnectorRepository";
import { MoodleClient } from "./MoodleClient";

export class MoodleConnectorService {
  constructor(
    private readonly repo: MoodleConnectorRepository,
    // simple pluggable crypto so you can replace with real encryption
    private readonly encrypt: (plain: string) => string,
    private readonly decrypt: (cipher: string) => string
  ) {}

  async getStatus(userId: string): Promise<MoodleConnectorConfig> {
    const config = await this.repo.getConfig(userId);
    if (!config) {
      return { status: "not_connected" };
    }
    return config;
  }

  async connect(
    userId: string,
    params: { baseUrl: string; token: string }
  ): Promise<MoodleConnectorConfig> {
    const client = new MoodleClient(params.baseUrl, params.token);
    const ok = await client.testConnection();

    const now = new Date().toISOString();

    if (!ok) {
      const errorConfig: MoodleConnectorConfig = {
        status: "error",
        baseUrl: params.baseUrl,
        lastTestAt: now,
        lastError: "invalid_credentials",
      };

      await this.repo.saveConfig(userId, errorConfig);
      return errorConfig;
    }

    const config: MoodleConnectorConfig = {
      status: "connected",
      baseUrl: params.baseUrl,
      tokenEncrypted: this.encrypt(params.token),
      lastTestAt: now,
      lastError: null,
    };

    await this.repo.saveConfig(userId, config);
    return config;
  }

  async disconnect(userId: string): Promise<void> {
    await this.repo.deleteConfig(userId);
  }

  async test(userId: string): Promise<MoodleConnectorConfig> {
    const existing = await this.repo.getConfig(userId);
    if (
      !existing ||
      !existing.baseUrl ||
      !existing.tokenEncrypted
    ) {
      return { status: "not_connected" };
    }

    const token = this.decrypt(existing.tokenEncrypted);
    const client = new MoodleClient(existing.baseUrl, token);
    const ok = await client.testConnection();
    const now = new Date().toISOString();

    const updated: MoodleConnectorConfig = {
      ...existing,
      status: ok ? "connected" : "error",
      lastTestAt: now,
      lastError: ok ? null : "test_failed",
    };

    await this.repo.saveConfig(userId, updated);
    return updated;
  }
}