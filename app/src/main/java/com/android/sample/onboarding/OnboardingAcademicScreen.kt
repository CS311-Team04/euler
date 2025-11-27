package com.android.sample.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingAcademicScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingAcademicViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val colorScheme = MaterialTheme.colorScheme

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .padding(horizontal = 24.dp)
              .padding(top = 32.dp, bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween) {
        // Title
        Text(
            text = "Academic Information",
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // Input fields
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // Faculty dropdown
              AcademicDropdownField(
                  label = "Faculty",
                  selectedValue = uiState.selectedFaculty,
                  options = uiState.availableFaculties,
                  placeholder = "Select faculty",
                  enabled = true,
                  onValueChange = { viewModel.updateFaculty(it) })

              // Section dropdown
              AcademicDropdownField(
                  label = "Section",
                  selectedValue = uiState.selectedSection,
                  options = uiState.availableSections,
                  placeholder =
                      if (uiState.selectedFaculty.isBlank()) {
                        "Select faculty first"
                      } else {
                        "Select section"
                      },
                  enabled = uiState.selectedFaculty.isNotBlank(),
                  onValueChange = { viewModel.updateSection(it) })
            }

        // Error message
        uiState.saveError?.let { error ->
          Text(
              text = error,
              color = colorScheme.error,
              fontSize = 14.sp,
              modifier = Modifier.padding(bottom = 8.dp))
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              // Continue button
              Button(
                  onClick = { viewModel.continueToNext(onContinue) },
                  enabled = uiState.canContinue,
                  modifier = Modifier.fillMaxWidth().height(56.dp).testTag("continue_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = colorScheme.primary,
                          disabledContainerColor = colorScheme.surfaceVariant)) {
                    Text(
                        text = if (uiState.isSaving) "Saving..." else "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium)
                  }

              // Skip button
              OutlinedButton(
                  onClick = { viewModel.skip(onContinue) },
                  enabled = !uiState.isSaving,
                  modifier = Modifier.fillMaxWidth().height(56.dp).testTag("skip_button"),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                      ButtonDefaults.outlinedButtonColors(
                          contentColor = colorScheme.onSurfaceVariant)) {
                    Text(
                        text = if (uiState.isSaving) "Saving..." else "Skip",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }
      }
}

@Composable
private fun AcademicDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
  var expanded by rememberSaveable(label) { mutableStateOf(false) }
  val colorScheme = MaterialTheme.colorScheme

  val displayText = if (selectedValue.isNotBlank()) selectedValue else placeholder
  val displayColor =
      if (selectedValue.isNotBlank()) {
        colorScheme.onSurface
      } else {
        colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
      }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))

    Surface(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier),
        color = colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(12.dp)) {
          Box(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
              contentAlignment = Alignment.CenterStart) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = displayText,
                          color = displayColor,
                          fontSize = 16.sp,
                          modifier = Modifier.weight(1f))
                      Icon(
                          imageVector = Icons.Filled.ExpandMore,
                          contentDescription = null,
                          tint =
                              colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f))
                    }
              }
        }

    if (enabled) {
      DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.fillMaxWidth(),
          containerColor = colorScheme.surface,
          properties = PopupProperties(focusable = true, dismissOnClickOutside = true)) {
            options.forEach { option ->
              DropdownMenuItem(
                  text = { Text(option, color = colorScheme.onSurface) },
                  onClick = {
                    onValueChange(option)
                    expanded = false
                  })
            }
          }
    }
  }
}
