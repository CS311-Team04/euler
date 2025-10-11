package ch.epfl.euler

import android.app.Application
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class EulerApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Emulateurs uniquement en DEBUG
        if (BuildConfig.DEBUG) {
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
        }
    }
}