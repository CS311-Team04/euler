package com.android.sample.epfl.print

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

/**
 * ViewModel for EPFL Print connector.
 *
 * Manages:
 * - OAuth WebView flow
 * - Connection status
 * - Print job submission
 */
class EpflPrintViewModel(
    context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "EpflPrintViewModel"
    }

    private val repository = EpflPrintRepository(context)

    private val _uiState = MutableStateFlow(EpflPrintUiState())
    val uiState: StateFlow<EpflPrintUiState> = _uiState.asStateFlow()

    init {
        loadConnectionStatus()
    }

    // =========================================================================
    // CONNECTION MANAGEMENT
    // =========================================================================

    /**
     * Load current connection status
     */
    fun loadConnectionStatus() {
        val isConnected = repository.isConnected()
        val email = repository.getConnectedEmail()

        _uiState.value = _uiState.value.copy(
            isConnected = isConnected,
            connectedEmail = email,
            isLoading = false
        )
    }

    /**
     * Get the OAuth URL to load in WebView
     */
    fun getOAuthUrl(): String {
        return EpflPrintRepository.OAuthConfig.buildAuthUrl()
    }

    /**
     * Check if a URL is the OAuth callback
     */
    fun isOAuthCallback(url: String): Boolean {
        return EpflPrintRepository.OAuthConfig.isCallbackUrl(url)
    }

    /**
     * Handle OAuth callback URL - extract code and exchange for tokens
     */
    fun handleOAuthCallback(callbackUrl: String) {
        val authCode = EpflPrintRepository.OAuthConfig.extractAuthCode(callbackUrl)
        
        if (authCode == null) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to extract authorization code",
                showWebView = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showWebView = false
            )

            when (val result = repository.exchangeCodeForTokens(authCode)) {
                is TokenExchangeResult.Success -> {
                    Log.d(TAG, "Connected as ${result.userEmail}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isConnected = true,
                        connectedEmail = result.userEmail,
                        successMessage = "Connected to EPFL Print",
                        error = null
                    )
                }
                is TokenExchangeResult.Error -> {
                    Log.e(TAG, "Token exchange failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /**
     * Start OAuth flow - show WebView
     */
    fun startOAuthFlow() {
        _uiState.value = _uiState.value.copy(
            showWebView = true,
            error = null
        )
    }

    /**
     * Cancel OAuth flow - hide WebView
     */
    fun cancelOAuthFlow() {
        _uiState.value = _uiState.value.copy(
            showWebView = false
        )
    }

    /**
     * Disconnect from EPFL Print
     */
    fun disconnect() {
        repository.clearTokens()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedEmail = null,
            successMessage = "Disconnected from EPFL Print"
        )
    }

    // =========================================================================
    // PRINT JOB MANAGEMENT
    // =========================================================================

    /**
     * Update print options
     */
    fun updatePrintOptions(options: PrintOptions) {
        _uiState.value = _uiState.value.copy(printOptions = options)
    }

    /**
     * Set file to print (from URI)
     */
    fun setFileToPrint(
        fileName: String,
        mimeType: String,
        inputStream: InputStream
    ) {
        viewModelScope.launch {
            try {
                val bytes = inputStream.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                
                _uiState.value = _uiState.value.copy(
                    pendingFile = PendingPrintFile(
                        fileName = fileName,
                        mimeType = mimeType,
                        base64Data = base64,
                        sizeBytes = bytes.size.toLong()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read file", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to read file: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear pending file
     */
    fun clearPendingFile() {
        _uiState.value = _uiState.value.copy(pendingFile = null)
    }

    /**
     * Submit the print job
     */
    fun submitPrintJob() {
        val file = _uiState.value.pendingFile
        if (file == null) {
            _uiState.value = _uiState.value.copy(error = "No file selected")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)

            val result = repository.submitPrintJob(
                fileBase64 = file.base64Data,
                fileName = file.fileName,
                mimeType = file.mimeType,
                options = _uiState.value.printOptions
            )

            when (result) {
                is PrintJobResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        pendingFile = null,
                        successMessage = result.message,
                        lastJobId = result.jobId
                    )
                }
                is PrintJobResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /**
     * Load credit balance
     */
    fun loadCreditBalance() {
        viewModelScope.launch {
            when (val result = repository.getCreditBalance()) {
                is CreditBalanceResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        creditBalance = result.balance
                    )
                }
                is CreditBalanceResult.Error -> {
                    Log.w(TAG, "Failed to load balance: ${result.message}")
                }
            }
        }
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}

// =============================================================================
// UI STATE
// =============================================================================

/**
 * UI state for EPFL Print connector
 */
data class EpflPrintUiState(
    // Connection state
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val connectedEmail: String? = null,
    val creditBalance: Double? = null,
    
    // OAuth WebView
    val showWebView: Boolean = false,
    
    // Print job state
    val pendingFile: PendingPrintFile? = null,
    val printOptions: PrintOptions = PrintOptions(),
    val isSubmitting: Boolean = false,
    val lastJobId: String? = null,
    
    // Messages
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * File pending to be printed
 */
data class PendingPrintFile(
    val fileName: String,
    val mimeType: String,
    val base64Data: String,
    val sizeBytes: Long
)

