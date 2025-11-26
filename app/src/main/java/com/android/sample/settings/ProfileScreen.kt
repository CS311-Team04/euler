package com.android.sample.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Composable wrapper for ProfilePage that manages the ViewModel and state.
 *
 * This screen:
 * - Instantiates ProfileViewModel
 * - Observes the userProfile state
 * - Renders ProfilePage with the loaded profile
 * - Connects save callbacks to the ViewModel
 */
@Composable
fun ProfileScreen(onBackClick: () -> Unit = {}, viewModel: ProfileViewModel = viewModel()) {
  val userProfile by viewModel.userProfile.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val isSaving by viewModel.isSaving.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  // Render ProfilePage with the loaded profile
  ProfilePage(
      onBackClick = onBackClick,
      onSaveProfile = { profile -> viewModel.saveProfile(profile) },
      initialProfile = userProfile)

  // Note: isLoading, isSaving, and errorMessage are available if you want to show
  // loading indicators or error messages in the UI. For now, ProfilePage handles
  // its own loading states via the form manager.
}
