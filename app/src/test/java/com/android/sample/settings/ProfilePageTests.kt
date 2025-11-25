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
            faculty = "IC — School of Computer and Communication Sciences",
            section = "Computer Science",
            email = "jack.reacher@epfl.ch",
            phone = "+41 79 000 00 00",
            roleDescription = "Student")

    val manager = ProfileFormManager("fallback@epfl.ch", profile)

    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("JR", manager.username)
    assertEquals("Student", manager.role)
    assertEquals("IC — School of Computer and Communication Sciences", manager.faculty)
    assertEquals("Computer Science", manager.section)
    assertEquals("jack.reacher@epfl.ch", manager.email)
    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)

    manager.updateFullName("Different")
    manager.updateEmail("other@epfl.ch")

    assertEquals("Jack Reacher", manager.fullName)
    assertEquals("jack.reacher@epfl.ch", manager.email)
  }

  @Test
  fun `sanitizes values on save and activates locks`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("  Jack Reacher  ")
    manager.updateUsername("  JR ")
    manager.updateRole(" Student ")
    manager.updateFaculty("IC — School of Computer and Communication Sciences")
    manager.updateSection("Computer Science")
    manager.updateEmail("  jack@epfl.ch ")
    manager.updatePhone("  +41 11 111 11 11  ")

    val saved = manager.buildSanitizedProfile()

    assertEquals("Jack Reacher", saved.fullName)
    assertEquals("JR", saved.preferredName)
    assertEquals("Student", saved.roleDescription)
    assertEquals("IC — School of Computer and Communication Sciences", saved.faculty)
    assertEquals("Computer Science", saved.section)
    assertEquals("jack@epfl.ch", saved.email)
    assertEquals("+41 11 111 11 11", saved.phone)

    assertTrue(manager.isFullNameLocked)
    assertTrue(manager.isEmailLocked)
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
  fun `faculty options are available`() {
    val manager = ProfileFormManager("", null)

    assertTrue(manager.facultyOptions.isNotEmpty())
    assertTrue(
        manager.facultyOptions.contains("IC — School of Computer and Communication Sciences"))
  }

  @Test
  fun `section options depend on faculty`() {
    val manager = ProfileFormManager("", null)

    assertEquals(emptyList<String>(), manager.sectionOptions)

    manager.updateFaculty("IC — School of Computer and Communication Sciences")
    assertTrue(manager.sectionOptions.contains("Computer Science"))
    assertTrue(manager.sectionOptions.contains("Communication Systems"))

    manager.updateFaculty("SB — School of Basic Sciences")
    assertTrue(manager.sectionOptions.contains("Mathematics"))
    assertTrue(manager.sectionOptions.contains("Physics"))
  }

  @Test
  fun `updating faculty resets invalid section`() {
    val manager = ProfileFormManager("", null)

    manager.updateFaculty("IC — School of Computer and Communication Sciences")
    manager.updateSection("Computer Science")

    manager.updateFaculty("SB — School of Basic Sciences")
    val sections = manager.sectionOptions
    assertTrue(manager.section in sections || sections.isEmpty())
  }

  @Test
  fun `section validation on init`() {
    val profile =
        UserProfile(
            fullName = "Test",
            preferredName = "",
            faculty = "IC — School of Computer and Communication Sciences",
            section = "Invalid Section",
            email = "test@epfl.ch",
            phone = "",
            roleDescription = "")

    val manager = ProfileFormManager("", profile)

    val validSections = manager.sectionOptions
    assertTrue(manager.section in validSections || validSections.isEmpty())
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

  @Test
  fun `all update methods work`() {
    val manager = ProfileFormManager("", null)

    manager.updateFullName("Name")
    manager.updateUsername("User")
    manager.updateRole("Student")
    manager.updateFaculty("IC — School of Computer and Communication Sciences")
    manager.updateSection("Computer Science")
    manager.updateEmail("test@epfl.ch")
    manager.updatePhone("+41 00 000 00 00")

    assertEquals("Name", manager.fullName)
    assertEquals("User", manager.username)
    assertEquals("Student", manager.role)
    assertEquals("IC — School of Computer and Communication Sciences", manager.faculty)
    assertEquals("Computer Science", manager.section)
    assertEquals("test@epfl.ch", manager.email)
    assertEquals("+41 00 000 00 00", manager.phone)
  }
}
