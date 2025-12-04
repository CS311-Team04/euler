package com.android.sample.conversations

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.toObject
import java.security.MessageDigest
import kotlinx.coroutines.tasks.await

/**
 * Repository for caching AI responses to predefined suggestion questions.
 *
 * Storage structure: globalCachedResponses/{questionHash}
 * - question: String (original question text)
 * - response: String (cached AI response)
 * - createdAt: Timestamp
 * - updatedAt: Timestamp
 *
 * This allows suggestion chips to work offline by retrieving pre-cached responses. Uses a global
 * collection so cached responses are available to all users.
 */
class CachedResponseRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
  /** `globalCachedResponses` collection reference - shared across all users. */
  private fun cachedResponsesCol() = db.collection("globalCachedResponses")

  /**
   * Generates a consistent hash for a question to use as document ID. This ensures the same
   * question always maps to the same cache entry.
   */
  private fun hashQuestion(question: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(question.trim().lowercase().toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  /**
   * Retrieves a cached response for a given question, if available. When offline, uses Firestore's
   * local cache (Source.CACHE). When online, tries cache first, then server.
   *
   * @param question The question text to look up.
   * @param preferCache If true, only reads from local cache (for offline mode).
   * @return The cached response text, or null if not found.
   */
  suspend fun getCachedResponse(question: String, preferCache: Boolean = false): String? {
    return try {
      val questionHash = hashQuestion(question)
      val docRef = cachedResponsesCol().document(questionHash)

      val doc =
          if (preferCache) {
            // Force read from local cache only (for offline mode)
            android.util.Log.d(
                "CachedResponseRepository",
                "Reading from local cache only for: ${question.take(50)}...")
            docRef.get(Source.CACHE).await()
          } else {
            // Try cache first, fallback to server
            docRef.get(Source.CACHE).await()
          }

      if (doc.exists()) {
        val response = doc.toObject<CachedResponse>()?.response
        android.util.Log.d(
            "CachedResponseRepository",
            "Found cached response for: ${question.take(50)}... (from ${if (preferCache) "cache" else "cache/server"})")
        response
      } else {
        android.util.Log.d(
            "CachedResponseRepository", "No cached response found for: ${question.take(50)}...")
        null
      }
    } catch (e: Exception) {
      // Log but don't throw - cache miss is acceptable
      android.util.Log.w(
          "CachedResponseRepository",
          "Error retrieving cached response for '${question.take(50)}...'",
          e)
      null
    }
  }

  /**
   * Saves or updates a cached response for a question.
   *
   * @param question The question text.
   * @param response The AI response to cache.
   */
  suspend fun saveCachedResponse(question: String, response: String) {
    try {
      android.util.Log.d(
          "CachedResponseRepository",
          "Attempting to save cached response for: ${question.take(50)}...")
      val questionHash = hashQuestion(question)
      android.util.Log.d("CachedResponseRepository", "Question hash: $questionHash")

      val now = com.google.firebase.firestore.FieldValue.serverTimestamp()
      val data =
          mapOf(
              "question" to question.trim(),
              "response" to response,
              "createdAt" to now,
              "updatedAt" to now)

      android.util.Log.d("CachedResponseRepository", "Saving to collection: globalCachedResponses")
      val docRef = cachedResponsesCol().document(questionHash)
      android.util.Log.d("CachedResponseRepository", "Document path: ${docRef.path}")
      docRef.set(data).await()

      // Verify the write by reading it back
      val verifyDoc = docRef.get().await()
      if (verifyDoc.exists()) {
        android.util.Log.d(
            "CachedResponseRepository",
            "✅ Verified: Document exists in Firestore at path: ${docRef.path}")
        android.util.Log.d(
            "CachedResponseRepository",
            "Document ID: ${verifyDoc.id}, Question: ${verifyDoc.getString("question")?.take(50)}...")
      } else {
        android.util.Log.w(
            "CachedResponseRepository",
            "⚠️ Warning: Document write completed but verification read shows it doesn't exist")
      }

      android.util.Log.d(
          "CachedResponseRepository",
          "Successfully saved cached response for: ${question.take(50)}...")
    } catch (e: Exception) {
      // Log but don't throw - caching failures shouldn't break the app
      android.util.Log.e(
          "CachedResponseRepository",
          "Error saving cached response for '${question.take(50)}...'",
          e)
      android.util.Log.e(
          "CachedResponseRepository",
          "Error type: ${e.javaClass.simpleName}, Message: ${e.message}")
      e.printStackTrace()
    }
  }

  /**
   * Checks if a cached response exists for a question (without retrieving it). Useful for quick
   * checks before attempting network calls.
   */
  suspend fun hasCachedResponse(question: String): Boolean {
    return try {
      val questionHash = hashQuestion(question)
      val doc = cachedResponsesCol().document(questionHash).get().await()
      doc.exists() && doc.toObject<CachedResponse>()?.response?.isNotBlank() == true
    } catch (e: Exception) {
      false
    }
  }
}

/** Data class for cached response documents in Firestore. */
private data class CachedResponse(
    val question: String = "",
    val response: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val updatedAt: com.google.firebase.Timestamp? = null
)
