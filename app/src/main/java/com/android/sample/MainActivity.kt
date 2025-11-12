package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.android.sample.navigation.AppNav
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.speech.TextToSpeechHelper
import com.android.sample.ui.theme.SampleAppTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
  private val speechHelper by lazy { SpeechToTextHelper(this, this) }
  private val speechPlayback: SpeechPlayback by lazy { TextToSpeechHelper(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /*if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }
    setContent { SampleAppTheme { AppNav(startOnSignedIn = false, activity = this@MainActivity) } }*/
    if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }
    // Initialize the helper here (lazy initialization happens on first access, but before
    // setContent)
    speechHelper
    setContent {
      SampleAppTheme {
        AppNav(
            startOnSignedIn = false,
            activity = this@MainActivity,
            speechHelper = speechHelper,
            textToSpeechHelper = speechPlayback)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    speechPlayback.shutdown()
  }
}
