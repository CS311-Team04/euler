package com.android.sample.logic

import com.android.sample.profile.UserProfile

/**
 * Pure Kotlin logic for mapping UserProfile to/from Firestore maps. Extracted from
 * UserProfileRepository for testability.
 */
object UserProfileMapper {

  /** Converts a UserProfile to a Firestore map representation. */
  fun toFirestoreMap(profile: UserProfile): Map<String, Any?> =
      mapOf(
          "fullName" to profile.fullName,
          "preferredName" to profile.preferredName,
          "faculty" to profile.faculty,
          "section" to profile.section,
          "email" to profile.email,
          "phone" to profile.phone,
          "roleDescription" to profile.roleDescription)

  /**
   * Creates a UserProfile from a Firestore map. Handles missing or null fields by using empty
   * strings.
   */
  fun fromFirestore(map: Map<*, *>): UserProfile =
      UserProfile(
          fullName = getStringFromMap(map, "fullName"),
          preferredName = getStringFromMap(map, "preferredName"),
          faculty = getStringFromMap(map, "faculty"),
          section = getStringFromMap(map, "section"),
          email = getStringFromMap(map, "email"),
          phone = getStringFromMap(map, "phone"),
          roleDescription = getStringFromMap(map, "roleDescription"))

  /** Safely extracts a String value from a map, defaulting to empty string. */
  private fun getStringFromMap(map: Map<*, *>, key: String): String {
    return map[key] as? String ?: ""
  }
}
