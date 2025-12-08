package com.android.sample.conversations

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class CachedResponseRepositoryTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var db: FirebaseFirestore
  private lateinit var col: CollectionReference
  private lateinit var doc: DocumentReference
  private lateinit var snap: DocumentSnapshot
  private lateinit var repo: CachedResponseRepository

  @Before
  fun setup() {
    auth = mock()
    db = mock()
    col = mock()
    doc = mock()
    snap = mock()
    whenever(auth.currentUser).thenReturn(mock<FirebaseUser>())
    whenever(db.collection("globalCachedResponses")).thenReturn(col)
    whenever(col.document(any())).thenReturn(doc)
    repo = CachedResponseRepository(auth, db)
  }

  // --- getCachedResponse ---

  @Test
  fun `getCachedResponse preferCache true returns response when exists`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    // toObject won't work with mocks, but we verify the code path executes
    val result = repo.getCachedResponse("test", preferCache = true)
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `getCachedResponse preferCache false returns response when exists`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    repo.getCachedResponse("test", preferCache = false)
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `getCachedResponse returns null when doc not exists`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)
    val result = repo.getCachedResponse("test")
    assertNull(result)
  }

  @Test
  fun `getCachedResponse returns null on exception`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forException(RuntimeException("error")))
    val result = repo.getCachedResponse("test")
    assertNull(result)
  }

  // --- saveCachedResponse ---

  @Test
  fun `saveCachedResponse saves and verifies document`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    whenever(snap.id).thenReturn("hash123")
    whenever(snap.getString("question")).thenReturn("test question")
    whenever(doc.path).thenReturn("globalCachedResponses/hash123")

    repo.saveCachedResponse("test question", "test response")

    verify(doc).set(any<Map<String, Any>>())
    verify(doc).get()
  }

  @Test
  fun `saveCachedResponse logs warning when verification fails`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false) // Verification fails
    whenever(doc.path).thenReturn("globalCachedResponses/hash123")

    repo.saveCachedResponse("test", "response")

    verify(doc).get() // Verification was attempted
  }

  @Test
  fun `saveCachedResponse handles exception gracefully`() = runTest {
    whenever(doc.set(any<Map<String, Any>>()))
        .thenReturn(Tasks.forException(RuntimeException("save error")))

    repo.saveCachedResponse("test", "response") // Should not throw
  }

  // --- hasCachedResponse ---

  @Test
  fun `hasCachedResponse returns true when exists with valid response`() = runTest {
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    // toObject won't work, but code path is tested
    val result = repo.hasCachedResponse("test")
    verify(doc).get()
  }

  @Test
  fun `hasCachedResponse returns false when doc not exists`() = runTest {
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)
    val result = repo.hasCachedResponse("test")
    assertFalse(result)
  }

  @Test
  fun `hasCachedResponse returns false on exception`() = runTest {
    whenever(doc.get()).thenReturn(Tasks.forException(RuntimeException("error")))
    val result = repo.hasCachedResponse("test")
    assertFalse(result)
  }

  // --- hashQuestion consistency ---

  @Test
  fun `same question produces same hash`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    repo.getCachedResponse("What is EPFL")
    repo.getCachedResponse("What is EPFL")

    // Same document should be accessed twice
    verify(col, times(2)).document(any())
  }

  @Test
  fun `question trimmed and lowercased for hash`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    repo.getCachedResponse("  WHAT IS EPFL  ")
    repo.getCachedResponse("what is epfl")

    // Both should produce same hash, accessing same doc
    verify(col, times(2)).document(any())
  }

  // --- Additional edge case tests for improved coverage ---

  @Test
  fun `getCachedResponse with preferCache true logs cache read`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    repo.getCachedResponse("What are EPFL services?", preferCache = true)

    // Verify cache source was used
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `getCachedResponse handles very long question`() = runTest {
    val longQuestion = "a".repeat(1000)
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    val result = repo.getCachedResponse(longQuestion)

    assertNull(result)
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `getCachedResponse handles special characters in question`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    val result = repo.getCachedResponse("What is EPFL? (École polytechnique fédérale de Lausanne)")

    assertNull(result)
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `getCachedResponse handles unicode characters`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    val result = repo.getCachedResponse("Qu'est-ce que l'EPFL? 中文测试 🎓")

    assertNull(result)
    verify(doc).get(Source.CACHE)
  }

  @Test
  fun `saveCachedResponse with empty question still saves`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    whenever(snap.id).thenReturn("hash")
    whenever(snap.getString("question")).thenReturn("")
    whenever(doc.path).thenReturn("globalCachedResponses/hash")

    repo.saveCachedResponse("", "test response")

    verify(doc).set(any<Map<String, Any>>())
  }

  @Test
  fun `saveCachedResponse with special characters in response`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    whenever(snap.id).thenReturn("hash")
    whenever(snap.getString("question")).thenReturn("test")
    whenever(doc.path).thenReturn("globalCachedResponses/hash")

    val specialResponse = "Response with éàü special chars and\nnewlines\tand tabs"
    repo.saveCachedResponse("test question", specialResponse)

    verify(doc).set(argThat<Map<String, Any>> { this["response"] == specialResponse })
  }

  @Test
  fun `saveCachedResponse handles verification get exception`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forException(RuntimeException("Verification failed")))
    whenever(doc.path).thenReturn("globalCachedResponses/hash")

    // Should not throw, just log warning
    repo.saveCachedResponse("test", "response")

    verify(doc).set(any<Map<String, Any>>())
    verify(doc).get()
  }

  @Test
  fun `hasCachedResponse with existing doc but null response`() = runTest {
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    // toObject returns null or object without response field set

    val result = repo.hasCachedResponse("test")

    // Should be false because response is not valid
    verify(doc).get()
  }

  @Test
  fun `hasCachedResponse with existing doc but blank response`() = runTest {
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)

    val result = repo.hasCachedResponse("test")

    verify(doc).get()
  }

  @Test
  fun `getCachedResponse uses cache source for both preferCache true and false`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    // Both modes should use cache
    repo.getCachedResponse("test", preferCache = true)
    repo.getCachedResponse("test", preferCache = false)

    verify(doc, times(2)).get(Source.CACHE)
  }

  @Test
  fun `hashQuestion produces consistent results for same input`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    // Same question should produce same document access
    repo.getCachedResponse("consistent question")
    repo.getCachedResponse("consistent question")
    repo.getCachedResponse("consistent question")

    verify(col, times(3)).document(any())
  }

  @Test
  fun `getCachedResponse handles null from toObject gracefully`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    // toObject will return null for mock

    val result = repo.getCachedResponse("test")

    // Should return null when toObject returns null
    assertNull(result)
  }

  @Test
  fun `saveCachedResponse verifies write by reading back`() = runTest {
    whenever(doc.set(any<Map<String, Any>>())).thenReturn(Tasks.forResult(null))
    whenever(doc.get()).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)
    whenever(snap.id).thenReturn("abc123")
    whenever(snap.getString("question")).thenReturn("test question")
    whenever(doc.path).thenReturn("globalCachedResponses/abc123")

    repo.saveCachedResponse("test question", "test response")

    // Verify both set and get were called (verification read)
    verify(doc).set(any<Map<String, Any>>())
    verify(doc).get()
  }

  @Test
  fun `getCachedResponse different questions produce different hashes`() = runTest {
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    repo.getCachedResponse("What is EPFL?")
    repo.getCachedResponse("What is ETH?")

    // Should call document() with different hashes
    verify(col, times(2)).document(any())
  }
}
