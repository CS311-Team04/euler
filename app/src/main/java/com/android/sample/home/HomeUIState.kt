package com.android.sample.home

/**
 * Immutable snapshot of everything the HomeScreen needs to display at a given moment. A change =
 * create a new copy via copy(...)
 */
data class HomeUiState(
    val userName: String = "Student",
    val systems: List<SystemItem> = emptyList(),
    val recent: List<ActionItem> = emptyList(),
    val messageDraft: String = "",
    val isDrawerOpen: Boolean = false,
    val isTopRightOpen: Boolean = false,
    val isLoading: Boolean = false
)

/** Represents an EPFL system (IS-Academia, Moodle, Drive, etc.) and its connection status. */
data class SystemItem(val id: String, val name: String, val isConnected: Boolean)

/** Represents an entry in the recent actions timeline. */
data class ActionItem(val id: String, val title: String, val time: String)
