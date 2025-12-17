package com.android.sample.conversations

import com.android.sample.home.EdIntentFilters
import com.android.sample.home.EdPost
import com.android.sample.home.EdPostCard
import com.android.sample.home.EdPostStatus
import com.android.sample.home.EdPostsCard
import com.android.sample.home.EdPostsStage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRepositoryTest {

  private val auth: FirebaseAuth = mock()
  private val firestore: FirebaseFirestore = mock()
  private val user: FirebaseUser = mock()

  private val usersCollection: CollectionReference = mock()
  private val userDocument: DocumentReference = mock()
  private val conversationsCollection: CollectionReference = mock()
  private val conversationDocument: DocumentReference = mock()
  private val messagesCollection: CollectionReference = mock()

  private lateinit var repository: ConversationRepository

  @Before
  fun setUp() {
    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("user-123")

    whenever(firestore.collection("users")).thenReturn(usersCollection)
    whenever(usersCollection.document("user-123")).thenReturn(userDocument)
    whenever(userDocument.collection("conversations")).thenReturn(conversationsCollection)
    whenever(conversationsCollection.document()).thenReturn(conversationDocument)
    whenever(conversationDocument.id).thenReturn("conversation-1")

    repository = ConversationRepository(auth, firestore)
  }

  @Test
  fun startNewConversation_sets_expected_payload_and_returns_id() = runTest {
    val payloadCaptor = argumentCaptor<Map<String, Any?>>()
    whenever(conversationDocument.set(payloadCaptor.capture())).thenReturn(Tasks.forResult(null))

    val id = repository.startNewConversation("Hello world")

    assertEquals("conversation-1", id)
    val payload = payloadCaptor.firstValue
    assertEquals("Hello world", payload["title"])
    assertEquals("", payload["lastMessagePreview"])
    assertEquals("user-123", payload["ownerUid"])
    assertEquals(false, payload["archived"])
    assertNotNull(payload["createdAt"])
    assertNotNull(payload["updatedAt"])
  }

  @Test
  fun startNewConversation_throws_when_user_missing() = runTest {
    whenever(auth.currentUser).thenReturn(null)
    val failure = runCatching { repository.startNewConversation() }.exceptionOrNull()
    assertTrue(failure is AuthNotReadyException)
  }

  @Test
  fun conversationsFlow_emits_documents_and_cleans_up_listener() = runTest {
    val query: Query = mock()
    val registration: ListenerRegistration = mock()
    whenever(conversationsCollection.orderBy("updatedAt", Query.Direction.DESCENDING))
        .thenReturn(query)
    val snapshot: QuerySnapshot = mock()
    val doc: QueryDocumentSnapshot = mock()
    whenever(snapshot.documents).thenReturn(listOf(doc))
    whenever(doc.id).thenReturn("conv-42")
    whenever(doc.toObject(Conversation::class.java))
        .thenReturn(Conversation(title = "Latest", ownerUid = "user-123"))

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    whenever(query.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration)

    val deferred = async { repository.conversationsFlow().first() }
    advanceUntilIdle()

    listenerCaptor.firstValue.onEvent(snapshot, null)
    advanceUntilIdle()

    val conversations = deferred.await()
    assertEquals(1, conversations.size)
    assertEquals("conv-42", conversations.first().id)
    assertEquals("Latest", conversations.first().title)

    verify(registration).remove()
  }

  @Test
  fun messagesFlow_emits_messages() = runTest {
    val query: Query = mock()
    val registration: ListenerRegistration = mock()
    whenever(conversationsCollection.document("abc")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.orderBy("createdAt", Query.Direction.ASCENDING)).thenReturn(query)

    val snapshot: QuerySnapshot = mock()
    val doc: QueryDocumentSnapshot = mock()
    whenever(snapshot.documents).thenReturn(listOf(doc))
    whenever(doc.toObject(MessageDTO::class.java))
        .thenReturn(MessageDTO(role = "user", text = "Hi"))

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    whenever(query.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration)

    val deferred = async { repository.messagesFlow("abc").first() }
    advanceUntilIdle()

    listenerCaptor.firstValue.onEvent(snapshot, null)
    advanceUntilIdle()

    val messages = deferred.await()
    assertEquals(1, messages.size)
    assertEquals("Hi", messages.first().first.text)

    verify(registration).remove()
  }

  @Test
  fun appendMessage_uses_batch_to_write_message_and_update_parent() = runTest {
    val msgDocument: DocumentReference = mock()
    val batch: WriteBatch = mock()
    whenever(conversationsCollection.document("conv-x")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document()).thenReturn(msgDocument)
    whenever(msgDocument.id).thenReturn("msg-id")

    doAnswer { invocation ->
          val lambda = invocation.arguments[0]
          val method =
              lambda::class.java.methods.firstOrNull { m ->
                (m.name == "apply" || m.name == "invoke") &&
                    m.parameterTypes.size == 1 &&
                    WriteBatch::class.java.isAssignableFrom(m.parameterTypes[0])
              } ?: error("No suitable method found on runBatch lambda")
          method.isAccessible = true
          method.invoke(lambda, batch)
          Tasks.forResult<Void>(null)
        }
        .whenever(firestore)
        .runBatch(any())

    whenever(batch.set(any<DocumentReference>(), any<Map<String, Any>>())).thenReturn(batch)
    whenever(batch.update(any<DocumentReference>(), any<Map<String, Any>>())).thenReturn(batch)

    repository.appendMessage("conv-x", role = "user", text = "Hello from tests")

    verify(batch).set(any<DocumentReference>(), any<Map<String, Any>>())
    verify(batch).update(any<DocumentReference>(), any<Map<String, Any>>())
  }

  @Test
  fun appendMessage_with_sourceMetadata_saves_source_fields() = runTest {
    val msgDocument: DocumentReference = mock()
    val batch: WriteBatch = mock()
    whenever(conversationsCollection.document("conv-x")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document()).thenReturn(msgDocument)
    whenever(msgDocument.id).thenReturn("msg-id")

    doAnswer { invocation ->
          val lambda = invocation.arguments[0]
          val method =
              lambda::class.java.methods.firstOrNull { m ->
                (m.name == "apply" || m.name == "invoke") &&
                    m.parameterTypes.size == 1 &&
                    WriteBatch::class.java.isAssignableFrom(m.parameterTypes[0])
              } ?: error("No suitable method found on runBatch lambda")
          method.isAccessible = true
          method.invoke(lambda, batch)
          Tasks.forResult<Void>(null)
        }
        .whenever(firestore)
        .runBatch(any())

    val messageDataCaptor = argumentCaptor<Map<String, Any>>()
    whenever(batch.set(any<DocumentReference>(), messageDataCaptor.capture())).thenReturn(batch)
    whenever(batch.update(any<DocumentReference>(), any<Map<String, Any>>())).thenReturn(batch)

    val sourceMetadata =
        MessageSourceMetadata(url = "https://www.epfl.ch/page", compactType = "NONE")
    repository.appendMessage(
        "conv-x", role = "assistant", text = "RAG answer", sourceMetadata = sourceMetadata)

    val messageData = messageDataCaptor.firstValue
    assertEquals("assistant", messageData["role"])
    assertEquals("RAG answer", messageData["text"])
    assertEquals("https://www.epfl.ch/page", messageData["sourceUrl"])
    assertEquals("NONE", messageData["sourceCompactType"])
  }

  @Test
  fun appendMessage_without_sourceMetadata_does_not_save_source_fields() = runTest {
    val msgDocument: DocumentReference = mock()
    val batch: WriteBatch = mock()
    whenever(conversationsCollection.document("conv-x")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document()).thenReturn(msgDocument)
    whenever(msgDocument.id).thenReturn("msg-id")

    doAnswer { invocation ->
          val lambda = invocation.arguments[0]
          val method =
              lambda::class.java.methods.firstOrNull { m ->
                (m.name == "apply" || m.name == "invoke") &&
                    m.parameterTypes.size == 1 &&
                    WriteBatch::class.java.isAssignableFrom(m.parameterTypes[0])
              } ?: error("No suitable method found on runBatch lambda")
          method.isAccessible = true
          method.invoke(lambda, batch)
          Tasks.forResult<Void>(null)
        }
        .whenever(firestore)
        .runBatch(any())

    val messageDataCaptor = argumentCaptor<Map<String, Any>>()
    whenever(batch.set(any<DocumentReference>(), messageDataCaptor.capture())).thenReturn(batch)
    whenever(batch.update(any<DocumentReference>(), any<Map<String, Any>>())).thenReturn(batch)

    repository.appendMessage("conv-x", role = "user", text = "User message")

    val messageData = messageDataCaptor.firstValue
    assertEquals("user", messageData["role"])
    assertEquals("User message", messageData["text"])
    assertTrue(!messageData.containsKey("sourceUrl"))
    assertTrue(!messageData.containsKey("sourceCompactType"))
  }

  @Test
  fun appendMessage_with_sourceMetadata_compactType_schedule() = runTest {
    val msgDocument: DocumentReference = mock()
    val batch: WriteBatch = mock()
    whenever(conversationsCollection.document("conv-x")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document()).thenReturn(msgDocument)
    whenever(msgDocument.id).thenReturn("msg-id")

    doAnswer { invocation ->
          val lambda = invocation.arguments[0]
          val method =
              lambda::class.java.methods.firstOrNull { m ->
                (m.name == "apply" || m.name == "invoke") &&
                    m.parameterTypes.size == 1 &&
                    WriteBatch::class.java.isAssignableFrom(m.parameterTypes[0])
              } ?: error("No suitable method found on runBatch lambda")
          method.isAccessible = true
          method.invoke(lambda, batch)
          Tasks.forResult<Void>(null)
        }
        .whenever(firestore)
        .runBatch(any())

    val messageDataCaptor = argumentCaptor<Map<String, Any>>()
    whenever(batch.set(any<DocumentReference>(), messageDataCaptor.capture())).thenReturn(batch)
    whenever(batch.update(any<DocumentReference>(), any<Map<String, Any>>())).thenReturn(batch)

    val sourceMetadata =
        MessageSourceMetadata(url = "https://epfl.ch/schedule", compactType = "SCHEDULE")
    repository.appendMessage(
        "conv-x", role = "assistant", text = "Schedule info", sourceMetadata = sourceMetadata)

    val messageData = messageDataCaptor.firstValue
    assertEquals("SCHEDULE", messageData["sourceCompactType"])
  }

  @Test
  fun deleteConversation_deletes_messages_and_conversation_doc() = runTest {
    val msgSnapshot: QuerySnapshot = mock()
    val msgDoc: QueryDocumentSnapshot = mock()
    val batch: WriteBatch = mock()
    val limitedQuery: Query = mock()

    whenever(conversationsCollection.document("conv-delete")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.limit(500)).thenReturn(limitedQuery)
    whenever(limitedQuery.get()).thenReturn(Tasks.forResult(msgSnapshot))
    whenever(msgSnapshot.documents).thenReturn(listOf(msgDoc))
    whenever(msgSnapshot.iterator()).thenReturn(mutableListOf(msgDoc).iterator())
    val msgDocRef: DocumentReference = mock()
    whenever(msgDoc.reference).thenReturn(msgDocRef)

    whenever(firestore.batch()).thenReturn(batch)
    whenever(batch.delete(any())).thenReturn(batch)
    whenever(batch.commit()).thenReturn(Tasks.forResult(null))

    repository.deleteConversation("conv-delete")

    verify(batch).delete(msgDocRef)
    verify(batch).delete(conversationDocument)
    verify(batch).commit()
  }

  @Test
  fun updateConversationTitle_updates_fields() = runTest {
    whenever(conversationsCollection.document("conv-title")).thenReturn(conversationDocument)
    val mapCaptor = argumentCaptor<Map<String, Any>>()
    whenever(conversationDocument.update(mapCaptor.capture())).thenReturn(Tasks.forResult(null))

    repository.updateConversationTitle("conv-title", "Renamed")

    val payload = mapCaptor.firstValue
    assertEquals("Renamed", payload["title"])
    assertTrue(payload.containsKey("updatedAt"))
  }

  // ==================== EDCARD TESTS ====================

  @Test
  fun saveEdCard_saves_card_and_updates_message() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-card-1")).thenReturn(edCardDocument)

    val edCardDataCaptor = argumentCaptor<Map<String, Any>>()
    whenever(edCardDocument.set(edCardDataCaptor.capture())).thenReturn(Tasks.forResult(null))

    val edCardIdCaptor = argumentCaptor<String>()
    whenever(messageDocument.update(eq("edCardId"), edCardIdCaptor.capture()))
        .thenReturn(Tasks.forResult(null))

    val edCard =
        EdPostCard(
            id = "ed-card-1",
            title = "Test Post",
            body = "Test Body",
            status = EdPostStatus.Published,
            createdAt = 1000000L)

    repository.saveEdCard("conv-1", "msg-1", edCard)

    // Verify edCard data was saved
    val edCardData = edCardDataCaptor.firstValue
    assertEquals("ed-card-1", edCardData["id"])
    assertEquals("Test Post", edCardData["title"])
    assertEquals("Test Body", edCardData["body"])
    assertEquals("Published", edCardData["status"])
    assertNotNull(edCardData["createdAt"])

    // Verify message was updated with edCardId
    assertEquals("ed-card-1", edCardIdCaptor.firstValue)
  }

  @Test
  fun loadEdCard_returns_card_when_found() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-card-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data)
        .thenReturn(
            mapOf(
                "id" to "ed-card-1",
                "title" to "Loaded Post",
                "body" to "Loaded Body",
                "status" to "Published",
                "createdAt" to Timestamp(1000, 0)))

    val result = repository.loadEdCard("conv-1", "msg-1", "ed-card-1")

    assertNotNull(result)
    assertEquals("ed-card-1", result?.id)
    assertEquals("Loaded Post", result?.title)
    assertEquals("Loaded Body", result?.body)
    assertEquals(EdPostStatus.Published, result?.status)
    assertEquals(1000000L, result?.createdAt)
  }

  @Test
  fun loadEdCard_returns_null_when_not_found() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-card-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(false)

    val result = repository.loadEdCard("conv-1", "msg-1", "ed-card-1")

    assertNull(result)
  }

  @Test
  fun loadEdCard_returns_null_when_invalid_status() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-card-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data)
        .thenReturn(
            mapOf(
                "id" to "ed-card-1",
                "title" to "Test",
                "body" to "Test",
                "status" to "InvalidStatus",
                "createdAt" to Timestamp(1000, 0)))

    val result = repository.loadEdCard("conv-1", "msg-1", "ed-card-1")

    assertNull(result)
  }

  @Test
  fun updateMessageWithEdCard_updates_message_with_edCardId() = runTest {
    val messageDocument: DocumentReference = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)

    val edCardIdCaptor = argumentCaptor<String>()
    whenever(messageDocument.update(eq("edCardId"), edCardIdCaptor.capture()))
        .thenReturn(Tasks.forResult(null))

    repository.updateMessageWithEdCard("conv-1", "msg-1", "ed-card-123")

    assertEquals("ed-card-123", edCardIdCaptor.firstValue)
    verify(messageDocument).update(eq("edCardId"), eq("ed-card-123"))
  }

  // ==================== EDPOSTSCARD TESTS ====================

  @Test
  fun saveEdPostsCard_saves_card_with_posts_type() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-posts-1")).thenReturn(edCardDocument)

    val edCardDataCaptor = argumentCaptor<Map<String, Any>>()
    whenever(edCardDocument.set(edCardDataCaptor.capture())).thenReturn(Tasks.forResult(null))

    whenever(messageDocument.update(eq("edCardId"), any())).thenReturn(Tasks.forResult(null))

    val edPostsCard =
        EdPostsCard(
            id = "ed-posts-1",
            messageId = "msg-1",
            query = "test query",
            posts =
                listOf(
                    EdPost(
                        title = "Post 1",
                        content = "Content 1",
                        date = 123456L,
                        author = "Author 1",
                        url = "https://example.com/1")),
            filters = EdIntentFilters(course = "CS-101"),
            stage = EdPostsStage.SUCCESS,
            errorMessage = null,
            createdAt = 1000000L)

    repository.saveEdPostsCard("conv-1", "msg-1", edPostsCard)

    // Verify edPostsCard data was saved with correct type
    val edCardData = edCardDataCaptor.firstValue
    assertEquals("ed-posts-1", edCardData["id"])
    assertEquals("posts", edCardData["type"]) // Should have type = "posts"
    assertEquals("msg-1", edCardData["messageId"])
    assertEquals("test query", edCardData["query"])
    assertEquals("SUCCESS", edCardData["stage"])

    // Verify posts were saved
    @Suppress("UNCHECKED_CAST") val postsData = edCardData["posts"] as List<Map<String, Any>>
    assertEquals(1, postsData.size)
    assertEquals("Post 1", postsData[0]["title"])
    assertEquals("Content 1", postsData[0]["content"])

    // Verify filters were saved
    @Suppress("UNCHECKED_CAST") val filtersData = edCardData["filters"] as Map<String, String>
    assertEquals("CS-101", filtersData["course"])
  }

  @Test
  fun loadEdPostsCard_returns_card_when_type_is_posts() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-posts-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data)
        .thenReturn(
            mapOf(
                "id" to "ed-posts-1",
                "type" to "posts",
                "messageId" to "msg-1",
                "query" to "search query",
                "posts" to
                    listOf(
                        mapOf(
                            "title" to "Post Title",
                            "content" to "Post Content",
                            "date" to 123456L,
                            "author" to "Author",
                            "url" to "https://example.com")),
                "filters" to mapOf("course" to "CS-101"),
                "stage" to "SUCCESS",
                "errorMessage" to "",
                "createdAt" to Timestamp(1000, 0)))

    val result = repository.loadEdPostsCard("conv-1", "msg-1", "ed-posts-1")

    assertNotNull(result)
    assertEquals("ed-posts-1", result?.id)
    assertEquals("msg-1", result?.messageId)
    assertEquals("search query", result?.query)
    assertEquals(EdPostsStage.SUCCESS, result?.stage)
    assertEquals(1, result?.posts?.size)
    assertEquals("Post Title", result?.posts?.get(0)?.title)
    assertEquals("CS-101", result?.filters?.course)
  }

  @Test
  fun loadEdPostsCard_returns_null_when_type_is_not_posts() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-card-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data)
        .thenReturn(
            mapOf(
                "id" to "ed-card-1",
                "type" to "post", // Wrong type - should be "posts"
                "messageId" to "msg-1",
                "query" to "test",
                "posts" to emptyList<Map<String, Any>>(),
                "filters" to emptyMap<String, String>(),
                "stage" to "SUCCESS",
                "createdAt" to Timestamp(1000, 0)))

    val result = repository.loadEdPostsCard("conv-1", "msg-1", "ed-card-1")

    assertNull(result) // Should return null because type != "posts"
  }

  @Test
  fun loadEdPostsCard_returns_null_when_not_found() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-posts-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(false)

    val result = repository.loadEdPostsCard("conv-1", "msg-1", "ed-posts-1")

    assertNull(result)
  }

  @Test
  fun loadEdPostsCard_returns_null_when_invalid_stage() = runTest {
    val edCardsCollection: CollectionReference = mock()
    val edCardDocument: DocumentReference = mock()
    val messageDocument: DocumentReference = mock()
    val snapshot: DocumentSnapshot = mock()

    whenever(conversationsCollection.document("conv-1")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document("msg-1")).thenReturn(messageDocument)
    whenever(messageDocument.collection("edCards")).thenReturn(edCardsCollection)
    whenever(edCardsCollection.document("ed-posts-1")).thenReturn(edCardDocument)
    whenever(edCardDocument.get()).thenReturn(Tasks.forResult(snapshot))

    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data)
        .thenReturn(
            mapOf(
                "id" to "ed-posts-1",
                "type" to "posts",
                "messageId" to "msg-1",
                "query" to "test",
                "posts" to emptyList<Map<String, Any>>(),
                "filters" to emptyMap<String, String>(),
                "stage" to "InvalidStage", // Invalid stage value
                "createdAt" to Timestamp(1000, 0)))

    val result = repository.loadEdPostsCard("conv-1", "msg-1", "ed-posts-1")

    assertNull(result)
  }
}
