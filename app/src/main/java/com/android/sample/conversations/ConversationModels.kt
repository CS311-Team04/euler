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
 * Lightweight message transfer object stored under:
 *
 * users/{ownerUid}/conversations/{conversationId}/messages/{messageId}
 *
 * Firestore mapping:
 * - role : String ("user" | "assistant") — drives bubble alignment/styling in UI
 * - text : String (full message content)
 * - createdAt : Timestamp (server timestamp for ordering and time labels)
 * - edCardId : String? (optional ID of associated EdCard stored in edCards subcollection)
 * - sourceSiteLabel : String? (optional label for RAG source, e.g. "epfl.ch")
 * - sourceUrl : String? (optional URL for RAG source)
 * - sourceCompactType : String? (optional compact type: "NONE", "SCHEDULE", "FOOD")
 *
 * Notes:
 * - Keep this DTO minimal for efficient real-time updates.
 * - Use `orderBy("createdAt", ASC)` when streaming a conversation.
 * - EdCard is stored in messages/{messageId}/edCards/{edCardId} subcollection.
 */
data class MessageDTO(
    val role: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null,
    val edCardId: String? = null,
    val sourceSiteLabel: String? = null,
    val sourceUrl: String? = null,
    val sourceCompactType: String? = null
)
