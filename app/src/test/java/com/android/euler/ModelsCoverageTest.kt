package com.android.euler

import com.android.sample.data.models
import com.google.firebase.Timestamp
import java.util.*
import org.junit.Assert.*
import org.junit.Test

class ModelsCoverageTest {

    @Test
    fun `UserProfile should create with all parameters`() {
        val timestamp = Timestamp.now()
        val roles = listOf("user", "admin")

        val profile =
            models.UserProfile(
                uid = "test-uid",
                email = "test@example.com",
                displayName = "Test User",
                photoUrl = "https://example.com/photo.jpg",
                createdAt = timestamp,
                roles = roles
            )

        assertEquals("test-uid", profile.uid)
        assertEquals("test@example.com", profile.email)
        assertEquals("Test User", profile.displayName)
        assertEquals("https://example.com/photo.jpg", profile.photoUrl)
        assertEquals(timestamp, profile.createdAt)
        assertEquals(roles, profile.roles)
    }

    @Test
    fun `UserProfile should create with default parameters`() {
        val profile = models.UserProfile()

        assertEquals("", profile.uid)
        assertNull(profile.email)
        assertNull(profile.displayName)
        assertNull(profile.photoUrl)
        assertNotNull(profile.createdAt)
        assertTrue(profile.roles.isEmpty())
    }

    @Test
    fun `UserSettings should create with all parameters`() {
        val settings =
            models.UserSettings(language = "fr", notificationsEnabled = false, theme = "dark")

        assertEquals("fr", settings.language)
        assertFalse(settings.notificationsEnabled)
        assertEquals("dark", settings.theme)
    }

    @Test
    fun `UserSettings should create with default parameters`() {
        val settings = models.UserSettings()

        assertEquals("en", settings.language)
        assertTrue(settings.notificationsEnabled)
        assertEquals("system", settings.theme)
    }

    @Test
    fun `QueryLog should create with all parameters`() {
        val timestamp = Timestamp.now()
        val sources = listOf("source1", "source2")

        val queryLog =
            models.QueryLog(
                prompt = "Test prompt",
                userUid = "test-uid",
                createdAt = timestamp,
                tokensIn = 10,
                tokensOut = 20,
                sources = sources,
                resultId = "result-123"
            )

        assertEquals("Test prompt", queryLog.prompt)
        assertEquals("test-uid", queryLog.userUid)
        assertEquals(timestamp, queryLog.createdAt)
        assertEquals(10, queryLog.tokensIn)
        assertEquals(20, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertEquals("result-123", queryLog.resultId)
    }

    @Test
    fun `QueryLog should create with default parameters`() {
        val queryLog = models.QueryLog()

        assertEquals("", queryLog.prompt)
        assertEquals("", queryLog.userUid)
        assertNotNull(queryLog.createdAt)
        assertEquals(0, queryLog.tokensIn)
        assertEquals(0, queryLog.tokensOut)
        assertTrue(queryLog.sources.isEmpty())
        assertNull(queryLog.resultId)
    }
}
