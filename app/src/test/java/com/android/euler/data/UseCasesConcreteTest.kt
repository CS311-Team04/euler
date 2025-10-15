package com.android.euler.data

import com.android.sample.data.UserRepository
import com.android.sample.data.ensureProfile
import com.android.sample.data.logRag
import com.android.sample.data.models
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UseCasesConcreteTest {

    private lateinit var mockRepo: UserRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setUp() {
        mockRepo = mockk<UserRepository>(relaxed = true)
        mockAuth = mockk<FirebaseAuth>(relaxed = true)
        mockUser = mockk<FirebaseUser>(relaxed = true)
    }

    @Test
    fun `ensureProfile should create profile when user is signed in`() = runBlocking {
        // Arrange
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-uid-123"
        every { mockUser.email } returns "test@example.com"
        every { mockUser.displayName } returns "Test User"
        every { mockUser.photoUrl } returns null

        coEvery { mockRepo.upsertProfile(any()) } just Runs

        // Act
        ensureProfile(mockRepo, mockAuth)

        // Assert
        coVerify { mockRepo.upsertProfile(any()) }
        coVerify { mockAuth.currentUser }

        // Vérifier les paramètres exacts
        val capturedProfile = slot<models.UserProfile>()
        coVerify { mockRepo.upsertProfile(capture(capturedProfile)) }

        val profile = capturedProfile.captured
        assertEquals("test-uid-123", profile.uid)
        assertEquals("test@example.com", profile.email)
        assertEquals("Test User", profile.displayName)
        assertNull(profile.photoUrl)
    }

    @Test
    fun `ensureProfile should do nothing when user is not signed in`() = runBlocking {
        // Arrange
        every { mockAuth.currentUser } returns null

        // Act
        ensureProfile(mockRepo, mockAuth)

        // Assert
        coVerify(exactly = 0) { mockRepo.upsertProfile(any()) }
        coVerify { mockAuth.currentUser }
    }

    @Test
    fun `ensureProfile should handle user with photo URL`() = runBlocking {
        // Arrange
        val photoUrl = mockk<android.net.Uri>(relaxed = true)
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-uid-456"
        every { mockUser.email } returns "user@example.com"
        every { mockUser.displayName } returns "User With Photo"
        every { mockUser.photoUrl } returns photoUrl
        every { photoUrl.toString() } returns "https://example.com/photo.jpg"

        coEvery { mockRepo.upsertProfile(any()) } just Runs

        // Act
        ensureProfile(mockRepo, mockAuth)

        // Assert
        val capturedProfile = slot<models.UserProfile>()
        coVerify { mockRepo.upsertProfile(capture(capturedProfile)) }

        val profile = capturedProfile.captured
        assertEquals("test-uid-456", profile.uid)
        assertEquals("user@example.com", profile.email)
        assertEquals("User With Photo", profile.displayName)
        assertEquals("https://example.com/photo.jpg", profile.photoUrl)
    }

    @Test
    fun `logRag should log query with all parameters`() = runBlocking {
        // Arrange
        val prompt = "What is Euler's formula?"
        val tokensIn = 10
        val tokensOut = 25
        val sources = listOf("source1", "source2", "source3")
        val resultId = "result-123"

        coEvery { mockRepo.logQuery(any()) } just Runs

        // Act
        logRag(mockRepo, prompt, tokensIn, tokensOut, sources, resultId)

        // Assert
        coVerify { mockRepo.logQuery(any()) }

        // Vérifier les paramètres exacts
        val capturedLog = slot<models.QueryLog>()
        coVerify { mockRepo.logQuery(capture(capturedLog)) }

        val queryLog = capturedLog.captured
        assertEquals(prompt, queryLog.prompt)
        assertEquals(tokensIn, queryLog.tokensIn)
        assertEquals(tokensOut, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertEquals(resultId, queryLog.resultId)
        assertNotNull("userUid should be set", queryLog.userUid)
    }

    @Test
    fun `logRag should handle null resultId`() = runBlocking {
        // Arrange
        val prompt = "Test query"
        val tokensIn = 5
        val tokensOut = 15
        val sources = listOf("single_source")
        val resultId = null

        coEvery { mockRepo.logQuery(any()) } just Runs

        // Act
        logRag(mockRepo, prompt, tokensIn, tokensOut, sources, resultId)

        // Assert
        val capturedLog = slot<models.QueryLog>()
        coVerify { mockRepo.logQuery(capture(capturedLog)) }

        val queryLog = capturedLog.captured
        assertEquals(prompt, queryLog.prompt)
        assertEquals(tokensIn, queryLog.tokensIn)
        assertEquals(tokensOut, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertNull(queryLog.resultId)
    }

    @Test
    fun `logRag should handle empty sources list`() = runBlocking {
        // Arrange
        val prompt = "Query with no sources"
        val tokensIn = 3
        val tokensOut = 8
        val sources = emptyList<String>()
        val resultId = "result-456"

        coEvery { mockRepo.logQuery(any()) } just Runs

        // Act
        logRag(mockRepo, prompt, tokensIn, tokensOut, sources, resultId)

        // Assert
        val capturedLog = slot<models.QueryLog>()
        coVerify { mockRepo.logQuery(capture(capturedLog)) }

        val queryLog = capturedLog.captured
        assertEquals(prompt, queryLog.prompt)
        assertEquals(tokensIn, queryLog.tokensIn)
        assertEquals(tokensOut, queryLog.tokensOut)
        assertTrue("Sources should be empty", queryLog.sources.isEmpty())
        assertEquals(resultId, queryLog.resultId)
    }

    @Test
    fun `logRag should handle zero tokens`() = runBlocking {
        // Arrange
        val prompt = "Zero token query"
        val tokensIn = 0
        val tokensOut = 0
        val sources = listOf("source1")
        val resultId = "result-789"

        coEvery { mockRepo.logQuery(any()) } just Runs

        // Act
        logRag(mockRepo, prompt, tokensIn, tokensOut, sources, resultId)

        // Assert
        val capturedLog = slot<models.QueryLog>()
        coVerify { mockRepo.logQuery(capture(capturedLog)) }

        val queryLog = capturedLog.captured
        assertEquals(prompt, queryLog.prompt)
        assertEquals(0, queryLog.tokensIn)
        assertEquals(0, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertEquals(resultId, queryLog.resultId)
    }

    @Test
    fun `ensureProfile should handle repository errors gracefully`() = runBlocking {
        // Arrange
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-uid"
        every { mockUser.email } returns "test@example.com"
        every { mockUser.displayName } returns "Test User"
        every { mockUser.photoUrl } returns null

        coEvery { mockRepo.upsertProfile(any()) } throws Exception("Database error")

        // Act & Assert
        try {
            ensureProfile(mockRepo, mockAuth)
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals("Database error", e.message)
        }

        coVerify { mockRepo.upsertProfile(any()) }
    }

    @Test
    fun `logRag should handle repository errors gracefully`() = runBlocking {
        // Arrange
        coEvery { mockRepo.logQuery(any()) } throws Exception("Logging error")

        // Act & Assert
        try {
            logRag(mockRepo, "test prompt", 1, 1, listOf("source"), "result")
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals("Logging error", e.message)
        }

        coVerify { mockRepo.logQuery(any()) }
    }

    @Test
    fun `ensureProfile should handle multiple calls`() = runBlocking {
        // Arrange
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "test-uid"
        every { mockUser.email } returns "test@example.com"
        every { mockUser.displayName } returns "Test User"
        every { mockUser.photoUrl } returns null

        coEvery { mockRepo.upsertProfile(any()) } just Runs

        // Act
        ensureProfile(mockRepo, mockAuth)
        ensureProfile(mockRepo, mockAuth)
        ensureProfile(mockRepo, mockAuth)

        // Assert
        coVerify(exactly = 3) { mockRepo.upsertProfile(any()) }
    }

    @Test
    fun `logRag should handle multiple calls with different data`() = runBlocking {
        // Arrange
        coEvery { mockRepo.logQuery(any()) } just Runs

        // Act
        logRag(mockRepo, "Query 1", 5, 10, listOf("source1"), "result1")
        logRag(mockRepo, "Query 2", 8, 15, listOf("source2"), "result2")
        logRag(mockRepo, "Query 3", 12, 20, listOf("source3"), null)

        // Assert
        coVerify(exactly = 3) { mockRepo.logQuery(any()) }
    }
}
