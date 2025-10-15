package com.android.sample.data

import com.google.firebase.auth.FirebaseAuth

/**
 * Crée/Merge le doc users/{uid} à partir de l'utilisateur Firebase courant. À appeler juste après
 * un sign-in réussi.
 */
suspend fun ensureProfile(
    repo: UserRepository,
    auth: FirebaseAuth,
) {
    val u = auth.currentUser ?: return
    repo.upsertProfile(
        models.UserProfile(
            uid = u.uid,
            email = u.email,
            displayName = u.displayName,
            photoUrl = u.photoUrl?.toString(),
        ),
    )
}

/** log d'une requête RAG (métadonnées uniquement). */
suspend fun logRag(
    repo: UserRepository,
    prompt: String,
    tokensIn: Int,
    tokensOut: Int,
    sources: List<String>,
    resultId: String?,
) {
    repo.logQuery(
        models.QueryLog(
            prompt = prompt,
            tokensIn = tokensIn,
            tokensOut = tokensOut,
            sources = sources,
            resultId = resultId,
        ),
    )
}
