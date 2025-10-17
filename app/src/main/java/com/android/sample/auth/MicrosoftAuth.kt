package com.android.sample.auth

import android.app.Activity
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.json.JSONObject

/**
 * Microsoft OAuth authentication using Firebase Auth. Provides tenant validation and error
 * handling.
 */
object MicrosoftAuth {
  private const val TAG = "MicrosoftAuth"

  // Configuration via environment variables or BuildConfig
  private val expectedTenantId: String = System.getenv("MICROSOFT_TENANT_ID") ?: ""
  private val domainHint: String = System.getenv("MICROSOFT_DOMAIN_HINT") ?: ""

  /**
   * Initiates Microsoft sign-in flow.
   *
   * @param activity Current activity context
   * @param onSuccess Callback for successful authentication
   * @param onError Callback for authentication errors
   */
  fun signIn(activity: Activity, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    Log.d(TAG, "Starting Microsoft sign-in process")

    try {
      val builder =
          OAuthProvider.newBuilder("microsoft.com")
              .setScopes(
                  listOf("openid", "email", "profile", "https://graph.microsoft.com/User.Read"))
              .addCustomParameter("prompt", "select_account")

      if (domainHint.isNotBlank()) {
        builder.addCustomParameter("domain_hint", domainHint)
        Log.d(TAG, "Added domain hint: $domainHint")
      }

      val auth = Firebase.auth

      // Handle pending auth result (e.g., after rotation)
      val pending = auth.pendingAuthResult
      if (pending != null) {
        Log.d(TAG, "Resuming pending authentication flow")
        pending
            .addOnSuccessListener { result ->
              Log.d(TAG, "Pending authentication successful")
              handleAuthResult(result.credential as? OAuthCredential, onSuccess, onError)
            }
            .addOnFailureListener { exception ->
              Log.e(TAG, "Pending authentication failed", exception)
              onError(exception)
            }
        return
      }

      Log.d(TAG, "Starting new authentication flow")
      auth
          .startActivityForSignInWithProvider(activity, builder.build())
          .addOnSuccessListener { result ->
            Log.d(TAG, "Authentication successful")
            handleAuthResult(result.credential as? OAuthCredential, onSuccess, onError)
          }
          .addOnFailureListener { exception ->
            Log.e(TAG, "Authentication failed", exception)
            onError(exception)
          }
    } catch (exception: Exception) {
      Log.e(TAG, "Unexpected error during sign-in setup", exception)
      onError(exception)
    }
  }

  /** Handles authentication result and validates tenant if required. */
  private fun handleAuthResult(
      credential: OAuthCredential?,
      onSuccess: () -> Unit,
      onError: (Exception) -> Unit
  ) {
    try {
      Log.d(TAG, "Processing authentication result...")
      Log.d(TAG, "Credential: ${credential != null}")
      Log.d(TAG, "ID Token: ${credential?.idToken != null}")
      Log.d(TAG, "Access Token: ${credential?.accessToken != null}")

      if (validateTenantIfRequired(credential)) {
        Log.d(TAG, "Tenant validation passed")

        // Check Firebase auth state
        val currentUser = Firebase.auth.currentUser
        Log.d(TAG, "Firebase currentUser: ${currentUser?.uid}")
        Log.d(TAG, "Firebase currentUser email: ${currentUser?.email}")
        Log.d(TAG, "Firebase currentUser displayName: ${currentUser?.displayName}")

        onSuccess()
      } else {
        Log.w(TAG, "Tenant validation failed - signing out user")
        Firebase.auth.signOut()
        onError(IllegalAccessException("Access denied: unauthorized tenant"))
      }
    } catch (exception: Exception) {
      Log.e(TAG, "Error during tenant validation", exception)
      onError(exception)
    }
  }

  /** Validates tenant ID if required by configuration. */
  private fun validateTenantIfRequired(credential: OAuthCredential?): Boolean {
    if (expectedTenantId.isBlank()) {
      Log.d(TAG, "Tenant validation disabled - no expected tenant ID configured")
      return true
    }

    val idToken = credential?.idToken
    if (idToken == null) {
      Log.w(TAG, "No ID token found in credential")
      return false
    }

    val tenantId = extractTenantIdFromJwt(idToken)
    if (tenantId == null) {
      Log.w(TAG, "Could not extract tenant ID from JWT")
      return false
    }

    val isValid = tenantId.equals(expectedTenantId, ignoreCase = true)
    Log.d(TAG, "Tenant validation: expected=$expectedTenantId, actual=$tenantId, valid=$isValid")

    return isValid
  }

  /** Extracts tenant ID from JWT token. */
  private fun extractTenantIdFromJwt(idToken: String): String? {
    try {
      val parts = idToken.split(".")
      if (parts.size < 2) {
        Log.w(TAG, "Invalid JWT format: expected at least 2 parts, got ${parts.size}")
        return null
      }

      val payloadB64url = parts[1]
      val payloadB64 = payloadB64url.replace('-', '+').replace('_', '/')
      val padded =
          when (payloadB64.length % 4) {
            2 -> "$payloadB64=="
            3 -> "$payloadB64="
            else -> payloadB64
          }

      val bytes =
          try {
            Base64.decode(padded, Base64.DEFAULT)
          } catch (e: Exception) {
            Log.w(TAG, "Failed to decode JWT payload", e)
            return null
          }

      val json =
          try {
            JSONObject(String(bytes))
          } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JWT payload as JSON", e)
            return null
          }

      val tenantId = json.optString("tid", "")
      Log.d(TAG, "Extracted tenant ID from JWT: $tenantId")
      return tenantId
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error extracting tenant ID from JWT", e)
      return null
    }
  }
}
