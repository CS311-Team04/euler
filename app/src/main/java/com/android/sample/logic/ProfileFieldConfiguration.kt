package com.android.sample.logic

import com.android.sample.profile.UserProfile
import com.android.sample.settings.ProfileFormManager

/**
 * Pure Kotlin logic for ProfilePage field configuration and display. Extracted from ProfilePage for
 * testability.
 */
object ProfileFieldConfiguration {

  data class FieldConfig(
      val key: String,
      val label: String,
      val placeholder: String,
      val getValue: (ProfileFormManager) -> String,
      val updateValue: (ProfileFormManager, String) -> Unit,
      val isLocked: (ProfileFormManager) -> Boolean = { false },
      val hasOptions: Boolean = false,
      val getOptions: (ProfileFormManager) -> List<String>? = { null },
      val supportingText: String? = null,
      val showSupportingTextWhenNotBlank: Boolean = false
  )

  /** Personal details section fields */
  fun getPersonalDetailsFields(): List<FieldConfig> {
    return listOf(
        FieldConfig(
            key = "fullName",
            label = "Full name",
            placeholder = "Enter your full name",
            getValue = { it.fullName },
            updateValue = { mgr, value -> mgr.updateFullName(value) },
            isLocked = { it.isFullNameLocked },
            supportingText = "This name can only be set once.",
            showSupportingTextWhenNotBlank = true),
        FieldConfig(
            key = "username",
            label = "Username",
            placeholder = "Let others know how to address you",
            getValue = { it.username },
            updateValue = { mgr, value -> mgr.updateUsername(value) }),
        FieldConfig(
            key = "role",
            label = "Role",
            placeholder = "Select your role",
            getValue = { it.role },
            updateValue = { mgr, value -> mgr.updateRole(value) },
            hasOptions = true,
            getOptions = { it.roleOptions }))
  }

  /** Academic information section fields */
  fun getAcademicFields(): List<FieldConfig> {
    return listOf(
        FieldConfig(
            key = "faculty",
            label = "Faculty",
            placeholder = "Add your faculty",
            getValue = { it.faculty },
            updateValue = { mgr, value -> mgr.updateFaculty(value) }),
        FieldConfig(
            key = "section",
            label = "Section",
            placeholder = "Add your section/program",
            getValue = { it.section },
            updateValue = { mgr, value -> mgr.updateSection(value) }))
  }

  /** Contact section fields */
  fun getContactFields(): List<FieldConfig> {
    return listOf(
        FieldConfig(
            key = "email",
            label = "Email",
            placeholder = ProfilePageLogic.getEmailPlaceholder(),
            getValue = { it.email },
            updateValue = { mgr, value -> mgr.updateEmail(value) },
            isLocked = { it.isEmailLocked },
            supportingText = "Managed via your Microsoft account"),
        FieldConfig(
            key = "phone",
            label = "Phone",
            placeholder = ProfilePageLogic.getPhonePlaceholder(),
            getValue = { it.phone },
            updateValue = { mgr, value -> mgr.updatePhone(value) }))
  }

  /** All section titles */
  fun getSectionTitles(): List<String> {
    return listOf("Personal details", "Academic information", "Contact")
  }

  /** Determines display text for a field (value or placeholder) */
  fun getDisplayText(value: String, placeholder: String): String {
    return if (value.isNotBlank()) value else placeholder
  }

  /** Determines if display text should use primary color (has value) */
  fun shouldUsePrimaryColor(value: String): Boolean {
    return value.isNotBlank()
  }

  /** Calculates content alpha based on enabled state */
  fun getContentAlpha(enabled: Boolean): Float {
    return if (enabled) 1f else 0.5f
  }

  /** Determines if supporting text should be shown */
  fun shouldShowSupportingText(
      supportingText: String?,
      showWhenNotBlank: Boolean,
      value: String
  ): Boolean {
    if (supportingText == null) return false
    return !showWhenNotBlank || value.isNotBlank()
  }

  /** Gets field configuration by key */
  fun getFieldByKey(key: String): FieldConfig? {
    return (getPersonalDetailsFields() + getAcademicFields() + getContactFields()).firstOrNull {
      it.key == key
    }
  }

  /** Validates that all required personal fields are filled */
  fun validatePersonalDetails(formManager: ProfileFormManager): Boolean {
    val fields = getPersonalDetailsFields()
    return fields.all { it.getValue(formManager).isNotBlank() }
  }

  /** Validates that all required contact fields are filled */
  fun validateContact(formManager: ProfileFormManager): Boolean {
    val emailField = getContactFields().firstOrNull { it.key == "email" }
    return emailField?.let { it.getValue(formManager).isNotBlank() } ?: false
  }

  /** Gets all field values as a map */
  fun getAllFieldValues(formManager: ProfileFormManager): Map<String, String> {
    val allFields = getPersonalDetailsFields() + getAcademicFields() + getContactFields()
    return allFields.associate { it.key to it.getValue(formManager) }
  }

  /** Checks if any field has changed from initial profile */
  fun hasAnyFieldChanged(formManager: ProfileFormManager, initialProfile: UserProfile?): Boolean {
    if (initialProfile == null) {
      return getAllFieldValues(formManager).values.any { it.isNotBlank() }
    }

    val currentValues = getAllFieldValues(formManager)
    val initialValues =
        mapOf(
            "fullName" to initialProfile.fullName,
            "username" to initialProfile.preferredName,
            "role" to initialProfile.roleDescription,
            "faculty" to initialProfile.faculty,
            "section" to initialProfile.section,
            "email" to initialProfile.email,
            "phone" to initialProfile.phone)

    return currentValues.any { (key, value) -> value != initialValues[key].orEmpty() }
  }

  /** Counts number of filled fields */
  fun countFilledFields(formManager: ProfileFormManager): Int {
    return getAllFieldValues(formManager).values.count { it.isNotBlank() }
  }
}
