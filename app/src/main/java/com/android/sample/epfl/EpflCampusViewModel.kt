package com.android.sample.epfl

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for EPFL Campus schedule connector screen.
 *
 * Manages:
 * - Connection status
 * - ICS URL input and validation
 * - Clipboard detection
 * - Sync operations
 */
class EpflCampusViewModel(
    private val repository: EpflScheduleRepository = EpflScheduleRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(EpflCampusUiState())
  val uiState: StateFlow<EpflCampusUiState> = _uiState.asStateFlow()

  init {
    loadStatus()
  }

  /** Load current connection status */
  fun loadStatus() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      when (val status = repository.getStatus()) {
        is ScheduleStatus.Connected -> {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false,
                  isConnected = true,
                  weeklySlots = status.weeklySlots,
                  finalExams = status.finalExams,
                  lastSync = status.lastSync,
                  error = null)
        }
        is ScheduleStatus.NotConnected -> {
          _uiState.value =
              _uiState.value.copy(
                  isLoading = false,
                  isConnected = false,
                  weeklySlots = 0,
                  finalExams = 0,
                  lastSync = null,
                  error = null)
        }
        is ScheduleStatus.Error -> {
          _uiState.value = _uiState.value.copy(isLoading = false, error = status.message)
        }
      }
    }
  }

  /** Update the ICS URL input */
  fun updateIcsUrl(url: String) {
    _uiState.value =
        _uiState.value.copy(
            icsUrlInput = url,
            isValidUrl = repository.isValidHttpUrl(url),
            isLikelyEpflUrl = repository.isLikelyEpflUrl(url))
  }

  /** Check clipboard for ICS URL when app resumes */
  fun checkClipboard(context: Context) {
    try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()

      if (clip != null && repository.isValidHttpUrl(clip)) {
        // Check if it looks like an EPFL/calendar URL
        if (repository.isLikelyEpflUrl(clip) && clip != _uiState.value.icsUrlInput) {
          _uiState.value =
              _uiState.value.copy(detectedClipboardUrl = clip, showClipboardSuggestion = true)
        }
      }
    } catch (e: Exception) {
      // Clipboard access might fail on some devices
    }
  }

  /** Accept the detected clipboard URL */
  fun acceptClipboardUrl() {
    val url = _uiState.value.detectedClipboardUrl
    if (url != null) {
      updateIcsUrl(url)
    }
    dismissClipboardSuggestion()
  }

  /** Dismiss clipboard suggestion */
  fun dismissClipboardSuggestion() {
    _uiState.value =
        _uiState.value.copy(showClipboardSuggestion = false, detectedClipboardUrl = null)
  }

  /** Sync schedule from the current ICS URL */
  fun syncSchedule() {
    val url = _uiState.value.icsUrlInput
    if (!repository.isValidHttpUrl(url)) {
      _uiState.value = _uiState.value.copy(error = "Invalid URL")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSyncing = true, error = null)

      when (val result = repository.syncSchedule(url)) {
        is SyncResult.Success -> {
          _uiState.value =
              _uiState.value.copy(
                  isSyncing = false,
                  isConnected = true,
                  weeklySlots = result.weeklySlots,
                  finalExams = result.finalExams,
                  successMessage = result.message,
                  icsUrlInput = "")
          // Reload status to get lastSync
          loadStatus()
        }
        is SyncResult.Error -> {
          _uiState.value = _uiState.value.copy(isSyncing = false, error = result.message)
        }
      }
    }
  }

  /** Disconnect EPFL schedule */
  fun disconnect() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      val success = repository.disconnect()
      if (success) {
        _uiState.value = EpflCampusUiState() // Reset to initial state
      } else {
        _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to disconnect")
      }
    }
  }

  /** Clear error message */
  fun clearError() {
    _uiState.value = _uiState.value.copy(error = null)
  }

  /** Clear success message */
  fun clearSuccessMessage() {
    _uiState.value = _uiState.value.copy(successMessage = null)
  }

  /** Open EPFL Campus app or web */
  fun openEpflCampus(context: Context) {
    // Try to open EPFL Campus (PocketCampus) app first
    val pocketCampusPackage = "org.pocketcampus"

    try {
      // getLaunchIntentForPackage returns null if app isn't installed
      val launchIntent = context.packageManager.getLaunchIntentForPackage(pocketCampusPackage)
      if (launchIntent != null) {
        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(launchIntent)
        return
      }
    } catch (e: Exception) {
      // Error getting launch intent, fall through to web
    }

    // Fallback to EPFL Campus web
    try {
      val webIntent =
          Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://campus.epfl.ch")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
      context.startActivity(webIntent)
    } catch (e: Exception) {
      _uiState.value = _uiState.value.copy(error = "Could not open EPFL Campus")
    }
  }
}

/** UI state for EPFL Campus connector */
data class EpflCampusUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isConnected: Boolean = false,
    val weeklySlots: Int = 0,
    val finalExams: Int = 0,
    val lastSync: String? = null,
    val icsUrlInput: String = "",
    val isValidUrl: Boolean = false,
    val isLikelyEpflUrl: Boolean = false,
    val detectedClipboardUrl: String? = null,
    val showClipboardSuggestion: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
