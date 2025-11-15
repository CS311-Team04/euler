package com.android.sample.logic

import com.android.sample.profile.UserProfile

/**
 * Pure Kotlin logic for ProfilePage business decisions. Extracted from ProfilePage for testability.
 */
object ProfilePageLogic {

  /**
   * Resolves the email to use for profile initialization. Priority: profile email > auth email >
   * empty string
   */
  fun resolveEmail(profileEmail: String?, authEmail: String): String {
    val trimmedProfileEmail = profileEmail?.trim().orEmpty()
    return when {
      trimmedProfileEmail.isNotBlank() -> trimmedProfileEmail
      authEmail.isNotBlank() -> authEmail
      else -> ""
    }
  }

  /**
   * Extracts email from Firebase auth result (simulated for testing). In real code, this would be
   * FirebaseAuth.getInstance().currentUser?.email
   */
  fun extractAuthEmail(firebaseAuthEmail: String?): String {
    return firebaseAuthEmail.orEmpty()
  }

  /** Determines if a field should be enabled based on lock state. */
  fun isFieldEnabled(isLocked: Boolean): Boolean {
    return !isLocked
  }

  /** Determines if saved confirmation banner should show. */
  fun shouldShowSavedConfirmation(showConfirmation: Boolean): Boolean {
    return showConfirmation
  }

  /** Determines if confirmation should be reset immediately (test mode). */
  fun shouldResetConfirmationImmediately(isTesting: Boolean, skipDelayForTests: Boolean): Boolean {
    return isTesting || skipDelayForTests
  }

  /** Determines confirmation delay duration in milliseconds. */
  fun getConfirmationDelay(isTesting: Boolean, skipDelayForTests: Boolean): Long {
    return if (shouldResetConfirmationImmediately(isTesting, skipDelayForTests)) {
      0L
    } else {
      2500L
    }
  }

  /**
   * Determines if save button should be enabled. In a real scenario, this might check if form has
   * changes.
   */
  fun isSaveButtonEnabled(): Boolean = true

  /** Validates if profile can be saved. */
  fun canSaveProfile(profile: UserProfile): Boolean {
    // Basic validation - at least one field should be filled
    return profile.fullName.isNotBlank() ||
        profile.preferredName.isNotBlank() ||
        profile.email.isNotBlank() ||
        profile.faculty.isNotBlank() ||
        profile.section.isNotBlank() ||
        profile.roleDescription.isNotBlank() ||
        profile.phone.isNotBlank()
  }

  /** Determines placeholder text for email field. */
  fun getEmailPlaceholder(): String = "name@epfl.ch"

  /** Determines placeholder text for phone field. */
  fun getPhonePlaceholder(): String = "+41 00 000 00 00"

  /** Determines default role options. */
  fun getDefaultRoleOptions(): List<String> {
    return listOf("Student", "PhD", "Post-Doc", "Teacher", "Administration", "Staff")
  }

  /** Checks if initial profile needs to be reset. */
  fun shouldResetInitialProfile(
      currentInitialProfile: UserProfile?,
      newInitialProfile: UserProfile?
  ): Boolean {
    // Reset if profiles are different
    return currentInitialProfile != newInitialProfile
  }

  /** Checks if auth email changed and needs reset. */
  fun shouldResetForAuthEmail(currentAuthEmail: String, newAuthEmail: String): Boolean {
    return currentAuthEmail != newAuthEmail
  }
}
