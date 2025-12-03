package com.android.sample.epfl

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Repository for EPFL Campus schedule integration.
 *
 * Handles:
 * - Syncing schedule from ICS URL
 * - Getting connection status
 * - Disconnecting schedule
 */
class EpflScheduleRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west6")
) {
  companion object {
    private const val TAG = "EpflScheduleRepository"
  }

  /** Check if user is authenticated */
  fun isAuthenticated(): Boolean = auth.currentUser != null

  /**
   * Sync EPFL schedule from ICS URL
   *
   * @param icsUrl The ICS calendar URL from IS-Academia
   * @return Result with success status and event count
   */
  suspend fun syncSchedule(icsUrl: String): SyncResult {
    if (!isAuthenticated()) {
      return SyncResult.Error("User not authenticated")
    }

    return try {
      Log.d(TAG, "Syncing schedule from ICS URL...")

      val data = hashMapOf("icsUrl" to icsUrl)
      val result = functions.getHttpsCallable("syncEpflScheduleFn").call(data).await()

      @Suppress("UNCHECKED_CAST") val response = result.getData() as? Map<String, Any>

      val success = response?.get("success") as? Boolean ?: false
      val weeklySlots = (response?.get("weeklySlots") as? Number)?.toInt() ?: 0
      val finalExams = (response?.get("finalExams") as? Number)?.toInt() ?: 0
      val message = response?.get("message") as? String ?: "Schedule synced"

      if (success) {
        Log.d(TAG, "Schedule synced: $weeklySlots weekly slots, $finalExams exams")
        SyncResult.Success(weeklySlots, finalExams, message)
      } else {
        SyncResult.Error(message)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync schedule", e)
      SyncResult.Error(e.message ?: "Unknown error")
    }
  }

  /** Get current schedule connection status */
  suspend fun getStatus(): ScheduleStatus {
    if (!isAuthenticated()) {
      return ScheduleStatus.NotConnected
    }

    return try {
      val result = functions.getHttpsCallable("getEpflScheduleStatusFn").call().await()

      @Suppress("UNCHECKED_CAST") val response = result.getData() as? Map<String, Any>

      val connected = response?.get("connected") as? Boolean ?: false

      if (connected) {
        val weeklySlots = (response?.get("weeklySlots") as? Number)?.toInt() ?: 0
        val finalExams = (response?.get("finalExams") as? Number)?.toInt() ?: 0
        val lastSync = response?.get("lastSync") as? String
        val optimized = response?.get("optimized") as? Boolean ?: true
        ScheduleStatus.Connected(weeklySlots, finalExams, lastSync, optimized)
      } else {
        ScheduleStatus.NotConnected
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get status", e)
      ScheduleStatus.Error(e.message ?: "Unknown error")
    }
  }

  /** Disconnect EPFL schedule */
  suspend fun disconnect(): Boolean {
    if (!isAuthenticated()) {
      return false
    }

    return try {
      functions.getHttpsCallable("disconnectEpflScheduleFn").call().await()

      Log.d(TAG, "Schedule disconnected successfully")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to disconnect", e)
      false
    }
  }

  /** Validate if a URL has a valid HTTP/HTTPS scheme */
  fun isValidHttpUrl(url: String): Boolean {
    val trimmed = url.trim()
    return trimmed.startsWith("http://") || trimmed.startsWith("https://")
  }

  /**
   * Check if URL looks like it might be from EPFL/IS-Academia
   *
   * EPFL Campus ICS URLs look like:
   * https://campus.epfl.ch/deploy/backend_proxy/{id}/raw-isacademia?action=get_ics&key={key}
   *
   * Note: We intentionally do NOT match generic ".ics" URLs since those could be from any calendar
   * provider (Google, Outlook, etc.). Only match EPFL-specific patterns.
   */
  fun isLikelyEpflUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains("campus.epfl.ch") ||
        lower.contains("raw-isacademia") ||
        lower.contains("action=get_ics") ||
        lower.contains(".epfl.ch") ||
        lower.contains("isa.epfl") ||
        lower.contains("isacademia")
  }
}

/** Result of schedule sync operation */
sealed class SyncResult {
  data class Success(val weeklySlots: Int, val finalExams: Int, val message: String) : SyncResult()

  data class Error(val message: String) : SyncResult()
}

/** Current schedule connection status */
sealed class ScheduleStatus {
  object NotConnected : ScheduleStatus()

  data class Connected(
      val weeklySlots: Int,
      val finalExams: Int,
      val lastSync: String?,
      val optimized: Boolean = true
  ) : ScheduleStatus()

  data class Error(val message: String) : ScheduleStatus()
}
