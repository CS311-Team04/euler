export type MoodleConnectorStatus = "not_connected" | "connected" | "error";

export interface MoodleConnectorConfig {
  status: MoodleConnectorStatus;
  baseUrl?: string;
  tokenEncrypted?: string;
  lastTestAt?: string;
  lastError?: string | null;
}