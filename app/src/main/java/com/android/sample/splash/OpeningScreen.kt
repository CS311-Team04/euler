package com.android.sample.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.authentification.AuthUiState
import com.android.sample.settings.Localization

@Composable
fun OpeningScreen(
    authState: AuthUiState,
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Navigate after 2.5 seconds
  LaunchedEffect(Unit) {
    handleOpeningScreenNavigation(
        authState = authState,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSignIn = onNavigateToSignIn)
  }

  val colorScheme = MaterialTheme.colorScheme

  Box(
      modifier =
          modifier.fillMaxSize().background(colorScheme.background).semantics {
            contentDescription = "Opening Screen"
          }) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Image(
                  painter = painterResource(id = R.drawable.euler_logo),
                  contentDescription = null,
                  modifier = Modifier.fillMaxWidth(0.45f),
                  alignment = Alignment.Center)
            }
        Text(
            text = Localization.t("by_epfl"),
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
      }
}
