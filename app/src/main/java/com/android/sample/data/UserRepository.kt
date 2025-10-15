package com.android.sample.data

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun uid() = auth.currentUser?.uid ?: error("Not signed in")

    suspend fun upsertProfile(p: models.UserProfile) {
        db.document(DbPaths.userDoc(p.uid)).set(p, SetOptions.merge()).await()
    }

    suspend fun getProfile(): models.UserProfile? {
        val snap = db.document(DbPaths.userDoc(uid())).get().await()
        return snap.toObject<models.UserProfile>() // <-- extension KTX
    }

    suspend fun upsertSettings(s: models.UserSettings) {
        db.document(DbPaths.userSettings(uid())).set(s, SetOptions.merge()).await()
    }

    suspend fun logQuery(meta: models.QueryLog) {
        db.collection(DbPaths.userQueries(uid())).add(meta.copy(userUid = uid())).await()
    }
}
