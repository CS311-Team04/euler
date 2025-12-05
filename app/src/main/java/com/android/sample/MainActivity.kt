package com.android.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import com.android.sample.navigation.AppNav
import com.android.sample.settings.AppSettings
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.speech.TextToSpeechHelper
import com.android.sample.ui.theme.SampleAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.Locale

class MainActivity : ComponentActivity() {
  private val speechHelper by lazy { SpeechToTextHelper(this, this, Locale("fr", "FR")) }
  private val ttsHelper by lazy { TextToSpeechHelper(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /*if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }
    setContent { SampleAppTheme { AppNav(startOnSignedIn = false, activity = this@MainActivity) } }*/
    if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }

    // Enable Firestore offline persistence for cached responses
    val db = FirebaseFirestore.getInstance()
    val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    db.firestoreSettings = settings

    // Initialize app settings (loads saved preferences)
    AppSettings.initialize(this)
    // Initialize the helper here (lazy initialization happens on first access, but before
    // setContent)
    speechHelper
    ttsHelper
    setContent {
      val appearanceMode by AppSettings.appearanceState
      SampleAppTheme(appearanceMode = appearanceMode) {
        AppNav(
            startOnSignedIn = false,
            activity = this@MainActivity,
            speechHelper = speechHelper,
            ttsHelper = ttsHelper)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    speechHelper.destroy()
    ttsHelper.shutdown()
  }
}
