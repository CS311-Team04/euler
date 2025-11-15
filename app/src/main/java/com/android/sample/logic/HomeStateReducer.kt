package com.android.sample.logic

import com.android.sample.home.HomeUiState
import com.android.sample.profile.UserProfile

/**
 * Pure Kotlin logic for HomeViewModel state reductions. Extracted from HomeViewModel for
 * testability.
 */
object HomeStateReducer {

  private const val DEFAULT_USER_NAME = "Student"

  /**
   * Calculates the display name from a UserProfile. Prefers preferredName, then fullName, then
   * default.
   */
  fun calculateUserName(profile: UserProfile?): String {
    return when {
      profile == null -> DEFAULT_USER_NAME
      profile.preferredName.isNotBlank() -> profile.preferredName
      profile.fullName.isNotBlank() -> profile.fullName
      else -> DEFAULT_USER_NAME
    }
  }

  /** Calculates the display name with fallback to existing userName. */
  fun calculateUserNameWithFallback(profile: UserProfile?, currentUserName: String): String {
    val calculated = calculateUserName(profile)
    return if (calculated != DEFAULT_USER_NAME || currentUserName.isBlank()) {
      calculated
    } else {
      currentUserName
    }
  }

  /** Reduces state for setting guest mode. */
  fun setGuestMode(currentState: HomeUiState, isGuest: Boolean): HomeUiState {
    return if (isGuest) {
      currentState.copy(
          isGuest = true, profile = null, userName = "guest", showGuestProfileWarning = false)
    } else {
      val userName = currentState.userName.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME
      currentState.copy(isGuest = false, userName = userName)
    }
  }

  /** Reduces state for refreshing profile. */
  fun refreshProfile(currentState: HomeUiState, profile: UserProfile?): HomeUiState {
    return if (profile != null) {
      val userName = calculateUserNameWithFallback(profile, currentState.userName)
      currentState.copy(profile = profile, userName = userName, isGuest = false)
    } else {
      val userName = currentState.userName.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME
      currentState.copy(profile = null, userName = userName, isGuest = false)
    }
  }

  /** Reduces state for saving profile. */
  fun saveProfile(currentState: HomeUiState, profile: UserProfile): HomeUiState {
    val userName = calculateUserName(profile)
    return currentState.copy(profile = profile, userName = userName, isGuest = false)
  }

  /** Reduces state for clearing profile. */
  fun clearProfile(currentState: HomeUiState): HomeUiState {
    return currentState.copy(
        profile = null,
        userName = DEFAULT_USER_NAME,
        isGuest = false,
        showGuestProfileWarning = false)
  }

  /** Reduces state for showing guest profile warning. */
  fun showGuestProfileWarning(currentState: HomeUiState): HomeUiState {
    return currentState.copy(showGuestProfileWarning = true)
  }

  /** Reduces state for hiding guest profile warning. */
  fun hideGuestProfileWarning(currentState: HomeUiState): HomeUiState {
    return currentState.copy(showGuestProfileWarning = false)
  }

  /** Reduces state for toggling drawer. */
  fun toggleDrawer(currentState: HomeUiState): HomeUiState {
    return currentState.copy(isDrawerOpen = !currentState.isDrawerOpen)
  }

  /** Reduces state for setting top right menu visibility. */
  fun setTopRightOpen(currentState: HomeUiState, open: Boolean): HomeUiState {
    return currentState.copy(isTopRightOpen = open)
  }

  /** Reduces state for starting local new chat. */
  fun startLocalNewChat(currentState: HomeUiState): HomeUiState {
    return currentState.copy(
        currentConversationId = null,
        messages = emptyList(),
        messageDraft = "",
        isSending = false,
        showDeleteConfirmation = false)
  }

  /** Reduces state for signing out (reset to defaults). */
  fun onSignedOut(currentState: HomeUiState): HomeUiState {
    return HomeUiState(
        systems = currentState.systems, // Preserve system items
        messages = emptyList())
  }
}
