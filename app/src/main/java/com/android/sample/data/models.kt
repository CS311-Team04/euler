package com.android.sample.data

import com.google.firebase.Timestamp

class models {
    data class UserProfile(
        val uid: String = "",
        val email: String? = null,
        val displayName: String? = null,
        val photoUrl: String? = null,
        val createdAt: Timestamp = Timestamp.now(),
        val roles: List<String> = emptyList(),
    )

    data class UserSettings(
        val language: String = "en",
        val notificationsEnabled: Boolean = true,
        val theme: String = "system",
    )

    data class QueryLog(
        val prompt: String = "",
        val userUid: String = "",
        val createdAt: Timestamp = Timestamp.now(),
        val tokensIn: Int = 0,
        val tokensOut: Int = 0,
        val sources: List<String> = emptyList(),
        val resultId: String? = null,
    )
}
