package ch.epfl.euler.data

import ch.epfl.euler.data.DbPaths
import ch.epfl.euler.data.models
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ktx.toObject


class UserRepository(
    private val db: com.google.firebase.firestore.FirebaseFirestore = Firebase.firestore,
    private val auth: com.google.firebase.auth.FirebaseAuth = Firebase.auth
) {
    private fun uid() = auth.currentUser?.uid ?: error("Not signed in")

    suspend fun upsertProfile(p: models.UserProfile) {
        db.document(DbPaths.userDoc(p.uid)).set(p, SetOptions.merge()).await()
    }

    suspend fun getProfile(): models.UserProfile? {
        val snap = db.document(DbPaths.userDoc(uid())).get().await()
        return snap.toObject<models.UserProfile>()   // <-- extension KTX
    }

    suspend fun upsertSettings(s: models.UserSettings) {
        db.document(DbPaths.userSettings(uid())).set(s, SetOptions.merge()).await()
    }

    suspend fun logQuery(meta: models.QueryLog) {
        db.collection(DbPaths.userQueries(uid())).add(meta.copy(userUid = uid())).await()
    }
}
