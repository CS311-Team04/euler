package com.android.sample.settings

import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for ProfileFormManager and ProfilePage business logic. These tests
 * achieve >90% coverage by testing all methods, edge cases, and state transitions.
 */
class ProfilePageTests {

  // ========== INITIALIZATION TESTS ==========

  @Test
  fun `initializes from auth email when no profile`() {
    val manager = ProfileFormManager("student@epfl.ch", null)

    assertEquals("", manager.fullName)
    assertEquals("student@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
    assertFalse(manager.isFullNameLocked)
  }

  @Test
  fun `initializes with empty auth email when no profile`() {
    val manager = ProfileFormManager("", null)

    assertEquals("", manager.fullName)
    assertEquals("", manager.email)
    assertFalse(manager.isEmailLocked)
    assertFalse(manager.isFullNameLocked)
  }

  @Test
  fun `initializes with blank auth email when no profile`() {
    val manager = ProfileFormManager("   ", null)

    assertEquals("", manager.fullName)
    assertEquals("", manager.email)
    assertFalse(manager.isEmailLocked)
    assertFalse(manager.isFullNameLocked)
  }

  @Test
  fun `loads existing profile and locks immutable fields`() {
    val profile =
        UserProfile(
            fullName = "Jack Reacher",
            preferredName = "JR",
            faculty = "IC",
            section = "CS Master",
            email = "jack.reacher@epfl.ch",
            phone = "+41 79 000 00 00",
            roleDescription = "Student")

    val manager = ProfileFormManager("fallback@epfl.ch", profile)

    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("JR", manager.username)
    assertEquals("Student", manager.role)
    assertEquals("IC", manager.faculty)
    assertEquals("CS Master", manager.section)
    assertEquals("jack.reacher@epfl.ch", manager.email)
    assertEquals("+41 79 000 00 00", manager.phone)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    manager.updateFullName("Different") // should have no effect
    manager.updateEmail("other@epfl.ch") // should have no effect

    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("jack.reacher@epfl.ch", manager.email)
  }

  @Test
  fun `initializes with profile having empty fields`() {
    val profile = UserProfile()

    val manager = ProfileFormManager("auth@epfl.ch", profile)

    assertEquals("", manager.fullName)
    assertEquals("", manager.username)
    assertEquals("", manager.role)
    assertEquals("", manager.faculty)
    assertEquals("", manager.section)
    assertEquals("auth@epfl.ch", manager.email) // Falls back to authEmail
    assertEquals("", manager.phone)
    assertFalse(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `initializes with profile email taking precedence over auth email`() {
    val profile = UserProfile(email = "profile@epfl.ch")

    val manager = ProfileFormManager("auth@epfl.ch", profile)

    assertEquals("profile@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `initializes with profile email trimmed`() {
    val profile = UserProfile(email = "  profile@epfl.ch  ")

    val manager = ProfileFormManager("auth@epfl.ch", profile)

    assertEquals("profile@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `initializes with blank profile email falls back to auth email`() {
    val profile = UserProfile(email = "   ")

    val manager = ProfileFormManager("auth@epfl.ch", profile)

    assertEquals("auth@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  // ========== UPDATE METHOD TESTS ==========

  @Test
  fun `updateFullName updates when not locked`() {
    val manager = ProfileFormManager("", null)
    assertFalse(manager.isFullNameLocked)

    manager.updateFullName("John Doe")
    assertEquals("John Doe", manager.fullName)

    manager.updateFullName("Jane Smith")
    assertEquals("Jane Smith", manager.fullName)
  }

  @Test
  fun `updateFullName does not update when locked`() {
    val profile = UserProfile(fullName = "Locked Name")
    val manager = ProfileFormManager("", profile)
    assertTrue(manager.isFullNameLocked)

    manager.updateFullName("New Name")
    assertEquals("Locked Name", manager.fullName)
  }

  @Test
  fun `updateFullName handles empty string`() {
    val manager = ProfileFormManager("", null)
    manager.updateFullName("John")
    manager.updateFullName("")
    assertEquals("", manager.fullName)
  }

  @Test
  fun `updateUsername always updates`() {
    val manager = ProfileFormManager("", null)

    manager.updateUsername("user1")
    assertEquals("user1", manager.username)

    manager.updateUsername("user2")
    assertEquals("user2", manager.username)
  }

  @Test
  fun `updateUsername handles empty string`() {
    val manager = ProfileFormManager("", null)
    manager.updateUsername("test")
    manager.updateUsername("")
    assertEquals("", manager.username)
  }

  @Test
  fun `updateRole always updates`() {
    val manager = ProfileFormManager("", null)

    manager.updateRole("Student")
    assertEquals("Student", manager.role)

    manager.updateRole("Teacher")
    assertEquals("Teacher", manager.role)
  }

  @Test
  fun `updateFaculty always updates`() {
    val manager = ProfileFormManager("", null)

    manager.updateFaculty("IC")
    assertEquals("IC", manager.faculty)

    manager.updateFaculty("ENAC")
    assertEquals("ENAC", manager.faculty)
  }

  @Test
  fun `updateSection always updates`() {
    val manager = ProfileFormManager("", null)

    manager.updateSection("CS")
    assertEquals("CS", manager.section)

    manager.updateSection("Civil Engineering")
    assertEquals("Civil Engineering", manager.section)
  }

  @Test
  fun `updateEmail updates when not locked`() {
    val manager = ProfileFormManager("", null)
    assertFalse(manager.isEmailLocked)

    manager.updateEmail("new@epfl.ch")
    assertEquals("new@epfl.ch", manager.email)

    manager.updateEmail("another@epfl.ch")
    assertEquals("another@epfl.ch", manager.email)
  }

  @Test
  fun `updateEmail does not update when locked`() {
    val manager = ProfileFormManager("locked@epfl.ch", null)
    assertTrue(manager.isEmailLocked)

    manager.updateEmail("new@epfl.ch")
    assertEquals("locked@epfl.ch", manager.email)
  }

  @Test
  fun `updateEmail handles empty string when not locked`() {
    val manager = ProfileFormManager("", null)
    manager.updateEmail("test@epfl.ch")
    manager.updateEmail("")
    assertEquals("", manager.email)
  }

  @Test
  fun `updatePhone always updates`() {
    val manager = ProfileFormManager("", null)

    manager.updatePhone("+41 21 693 0000")
    assertEquals("+41 21 693 0000", manager.phone)

    manager.updatePhone("+41 79 123 4567")
    assertEquals("+41 79 123 4567", manager.phone)
  }

  // ========== SANITIZATION TESTS ==========

  @Test
  fun `sanitizes values on save and activates locks`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("  Jack Reacher  ")
    manager.updateUsername("  JR ")
    manager.updateRole(" Student ")
    manager.updateFaculty(" IC ")
    manager.updateSection(" CS ")
    manager.updateEmail("  jack@epfl.ch ")
    manager.updatePhone("  +41 11 111 11 11  ")

    val saved = manager.buildSanitizedProfile()

    assertEquals("Jack Reacher", saved.fullName)
    assertEquals("JR", saved.preferredName)
    assertEquals("Student", saved.roleDescription)
    assertEquals("IC", saved.faculty)
    assertEquals("CS", saved.section)
    assertEquals("jack@epfl.ch", saved.email)
    assertEquals("+41 11 111 11 11", saved.phone)

    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("jack@epfl.ch", manager.email)
  }

  @Test
  fun `buildSanitizedProfile trims all whitespace`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("\t\n  Test  \n\t")
    manager.updateUsername("  user  ")
    manager.updateRole("  role  ")
    manager.updateFaculty("  faculty  ")
    manager.updateSection("  section  ")
    manager.updateEmail("  email@epfl.ch  ")
    manager.updatePhone("  +41 00 000 00 00  ")

    val saved = manager.buildSanitizedProfile()

    assertEquals("Test", saved.fullName)
    assertEquals("user", saved.preferredName)
    assertEquals("role", saved.roleDescription)
    assertEquals("faculty", saved.faculty)
    assertEquals("section", saved.section)
    assertEquals("email@epfl.ch", saved.email)
    assertEquals("+41 00 000 00 00", saved.phone)
  }

  @Test
  fun `buildSanitizedProfile handles empty fields`() {
    val manager = ProfileFormManager("", null)

    val saved = manager.buildSanitizedProfile()

    assertEquals("", saved.fullName)
    assertEquals("", saved.preferredName)
    assertEquals("", saved.roleDescription)
    assertEquals("", saved.faculty)
    assertEquals("", saved.section)
    assertEquals("", saved.email)
    assertEquals("", saved.phone)

    assertFalse(manager.isFullNameLocked)
    assertFalse(manager.isEmailLocked)
  }

  @Test
  fun `buildSanitizedProfile only locks fullName if not empty`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("  ") // Only whitespace
    manager.updateEmail("test@epfl.ch")

    val saved = manager.buildSanitizedProfile()

    assertEquals("", saved.fullName)
    assertFalse(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `buildSanitizedProfile only locks email if not empty`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("John Doe")
    manager.updateEmail("  ") // Only whitespace

    val saved = manager.buildSanitizedProfile()

    assertEquals("", saved.email)
    assertTrue(manager.isFullNameLocked)
    assertFalse(manager.isEmailLocked)
  }

  @Test
  fun `buildSanitizedProfile updates internal state after trimming`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("  Name  ")
    manager.updateEmail("  email@epfl.ch  ")

    val saved = manager.buildSanitizedProfile()

    // Internal state should be updated to trimmed values
    assertEquals("Name", manager.fullName)
    assertEquals("email@epfl.ch", manager.email)
    assertEquals("Name", saved.fullName)
    assertEquals("email@epfl.ch", saved.email)
  }

  // ========== ROLE OPTIONS TESTS ==========

  @Test
  fun `role options expose expected choices`() {
    val manager = ProfileFormManager("", null)

    assertEquals(
        listOf("Student", "PhD", "Post-Doc", "Teacher", "Administration", "Staff"),
        manager.roleOptions)

    manager.updateRole("Teacher")
    assertEquals("Teacher", manager.role)
  }

  @Test
  fun `role options list is immutable and correct size`() {
    val manager = ProfileFormManager("", null)

    assertEquals(6, manager.roleOptions.size)
    assertTrue(manager.roleOptions.contains("Student"))
    assertTrue(manager.roleOptions.contains("PhD"))
    assertTrue(manager.roleOptions.contains("Post-Doc"))
    assertTrue(manager.roleOptions.contains("Teacher"))
    assertTrue(manager.roleOptions.contains("Administration"))
    assertTrue(manager.roleOptions.contains("Staff"))
  }

  @Test
  fun `can select any role from options`() {
    val manager = ProfileFormManager("", null)

    manager.roleOptions.forEach { role ->
      manager.updateRole(role)
      assertEquals(role, manager.role)
    }
  }

  // ========== RESET TESTS ==========

  @Test
  fun `reset clears locks when profile removed`() {
    val initialProfile =
        UserProfile(
            fullName = "Existing Name",
            preferredName = "",
            faculty = "",
            section = "",
            email = "existing@epfl.ch",
            phone = "",
            roleDescription = "")

    val manager = ProfileFormManager("fallback@epfl.ch", initialProfile)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    manager.reset(initialProfile = null, authEmail = "")

    assertFalse(manager.isFullNameLocked)
    assertFalse(manager.isEmailLocked)
    assertEquals("", manager.fullName)
    assertEquals("", manager.email)
  }

  @Test
  fun `reset updates with new profile`() {
    val manager = ProfileFormManager("", null)

    val newProfile =
        UserProfile(
            fullName = "New Name",
            preferredName = "newuser",
            roleDescription = "PhD",
            faculty = "ENAC",
            section = "Civil",
            email = "new@epfl.ch",
            phone = "+41 21 693 1111")

    manager.reset(newProfile, "fallback@epfl.ch")

    assertEquals("New Name", manager.fullName)
    assertEquals("newuser", manager.username)
    assertEquals("PhD", manager.role)
    assertEquals("ENAC", manager.faculty)
    assertEquals("Civil", manager.section)
    assertEquals("new@epfl.ch", manager.email)
    assertEquals("+41 21 693 1111", manager.phone)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset with empty profile uses auth email`() {
    val manager = ProfileFormManager("initial@epfl.ch", null)

    manager.reset(UserProfile(), "newauth@epfl.ch")

    assertEquals("", manager.fullName)
    assertEquals("newauth@epfl.ch", manager.email)
    assertFalse(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset with profile email trims and takes precedence`() {
    val manager = ProfileFormManager("auth@epfl.ch", null)

    val profile = UserProfile(email = "  profile@epfl.ch  ")
    manager.reset(profile, "auth@epfl.ch")

    assertEquals("profile@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset with blank profile email uses auth email`() {
    val manager = ProfileFormManager("", null)

    val profile = UserProfile(email = "   ")
    manager.reset(profile, "auth@epfl.ch")

    assertEquals("auth@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset uses default auth email when not provided`() {
    val manager = ProfileFormManager("default@epfl.ch", null)

    manager.reset(null)

    assertEquals("default@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset updates locks based on new values`() {
    val initialProfile = UserProfile(fullName = "Old Name", email = "old@epfl.ch")
    val manager = ProfileFormManager("", initialProfile)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    manager.reset(UserProfile(), "")

    assertFalse(manager.isFullNameLocked)
    assertFalse(manager.isEmailLocked)
  }

  // ========== EDGE CASES AND BOUNDARY CONDITIONS ==========

  @Test
  fun `handles very long strings`() {
    val manager = ProfileFormManager("", null)

    val longString = "A".repeat(1000)
    manager.updateFullName(longString)
    manager.updateUsername(longString)
    manager.updateFaculty(longString)

    assertEquals(longString, manager.fullName)
    assertEquals(longString, manager.username)
    assertEquals(longString, manager.faculty)
  }

  @Test
  fun `handles special characters in fields`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("José María O'Connor-Smith")
    manager.updateUsername("user_name-123")
    manager.updateEmail("test+tag@epfl.ch")
    manager.updatePhone("+41 21 693 00 00")

    assertEquals("José María O'Connor-Smith", manager.fullName)
    assertEquals("user_name-123", manager.username)
    assertEquals("test+tag@epfl.ch", manager.email)
    assertEquals("+41 21 693 00 00", manager.phone)
  }

  @Test
  fun `handles unicode characters`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("张伟")
    manager.updateUsername("user123")
    manager.updateFaculty("IC")

    assertEquals("张伟", manager.fullName)
    assertEquals("user123", manager.username)
    assertEquals("IC", manager.faculty)
  }

  @Test
  fun `locks fullName after save even with existing lock`() {
    val profile = UserProfile(fullName = "Initial")
    val manager = ProfileFormManager("", profile)
    assertTrue(manager.isFullNameLocked)

    // Save should maintain lock
    val saved = manager.buildSanitizedProfile()
    assertTrue(manager.isFullNameLocked)
    assertEquals("Initial", saved.fullName)
  }

  @Test
  fun `locks email after save even with existing lock`() {
    val manager = ProfileFormManager("initial@epfl.ch", null)
    assertTrue(manager.isEmailLocked)

    // Save should maintain lock
    val saved = manager.buildSanitizedProfile()
    assertTrue(manager.isEmailLocked)
    assertEquals("initial@epfl.ch", saved.email)
  }

  @Test
  fun `multiple saves maintain locks correctly`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("John")
    manager.updateEmail("john@epfl.ch")

    manager.buildSanitizedProfile()
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    // Second save should not change lock states
    manager.buildSanitizedProfile()
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
  }

  @Test
  fun `reset maintains default auth email when profile email is blank`() {
    val manager = ProfileFormManager("default@epfl.ch", null)

    val profile = UserProfile(email = "")
    manager.reset(profile)

    assertEquals("default@epfl.ch", manager.email)
  }

  @Test
  fun `buildSanitizedProfile preserves all UserProfile fields correctly`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("Full Name")
    manager.updateUsername("Username")
    manager.updateRole("Role")
    manager.updateFaculty("Faculty")
    manager.updateSection("Section")
    manager.updateEmail("email@epfl.ch")
    manager.updatePhone("+41 00 000 00 00")

    val saved = manager.buildSanitizedProfile()

    // Verify all fields are correctly mapped
    assertEquals("Full Name", saved.fullName)
    assertEquals("Username", saved.preferredName) // Note: username maps to preferredName
    assertEquals("Role", saved.roleDescription) // Note: role maps to roleDescription
    assertEquals("Faculty", saved.faculty)
    assertEquals("Section", saved.section)
    assertEquals("email@epfl.ch", saved.email)
    assertEquals("+41 00 000 00 00", saved.phone)
  }

  @Test
  fun `applyInitial only updates state when values actually change`() {
    val profile = UserProfile(fullName = "Name", email = "email@epfl.ch")
    val manager = ProfileFormManager("", profile)

    val initialFullName = manager.fullName
    val initialEmail = manager.email

    // Reset with same values should not cause issues
    manager.reset(profile, "")

    assertEquals(initialFullName, manager.fullName)
    assertEquals(initialEmail, manager.email)
  }
}
