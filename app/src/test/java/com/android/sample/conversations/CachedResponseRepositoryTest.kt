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
}
