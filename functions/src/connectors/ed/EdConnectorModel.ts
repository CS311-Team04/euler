/**
* Possible high-level states for the ED Discussion connector.
*
* - "not_connected": no valid configuration stored for this user
* - "connected": credentials are stored and last test was successful
* - "error": last connect/test attempt failed (invalid token, API error, etc.)
*
*/
export type EdConnectorStatus = "not_connected" | "connected" | "error";

/**
 * Centralised error codes for the ED connector.
 * Using constants + a type helps avoid typos and keeps a single source of truth.
 */
export const ED_CONNECTOR_ERROR_CODES = {
  INVALID_CREDENTIALS: "invalid_credentials",
  API_UNREACHABLE: "api_unreachable",
  TEST_FAILED: "test_failed",
} as const;

export type EdConnectorErrorCode =
  (typeof ED_CONNECTOR_ERROR_CODES)[keyof typeof ED_CONNECTOR_ERROR_CODES];

/**
* Persisted connector configuration for a given user.
* This is what we read/write in the database (e.g. Firestore).
*/
export interface EdConnectorConfig {
    /** Current status of the connector for this user. */
    status: EdConnectorStatus;

    /** Base URL of the ED API server. */
    baseUrl?: string;

    /**
    * Encrypted API token used in the Authorization: Bearer <token> header.
    * Never store the raw token here – always encrypt it before saving.
    */
    apiKeyEncrypted?: string;

    /**
    * ISO timestamp of the last successful or failed connectivity test.
    */
    lastTestAt?: string;

    /**
    * Optional error message/code describing the last failure
    * (e.g. "invalid_credentials", "api_unreachable", "test_failed").
    * Should be null or undefined when status is "connected".
    */
    lastError?: EdConnectorErrorCode | null;
}
