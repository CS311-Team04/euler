package com.android.sample.logic

import com.android.sample.profile.UserProfile

/**
 * Pure Kotlin logic for UserProfileRepository decision-making. Extracted from UserProfileRepository
 * for testability.
 */
object UserProfileRepositoryLogic {
  const val COLLECTION_USERS = "users"
  const val PROFILE_FIELD = "profile"
  const val PROFILE_UPDATED_AT_FIELD = "profileUpdatedAt"

  /** Determines if save operation should proceed based on auth availability. */
  fun shouldProceedWithSave(authAvailable: Boolean, uidAvailable: Boolean): Boolean {
    return authAvailable && uidAvailable
  }

  /** Determines if load operation should proceed based on auth availability. */
  fun shouldProceedWithLoad(authAvailable: Boolean, uidAvailable: Boolean): Boolean {
    return authAvailable && uidAvailable
  }

  /** Builds the payload map for saving profile. */
  fun buildSavePayload(profile: UserProfile): Map<String, Any> {
    return mapOf(PROFILE_FIELD to UserProfileMapper.toFirestoreMap(profile))
  }

  /** Builds the document path for user profile. */
  fun buildDocumentPath(uid: String): String {
    return "$COLLECTION_USERS/$uid"
  }

  /** Determines if profile exists in snapshot. */
  fun hasProfileInSnapshot(snapshotExists: Boolean, hasProfileField: Boolean): Boolean {
    return snapshotExists && hasProfileField
  }

  /** Extracts profile from snapshot data. */
  fun extractProfileFromSnapshot(profileData: Any?): Map<*, *>? {
    return profileData as? Map<*, *>
  }

  /** Determines if operation should return null (error handling). */
  fun shouldReturnNull(
      authAvailable: Boolean,
      uidAvailable: Boolean,
      firestoreAvailable: Boolean
  ): Boolean {
    return !authAvailable || !uidAvailable || !firestoreAvailable
  }

  /** Validates UID is not empty. */
  fun isValidUid(uid: String?): Boolean {
    return !uid.isNullOrBlank()
  }

  /** Handles error in repository operation. */
  fun handleRepositoryError(operation: String, error: Throwable?): String {
    return "Failed to $operation: ${error?.message ?: "Unknown error"}"
  }
}
