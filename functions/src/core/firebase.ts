// functions/src/core/firebase.ts

import * as functions from "firebase-functions/v1";
import admin from "firebase-admin";

// Firebase Admin (Firestore)
if (!admin.apps.length) {
  admin.initializeApp();
}
export const db = admin.firestore();

// Region configuration
export const europeFunctions = functions.region("europe-west6");

// API key for HTTP endpoints
const INDEX_API_KEY = process.env.INDEX_API_KEY || "";

/**
 * Check API key for HTTP endpoints
 */
export function checkKey(req: functions.https.Request) {
  if (!INDEX_API_KEY) return;
  if (req.get("x-api-key") !== INDEX_API_KEY) {
    const e = new Error("unauthorized");
    (e as any).code = 401;
    throw e;
  }
}

/**
 * Require authentication and return UID
 */
export function requireAuth(context: functions.https.CallableContext): string {
  const uid = context.auth?.uid;
  if (!uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication is required"
    );
  }
  return uid;
}

