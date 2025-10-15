package com.android.sample.home

import androidx.lifecycle.ViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the UI state (HomeUiState) and exposes methods to update it. The UI (Compose) observes
 * uiState and updates automatically.
 */
class HomeViewModel : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          HomeUiState(
              systems =
                  listOf(
                      SystemItem(id = "isa", name = "IS-Academia", isConnected = true),
                      SystemItem(id = "moodle", name = "Moodle", isConnected = true),
                      SystemItem(id = "ed", name = "Ed Discussion", isConnected = true),
                      SystemItem(id = "camipro", name = "Camipro", isConnected = false),
                      SystemItem(id = "mail", name = "EPFL Mail", isConnected = false),
                      SystemItem(id = "drive", name = "EPFL Drive", isConnected = true),
                  ),
              recent =
                  listOf(
                      ActionItem(
                          id = UUID.randomUUID().toString(),
                          title = "Posted a question on Ed Discussion",
                          time = "2h ago"),
                      ActionItem(
                          id = UUID.randomUUID().toString(),
                          title = "Synced notes with EPFL Drive",
                          time = "Yesterday"),
                      ActionItem(
                          id = UUID.randomUUID().toString(),
                          title = "Checked IS-Academia timetable",
                          time = "2 days ago"),
                  )))
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  // --- State mutations ---

  fun toggleDrawer() {
    _uiState.value = _uiState.value.copy(isDrawerOpen = !_uiState.value.isDrawerOpen)
  }

  fun setTopRightOpen(open: Boolean) {
    _uiState.value = _uiState.value.copy(isTopRightOpen = open)
  }

  fun updateMessageDraft(text: String) {
    _uiState.value = _uiState.value.copy(messageDraft = text)
  }

  fun sendMessage() {
    val msg = _uiState.value.messageDraft.trim()
    if (msg.isEmpty()) return

    val newAction =
        ActionItem(
            id = UUID.randomUUID().toString(),
            title = "Messaged EULER: \"$msg\"",
            time = "Just now")

    _uiState.value =
        _uiState.value.copy(recent = listOf(newAction) + _uiState.value.recent, messageDraft = "")
  }

  fun toggleSystemConnection(systemId: String) {
    val updated =
        _uiState.value.systems.map { s ->
          if (s.id == systemId) s.copy(isConnected = !s.isConnected) else s
        }
    _uiState.value = _uiState.value.copy(systems = updated)
  }

  fun setLoading(loading: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = loading)
  }
}
