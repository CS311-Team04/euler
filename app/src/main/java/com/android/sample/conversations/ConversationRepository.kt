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

  /** `.../messages/{messageId}/edCards` collection reference. */
  private fun edCardCol(conversationId: String, messageId: String) =
      msgCol(conversationId).document(messageId).collection("edCards")

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
  fun messagesFlow(conversationId: String): Flow<List<Pair<MessageDTO, String>>> = callbackFlow {
    val reg =
        msgCol(conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { s, e ->
              if (e != null) {
                close(e)
              } else {
                val items =
                    s?.documents?.mapNotNull { doc ->
                      val dto = doc.toObject<MessageDTO>() ?: return@mapNotNull null
                      Pair(dto, doc.id)
                    } ?: emptyList()
                trySend(items)
              }
            }
    awaitClose { reg.remove() }
  }

  /**
   * Appends a message to `.../messages` and updates the parent conversation metadata atomically.
   *
   * Atomic batch contents:
   * - Insert message (`role`, `text`, `createdAt=serverTimestamp`, optional `edCardId`).
   * - Update parent conversation:
   *     - `lastMessagePreview` := first 120 chars of [text]
   *     - `updatedAt` := serverTimestamp
   *
   * @param conversationId Parent conversation ID.
   * @param role "user" or "assistant".
   * @param text Message body.
   * @param edCardId Optional ID of associated EdCard (for assistant messages that generate
   *   EdCards).
   * @return The Firestore document ID of the created message.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun appendMessage(
      conversationId: String,
      role: String,
      text: String,
      edCardId: String? = null
  ): String {
    val convRef = convCol().document(conversationId)
    val msgRef = msgCol(conversationId).document()
    val now = FieldValue.serverTimestamp()
    val messageData =
        mutableMapOf<String, Any>(
            "role" to role, "text" to text, "content" to text, "createdAt" to now)
    if (edCardId != null) {
      messageData["edCardId"] = edCardId
    }
    db.runBatch { b ->
          b.set(msgRef, messageData)
          b.update(convRef, mapOf("lastMessagePreview" to text.take(120), "updatedAt" to now))
        }
        .await()
    return msgRef.id
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

  /**
   * Saves an EdCard to Firebase and associates it with a message.
   *
   * Structure: messages/{messageId}/edCards/{edCardId}
   *
   * @param conversationId Parent conversation ID.
   * @param messageId Message ID to associate the EdCard with.
   * @param edCard The EdCard to save.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun saveEdCard(
      conversationId: String,
      messageId: String,
      edCard: com.android.sample.home.EdPostCard
  ) {
    val edCardRef = edCardCol(conversationId, messageId).document(edCard.id)
    val edCardData =
        mapOf(
            "id" to edCard.id,
            "title" to edCard.title,
            "body" to edCard.body,
            "status" to edCard.status.name,
            "createdAt" to com.google.firebase.Timestamp(edCard.createdAt / 1000, 0))
    edCardRef.set(edCardData).await()

    // Update the message to reference this EdCard
    val msgRef = msgCol(conversationId).document(messageId)
    msgRef.update("edCardId", edCard.id).await()
  }

  /**
   * Loads an EdCard from Firebase.
   *
   * @param conversationId Parent conversation ID.
   * @param messageId Message ID that the EdCard is associated with.
   * @param edCardId EdCard ID to load.
   * @return The EdCard or null if not found.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun loadEdCard(
      conversationId: String,
      messageId: String,
      edCardId: String
  ): com.android.sample.home.EdPostCard? {
    val snapshot = edCardCol(conversationId, messageId).document(edCardId).get().await()
    if (!snapshot.exists()) return null

    val data = snapshot.data ?: return null
    val statusStr = data["status"] as? String ?: return null
    val status =
        try {
          com.android.sample.home.EdPostStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
          return null
        }
    val createdAtTimestamp = data["createdAt"] as? com.google.firebase.Timestamp
    val createdAt = createdAtTimestamp?.seconds?.times(1000)?.toLong() ?: return null

    return com.android.sample.home.EdPostCard(
        id = data["id"] as? String ?: return null,
        title = data["title"] as? String ?: return null,
        body = data["body"] as? String ?: return null,
        status = status,
        createdAt = createdAt)
  }

  /**
   * Updates a message to associate it with an EdCard.
   *
   * @param conversationId Parent conversation ID.
   * @param messageId Message ID to update.
   * @param edCardId EdCard ID to associate.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun updateMessageWithEdCard(conversationId: String, messageId: String, edCardId: String) {
    val msgRef = msgCol(conversationId).document(messageId)
    msgRef.update("edCardId", edCardId).await()
  }

  /**
   * Saves an EdPostsCard (for fetched posts) to Firebase and associates it with a message.
   *
   * Structure: messages/{messageId}/edCards/{edCardId} Type field: "posts" to differentiate from
   * "post" (EdPostCard)
   *
   * @param conversationId Parent conversation ID.
   * @param messageId Message ID to associate the EdPostsCard with.
   * @param edPostsCard The EdPostsCard to save.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun saveEdPostsCard(
      conversationId: String,
      messageId: String,
      edPostsCard: com.android.sample.home.EdPostsCard
  ) {
    val edCardRef = edCardCol(conversationId, messageId).document(edPostsCard.id)
    val postsData =
        edPostsCard.posts.map { post ->
          mapOf(
              "title" to post.title,
              "content" to post.content,
              "date" to post.date,
              "author" to post.author,
              "url" to post.url)
        }
    val edCardData =
        mapOf(
            "id" to edPostsCard.id,
            "type" to "posts", // Differentiate from "post" type
            "messageId" to edPostsCard.messageId,
            "query" to edPostsCard.query,
            "posts" to postsData,
            "filters" to
                mapOf(
                    "course" to (edPostsCard.filters.course ?: ""),
                ),
            "stage" to edPostsCard.stage.name,
            "errorMessage" to (edPostsCard.errorMessage ?: ""),
            "createdAt" to com.google.firebase.Timestamp(edPostsCard.createdAt / 1000, 0))
    edCardRef.set(edCardData).await()

    // Update the message to reference this EdCard
    val msgRef = msgCol(conversationId).document(messageId)
    msgRef.update("edCardId", edPostsCard.id).await()
  }

  /**
   * Loads an EdPostsCard from Firebase.
   *
   * @param conversationId Parent conversation ID.
   * @param messageId Message ID that the EdPostsCard is associated with.
   * @param edCardId EdCard ID to load.
   * @return The EdPostsCard or null if not found or wrong type.
   * @throws AuthNotReadyException if user is signed out.
   */
  suspend fun loadEdPostsCard(
      conversationId: String,
      messageId: String,
      edCardId: String
  ): com.android.sample.home.EdPostsCard? {
    val snapshot = edCardCol(conversationId, messageId).document(edCardId).get().await()
    if (!snapshot.exists()) return null

    val data = snapshot.data ?: return null
    val type = data["type"] as? String
    if (type != "posts") return null // Only load "posts" type cards

    val stageStr = data["stage"] as? String ?: return null
    val stage =
        try {
          com.android.sample.home.EdPostsStage.valueOf(stageStr)
        } catch (e: IllegalArgumentException) {
          return null
        }

    val postsList = data["posts"] as? List<*> ?: return null
    val posts =
        postsList.mapNotNull { postData ->
          if (postData is Map<*, *>) {
            com.android.sample.home.EdPost(
                title = (postData["title"] as? String) ?: "",
                content = (postData["content"] as? String) ?: "",
                date = (postData["date"] as? Long) ?: 0L,
                author = (postData["author"] as? String) ?: "",
                url = (postData["url"] as? String) ?: "")
          } else {
            null
          }
        }

    val filtersData = data["filters"] as? Map<*, *>
    val course = filtersData?.get("course") as? String
    val filters =
        com.android.sample.home.EdIntentFilters(course = course?.takeIf { it.isNotBlank() })

    val createdAtTimestamp = data["createdAt"] as? com.google.firebase.Timestamp
    val createdAt = createdAtTimestamp?.seconds?.times(1000)?.toLong() ?: return null

    return com.android.sample.home.EdPostsCard(
        id = data["id"] as? String ?: return null,
        messageId = data["messageId"] as? String ?: messageId,
        query = data["query"] as? String ?: "",
        posts = posts,
        filters = filters,
        stage = stage,
        errorMessage = (data["errorMessage"] as? String)?.takeIf { it.isNotBlank() },
        createdAt = createdAt)
  }
}
