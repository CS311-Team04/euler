package com.android.sample.epfl.print

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Repository for EPFL Print (PocketCampus CloudPrint) integration.
 *
 * Handles:
 * - OAuth token storage (encrypted)
 * - Print job submission via Cloud Functions
 * - Connection status management
 */
class EpflPrintRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west6")
) {
    companion object {
        private const val TAG = "EpflPrintRepository"
        private const val PREFS_FILE = "epfl_print_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_EMAIL = "user_email"
    }

    // Encrypted SharedPreferences for secure token storage
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // =========================================================================
    // OAUTH CONFIGURATION
    // =========================================================================

    /**
     * OAuth configuration for PocketCampus/Microsoft Entra ID
     */
    object OAuthConfig {
        const val AUTHORIZE_URL = "https://login.microsoftonline.com/f6c2556a-c4fb-4ab1-a2c7-9e220df11c43/oauth2/v2.0/authorize"
        const val CLIENT_ID = "10ffa8fc-719a-4695-8e10-192edcc095c1"
        const val SCOPE = "offline_access openid profile email"
        const val REDIRECT_URI = "https://link.pocketcampus.org/pocketcampus:WEB/authentication/callback"
        
        /**
         * Build the full OAuth authorization URL
         */
        fun buildAuthUrl(): String {
            return "$AUTHORIZE_URL?" +
                "scope=${java.net.URLEncoder.encode(SCOPE, "UTF-8")}&" +
                "response_type=code&" +
                "client_id=$CLIENT_ID&" +
                "redirect_uri=${java.net.URLEncoder.encode(REDIRECT_URI, "UTF-8")}&" +
                "prompt=select_account"
        }
        
        /**
         * Check if a URL is the OAuth callback
         */
        fun isCallbackUrl(url: String): Boolean {
            return url.startsWith(REDIRECT_URI) || 
                   url.startsWith("https://campus.epfl.ch/authentication/callback")
        }
        
        /**
         * Extract authorization code from callback URL
         */
        fun extractAuthCode(callbackUrl: String): String? {
            val uri = android.net.Uri.parse(callbackUrl)
            return uri.getQueryParameter("code")
        }
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Store OAuth tokens securely
     */
    fun storeTokens(accessToken: String, refreshToken: String?, expiresIn: Long, userEmail: String?) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            putLong(KEY_TOKEN_EXPIRY, expiryTime)
            userEmail?.let { putString(KEY_USER_EMAIL, it) }
            apply()
        }
        
        Log.d(TAG, "Tokens stored successfully for ${userEmail ?: "unknown user"}")
    }

    /**
     * Get stored access token (null if not connected or expired)
     */
    fun getAccessToken(): String? {
        val token = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        
        // Check if token is expired (with 5 min buffer)
        if (System.currentTimeMillis() > expiry - 300_000) {
            Log.d(TAG, "Access token expired")
            return null
        }
        
        return token
    }

    /**
     * Get access token, refreshing if needed
     * This is a suspend function that will attempt to refresh the token if expired
     */
    suspend fun getAccessTokenRefreshing(): String? {
        // First check if we have a valid token
        val validToken = getAccessToken()
        if (validToken != null) {
            return validToken
        }
        
        // Token is expired or missing, try to refresh
        val refreshToken = getRefreshToken() ?: return null
        
        Log.d(TAG, "Access token expired, attempting refresh...")
        return refreshTokens(refreshToken)
    }

    /**
     * Force refresh the access token using the refresh token
     */
    suspend fun refreshTokens(refreshToken: String? = null): String? {
        val tokenToUse = refreshToken ?: getRefreshToken() ?: return null
        
        if (auth.currentUser == null) {
            Log.e(TAG, "Cannot refresh tokens: user not authenticated")
            return null
        }

        return try {
            val data = hashMapOf(
                "refreshToken" to tokenToUse
            )
            
            val result = functions
                .getHttpsCallable("epflPrintRefreshTokenFn")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.getData() as? Map<String, Any>

            val success = response?.get("success") as? Boolean ?: false
            
            if (success) {
                val accessToken = response["accessToken"] as? String ?: return null
                val newRefreshToken = response["refreshToken"] as? String
                val expiresIn = (response["expiresIn"] as? Number)?.toLong() ?: 3600
                
                // Store new tokens
                storeTokens(
                    accessToken, 
                    newRefreshToken ?: tokenToUse, 
                    expiresIn, 
                    getConnectedEmail()
                )
                
                Log.d(TAG, "Token refresh successful")
                accessToken
            } else {
                val error = response?.get("error") as? String ?: "Token refresh failed"
                Log.e(TAG, "Token refresh failed: $error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception", e)
            null
        }
    }

    /**
     * Get stored refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Get connected user email
     */
    fun getConnectedEmail(): String? {
        return encryptedPrefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Check if user is connected to EPFL Print
     */
    fun isConnected(): Boolean {
        return getAccessToken() != null
    }

    /**
     * Clear all stored tokens (disconnect)
     */
    fun clearTokens() {
        encryptedPrefs.edit().clear().apply()
        Log.d(TAG, "Tokens cleared - disconnected from EPFL Print")
    }

    // =========================================================================
    // CLOUD FUNCTION CALLS
    // =========================================================================

    /**
     * Exchange OAuth code for tokens via Cloud Function
     * 
     * The Cloud Function handles the actual token exchange with Microsoft,
     * so we don't expose client secrets in the app.
     */
    suspend fun exchangeCodeForTokens(authCode: String): TokenExchangeResult {
        if (auth.currentUser == null) {
            return TokenExchangeResult.Error("User not authenticated with EULER")
        }

        return try {
            Log.d(TAG, "Exchanging auth code for tokens...")
            
            val data = hashMapOf(
                "authCode" to authCode,
                "redirectUri" to OAuthConfig.REDIRECT_URI
            )
            
            val result = functions
                .getHttpsCallable("epflPrintExchangeTokenFn")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.getData() as? Map<String, Any>

            val success = response?.get("success") as? Boolean ?: false
            
            if (success) {
                val accessToken = response["accessToken"] as? String ?: ""
                val refreshToken = response["refreshToken"] as? String
                val expiresIn = (response["expiresIn"] as? Number)?.toLong() ?: 3600
                val userEmail = response["userEmail"] as? String
                
                // Store tokens locally
                storeTokens(accessToken, refreshToken, expiresIn, userEmail)
                
                TokenExchangeResult.Success(userEmail ?: "Connected")
            } else {
                val error = response?.get("error") as? String ?: "Token exchange failed"
                TokenExchangeResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed", e)
            TokenExchangeResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Submit a print job via Cloud Function
     */
    suspend fun submitPrintJob(
        fileBase64: String,
        fileName: String,
        mimeType: String,
        options: PrintOptions
    ): PrintJobResult {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            return PrintJobResult.Error("Not connected to EPFL Print")
        }

        if (auth.currentUser == null) {
            return PrintJobResult.Error("User not authenticated with EULER")
        }

        return try {
            Log.d(TAG, "Submitting print job: $fileName")
            
            val data = hashMapOf(
                "accessToken" to accessToken,
                "file" to fileBase64,
                "fileName" to fileName,
                "mimeType" to mimeType,
                "copies" to options.copies,
                "paperSize" to options.paperSize,
                "duplex" to options.duplex,
                "color" to options.color,
                "pageRange" to options.pageRange
            )
            
            val result = functions
                .getHttpsCallable("epflPrintSubmitJobFn")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.getData() as? Map<String, Any>

            val success = response?.get("success") as? Boolean ?: false
            
            if (success) {
                val jobId = response["jobId"] as? String ?: "unknown"
                val message = response["message"] as? String ?: "Print job submitted"
                PrintJobResult.Success(jobId, message)
            } else {
                val error = response?.get("error") as? String ?: "Print job failed"
                PrintJobResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Print job submission failed", e)
            PrintJobResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Get print credit balance
     */
    suspend fun getCreditBalance(): CreditBalanceResult {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            return CreditBalanceResult.Error("Not connected to EPFL Print")
        }

        return try {
            val data = hashMapOf("accessToken" to accessToken)
            
            val result = functions
                .getHttpsCallable("epflPrintGetBalanceFn")
                .call(data)
                .await()

            @Suppress("UNCHECKED_CAST")
            val response = result.getData() as? Map<String, Any>

            val success = response?.get("success") as? Boolean ?: false
            
            if (success) {
                val balance = (response["balance"] as? Number)?.toDouble() ?: 0.0
                CreditBalanceResult.Success(balance)
            } else {
                CreditBalanceResult.Error("Failed to get balance")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get balance failed", e)
            CreditBalanceResult.Error(e.message ?: "Unknown error")
        }
    }
}

// =============================================================================
// DATA CLASSES
// =============================================================================

/**
 * Print job options
 */
data class PrintOptions(
    val copies: Int = 1,
    val paperSize: String = "A4",
    val duplex: String = "TWO_SIDED_LONG_EDGE",
    val color: String = "BLACK_AND_WHITE",
    val pageRange: String = "ALL"
)

/**
 * Result of token exchange operation
 */
sealed class TokenExchangeResult {
    data class Success(val userEmail: String) : TokenExchangeResult()
    data class Error(val message: String) : TokenExchangeResult()
}

/**
 * Result of print job submission
 */
sealed class PrintJobResult {
    data class Success(val jobId: String, val message: String) : PrintJobResult()
    data class Error(val message: String) : PrintJobResult()
}

/**
 * Result of credit balance query
 */
sealed class CreditBalanceResult {
    data class Success(val balance: Double) : CreditBalanceResult()
    data class Error(val message: String) : CreditBalanceResult()
}

