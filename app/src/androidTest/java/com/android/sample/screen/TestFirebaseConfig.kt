package com.android.sample.screen

import com.google.firebase.FirebaseOptions

/**
 * Configuration for Firebase test initialization. Centralizes Firebase options to avoid magic
 * strings and make changes easier.
 */
object TestFirebaseConfig {
  const val APPLICATION_ID = "1:1234567890:android:integration-test"
  const val PROJECT_ID = "integration-test"
  const val API_KEY = "fake-api-key"

  /** Creates FirebaseOptions for test initialization. */
  fun createFirebaseOptions(): FirebaseOptions {
    return FirebaseOptions.Builder()
        .setApplicationId(APPLICATION_ID)
        .setProjectId(PROJECT_ID)
        .setApiKey(API_KEY)
        .build()
  }
}
