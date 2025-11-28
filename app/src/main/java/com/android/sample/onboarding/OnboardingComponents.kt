package com.android.sample.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

/**
 * Shared navigation footer component for onboarding screens. Displays Continue and Skip buttons
 * with consistent styling.
 *
 * @param onContinue Callback invoked when Continue button is clicked
 * @param onSkip Callback invoked when Skip button is clicked
 * @param canContinue Whether the Continue button should be enabled
 * @param isSaving Whether a save operation is in progress
 */
@Composable
fun OnboardingNavigationFooter(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    canContinue: Boolean,
    isSaving: Boolean
) {
  val colorScheme = MaterialTheme.colorScheme

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Continue button
    Button(
        onClick = onContinue,
        enabled = canContinue,
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("continue_button"),
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                disabledContainerColor = colorScheme.surfaceVariant)) {
          Text(
              text = if (isSaving) "Saving..." else "Continue",
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium)
        }

    // Skip button
    OutlinedButton(
        onClick = onSkip,
        enabled = !isSaving,
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("skip_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurfaceVariant)) {
          Text(
              text = if (isSaving) "Saving..." else "Skip",
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium)
        }
  }
}

/**
 * Reusable dropdown field component for onboarding screens.
 *
 * @param label The label text displayed above the dropdown
 * @param selectedValue The currently selected value
 * @param options List of available options to display in the dropdown
 * @param placeholder Text shown when no value is selected
 * @param enabled Whether the dropdown is enabled (disabled dropdowns are grayed out)
 * @param onValueChange Callback invoked when a new value is selected
 */
@Composable
fun OnboardingDropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
  var expanded by rememberSaveable(label) { mutableStateOf(false) }
  val colorScheme = MaterialTheme.colorScheme

  // Calculate alpha for disabled state
  val alpha = if (enabled) 1f else 0.5f

  // Calculate display text and color
  val displayText = if (selectedValue.isNotBlank()) selectedValue else placeholder
  val hasValue = selectedValue.isNotBlank()
  val displayColor =
      if (hasValue) {
        colorScheme.onSurface
      } else {
        colorScheme.onSurfaceVariant.copy(alpha = alpha)
      }

  // Calculate colors upfront
  val labelColor = colorScheme.onSurfaceVariant.copy(alpha = alpha)
  val surfaceColor = colorScheme.surfaceVariant.copy(alpha = alpha)
  val iconTint = colorScheme.onSurfaceVariant.copy(alpha = alpha)

  // Modifier for clickable behavior
  val clickableModifier =
      if (enabled) {
        Modifier.fillMaxWidth().clickable { expanded = true }
      } else {
        Modifier.fillMaxWidth()
      }

  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = labelColor)

    DropdownSelector(
        displayText = displayText,
        displayColor = displayColor,
        iconTint = iconTint,
        surfaceColor = surfaceColor,
        modifier = clickableModifier)

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

@Composable
private fun DropdownSelector(
    displayText: String,
    displayColor: Color,
    iconTint: Color,
    surfaceColor: Color,
    modifier: Modifier
) {
  Surface(modifier = modifier, color = surfaceColor, shape = RoundedCornerShape(12.dp)) {
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
                    tint = iconTint)
              }
        }
  }
}
