export type MoodleConnectorStatus = "not_connected" | "connected" | "error";

/**
    * Configuration and status for a Moodle connector.
 */
export interface MoodleConnectorConfig {
  status: MoodleConnectorStatus;
  baseUrl?: string;
  tokenEncrypted?: string;
  lastTestAt?: string;
  lastError?: string | null;
}