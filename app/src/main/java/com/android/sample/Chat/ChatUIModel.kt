package com.android.sample.Chat

enum class ChatType {
  USER,
  AI
}

data class ChatUIModel(
    val id: String,
    val text: String,
    val timestamp: Long,
    val type: ChatType,
    val isThinking: Boolean = false
)
