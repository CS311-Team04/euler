package com.android.sample.conversations

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
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
    assertEquals("Hi", messages.first().text)

    verify(registration).remove()
  }

  @Test
  fun appendMessage_uses_batch_to_write_message_and_update_parent() = runTest {
    val msgDocument: DocumentReference = mock()
    val batch: WriteBatch = mock()
    whenever(conversationsCollection.document("conv-x")).thenReturn(conversationDocument)
    whenever(conversationDocument.collection("messages")).thenReturn(messagesCollection)
    whenever(messagesCollection.document()).thenReturn(msgDocument)

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
}
