package com.android.sample.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
