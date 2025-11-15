package com.android.sample.logic

import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM unit tests for ProfilePageLogic. */
class ProfilePageLogicTest {

  @Test
  fun `resolveEmail prefers profile email over auth email`() {
    val result =
        ProfilePageLogic.resolveEmail(profileEmail = "profile@epfl.ch", authEmail = "auth@epfl.ch")

    assertEquals("profile@epfl.ch", result)
  }

  @Test
  fun `resolveEmail uses auth email when profile email is empty`() {
    val result = ProfilePageLogic.resolveEmail(profileEmail = null, authEmail = "auth@epfl.ch")

    assertEquals("auth@epfl.ch", result)
  }

  @Test
  fun `resolveEmail uses auth email when profile email is blank`() {
    val result = ProfilePageLogic.resolveEmail(profileEmail = "   ", authEmail = "auth@epfl.ch")

    assertEquals("auth@epfl.ch", result)
  }

  @Test
  fun `resolveEmail returns empty string when both are empty`() {
    val result = ProfilePageLogic.resolveEmail(profileEmail = null, authEmail = "")

    assertEquals("", result)
  }

  @Test
  fun `resolveEmail trims profile email`() {
    val result = ProfilePageLogic.resolveEmail(profileEmail = "  profile@epfl.ch  ", authEmail = "")

    assertEquals("profile@epfl.ch", result)
  }

  @Test
  fun `extractAuthEmail returns email when available`() {
    assertEquals("test@epfl.ch", ProfilePageLogic.extractAuthEmail("test@epfl.ch"))
  }

  @Test
  fun `extractAuthEmail returns empty string when null`() {
    assertEquals("", ProfilePageLogic.extractAuthEmail(null))
  }

  @Test
  fun `isFieldEnabled returns true when not locked`() {
    assertTrue(ProfilePageLogic.isFieldEnabled(isLocked = false))
  }

  @Test
  fun `isFieldEnabled returns false when locked`() {
    assertFalse(ProfilePageLogic.isFieldEnabled(isLocked = true))
  }

  @Test
  fun `shouldShowSavedConfirmation returns true when showConfirmation is true`() {
    assertTrue(ProfilePageLogic.shouldShowSavedConfirmation(true))
  }

  @Test
  fun `shouldShowSavedConfirmation returns false when showConfirmation is false`() {
    assertFalse(ProfilePageLogic.shouldShowSavedConfirmation(false))
  }

  @Test
  fun `shouldResetConfirmationImmediately returns true when isTesting is true`() {
    assertTrue(
        ProfilePageLogic.shouldResetConfirmationImmediately(
            isTesting = true, skipDelayForTests = false))
  }

  @Test
  fun `shouldResetConfirmationImmediately returns true when skipDelayForTests is true`() {
    assertTrue(
        ProfilePageLogic.shouldResetConfirmationImmediately(
            isTesting = false, skipDelayForTests = true))
  }

  @Test
  fun `shouldResetConfirmationImmediately returns false when both are false`() {
    assertFalse(
        ProfilePageLogic.shouldResetConfirmationImmediately(
            isTesting = false, skipDelayForTests = false))
  }

  @Test
  fun `getConfirmationDelay returns 0 when should reset immediately`() {
    val delay1 = ProfilePageLogic.getConfirmationDelay(isTesting = true, skipDelayForTests = false)
    val delay2 = ProfilePageLogic.getConfirmationDelay(isTesting = false, skipDelayForTests = true)

    assertEquals(0L, delay1)
    assertEquals(0L, delay2)
  }

  @Test
  fun `getConfirmationDelay returns 2500 when not in test mode`() {
    val delay = ProfilePageLogic.getConfirmationDelay(isTesting = false, skipDelayForTests = false)

    assertEquals(2500L, delay)
  }

  @Test
  fun `isSaveButtonEnabled always returns true`() {
    assertTrue(ProfilePageLogic.isSaveButtonEnabled())
  }

  @Test
  fun `canSaveProfile returns true when fullName is filled`() {
    val profile = UserProfile(fullName = "John Doe")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when preferredName is filled`() {
    val profile = UserProfile(preferredName = "John")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when email is filled`() {
    val profile = UserProfile(email = "john@epfl.ch")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when faculty is filled`() {
    val profile = UserProfile(faculty = "IC")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when section is filled`() {
    val profile = UserProfile(section = "CS")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when roleDescription is filled`() {
    val profile = UserProfile(roleDescription = "Student")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns true when phone is filled`() {
    val profile = UserProfile(phone = "+41 21 693 0000")
    assertTrue(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `canSaveProfile returns false when all fields are empty`() {
    val profile = UserProfile()
    assertFalse(ProfilePageLogic.canSaveProfile(profile))
  }

  @Test
  fun `getEmailPlaceholder returns correct placeholder`() {
    assertEquals("name@epfl.ch", ProfilePageLogic.getEmailPlaceholder())
  }

  @Test
  fun `getPhonePlaceholder returns correct placeholder`() {
    assertEquals("+41 00 000 00 00", ProfilePageLogic.getPhonePlaceholder())
  }

  @Test
  fun `getDefaultRoleOptions returns expected roles`() {
    val roles = ProfilePageLogic.getDefaultRoleOptions()

    assertEquals(6, roles.size)
    assertTrue(roles.contains("Student"))
    assertTrue(roles.contains("PhD"))
    assertTrue(roles.contains("Post-Doc"))
    assertTrue(roles.contains("Teacher"))
    assertTrue(roles.contains("Administration"))
    assertTrue(roles.contains("Staff"))
  }

  @Test
  fun `shouldResetInitialProfile returns true when profiles differ`() {
    val profile1 = UserProfile(fullName = "John")
    val profile2 = UserProfile(fullName = "Jane")

    assertTrue(ProfilePageLogic.shouldResetInitialProfile(profile1, profile2))
  }

  @Test
  fun `shouldResetInitialProfile returns false when profiles are same`() {
    val profile1 = UserProfile(fullName = "John")
    val profile2 = UserProfile(fullName = "John")

    assertFalse(ProfilePageLogic.shouldResetInitialProfile(profile1, profile2))
  }

  @Test
  fun `shouldResetInitialProfile returns true when one is null`() {
    val profile = UserProfile(fullName = "John")

    assertTrue(ProfilePageLogic.shouldResetInitialProfile(null, profile))
    assertTrue(ProfilePageLogic.shouldResetInitialProfile(profile, null))
  }

  @Test
  fun `shouldResetInitialProfile returns false when both are null`() {
    assertFalse(ProfilePageLogic.shouldResetInitialProfile(null, null))
  }

  @Test
  fun `shouldResetForAuthEmail returns true when emails differ`() {
    assertTrue(ProfilePageLogic.shouldResetForAuthEmail("old@epfl.ch", "new@epfl.ch"))
  }

  @Test
  fun `shouldResetForAuthEmail returns false when emails are same`() {
    assertFalse(ProfilePageLogic.shouldResetForAuthEmail("test@epfl.ch", "test@epfl.ch"))
  }
}
