package com.android.sample.screen

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.sample.MainActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.BeforeClass
import org.junit.Rule

/**
 * Base class for end-to-end tests that use MainActivity. Handles Firebase initialization before
 * MainActivity is created.
 */
abstract class BaseE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  companion object {
    @BeforeClass
    @JvmStatic
    fun initializeFirebase() {
      val context = ApplicationProvider.getApplicationContext<Context>()
      if (FirebaseApp.getApps(context).isEmpty()) {
        val options =
            FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:integration-test")
                .setProjectId("integration-test")
                .setApiKey("fake-api-key")
                .build()
        FirebaseApp.initializeApp(context, options)
      }
    }
  }
}
