package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.android.sample.navigation.AppNav
import com.android.sample.ui.theme.SampleAppTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase to use emulator for development
    try {
      Firebase.auth.useEmulator("10.0.2.2", 9099)
      Firebase.firestore.useEmulator("10.0.2.2", 8080)
    } catch (e: Exception) {
      // Emulator already configured or not available
    }

    setContent { SampleAppTheme { AppNav(startOnSignedIn = false, activity = this@MainActivity) } }
  }
}
