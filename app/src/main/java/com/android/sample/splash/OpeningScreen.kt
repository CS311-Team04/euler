package com.android.sample.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.android.sample.R
import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.delay

@Composable
fun OpeningScreen(
    authState: AuthUiState,
    onNavigateToSignIn: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Navigate after 2.5 seconds
  LaunchedEffect(Unit) {
    delay(2500)
    // Navigate based on authentication state
    if (authState is AuthUiState.SignedIn) {
      onNavigateToHome()
    } else {
      onNavigateToSignIn()
    }
  }

  // Display the splash image from resources
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    // Background color as fallback
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)))

    // Display the splash image
    Image(
        painter = painterResource(id = R.drawable.opening_screen),
        contentDescription = "Opening Screen",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds)
  }
}
