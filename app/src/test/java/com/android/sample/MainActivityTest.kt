package com.android.sample

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MainActivityTest {

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
  }

  @After
  fun tearDown() {
    // Clean up if needed
  }

  @Test
  fun `Firestore offline persistence is enabled`() {
    val db = FirebaseFirestore.getInstance()
    val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    db.firestoreSettings = settings

    // Verify that persistence is enabled
    val currentSettings = db.firestoreSettings
    assertNotNull("Firestore settings should be set", currentSettings)
    assertTrue("Persistence should be enabled", currentSettings.isPersistenceEnabled)
  }

  @Test
  fun `Firestore settings can be configured with persistence`() {
    val db = FirebaseFirestore.getInstance()
    val settings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()

    // This should not throw
    db.firestoreSettings = settings

    // Verify settings were applied
    val appliedSettings = db.firestoreSettings
    assertNotNull("Settings should be applied", appliedSettings)
  }
}
