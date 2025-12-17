package com.android.sample.conversations

import com.google.firebase.Timestamp

/**
 * Immutable snapshot of a single chat conversation stored under:
 *
 * users/{ownerUid}/conversations/{conversationId}
 *
 * A conversation document holds lightweight metadata used for listing and ordering chats in the UI.
 * Individual messages live in the "messages" subcollection of each conversation.
 *
 * Firestore mapping:
 * - id ← document ID
 * - title : String
 * - lastMessagePreview : String (short excerpt of the last message)
 * - createdAt : Timestamp (usually FieldValue.serverTimestamp())
 * - updatedAt : Timestamp (update whenever a new message is appended)
 * - ownerUid : String (Firebase Auth UID)
 * - archived : Boolean (soft-archive flag; not used to delete data)
 *
 * Ordering:
 * - List screens should order by `updatedAt` DESC to surface the most recent chats.
 */
data class Conversation(
    val id: String = "",
    val title: String = "",
    val lastMessagePreview: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val ownerUid: String = "",
    val archived: Boolean = false
)

/**
 * Source metadata for RAG answers, stored as part of MessageDTO.
 *
 * Note: sourceSiteLabel is derived from sourceUrl at persistence time to avoid trusting UI-provided
 * labels. The label is stored for display efficiency but can be regenerated from the URL if needed.
 */
data class MessageSourceMetadata(
    val url: String,
    val compactType: String // "NONE", "SCHEDULE", or "FOOD"
) {
  /** Derives a display label from the URL domain. */
  fun deriveLabel(): String {
    return runCatching { android.net.Uri.parse(url).host?.removePrefix("www.") ?: url }.getOrNull()
        ?: url
  }
}

/**
 * Lightweight message transfer object stored under:
 *
 * users/{ownerUid}/conversations/{conversationId}/messages/{messageId}
 *
 * Firestore mapping:
 * - role : String ("user" | "assistant") — drives bubble alignment/styling in UI
 * - text : String (full message content)
 * - createdAt : Timestamp (server timestamp for ordering and time labels)
 * - edCardId : String? (optional ID of associated EdCard stored in edCards subcollection)
 * - sourceUrl : String? (optional URL for RAG source)
 * - sourceCompactType : String? (optional compact type: "NONE", "SCHEDULE", "FOOD")
 *
 * Notes:
 * - Source metadata is stored as separate fields for Firestore compatibility.
 * - Use `orderBy("createdAt", ASC)` when streaming a conversation.
 * - EdCard is stored in messages/{messageId}/edCards/{edCardId} subcollection.
 * - sourceSiteLabel was removed; derive display labels from sourceUrl when needed.
 */
data class MessageDTO(
    val role: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val edCardId: String? = null,
    val sourceUrl: String? = null,
    val sourceCompactType: String? = null
)
