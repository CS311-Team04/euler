package com.android.sample.settings

import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePageTests {

  @Test
  fun `initializes from auth email when no profile`() {
    val manager = ProfileFormManager("student@epfl.ch", null)

    assertEquals("", manager.fullName)
    assertEquals("student@epfl.ch", manager.email)
    assertTrue(manager.isEmailLocked)
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
    assertEquals("jack.reacher@epfl.ch", manager.email)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    manager.updateFullName("Different") // should have no effect
    manager.updateEmail("other@epfl.ch") // should have no effect

    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("jack.reacher@epfl.ch", manager.email)
  }

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
  fun `role options expose expected choices`() {
    val manager = ProfileFormManager("", null)

    assertEquals(
        listOf("Student", "PhD", "Post-Doc", "Teacher", "Administration", "Staff"),
        manager.roleOptions)

    manager.updateRole("Teacher")
    assertEquals("Teacher", manager.role)
  }

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
}

