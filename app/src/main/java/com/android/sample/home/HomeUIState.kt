package com.android.sample.home

import com.android.sample.Chat.ChatUIModel

/**
 * Immutable snapshot of everything the HomeScreen needs to render at a given time. Any UI change
 * produces a new instance via copy(...).
 */
data class HomeUiState(
    val userName: String = "Student",
    val systems: List<SystemItem> = emptyList(),
    val messages: List<ChatUIModel> = emptyList(),
    val messageDraft: String = "",
    val isDrawerOpen: Boolean = false,
    val isTopRightOpen: Boolean = false,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isSending: Boolean = false
)

/** Represents an EPFL system (e.g., IS-Academia, Moodle, Drive) and its connection state. */
data class SystemItem(val id: String, val name: String, val isConnected: Boolean)

/**
 * Legacy timeline entry used before migrating to message-based chat. Kept temporarily for backward
 * compatibility in tests or old screens.
 */
data class ActionItem(val id: String, val title: String, val time: String)
