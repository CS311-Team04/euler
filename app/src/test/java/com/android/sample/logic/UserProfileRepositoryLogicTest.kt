package com.android.sample.logic

import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM unit tests for UserProfileRepositoryLogic. */
class UserProfileRepositoryLogicTest {

  @Test
  fun `shouldProceedWithSave returns true when both available`() {
    assertTrue(
        UserProfileRepositoryLogic.shouldProceedWithSave(authAvailable = true, uidAvailable = true))
  }

  @Test
  fun `shouldProceedWithSave returns false when auth not available`() {
    assertFalse(
        UserProfileRepositoryLogic.shouldProceedWithSave(
            authAvailable = false, uidAvailable = true))
  }

  @Test
  fun `shouldProceedWithSave returns false when uid not available`() {
    assertFalse(
        UserProfileRepositoryLogic.shouldProceedWithSave(
            authAvailable = true, uidAvailable = false))
  }

  @Test
  fun `shouldProceedWithSave returns false when both not available`() {
    assertFalse(
        UserProfileRepositoryLogic.shouldProceedWithSave(
            authAvailable = false, uidAvailable = false))
  }

  @Test
  fun `shouldProceedWithLoad returns true when both available`() {
    assertTrue(
        UserProfileRepositoryLogic.shouldProceedWithLoad(authAvailable = true, uidAvailable = true))
  }

  @Test
  fun `shouldProceedWithLoad returns false when auth not available`() {
    assertFalse(
        UserProfileRepositoryLogic.shouldProceedWithLoad(
            authAvailable = false, uidAvailable = true))
  }

  @Test
  fun `buildSavePayload contains profile field`() {
    val profile = UserProfile(fullName = "Test")
    val payload = UserProfileRepositoryLogic.buildSavePayload(profile)

    assertTrue(payload.containsKey(UserProfileRepositoryLogic.PROFILE_FIELD))
    assertNotNull(payload[UserProfileRepositoryLogic.PROFILE_FIELD])
  }

  @Test
  fun `buildSavePayload uses UserProfileMapper`() {
    val profile =
        UserProfile(
            fullName = "John",
            preferredName = "J",
            email = "john@epfl.ch",
            roleDescription = "Student")
    val payload = UserProfileRepositoryLogic.buildSavePayload(profile)
    val profileMap = payload[UserProfileRepositoryLogic.PROFILE_FIELD] as? Map<*, *>

    assertNotNull(profileMap)
    assertEquals("John", profileMap!!["fullName"])
    assertEquals("J", profileMap["preferredName"])
    assertEquals("john@epfl.ch", profileMap["email"])
  }

  @Test
  fun `buildDocumentPath creates correct path`() {
    val path = UserProfileRepositoryLogic.buildDocumentPath("user123")

    assertEquals("users/user123", path)
  }

  @Test
  fun `hasProfileInSnapshot returns true when both exist`() {
    assertTrue(
        UserProfileRepositoryLogic.hasProfileInSnapshot(
            snapshotExists = true, hasProfileField = true))
  }

  @Test
  fun `hasProfileInSnapshot returns false when snapshot does not exist`() {
    assertFalse(
        UserProfileRepositoryLogic.hasProfileInSnapshot(
            snapshotExists = false, hasProfileField = true))
  }

  @Test
  fun `hasProfileInSnapshot returns false when profile field missing`() {
    assertFalse(
        UserProfileRepositoryLogic.hasProfileInSnapshot(
            snapshotExists = true, hasProfileField = false))
  }

  @Test
  fun `extractProfileFromSnapshot returns map when valid`() {
    val map = mapOf("fullName" to "Test")
    val result = UserProfileRepositoryLogic.extractProfileFromSnapshot(map)

    assertEquals(map, result)
  }

  @Test
  fun `extractProfileFromSnapshot returns null when not a map`() {
    assertNull(UserProfileRepositoryLogic.extractProfileFromSnapshot("not a map"))
    assertNull(UserProfileRepositoryLogic.extractProfileFromSnapshot(123))
    assertNull(UserProfileRepositoryLogic.extractProfileFromSnapshot(null))
  }

  @Test
  fun `shouldReturnNull returns true when any is unavailable`() {
    assertTrue(
        UserProfileRepositoryLogic.shouldReturnNull(
            authAvailable = false, uidAvailable = true, firestoreAvailable = true))
    assertTrue(
        UserProfileRepositoryLogic.shouldReturnNull(
            authAvailable = true, uidAvailable = false, firestoreAvailable = true))
    assertTrue(
        UserProfileRepositoryLogic.shouldReturnNull(
            authAvailable = true, uidAvailable = true, firestoreAvailable = false))
  }

  @Test
  fun `shouldReturnNull returns false when all available`() {
    assertFalse(
        UserProfileRepositoryLogic.shouldReturnNull(
            authAvailable = true, uidAvailable = true, firestoreAvailable = true))
  }

  @Test
  fun `isValidUid returns true for valid uid`() {
    assertTrue(UserProfileRepositoryLogic.isValidUid("user123"))
    assertTrue(UserProfileRepositoryLogic.isValidUid("abc-def-123"))
  }

  @Test
  fun `isValidUid returns false for null`() {
    assertFalse(UserProfileRepositoryLogic.isValidUid(null))
  }

  @Test
  fun `isValidUid returns false for empty string`() {
    assertFalse(UserProfileRepositoryLogic.isValidUid(""))
  }

  @Test
  fun `isValidUid returns false for blank string`() {
    assertFalse(UserProfileRepositoryLogic.isValidUid("   "))
  }

  @Test
  fun `handleRepositoryError formats error message correctly`() {
    val error = RuntimeException("Test error message")
    val result = UserProfileRepositoryLogic.handleRepositoryError("save", error)

    assertEquals("Failed to save: Test error message", result)
  }

  @Test
  fun `handleRepositoryError handles null error`() {
    val result = UserProfileRepositoryLogic.handleRepositoryError("load", null)

    assertEquals("Failed to load: Unknown error", result)
  }

  @Test
  fun `handleRepositoryError handles null error message`() {
    val error = RuntimeException(null as String?)
    val result = UserProfileRepositoryLogic.handleRepositoryError("save", error)

    assertEquals("Failed to save: Unknown error", result)
  }

  @Test
  fun `constants are correct`() {
    assertEquals("users", UserProfileRepositoryLogic.COLLECTION_USERS)
    assertEquals("profile", UserProfileRepositoryLogic.PROFILE_FIELD)
    assertEquals("profileUpdatedAt", UserProfileRepositoryLogic.PROFILE_UPDATED_AT_FIELD)
  }
}
