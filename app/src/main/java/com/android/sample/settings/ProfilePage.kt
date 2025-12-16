package com.android.sample.settings

import AnimationConfig
import TestFlags
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun ProfilePage(
    onBackClick: () -> Unit = {},
    onSaveProfile: (UserProfile) -> Unit = {},
    initialProfile: UserProfile? = null
) {
  val colorScheme = MaterialTheme.colorScheme
  val background = colorScheme.background
  val cardColor = colorScheme.surface
  val textPrimary = colorScheme.onBackground
  val textSecondary = colorScheme.onSurfaceVariant
  val accent = colorScheme.primary

  val authEmail = remember {
    if (TestFlags.fakeEmail != null) {
      TestFlags.fakeEmail ?: ""
    } else {
      runCatching { FirebaseAuth.getInstance().currentUser?.email.orEmpty() }.getOrDefault("")
    }
  }

  val formManager = remember { ProfileFormManager(authEmail, initialProfile) }
  var showSavedConfirmation by remember { mutableStateOf(false) }

  LaunchedEffect(initialProfile, authEmail) { formManager.reset(initialProfile, authEmail) }

  LaunchedEffect(showSavedConfirmation) {
    if (showSavedConfirmation) {
      if (AnimationConfig.disableAnimations) {
        showSavedConfirmation = false
      } else {
        delay(2500)
        showSavedConfirmation = false
      }
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(background)) {
    Column(
        modifier =
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = textPrimary)
            }
            Text(
                text = "Profile",
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center)
            TextButton(
                onClick = {
                  val formState = formManager.buildSanitizedProfile()
                  onSaveProfile(formState)
                  showSavedConfirmation = true
                }) {
                  Text(text = "Save", color = textPrimary)
                }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (showSavedConfirmation) {
            Text(
                text = "Saved",
                color = accent,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
          }

          Spacer(modifier = Modifier.height(24.dp))

          EditableInfoSection(
              title = "Personal details",
              fields =
                  listOf(
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.Person,
                                  label = "Full name",
                                  placeholder = "Enter your full name"),
                          value = formManager.fullName,
                          onValueChange = { formManager.updateFullName(it) },
                          enabled = !formManager.isFullNameLocked,
                          supportingText = "This name can only be set once.",
                          supportingTextColor = accent,
                          showSupportingTextWhenNotBlank = true),
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.Badge,
                                  label = "Username",
                                  placeholder = "Let others know how to address you"),
                          value = formManager.username,
                          onValueChange = { formManager.updateUsername(it) }),
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.Badge,
                                  label = "Role",
                                  placeholder = "Select your role"),
                          value = formManager.role,
                          onValueChange = { formManager.updateRole(it) },
                          options = formManager.roleOptions,
                          placeholderColor = textSecondary.copy(alpha = 0.6f))),
              backgroundColor = cardColor,
              textPrimary = textPrimary,
              textSecondary = textSecondary)

          Spacer(modifier = Modifier.height(24.dp))

          EditableInfoSection(
              title = "Academic information",
              fields =
                  listOf(
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.HomeWork,
                                  label = "Faculty",
                                  placeholder = "Select your faculty"),
                          value = formManager.faculty,
                          onValueChange = { formManager.updateFaculty(it) },
                          options = formManager.facultyOptions,
                          placeholderColor = textSecondary.copy(alpha = 0.6f)),
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.LocationCity,
                                  label = "Section",
                                  placeholder = "Select your section/program"),
                          value = formManager.section,
                          onValueChange = { formManager.updateSection(it) },
                          options = formManager.sectionOptions,
                          placeholderColor = textSecondary.copy(alpha = 0.6f))),
              backgroundColor = cardColor,
              textPrimary = textPrimary,
              textSecondary = textSecondary)

          Spacer(modifier = Modifier.height(24.dp))

          EditableInfoSection(
              title = "Contact",
              fields =
                  listOf(
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.MailOutline,
                                  label = "Email",
                                  placeholder = "name@epfl.ch"),
                          value = formManager.email,
                          onValueChange = { formManager.updateEmail(it) },
                          enabled = !formManager.isEmailLocked,
                          supportingText = "Managed via your Microsoft account",
                          supportingTextColor = textSecondary.copy(alpha = 0.7f),
                          placeholderColor = textSecondary.copy(alpha = 0.5f)),
                      ProfileFieldState(
                          definition =
                              ProfileFieldDefinition(
                                  icon = Icons.Filled.Phone,
                                  label = "Phone",
                                  placeholder = "+41 00 000 00 00"),
                          value = formManager.phone,
                          onValueChange = { formManager.updatePhone(it) },
                          placeholderColor = textSecondary.copy(alpha = 0.5f))),
              backgroundColor = cardColor,
              textPrimary = textPrimary,
              textSecondary = textSecondary)

          Spacer(modifier = Modifier.height(24.dp))

          Spacer(modifier = Modifier.height(32.dp))

          Text(
              text = "BY EPFL",
              color = textSecondary,
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
  }
}

private data class ProfileFieldDefinition(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val placeholder: String
)

private data class ProfileFieldState(
    val definition: ProfileFieldDefinition,
    val value: String,
    val onValueChange: (String) -> Unit,
    val enabled: Boolean = true,
    val options: List<String>? = null,
    val placeholderColor: Color? = null,
    val supportingText: String? = null,
    val supportingTextColor: Color? = null,
    val showSupportingTextWhenNotBlank: Boolean = false
)

@Composable
private fun EditableInfoSection(
    title: String,
    fields: List<ProfileFieldState>,
    backgroundColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
  val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
  Surface(
      color = backgroundColor,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth(),
      tonalElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
          Text(
              text = title,
              color = textPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(bottom = 12.dp))

          fields.forEachIndexed { index, field ->
            EditableInfoRow(
                field = field,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                backgroundColor = backgroundColor)

            if (index < fields.lastIndex) {
              HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = dividerColor)
            }
          }
        }
      }
}

@Composable
private fun EditableInfoRow(
    field: ProfileFieldState,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
  val contentAlpha = if (field.enabled) 1f else 0.5f
  Column(
      modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabelRow(
            icon = field.definition.icon,
            label = field.definition.label,
            textPrimary = textPrimary,
            textSecondary = textSecondary)

        if (field.options != null) {
          DropdownFieldContent(
              field = field,
              textPrimary = textPrimary,
              textSecondary = textSecondary,
              backgroundColor = backgroundColor)
        } else {
          ProfileTextField(
              value = field.value,
              onValueChange = field.onValueChange,
              placeholder = field.definition.placeholder,
              textColor = textPrimary,
              placeholderColor = field.placeholderColor ?: textSecondary,
              enabled = field.enabled)
        }

        FieldSupportingText(field = field, textSecondary = textSecondary)
      }
}

@Composable
private fun FieldLabelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textPrimary: Color,
    textSecondary: Color
) {
  val iconBackground = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.size(40.dp), shape = CircleShape, color = iconBackground) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp))
              }
            }
        Text(text = label, color = textSecondary, fontSize = 13.sp)
      }
}

@Composable
private fun DropdownFieldContent(
    field: ProfileFieldState,
    textPrimary: Color,
    textSecondary: Color,
    backgroundColor: Color
) {
  var expanded by rememberSaveable(field.definition.label) { mutableStateOf(false) }
  val dropdownBackground = MaterialTheme.colorScheme.surfaceVariant
  val hasValue = field.value.isNotBlank()
  val displayText = if (hasValue) field.value else field.definition.placeholder
  val displayColor =
      if (hasValue) {
        textPrimary
      } else {
        field.placeholderColor ?: textSecondary
      }
  val clickableModifier =
      if (field.enabled) {
        Modifier.fillMaxWidth().clickable { expanded = true }
      } else {
        Modifier.fillMaxWidth()
      }

  Surface(modifier = clickableModifier, color = dropdownBackground, shape = RoundedCornerShape(12.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = displayText, color = displayColor, fontSize = 15.sp)
          Icon(
              imageVector = Icons.Filled.ExpandMore,
              contentDescription = null,
              tint = textSecondary)
        }
  }

  DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      containerColor = backgroundColor,
      properties =
          androidx.compose.ui.window.PopupProperties(
              focusable = true,
              dismissOnClickOutside = true,
              usePlatformDefaultWidth = !AnimationConfig.disableAnimations)) {
        field.options?.forEach { option ->
          DropdownMenuItem(
              text = { Text(option, color = textPrimary) },
              onClick = {
                field.onValueChange(option)
                expanded = false
              },
              enabled = field.enabled)
        }
      }
}

@Composable
private fun FieldSupportingText(field: ProfileFieldState, textSecondary: Color) {
  val shouldShowSupportingText =
      field.supportingText != null &&
          (!field.showSupportingTextWhenNotBlank || field.value.isNotBlank())
  if (shouldShowSupportingText) {
    Text(
        text = field.supportingText ?: "",
        color = field.supportingTextColor ?: textSecondary.copy(alpha = 0.7f),
        fontSize = 12.sp)
  }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textColor: Color,
    placeholderColor: Color,
    enabled: Boolean = true
) {
  val colorScheme = MaterialTheme.colorScheme
  val containerColor = colorScheme.surfaceVariant
  val indicatorColor = colorScheme.outline
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = enabled,
      readOnly = !enabled,
      placeholder = { Text(text = placeholder, color = placeholderColor) },
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = containerColor,
              unfocusedContainerColor = containerColor,
              disabledContainerColor = containerColor,
              focusedIndicatorColor = indicatorColor,
              unfocusedIndicatorColor = indicatorColor.copy(alpha = 0.7f),
              disabledIndicatorColor = indicatorColor.copy(alpha = 0.4f),
              cursorColor = textColor,
              focusedTextColor = textColor,
              unfocusedTextColor = textColor,
              disabledTextColor = textColor.copy(alpha = 0.8f),
              focusedPlaceholderColor = placeholderColor,
              unfocusedPlaceholderColor = placeholderColor,
              disabledPlaceholderColor = placeholderColor),
      shape = RoundedCornerShape(12.dp))
}
