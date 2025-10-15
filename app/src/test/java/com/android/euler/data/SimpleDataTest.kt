package com.android.euler.data

import com.android.sample.data.DbPaths
import org.junit.Assert.*
import org.junit.Test

class SimpleDataTest {

    @Test
    fun `DbPaths should generate correct paths`() {
        val userId = "test-user-123"
        assertEquals("users/$userId", DbPaths.userDoc(userId))
        assertEquals("users/$userId/settings/app", DbPaths.userSettings(userId))
        assertEquals("users/$userId/queries", DbPaths.userQueries(userId))
        assertEquals("users/$userId/logs", DbPaths.userLogs(userId))
    }
}
