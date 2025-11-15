package com.android.sample.profile

import com.android.sample.logic.UserProfileMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

interface ProfileDataSource {
  suspend fun saveProfile(profile: UserProfile)

  suspend fun loadProfile(): UserProfile?
}

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
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() },
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
) : ProfileDataSource {

  /**
   * Persist the supplied [profile] for the currently signed-in user. The profile map is merged into
   * the existing `users/{uid}` document so no other fields (like the `logged` flag) are lost.
   */
  override suspend fun saveProfile(profile: UserProfile) {
    val auth = runCatching { authProvider() }.getOrNull() ?: return
    val uid = auth.currentUser?.uid ?: return

    val payload =
        mapOf(
            PROFILE_FIELD to UserProfileMapper.toFirestoreMap(profile),
            PROFILE_UPDATED_AT_FIELD to FieldValue.serverTimestamp())

    val firestore = runCatching { firestoreProvider() }.getOrNull() ?: return
    firestore.collection(COLLECTION_USERS).document(uid).set(payload, SetOptions.merge()).await()
  }

  /**
   * Retrieve the stored profile for the currently signed-in user. Returns `null` when no profile
   * has been saved yet.
   */
  override suspend fun loadProfile(): UserProfile? {
    val auth = runCatching { authProvider() }.getOrNull() ?: return null
    val uid = auth.currentUser?.uid ?: return null

    val firestore = runCatching { firestoreProvider() }.getOrNull() ?: return null
    val snapshot =
        runCatching { firestore.collection(COLLECTION_USERS).document(uid).get().await() }
            .getOrNull() ?: return null
    if (!snapshot.exists()) return null

    val rawProfile = snapshot.get(PROFILE_FIELD) as? Map<*, *> ?: return null
    return UserProfileMapper.fromFirestore(rawProfile)
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
)
