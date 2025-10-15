package com.android.euler.data

import com.android.sample.data.DbPaths
import com.android.sample.data.models.UserProfile
import com.android.sample.data.models.UserSettings
import com.android.sample.data.models.QueryLog
import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DataCoverageTest {

    @Test
    fun `DbPaths should generate correct paths`() {
        val uid = "test-uid-123"
        
        assertEquals("users/$uid", DbPaths.userDoc(uid))
        assertEquals("users/$uid/settings/app", DbPaths.userSettings(uid))
        assertEquals("users/$uid/queries", DbPaths.userQueries(uid))
        assertEquals("users/$uid/logs", DbPaths.userLogs(uid))
    }

    @Test
    fun `UserProfile should be created with all fields`() {
        val timestamp = Timestamp.now()
        val profile = UserProfile(
            uid = "test-uid",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = "http://example.com/photo.jpg",
            createdAt = timestamp,
            roles = listOf("user", "admin")
        )
        
        assertEquals("test-uid", profile.uid)
        assertEquals("test@example.com", profile.email)
        assertEquals("Test User", profile.displayName)
        assertEquals("http://example.com/photo.jpg", profile.photoUrl)
        assertEquals(timestamp, profile.createdAt)
        assertEquals(listOf("user", "admin"), profile.roles)
    }

    @Test
    fun `UserSettings should be created with all fields`() {
        val settings = UserSettings(
            language = "fr",
            notificationsEnabled = true,
            theme = "dark"
        )
        
        assertEquals("fr", settings.language)
        assertTrue(settings.notificationsEnabled)
        assertEquals("dark", settings.theme)
    }

    @Test
    fun `QueryLog should be created with all fields`() {
        val timestamp = Timestamp.now()
        val queryLog = QueryLog(
            prompt = "Test query",
            userUid = "test-user",
            createdAt = timestamp,
            tokensIn = 10,
            tokensOut = 20,
            sources = listOf("source1", "source2"),
            resultId = "result-123"
        )
        
        assertEquals("Test query", queryLog.prompt)
        assertEquals("test-user", queryLog.userUid)
        assertEquals(timestamp, queryLog.createdAt)
        assertEquals(10, queryLog.tokensIn)
        assertEquals(20, queryLog.tokensOut)
        assertEquals(listOf("source1", "source2"), queryLog.sources)
        assertEquals("result-123", queryLog.resultId)
    }
}
