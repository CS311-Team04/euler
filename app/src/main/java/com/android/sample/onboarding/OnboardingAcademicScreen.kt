package com.android.sample.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Stateful screen composable for the academic information onboarding step. Manages the ViewModel
 * and state collection, delegating UI rendering to the stateless content.
 */
@Composable
fun OnboardingAcademicScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingAcademicViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  OnboardingAcademicContent(
      uiState = uiState,
      onContinue = { viewModel.continueToNext(onContinue) },
      onSkip = { viewModel.skip(onContinue) },
      onFacultyChange = { viewModel.updateFaculty(it) },
      onSectionChange = { viewModel.updateSection(it) })
}

/**
 * Stateless content composable for the academic information onboarding step. Accepts pure data and
 * callbacks, making it easily testable and previewable.
 *
 * @param uiState The current UI state
 * @param onContinue Callback invoked when Continue button is clicked
 * @param onSkip Callback invoked when Skip button is clicked
 * @param onFacultyChange Callback invoked when faculty selection changes
 * @param onSectionChange Callback invoked when section selection changes
 */
@Composable
fun OnboardingAcademicContent(
    uiState: OnboardingAcademicUiState,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onFacultyChange: (String) -> Unit,
    onSectionChange: (String) -> Unit
) {
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
              OnboardingDropdownField(
                  label = "Faculty",
                  selectedValue = uiState.selectedFaculty,
                  options = uiState.availableFaculties,
                  placeholder = "Select faculty",
                  enabled = true,
                  onValueChange = onFacultyChange)

              // Section dropdown
              OnboardingDropdownField(
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
                  onValueChange = onSectionChange)
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
        OnboardingNavigationFooter(
            onContinue = onContinue,
            onSkip = onSkip,
            canContinue = uiState.canContinue,
            isSaving = uiState.isSaving)
      }
}
