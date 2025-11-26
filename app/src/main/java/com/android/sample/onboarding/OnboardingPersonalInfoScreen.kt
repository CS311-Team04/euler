package com.android.sample.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingPersonalInfoScreen(
    viewModel: OnboardingPersonalInfoViewModel = viewModel(),
    onContinue: () -> Unit
) {
  val uiState by viewModel.uiState.collectAsState()
  val colorScheme = MaterialTheme.colorScheme
  val background = colorScheme.background
  val cardColor = colorScheme.surface
  val textPrimary = colorScheme.onBackground
  val textSecondary = colorScheme.onSurfaceVariant
  val accent = colorScheme.primary
  val errorColor = MaterialTheme.colorScheme.error

  // Navigate to next step when onboarding is complete
  LaunchedEffect(uiState.isComplete) {
    if (uiState.isComplete) {
      onContinue()
    }
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(background)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 20.dp, vertical = 16.dp)) {
        // Title
        Text(
            text = "Welcome to EULER",
            color = textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center)

        Text(
            text = "Step 1: Personal Information",
            color = textSecondary,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            textAlign = TextAlign.Center)

        Text(
            text = "Please provide your personal details to get started.",
            color = textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            textAlign = TextAlign.Center)

        // Form Card
        Surface(
            color = cardColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp) {
              Column(
                  modifier =
                      Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Text(
                        text = "Personal details",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp))

                    // First Name Field
                    OnboardingTextField(
                        label = "First Name",
                        value = uiState.firstName,
                        onValueChange = viewModel::updateFirstName,
                        placeholder = "Enter your first name",
                        icon = Icons.Filled.Person,
                        errorMessage = uiState.firstNameError,
                        enabled = !uiState.isSubmitting,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        errorColor = errorColor)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Last Name Field
                    OnboardingTextField(
                        label = "Last Name",
                        value = uiState.lastName,
                        onValueChange = viewModel::updateLastName,
                        placeholder = "Enter your last name",
                        icon = Icons.Filled.Person,
                        errorMessage = uiState.lastNameError,
                        enabled = !uiState.isSubmitting,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        errorColor = errorColor)

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Username Field
                    OnboardingTextField(
                        label = "Username",
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
                        placeholder = "Enter your username (min. 3 characters)",
                        icon = Icons.Filled.Person,
                        errorMessage = uiState.usernameError,
                        enabled = !uiState.isSubmitting,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        errorColor = errorColor)

                    // Submit Error
                    if (uiState.submitError != null) {
                      Spacer(modifier = Modifier.height(16.dp))
                      Text(
                          text = uiState.submitError!!,
                          color = errorColor,
                          fontSize = 12.sp,
                          modifier = Modifier.fillMaxWidth())
                    }
                  }
            }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
        Surface(
            color =
                if (uiState.isFormValid && !uiState.isSubmitting) accent
                else accent.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()) {
              TextButton(
                  onClick = { viewModel.submitPersonalInfo() },
                  enabled = uiState.isFormValid && !uiState.isSubmitting,
                  modifier =
                      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (uiState.isSubmitting) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(20.dp),
                          color = MaterialTheme.colorScheme.onPrimary,
                          strokeWidth = 2.dp)
                      Spacer(modifier = Modifier.width(12.dp))
                      Text(
                          text = "Saving...",
                          color = MaterialTheme.colorScheme.onPrimary,
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Bold)
                    } else {
                      Text(
                          text = "Continue",
                          color = MaterialTheme.colorScheme.onPrimary,
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Bold)
                    }
                  }
            }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer
        Text(
            text = "BY EPFL",
            color = textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
      }
}

@Composable
private fun OnboardingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    errorMessage: String?,
    enabled: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    errorColor: Color
) {
  val iconBackground = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
  val containerColor = MaterialTheme.colorScheme.surfaceVariant
  val indicatorColor = MaterialTheme.colorScheme.outline
  val contentAlpha = if (enabled) 1f else 0.5f

  Column(
      modifier = Modifier.fillMaxWidth().alpha(contentAlpha),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Label with icon
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Surface(
                  modifier = Modifier.size(40.dp),
                  shape = androidx.compose.foundation.shape.CircleShape,
                  color = iconBackground) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                          imageVector = icon,
                          contentDescription = label,
                          tint = textPrimary,
                          modifier = Modifier.size(20.dp))
                    }
                  }
              Text(text = label, color = textSecondary, fontSize = 13.sp)
              Text(text = "*", color = errorColor, fontSize = 13.sp)
            }

        // Text Field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            placeholder = { Text(text = placeholder, color = textSecondary.copy(alpha = 0.6f)) },
            isError = errorMessage != null,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    disabledContainerColor = containerColor,
                    focusedIndicatorColor =
                        if (errorMessage != null) errorColor else indicatorColor,
                    unfocusedIndicatorColor =
                        if (errorMessage != null) errorColor else indicatorColor.copy(alpha = 0.7f),
                    disabledIndicatorColor = indicatorColor.copy(alpha = 0.4f),
                    cursorColor = textPrimary,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    disabledTextColor = textPrimary.copy(alpha = 0.8f),
                    focusedPlaceholderColor = textSecondary.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = textSecondary.copy(alpha = 0.6f),
                    disabledPlaceholderColor = textSecondary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp))

        // Error Message
        if (errorMessage != null) {
          Text(
              text = errorMessage,
              color = errorColor,
              fontSize = 12.sp,
              modifier = Modifier.padding(start = 52.dp))
        }
      }
}
