package com.android.euler.data

import com.android.sample.data.models.QueryLog
import com.android.sample.data.models.UserProfile
import com.android.sample.data.models.UserSettings
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DataModelsExtendedTest {

    @Test
    fun `UserProfile constructor with all parameters`() {
        val timestamp = Timestamp.now()
        val roles = listOf("admin", "user", "moderator")

        val profile =
            UserProfile(
                uid = "test-uid-123",
                email = "test@example.com",
                displayName = "Test User Name",
                photoUrl = "https://example.com/photo.jpg",
                createdAt = timestamp,
                roles = roles
            )

        assertEquals("test-uid-123", profile.uid)
        assertEquals("test@example.com", profile.email)
        assertEquals("Test User Name", profile.displayName)
        assertEquals("https://example.com/photo.jpg", profile.photoUrl)
        assertEquals(timestamp, profile.createdAt)
        assertEquals(roles, profile.roles)
        assertEquals(3, profile.roles.size)
        assertTrue(profile.roles.contains("admin"))
    }

    @Test
    fun `UserProfile with empty roles list`() {
        val timestamp = Timestamp.now()

        val profile =
            UserProfile(
                uid = "empty-roles-uid",
                email = "empty@example.com",
                displayName = "Empty Roles User",
                photoUrl = "",
                createdAt = timestamp,
                roles = emptyList()
            )

        assertTrue("Roles should be empty", profile.roles.isEmpty())
        assertEquals("", profile.photoUrl)
    }

    @Test
    fun `UserSettings with all boolean combinations`() {
        // Test avec notifications activées
        val settingsEnabled =
            UserSettings(language = "en", notificationsEnabled = true, theme = "light")
        assertTrue("Notifications should be enabled", settingsEnabled.notificationsEnabled)
        assertEquals("en", settingsEnabled.language)
        assertEquals("light", settingsEnabled.theme)

        // Test avec notifications désactivées
        val settingsDisabled =
            UserSettings(language = "fr", notificationsEnabled = false, theme = "dark")
        assertFalse("Notifications should be disabled", settingsDisabled.notificationsEnabled)
        assertEquals("fr", settingsDisabled.language)
        assertEquals("dark", settingsDisabled.theme)
    }

    @Test
    fun `QueryLog with all fields populated`() {
        val timestamp = Timestamp.now()
        val sources = listOf("source1.pdf", "source2.txt", "source3.docx")

        val queryLog =
            QueryLog(
                prompt = "What is the meaning of life?",
                userUid = "user-123",
                createdAt = timestamp,
                tokensIn = 150,
                tokensOut = 300,
                sources = sources,
                resultId = "result-456"
            )

        assertEquals("What is the meaning of life?", queryLog.prompt)
        assertEquals("user-123", queryLog.userUid)
        assertEquals(timestamp, queryLog.createdAt)
        assertEquals(150, queryLog.tokensIn)
        assertEquals(300, queryLog.tokensOut)
        assertEquals(sources, queryLog.sources)
        assertEquals("result-456", queryLog.resultId)
        assertEquals(3, queryLog.sources.size)
    }

    @Test
    fun `QueryLog with zero tokens`() {
        val timestamp = Timestamp.now()

        val queryLog =
            QueryLog(
                prompt = "Empty query",
                userUid = "user-zero",
                createdAt = timestamp,
                tokensIn = 0,
                tokensOut = 0,
                sources = emptyList(),
                resultId = "empty-result"
            )

        assertEquals(0, queryLog.tokensIn)
        assertEquals(0, queryLog.tokensOut)
        assertTrue("Sources should be empty", queryLog.sources.isEmpty())
    }

    @Test
    fun `QueryLog with large token counts`() {
        val timestamp = Timestamp.now()

        val queryLog =
            QueryLog(
                prompt = "Large query with many tokens",
                userUid = "user-large",
                createdAt = timestamp,
                tokensIn = 10000,
                tokensOut = 50000,
                sources = listOf("large-document.pdf"),
                resultId = "large-result"
            )

        assertEquals(10000, queryLog.tokensIn)
        assertEquals(50000, queryLog.tokensOut)
        assertEquals(1, queryLog.sources.size)
    }
}
