package com.android.sample.conversations

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Thrown when an operation requires a signed-in user but Auth is not ready. */
class AuthNotReadyException : IllegalStateException("Auth not ready")

/**
 * Repository for user-scoped chat conversations and messages stored in Firestore.
 *
 * Collection layout (per authenticated user):
 *
 * users/{uid} └── conversations/{conversationId} (Conversation document) ├── title : String ├──
 * lastMessagePreview : String ├── createdAt : Timestamp (server) ├── updatedAt : Timestamp (server)
 * ├── ownerUid : String (same as {uid}) ├── archived : Boolean └── messages/{messageId} (MessageDTO
 * document) ├── role : "user" | "assistant" ├── text : String └── createdAt : Timestamp (server)
 *
 * Key points
 * - All read/write operations are scoped to the current FirebaseAuth user.
 * - Streaming APIs (`conversationsFlow`, `messagesFlow`) expose real-time updates via cold Flows.
 * - Writes use server timestamps; `createdAt`/`updatedAt` may be null immediately after write.
 * - Batch writes keep conversation metadata (preview/updatedAt) consistent with message appends.
 * - Deletion removes the conversation doc and messages in one batch.
 *
 * Expected security rules (conceptual):
 * - Read/write allowed only for `request.auth.uid == resource.data.ownerUid`.
 */
class ConversationRepository(private val auth: FirebaseAuth, private val db: FirebaseFirestore) {
  /** Current UID or throws [AuthNotReadyException] if signed out. */
  private fun uid(): String = auth.currentUser?.uid ?: throw AuthNotReadyException()

  /** `users/{uid}` document reference. */
  private fun userCol() = db.collection("users").document(uid())

  /** `users/{uid}/conversations` collection reference. */
  private fun convCol() = userCol().collection("conversations")

  /** `.../conversations/{conversationId}/messages` collection reference. */
  private fun msgCol(conversationId: String) =
      convCol().document(conversationId).collection("messages")

  /**
   * Creates a new conversation document and returns its ID.
   *
   * Side effects:
   * - Sets `createdAt` and `updatedAt` via server timestamp.
   * - Initializes `lastMessagePreview` to empty string.
   *
   * @param title Initial conversation title (shown in lists).
   * @return The Firestore document ID of the new conversation.
   * @throws AuthNotReadyException if no user is signed in.
   */
  suspend fun startNewConversation(title: String = "New conversation"): String {
    val uid = auth.currentUser?.uid ?: throw AuthNotReadyException()
    val ref = convCol().document()
    val now = FieldValue.serverTimestamp()
    ref.set(
            mapOf(
                "title" to title,
                "lastMessagePreview" to "",
                "createdAt" to now,
                "updatedAt" to now,
                "ownerUid" to uid,
                "archived" to false))
        .await()
    return ref.id
  }

  /**
   * Real-time stream of the current user's conversations, ordered by `updatedAt` DESC.
   *
   * Emits a new list on any change (add/update/delete). The `id` field is populated from the
   * document ID.
   *
   * @return Cold [Flow] that must be collected on a coroutine scope.
   * @throws AuthNotReadyException when collected if user is signed out.
   */
  fun conversationsFlow(): Flow<List<Conversation>> = callbackFlow {
    val reg =
        convCol().orderBy("updatedAt", Query.Direction.DESCENDING).addSnapshotListener { s, e ->
          if (e != null) {
            close(e)
          } else {
            val list =
                s?.documents?.mapNotNull { d -> d.toObject<Conversation>()?.copy(id = d.id) }
                    ?: emptyList()
            trySend(list)
          }
        }
    awaitClose { reg.remove() }
  }

  /**
   * Real-time stream of messages for a given conversation, ordered by `createdAt` ASC.
   *
   * Use this to render the chat thread. Each emission contains the full ordered list
   * (diffing/rendering is handled by the UI layer).
   *
   * @param conversationId Target conversation ID.
   * @return Cold [Flow] of message lists; empty list if no messages.
   * @throws AuthNotReadyException when collected if user is signed out.
   */
  fun messagesFlow(conversationId: String): Flow<List<MessageDTO>> = callbackFlow {
    val reg =
        msgCol(conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { s, e ->
              if (e != null) {
                close(e)
              } else {
                val items = s?.documents?.mapNotNull { it.toObject<MessageDTO>() } ?: emptyList()
                trySend(items)
              }
            }
    awaitClose { reg.remove() }
  }

  /**
   * Appends a message to `.../messages` and updates the parent conversation metadata atomically.
   *
   * Atomic batch contents:
   * - Insert message (`role`, `text`, `createdAt=serverTimestamp`).
   * - Update parent conversation:
   *     - `lastMessagePreview` := first 120 chars of [text]
   *     - `updatedAt` := serverTimestamp
   *
   * @param conversationId Parent conversation ID.
   * @param role "user" or "assistant".
   * @param text Message body.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun appendMessage(conversationId: String, role: String, text: String) {
    val convRef = convCol().document(conversationId)
    val msgRef = msgCol(conversationId).document()
    val now = FieldValue.serverTimestamp()
    db.runBatch { b ->
          // Write both "text" (for our app) and "content" (for Cloud Function compatibility)
          b.set(
              msgRef, mapOf("role" to role, "text" to text, "content" to text, "createdAt" to now))
          b.update(convRef, mapOf("lastMessagePreview" to text.take(120), "updatedAt" to now))
        }
        .await()
  }

  /**
   * Deletes a conversation in a single batch.
   *
   * Notes:
   * - Firestore limits a batch to 500 writes; this handles typical cases. If you expect >500
   *   messages per conversation, implement pagination and multiple batches.
   *
   * @param conversationId Conversation to delete.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun deleteConversation(conversationId: String) {
    val convRef = convCol().document(conversationId)
    // delete up to 500 messages
    val msgs = msgCol(conversationId).limit(500).get().await()
    val batch = db.batch()
    msgs.forEach { batch.delete(it.reference) }
    batch.delete(convRef)
    batch.commit().await()
  }

  /**
   * Updates the conversation title and bumps `updatedAt` (server timestamp).
   *
   * Typical usage:
   * - First message triggers title generation (local or via a model) → call this to persist.
   */
  suspend fun updateConversationTitle(conversationId: String, title: String) {
    convCol()
        .document(conversationId)
        .update(mapOf("title" to title, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
  }
}
