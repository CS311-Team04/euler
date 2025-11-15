package com.android.sample.profile

import com.android.sample.logic.UserProfileMapper
import com.android.sample.logic.UserProfileRepositoryLogic
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
    val auth = runCatching { authProvider() }.getOrNull()
    val authAvailable = auth != null
    val uid = auth?.currentUser?.uid
    val uidAvailable = UserProfileRepositoryLogic.isValidUid(uid)

    if (!UserProfileRepositoryLogic.shouldProceedWithSave(authAvailable, uidAvailable)) {
      return
    }

    val basePayload = UserProfileRepositoryLogic.buildSavePayload(profile)
    val payload =
        basePayload +
            mapOf(
                UserProfileRepositoryLogic.PROFILE_UPDATED_AT_FIELD to FieldValue.serverTimestamp())

    val firestore = runCatching { firestoreProvider() }.getOrNull()
    val firestoreAvailable = firestore != null

    if (UserProfileRepositoryLogic.shouldReturnNull(
        authAvailable, uidAvailable, firestoreAvailable)) {
      return
    }

    requireNotNull(uid) // Safe here due to isValidUid check above
    requireNotNull(firestore) // Safe here due to check above
    firestore
        .collection(UserProfileRepositoryLogic.COLLECTION_USERS)
        .document(uid)
        .set(payload, SetOptions.merge())
        .await()
  }

  /**
   * Retrieve the stored profile for the currently signed-in user. Returns `null` when no profile
   * has been saved yet.
   */
  override suspend fun loadProfile(): UserProfile? {
    val auth = runCatching { authProvider() }.getOrNull()
    val authAvailable = auth != null
    val uid = auth?.currentUser?.uid
    val uidAvailable = UserProfileRepositoryLogic.isValidUid(uid)

    if (!UserProfileRepositoryLogic.shouldProceedWithLoad(authAvailable, uidAvailable)) {
      return null
    }

    val firestore = runCatching { firestoreProvider() }.getOrNull()
    val firestoreAvailable = firestore != null

    if (UserProfileRepositoryLogic.shouldReturnNull(
        authAvailable, uidAvailable, firestoreAvailable)) {
      return null
    }

    requireNotNull(uid) // Safe here due to isValidUid check above
    requireNotNull(firestore) // Safe here due to check above
    val snapshot =
        runCatching {
              firestore
                  .collection(UserProfileRepositoryLogic.COLLECTION_USERS)
                  .document(uid)
                  .get()
                  .await()
            }
            .getOrNull() ?: return null

    val snapshotExists = snapshot.exists()
    val rawProfile =
        UserProfileRepositoryLogic.extractProfileFromSnapshot(
            snapshot.get(UserProfileRepositoryLogic.PROFILE_FIELD))
    val hasProfileField = rawProfile != null

    if (!UserProfileRepositoryLogic.hasProfileInSnapshot(snapshotExists, hasProfileField)) {
      return null
    }

    requireNotNull(rawProfile) // Safe here due to hasProfileField check above
    return UserProfileMapper.fromFirestore(rawProfile)
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
