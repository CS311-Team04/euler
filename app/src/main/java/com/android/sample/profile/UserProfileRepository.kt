package com.android.sample.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore access layer for reading and writing the authenticated user's profile information.
 *
 * Firestore structure created/used by this repository:
 * ```
 * users (collection)
 *   └── {uid} (document)
 *         ├── logged: Boolean            // existing flag maintained elsewhere
 *         ├── profile: {                 // map stored via [PROFILE_FIELD]
 *         │       fullName: String
 *         │       preferredName: String
 *         │       faculty: String
 *         │       section: String
 *         │       email: String
 *         │       phone: String
 *         │       roleDescription: String
 *         └── profileUpdatedAt: Timestamp // maintained by [saveProfile]
 * ```
 *
 * All operations assume the user is already authenticated with Firebase Auth.
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  private val usersCollection = firestore.collection(COLLECTION_USERS)

  /**
   * Persist the supplied [profile] for the currently signed-in user. The profile map is merged into
   * the existing `users/{uid}` document so no other fields (like the `logged` flag) are lost.
   */
  suspend fun saveProfile(profile: UserProfile) {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot save profile without an authenticated user.")

    val payload =
        mapOf(
            PROFILE_FIELD to profile.toFirestoreMap(),
            PROFILE_UPDATED_AT_FIELD to FieldValue.serverTimestamp())

    usersCollection.document(uid).set(payload, SetOptions.merge()).await()
  }

  /**
   * Retrieve the stored profile for the currently signed-in user. Returns `null` when no profile
   * has been saved yet.
   */
  suspend fun loadProfile(): UserProfile? {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot load profile without an authenticated user.")

    val snapshot = usersCollection.document(uid).get().await()
    if (!snapshot.exists()) return null

    val rawProfile = snapshot.get(PROFILE_FIELD) as? Map<*, *> ?: return null
    return UserProfile.fromFirestore(rawProfile)
  }

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val PROFILE_FIELD = "profile"
    private const val PROFILE_UPDATED_AT_FIELD = "profileUpdatedAt"
  }
}

/** Data model representing the editable user profile fields shown in the profile screen. */
data class UserProfile(
    val fullName: String = "",
    val preferredName: String = "",
    val faculty: String = "",
    val section: String = "",
    val email: String = "",
    val phone: String = "",
    val roleDescription: String = "",
) {
  internal fun toFirestoreMap(): Map<String, Any?> =
      mapOf(
          "fullName" to fullName,
          "preferredName" to preferredName,
          "faculty" to faculty,
          "section" to section,
          "email" to email,
          "phone" to phone,
          "roleDescription" to roleDescription)

  companion object {
    internal fun fromFirestore(map: Map<*, *>): UserProfile =
        UserProfile(
            fullName = map["fullName"] as? String ?: "",
            preferredName = map["preferredName"] as? String ?: "",
            faculty = map["faculty"] as? String ?: "",
            section = map["section"] as? String ?: "",
            email = map["email"] as? String ?: "",
            phone = map["phone"] as? String ?: "",
            roleDescription = map["roleDescription"] as? String ?: "")
  }
}
