package com.android.sample.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingRoleScreen(onContinue: () -> Unit, viewModel: OnboardingRoleViewModel = viewModel()) {
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
            text = "What is your role?",
            style = MaterialTheme.typography.headlineMedium,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // Role selection cards
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              RoleOptionCard(
                  role = "Student",
                  isSelected = uiState.selectedRole == "Student",
                  onClick = { viewModel.selectRole("Student") })

              RoleOptionCard(
                  role = "PhD Candidate",
                  isSelected = uiState.selectedRole == "PhD Candidate",
                  onClick = { viewModel.selectRole("PhD Candidate") })

              RoleOptionCard(
                  role = "Professor",
                  isSelected = uiState.selectedRole == "Professor",
                  onClick = { viewModel.selectRole("Professor") })

              RoleOptionCard(
                  role = "Administration",
                  isSelected = uiState.selectedRole == "Administration",
                  onClick = { viewModel.selectRole("Administration") })
            }

        // Error message
        uiState.saveError?.let { error ->
          Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
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
                  modifier = Modifier.fillMaxWidth().height(56.dp),
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
                  modifier = Modifier.fillMaxWidth().height(56.dp),
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
private fun RoleOptionCard(role: String, isSelected: Boolean, onClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme

  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .clickable(onClick = onClick)
              .then(
                  if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = colorScheme.primary,
                        shape = RoundedCornerShape(12.dp))
                  } else {
                    Modifier
                  }),
      shape = RoundedCornerShape(12.dp),
      color =
          if (isSelected) {
            colorScheme.primaryContainer
          } else {
            colorScheme.surface
          },
      tonalElevation = if (isSelected) 0.dp else 1.dp) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center) {
              Text(
                  text = role,
                  fontSize = 16.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                  color =
                      if (isSelected) {
                        colorScheme.onPrimaryContainer
                      } else {
                        colorScheme.onSurface
                      },
                  textAlign = TextAlign.Center)
            }
      }
}
