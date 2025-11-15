package com.android.sample.logic

import com.android.sample.profile.UserProfile
import com.android.sample.settings.ProfileFormManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM unit tests for ProfileFieldConfiguration. */
class ProfileFieldConfigurationTest {

  @Test
  fun `getPersonalDetailsFields returns correct fields`() {
    val fields = ProfileFieldConfiguration.getPersonalDetailsFields()

    assertEquals(3, fields.size)
    assertEquals("fullName", fields[0].key)
    assertEquals("username", fields[1].key)
    assertEquals("role", fields[2].key)
  }

  @Test
  fun `getAcademicFields returns correct fields`() {
    val fields = ProfileFieldConfiguration.getAcademicFields()

    assertEquals(2, fields.size)
    assertEquals("faculty", fields[0].key)
    assertEquals("section", fields[1].key)
  }

  @Test
  fun `getContactFields returns correct fields`() {
    val fields = ProfileFieldConfiguration.getContactFields()

    assertEquals(2, fields.size)
    assertEquals("email", fields[0].key)
    assertEquals("phone", fields[1].key)
  }

  @Test
  fun `getSectionTitles returns correct titles`() {
    val titles = ProfileFieldConfiguration.getSectionTitles()

    assertEquals(3, titles.size)
    assertTrue(titles.contains("Personal details"))
    assertTrue(titles.contains("Academic information"))
    assertTrue(titles.contains("Contact"))
  }

  @Test
  fun `getDisplayText returns value when not blank`() {
    val result = ProfileFieldConfiguration.getDisplayText("John Doe", "Enter name")

    assertEquals("John Doe", result)
  }

  @Test
  fun `getDisplayText returns placeholder when value is blank`() {
    val result = ProfileFieldConfiguration.getDisplayText("", "Enter name")

    assertEquals("Enter name", result)
  }

  @Test
  fun `shouldUsePrimaryColor returns true when value is not blank`() {
    assertTrue(ProfileFieldConfiguration.shouldUsePrimaryColor("John"))
    assertFalse(ProfileFieldConfiguration.shouldUsePrimaryColor(""))
  }

  @Test
  fun `getContentAlpha returns 1f when enabled`() {
    assertEquals(1f, ProfileFieldConfiguration.getContentAlpha(true), 0.001f)
  }

  @Test
  fun `getContentAlpha returns 0_5f when disabled`() {
    assertEquals(0.5f, ProfileFieldConfiguration.getContentAlpha(false), 0.001f)
  }

  @Test
  fun `shouldShowSupportingText returns false when supportingText is null`() {
    assertFalse(
        ProfileFieldConfiguration.shouldShowSupportingText(
            supportingText = null, showWhenNotBlank = true, value = "test"))
  }

  @Test
  fun `shouldShowSupportingText returns true when showWhenNotBlank is false`() {
    assertTrue(
        ProfileFieldConfiguration.shouldShowSupportingText(
            supportingText = "Help text", showWhenNotBlank = false, value = ""))
  }

  @Test
  fun `shouldShowSupportingText returns true when value is not blank and showWhenNotBlank is true`() {
    assertTrue(
        ProfileFieldConfiguration.shouldShowSupportingText(
            supportingText = "Help text", showWhenNotBlank = true, value = "test"))
  }

  @Test
  fun `shouldShowSupportingText returns false when value is blank and showWhenNotBlank is true`() {
    assertFalse(
        ProfileFieldConfiguration.shouldShowSupportingText(
            supportingText = "Help text", showWhenNotBlank = true, value = ""))
  }

  @Test
  fun `getFieldByKey returns correct field for fullName`() {
    val field = ProfileFieldConfiguration.getFieldByKey("fullName")

    assertNotNull(field)
    assertEquals("fullName", field!!.key)
    assertEquals("Full name", field.label)
  }

  @Test
  fun `getFieldByKey returns correct field for email`() {
    val field = ProfileFieldConfiguration.getFieldByKey("email")

    assertNotNull(field)
    assertEquals("email", field!!.key)
    assertEquals("Email", field.label)
  }

  @Test
  fun `getFieldByKey returns null for invalid key`() {
    val field = ProfileFieldConfiguration.getFieldByKey("invalid")

    assertNull(field)
  }

  @Test
  fun `validatePersonalDetails returns true when all fields filled`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateFullName("John")
    formManager.updateUsername("johnd")
    formManager.updateRole("Student")

    assertTrue(ProfileFieldConfiguration.validatePersonalDetails(formManager))
  }

  @Test
  fun `validatePersonalDetails returns false when any field empty`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateFullName("John")
    // username and role not set

    assertFalse(ProfileFieldConfiguration.validatePersonalDetails(formManager))
  }

  @Test
  fun `validateContact returns true when email filled`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateEmail("test@epfl.ch")

    assertTrue(ProfileFieldConfiguration.validateContact(formManager))
  }

  @Test
  fun `validateContact returns false when email empty`() {
    val formManager = ProfileFormManager("", null)

    assertFalse(ProfileFieldConfiguration.validateContact(formManager))
  }

  @Test
  fun `getAllFieldValues returns all field values`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateFullName("John")
    // Note: email is locked when initialized with authEmail, so updateEmail won't work
    // The email will remain "test@epfl.ch"

    val values = ProfileFieldConfiguration.getAllFieldValues(formManager)

    assertEquals("John", values["fullName"])
    assertEquals("test@epfl.ch", values["email"]) // email is locked, so it's authEmail
    assertTrue(values.containsKey("username"))
    assertTrue(values.containsKey("role"))
    assertTrue(values.containsKey("faculty"))
    assertTrue(values.containsKey("section"))
    assertTrue(values.containsKey("phone"))
  }

  @Test
  fun `hasAnyFieldChanged returns true when field changed`() {
    val initial = UserProfile(fullName = "John", email = "john@epfl.ch")
    val authEmail = "john@epfl.ch"
    val formManager = ProfileFormManager(authEmail, initial)
    formManager.updateFullName("Jane") // Changed

    assertTrue(ProfileFieldConfiguration.hasAnyFieldChanged(formManager, initial, authEmail))
  }

  @Test
  fun `hasAnyFieldChanged returns false when no fields changed`() {
    val initial = UserProfile(fullName = "John")
    val authEmail = "test@epfl.ch"
    val formManager = ProfileFormManager(authEmail, initial)
    // No changes made - email will be resolved to authEmail, but that matches what formManager has

    assertFalse(ProfileFieldConfiguration.hasAnyFieldChanged(formManager, initial, authEmail))
  }

  @Test
  fun `hasAnyFieldChanged returns true when initial is null and fields filled`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateFullName("John")

    assertTrue(ProfileFieldConfiguration.hasAnyFieldChanged(formManager, null))
  }

  @Test
  fun `hasAnyFieldChanged returns false when initial is null and no fields filled`() {
    val formManager = ProfileFormManager("", null)

    assertFalse(ProfileFieldConfiguration.hasAnyFieldChanged(formManager, null))
  }

  @Test
  fun `countFilledFields returns correct count`() {
    val formManager = ProfileFormManager("test@epfl.ch", null)
    formManager.updateFullName("John")
    formManager.updateEmail("john@epfl.ch")

    val count = ProfileFieldConfiguration.countFilledFields(formManager)

    assertEquals(2, count)
  }

  @Test
  fun `countFilledFields returns zero when no fields filled`() {
    val formManager = ProfileFormManager("", null)

    val count = ProfileFieldConfiguration.countFilledFields(formManager)

    assertEquals(0, count)
  }

  @Test
  fun `fullName field has correct properties`() {
    val fields = ProfileFieldConfiguration.getPersonalDetailsFields()
    val fullNameField = fields.first { it.key == "fullName" }

    assertEquals("Full name", fullNameField.label)
    assertEquals("Enter your full name", fullNameField.placeholder)
    assertNotNull(fullNameField.supportingText)
    assertTrue(fullNameField.showSupportingTextWhenNotBlank)
  }

  @Test
  fun `role field has options`() {
    val fields = ProfileFieldConfiguration.getPersonalDetailsFields()
    val roleField = fields.first { it.key == "role" }

    assertTrue(roleField.hasOptions)
  }

  @Test
  fun `email field is locked when email locked in formManager`() {
    val formManager = ProfileFormManager("test@epfl.ch", UserProfile(email = "test@epfl.ch"))
    val fields = ProfileFieldConfiguration.getContactFields()
    val emailField = fields.first { it.key == "email" }

    assertTrue(emailField.isLocked(formManager))
  }

  @Test
  fun `getFieldByKey handles all field keys`() {
    val allKeys = listOf("fullName", "username", "role", "faculty", "section", "email", "phone")

    allKeys.forEach { key ->
      val field = ProfileFieldConfiguration.getFieldByKey(key)
      assertNotNull("Field with key $key should exist", field)
      assertEquals(key, field!!.key)
    }
  }
}
