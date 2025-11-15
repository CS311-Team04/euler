package com.android.sample.logic

import com.android.sample.profile.UserProfile
import com.android.sample.settings.ProfileFormManager

/**
 * Pure Kotlin logic for ProfilePage display decisions. Extracted from ProfilePage for testability.
 */
object ProfileDisplayLogic {

  /** Determines if save button should be enabled based on form state */
  fun shouldEnableSaveButton(
      formManager: ProfileFormManager,
      initialProfile: UserProfile?
  ): Boolean {
    val hasChanges = ProfileFieldConfiguration.hasAnyFieldChanged(formManager, initialProfile)
    val isValid = ProfilePageLogic.canSaveProfile(formManager.buildSanitizedProfile())
    return hasChanges && isValid
  }

  /** Determines confirmation message text */
  fun getConfirmationMessage(): String {
    return "Saved"
  }

  /** Gets footer text */
  fun getFooterText(): String {
    return "BY EPFL"
  }

  /** Determines if dropdown should expand when field is enabled */
  fun shouldAllowDropdownExpansion(enabled: Boolean): Boolean {
    return enabled
  }

  /** Determines placeholder color alpha */
  fun getPlaceholderAlpha(isPrimary: Boolean): Float {
    return if (isPrimary) 0.5f else 0.6f
  }

  /** Determines supporting text color alpha */
  fun getSupportingTextAlpha(): Float {
    return 0.7f
  }

  /** Determines divider alpha */
  fun getDividerAlpha(): Float {
    return 0.08f
  }

  /** Determines icon background alpha */
  fun getIconBackgroundAlpha(): Float {
    return 0.06f
  }

  /** Determines disabled text alpha */
  fun getDisabledTextAlpha(): Float {
    return 0.8f
  }

  /** Calculates section spacing */
  fun getSectionSpacing(): Int {
    return 24
  }

  /** Calculates header spacing */
  fun getHeaderSpacing(): Int {
    return 12
  }

  /** Determines if role field should show dropdown */
  fun shouldShowRoleDropdown(roleOptions: List<String>): Boolean {
    return roleOptions.isNotEmpty()
  }

  /** Validates email format (basic validation) */
  fun isValidEmailFormat(email: String): Boolean {
    if (email.isBlank()) return true // Empty is valid (will use placeholder)
    return email.contains("@") && email.contains(".")
  }

  /** Validates phone format (basic validation) */
  fun isValidPhoneFormat(phone: String): Boolean {
    if (phone.isBlank()) return true // Empty is valid
    // Basic check: starts with + or digits, contains only digits, spaces, hyphens, parentheses
    return phone.matches(Regex("^[+]?[\\d\\s\\-()]+$"))
  }

  /** Gets field count per section */
  fun getFieldCountPerSection(sectionTitle: String): Int {
    return when (sectionTitle) {
      "Personal details" -> ProfileFieldConfiguration.getPersonalDetailsFields().size
      "Academic information" -> ProfileFieldConfiguration.getAcademicFields().size
      "Contact" -> ProfileFieldConfiguration.getContactFields().size
      else -> 0
    }
  }

  /** Determines if section should show divider after field at index */
  fun shouldShowDividerAfter(index: Int, totalFields: Int): Boolean {
    return index < totalFields - 1
  }
}
