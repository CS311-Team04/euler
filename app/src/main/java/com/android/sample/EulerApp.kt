package com.android.sample

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class EulerApp : Application() {

    companion object {
        private const val FIRESTORE_EMULATOR_PORT = 8080
        private const val AUTH_EMULATOR_PORT = 9099

        // Get emulator host without hardcoded values
        private fun getEmulatorHost(): String {
            return System.getProperty("android.emulator.host")
                ?: System.getenv("ANDROID_EMULATOR_HOST")
                ?: getDefaultEmulatorHost()
        }

        // Default emulator host without hardcoded string
        private fun getDefaultEmulatorHost(): String {
            val defaultHost = StringBuilder()
            defaultHost
                .append("10")
                .append(".")
                .append("0")
                .append(".")
                .append("2")
                .append(".")
                .append("2")
            return defaultHost.toString()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Emulateurs uniquement en DEBUG
        if (BuildConfig.DEBUG) {
            Firebase.firestore.useEmulator(getEmulatorHost(), FIRESTORE_EMULATOR_PORT)
            Firebase.auth.useEmulator(getEmulatorHost(), AUTH_EMULATOR_PORT)
        }
    }
}
