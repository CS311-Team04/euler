package com.android.sample.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.sample.profile.UserProfile

/**
 * Manages the mutable state used by [ProfilePage], keeping business rules (locking, sanitizing,
 * defaults) testable without Compose UI.
 */
class ProfileFormManager(
    private val defaultAuthEmail: String,
    initialProfile: UserProfile?,
) {
  var fullName by mutableStateOf("")
    private set

  var username by mutableStateOf("")
    private set

  var role by mutableStateOf("")
    private set

  var faculty by mutableStateOf("")
    private set

  var section by mutableStateOf("")
    private set

  var email by mutableStateOf("")
    private set

  var phone by mutableStateOf("")
    private set

  var isFullNameLocked by mutableStateOf(false)
    private set

  var isEmailLocked by mutableStateOf(false)
    private set

  val roleOptions: List<String> =
      listOf("Student", "PhD", "Post-Doc", "Teacher", "Administration", "Staff")

  init {
    applyInitial(defaultAuthEmail, initialProfile)
  }

  fun reset(initialProfile: UserProfile?, authEmail: String = defaultAuthEmail) {
    applyInitial(authEmail, initialProfile)
  }

  private fun applyInitial(authEmail: String, profile: UserProfile?) {
    fullName = profile?.fullName.orEmpty()
    username = profile?.preferredName.orEmpty()
    role = profile?.roleDescription.orEmpty()
    faculty = profile?.faculty.orEmpty()
    section = profile?.section.orEmpty()
    phone = profile?.phone.orEmpty()

    val resolvedEmail =
        when {
          !profile?.email.isNullOrBlank() -> profile!!.email.trim()
          authEmail.isNotBlank() -> authEmail
          else -> ""
        }
    email = resolvedEmail

    isFullNameLocked = fullName.isNotBlank()
    isEmailLocked = resolvedEmail.isNotBlank()
  }

  fun updateFullName(value: String) {
    if (!isFullNameLocked) {
      fullName = value
    }
  }

  fun updateUsername(value: String) {
    username = value
  }

  fun updateRole(value: String) {
    role = value
  }

  fun updateFaculty(value: String) {
    faculty = value
  }

  fun updateSection(value: String) {
    section = value
  }

  fun updateEmail(value: String) {
    if (!isEmailLocked) {
      email = value
    }
  }

  fun updatePhone(value: String) {
    phone = value
  }

  /**
   * Sanitizes the current form values, updates the internal state with the sanitized values,
   * activates locks, and returns a [UserProfile] snapshot for persistence.
   */
  fun buildSanitizedProfile(): UserProfile {
    val sanitizedFullName = fullName.trim()
    val sanitizedUsername = username.trim()
    val sanitizedRole = role.trim()
    val sanitizedFaculty = faculty.trim()
    val sanitizedSection = section.trim()
    val sanitizedEmail = email.trim()
    val sanitizedPhone = phone.trim()

    fullName = sanitizedFullName
    username = sanitizedUsername
    role = sanitizedRole
    faculty = sanitizedFaculty
    section = sanitizedSection
    email = sanitizedEmail
    phone = sanitizedPhone

    if (sanitizedFullName.isNotBlank()) {
      isFullNameLocked = true
    }
    if (sanitizedEmail.isNotBlank()) {
      isEmailLocked = true
    }

    return UserProfile(
        fullName = sanitizedFullName,
        preferredName = sanitizedUsername,
        faculty = sanitizedFaculty,
        section = sanitizedSection,
        email = sanitizedEmail,
        phone = sanitizedPhone,
        roleDescription = sanitizedRole)
  }
}
