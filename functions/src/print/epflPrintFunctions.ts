/**
 * EPFL Print Cloud Functions
 * 
 * These functions handle the server-side operations for EPFL Print integration:
 * - Token exchange (OAuth code -> access token)
 * - Print job submission
 * - Balance queries
 * 
 * The actual API calls to PocketCampus are proxied through these functions
 * to keep credentials and tokens secure.
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const logger = functions.logger;

// Europe region for EPFL users
const europeFunctions = functions.region("europe-west6");

// =============================================================================
// CONFIGURATION
// =============================================================================

const POCKETCAMPUS_CONFIG = {
  baseUrl: "https://campus.epfl.ch",
  backendProxy: "/deploy/backend_proxy/14620",
  endpoints: {
    cloudprint: "/js-cloudprint",
    authentication: "/js-authentication",
  },
};

/**
 * Generate random xhrId for PocketCampus requests
 */
function generateXhrId(): number {
  return Math.floor(Math.random() * 100000);
}

// =============================================================================
// TOKEN EXCHANGE FUNCTION
// =============================================================================

interface TokenExchangeInput {
  authCode: string;
  redirectUri: string;
}

/**
 * Exchange OAuth authorization code for access tokens
 * 
 * This function is called after the user completes the OAuth flow in the WebView.
 * It exchanges the authorization code for access/refresh tokens via PocketCampus backend.
 */
export const epflPrintExchangeTokenFn = europeFunctions.https.onCall(
  async (data: TokenExchangeInput, context) => {
    // Verify user is authenticated with Firebase
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { authCode, redirectUri } = data;

    if (!authCode) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Authorization code is required"
      );
    }

    try {
      logger.info("epflPrintExchangeToken.start", { 
        uid: context.auth.uid,
        hasCode: !!authCode 
      });

      // Call PocketCampus authentication endpoint to exchange code
      const url = `${POCKETCAMPUS_CONFIG.baseUrl}${POCKETCAMPUS_CONFIG.backendProxy}${POCKETCAMPUS_CONFIG.endpoints.authentication}?xhrId=${generateXhrId()}`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "EULER-App/1.0",
          "Accept": "application/json",
        },
        body: JSON.stringify({
          action: "complete_authentication",
          code: authCode,
          redirect_uri: redirectUri,
        }),
      });

      if (!response.ok) {
        logger.error("epflPrintExchangeToken.failed", {
          status: response.status,
          statusText: response.statusText,
        });
        throw new functions.https.HttpsError(
          "internal",
          "Failed to exchange token"
        );
      }

      const result = await response.json();

      // Extract tokens from response
      // Note: Actual response structure needs to be verified from PocketCampus
      const accessToken = result.access_token || result.token;
      const refreshToken = result.refresh_token;
      const expiresIn = result.expires_in || 3600;
      const userEmail = result.email || result.user?.email;

      if (!accessToken) {
        throw new functions.https.HttpsError(
          "internal",
          "No access token received"
        );
      }

      // Store token reference in Firestore (for tracking/revocation)
      const db = admin.firestore();
      await db.collection("users").doc(context.auth.uid).set({
        epflPrint: {
          connected: true,
          connectedAt: admin.firestore.FieldValue.serverTimestamp(),
          userEmail: userEmail || null,
        }
      }, { merge: true });

      logger.info("epflPrintExchangeToken.success", {
        uid: context.auth.uid,
        userEmail,
      });

      return {
        success: true,
        accessToken,
        refreshToken,
        expiresIn,
        userEmail,
      };
    } catch (error: any) {
      logger.error("epflPrintExchangeToken.error", { 
        error: String(error),
        uid: context.auth.uid 
      });
      
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }
      
      throw new functions.https.HttpsError(
        "internal",
        error.message || "Token exchange failed"
      );
    }
  }
);

// =============================================================================
// PRINT JOB SUBMISSION
// =============================================================================

interface PrintJobInput {
  accessToken: string;
  file: string; // base64 encoded
  fileName: string;
  mimeType: string;
  copies: number;
  paperSize: string;
  duplex: string;
  color: string;
  pageRange: string;
}

/**
 * Submit a print job to EPFL myPrint
 */
export const epflPrintSubmitJobFn = europeFunctions.https.onCall(
  async (data: PrintJobInput, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { accessToken, file, fileName, mimeType, copies, paperSize, duplex, color, pageRange } = data;

    if (!accessToken || !file || !fileName) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Missing required fields"
      );
    }

    // Validate file size (max 10MB base64 ~ 7.5MB file)
    if (file.length > 10 * 1024 * 1024) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "File too large (max 10MB)"
      );
    }

    try {
      logger.info("epflPrintSubmitJob.start", {
        uid: context.auth.uid,
        fileName,
        mimeType,
        copies,
      });

      // Build print job request for PocketCampus CloudPrint API
      const url = `${POCKETCAMPUS_CONFIG.baseUrl}${POCKETCAMPUS_CONFIG.backendProxy}${POCKETCAMPUS_CONFIG.endpoints.cloudprint}?xhrId=${generateXhrId()}`;

      // Map our options to PocketCampus format
      const printOptions = {
        copies: copies || 1,
        paperSize: mapPaperSize(paperSize),
        duplex: mapDuplex(duplex),
        color: mapColor(color),
        pageRange: pageRange === "ALL" ? "ENTIRE_DOCUMENT" : pageRange,
        pagesPerSheet: "___NO__SELECTION__VALUE___",
        stapling: "___NO__SELECTION__VALUE___",
        holePunching: "___NO__SELECTION__VALUE___",
      };

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "EULER-App/1.0",
          "Accept": "application/json",
          "Authorization": `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          action: "submit_print_job",
          job: {
            file: file,
            fileName: fileName,
            mimeType: mimeType,
            options: printOptions,
          },
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        logger.error("epflPrintSubmitJob.apiError", {
          status: response.status,
          error: errorText,
        });
        throw new functions.https.HttpsError(
          "internal",
          `Print job failed: ${response.status}`
        );
      }

      const result = await response.json();
      const jobId = result.job_id || result.jobId || `job-${Date.now()}`;

      // Log job to Firestore for tracking
      const db = admin.firestore();
      await db.collection("users").doc(context.auth.uid)
        .collection("printJobs").add({
          jobId,
          fileName,
          mimeType,
          copies,
          paperSize,
          duplex,
          color,
          status: "submitted",
          submittedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

      logger.info("epflPrintSubmitJob.success", {
        uid: context.auth.uid,
        jobId,
        fileName,
      });

      return {
        success: true,
        jobId,
        message: `Print job submitted! Go to any EPFL printer and swipe your Camipro card to release.`,
      };
    } catch (error: any) {
      logger.error("epflPrintSubmitJob.error", {
        error: String(error),
        uid: context.auth.uid,
      });

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      throw new functions.https.HttpsError(
        "internal",
        error.message || "Print job submission failed"
      );
    }
  }
);

// =============================================================================
// TOKEN REFRESH
// =============================================================================

interface TokenRefreshInput {
  refreshToken: string;
}

/**
 * Refresh access token using refresh token
 */
export const epflPrintRefreshTokenFn = europeFunctions.https.onCall(
  async (data: TokenRefreshInput, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { refreshToken } = data;

    if (!refreshToken) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Refresh token is required"
      );
    }

    try {
      logger.info("epflPrintRefreshToken.start", { uid: context.auth.uid });

      // Call PocketCampus/Microsoft to refresh the token
      const url = `${POCKETCAMPUS_CONFIG.baseUrl}${POCKETCAMPUS_CONFIG.backendProxy}${POCKETCAMPUS_CONFIG.endpoints.authentication}?xhrId=${generateXhrId()}`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "EULER-App/1.0",
          "Accept": "application/json",
        },
        body: JSON.stringify({
          action: "refresh_token",
          refresh_token: refreshToken,
        }),
      });

      if (!response.ok) {
        logger.error("epflPrintRefreshToken.failed", {
          status: response.status,
        });
        return {
          success: false,
          error: "Token refresh failed",
        };
      }

      const result = await response.json();

      const accessToken = result.access_token || result.token;
      const newRefreshToken = result.refresh_token;
      const expiresIn = result.expires_in || 3600;

      if (!accessToken) {
        return {
          success: false,
          error: "No access token received",
        };
      }

      logger.info("epflPrintRefreshToken.success", { uid: context.auth.uid });

      return {
        success: true,
        accessToken,
        refreshToken: newRefreshToken,
        expiresIn,
      };
    } catch (error: any) {
      logger.error("epflPrintRefreshToken.error", {
        error: String(error),
        uid: context.auth.uid,
      });

      return {
        success: false,
        error: error.message || "Token refresh failed",
      };
    }
  }
);

// =============================================================================
// BALANCE QUERY
// =============================================================================

interface BalanceInput {
  accessToken: string;
}

/**
 * Get print credit balance
 */
export const epflPrintGetBalanceFn = europeFunctions.https.onCall(
  async (data: BalanceInput, context) => {
    if (!context.auth?.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated"
      );
    }

    const { accessToken } = data;

    if (!accessToken) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Access token is required"
      );
    }

    try {
      const url = `${POCKETCAMPUS_CONFIG.baseUrl}${POCKETCAMPUS_CONFIG.backendProxy}${POCKETCAMPUS_CONFIG.endpoints.cloudprint}?xhrId=${generateXhrId()}`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "EULER-App/1.0",
          "Accept": "application/json",
          "Authorization": `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          action: "get_balance",
        }),
      });

      if (!response.ok) {
        throw new functions.https.HttpsError(
          "internal",
          "Failed to get balance"
        );
      }

      const result = await response.json();
      const balance = result.balance || result.credit || 0;

      return {
        success: true,
        balance: parseFloat(balance),
      };
    } catch (error: any) {
      logger.error("epflPrintGetBalance.error", {
        error: String(error),
        uid: context.auth.uid,
      });

      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      throw new functions.https.HttpsError(
        "internal",
        error.message || "Failed to get balance"
      );
    }
  }
);

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Map paper size to PocketCampus format
 */
function mapPaperSize(size: string): string {
  switch (size?.toUpperCase()) {
    case "A4":
      return "4";
    case "A3":
      return "3";
    case "LETTER":
      return "1";
    default:
      return "4"; // Default to A4
  }
}

/**
 * Map duplex option to PocketCampus format
 */
function mapDuplex(duplex: string): string {
  switch (duplex) {
    case "ONE_SIDED":
      return "0";
    case "TWO_SIDED_LONG_EDGE":
      return "1";
    case "TWO_SIDED_SHORT_EDGE":
      return "2";
    default:
      return "1"; // Default to two-sided long edge
  }
}

/**
 * Map color option to PocketCampus format
 */
function mapColor(color: string): string {
  switch (color) {
    case "COLOR":
      return "0";
    case "BLACK_AND_WHITE":
      return "1";
    default:
      return "1"; // Default to B&W
  }
}

