package com.android.euler.data

import com.android.sample.data.models
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    @Test
    fun `UserSettings should have correct default values`() {
        val settings = models.UserSettings()
        assertEquals("en", settings.language)
        assertEquals("system", settings.theme)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun `UserSettings should accept custom values`() {
        val settings =
            models.UserSettings(language = "fr", notificationsEnabled = false, theme = "dark")
        assertEquals("fr", settings.language)
        assertFalse(settings.notificationsEnabled)
        assertEquals("dark", settings.theme)
    }

    @Test
    fun `UserProfile should have correct default values`() {
        val profile = models.UserProfile()
        assertEquals("", profile.uid)
        assertNull(profile.email)
        assertNull(profile.displayName)
        assertNull(profile.photoUrl)
        assertNotNull(profile.createdAt)
        assertTrue(profile.roles.isEmpty())
    }

    @Test
    fun `UserProfile should accept custom values`() {
        val timestamp = Timestamp(Date())
        val roles = listOf("admin", "user")
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
    fun `QueryLog should have correct default values`() {
        val queryLog = models.QueryLog()
        assertEquals("", queryLog.prompt)
        assertEquals("", queryLog.userUid)
        assertNotNull(queryLog.createdAt)
        assertEquals(0, queryLog.tokensIn)
        assertEquals(0, queryLog.tokensOut)
        assertTrue(queryLog.sources.isEmpty())
        assertNull(queryLog.resultId)
    }

    @Test
    fun `QueryLog should accept custom values`() {
        val timestamp = Timestamp(Date())
        val sources = listOf("source1", "source2")
        val queryLog =
            models.QueryLog(
                prompt = "What is Euler?",
                userUid = "user-123",
                createdAt = timestamp,
                tokensIn = 10,
                tokensOut = 20,
                sources = sources,
                resultId = "result-456"
            )
        assertEquals("What is Euler?", queryLog.prompt)
        assertEquals("user-123", queryLog.userUid)
        assertEquals(timestamp, queryLog.createdAt)
        assertEquals(10, queryLog.tokensIn)
        assertEquals(20, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertEquals("result-456", queryLog.resultId)
    }
}
