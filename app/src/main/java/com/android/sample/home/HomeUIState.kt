package com.android.sample.home

/**
 * Photo immuable de tout ce que la HomeScreen doit afficher à un instant t. Un changement = on crée
 * une nouvelle copie via copy(...)
 */
data class HomeUiState(
    val userName: String = "Student",
    val systems: List<SystemItem> = emptyList(),
    val recent: List<ActionItem> = emptyList(),
    val messageDraft: String = "",
    val isDrawerOpen: Boolean = false,
    val isTopRightOpen: Boolean = false,
    val isLoading: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)

/** Représente un système EPFL (IS-Academia, Moodle, Drive, etc.) et son état de connexion. */
data class SystemItem(val id: String, val name: String, val isConnected: Boolean)

/** Représente une entrée dans la timeline des actions récentes. */
data class ActionItem(val id: String, val title: String, val time: String)
