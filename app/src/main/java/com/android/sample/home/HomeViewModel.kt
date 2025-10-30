package com.android.sample.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Détient l'état UI (HomeUiState) et expose des méthodes pour le mettre à jour. L'UI (Compose)
 * observe uiState et se met à jour automatiquement.
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
  // private val endpoint = "http://10.0.2.2:5001/euler-e8edb/us-central1/answerWithRagHttp"
  // private val apiKey = "db8e16080302b511c256794b26a6e80089c80e1c15b7927193e754b7fd87fc4e"
  private val functions: FirebaseFunctions by lazy {
    FirebaseFunctions.getInstance("us-central1").apply { useEmulator("10.0.2.2", 5002) }
  }
  // --- Mutations d'état ---

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

    val userAction =
        ActionItem(id = UUID.randomUUID().toString(), title = "You: \"$msg\"", time = "Just now")

    _uiState.value =
        _uiState.value.copy(
            recent = listOf(userAction) + _uiState.value.recent,
            isLoading = true,
            messageDraft = "")

    viewModelScope.launch {
      try {
        val answer = callAnswerWithRag(msg)
        val botAction =
            ActionItem(
                id = UUID.randomUUID().toString(), title = "EULER: $answer", time = "Just now")
        _uiState.value =
            _uiState.value.copy(
                recent = listOf(botAction) + _uiState.value.recent, isLoading = false)
      } catch (e: Exception) {
        val errAction =
            ActionItem(
                id = UUID.randomUUID().toString(),
                title = "Error: ${e.message ?: "request failed"}",
                time = "Just now")
        _uiState.value =
            _uiState.value.copy(
                recent = listOf(errAction) + _uiState.value.recent, isLoading = false)
      }
    }
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

  // New: simple HTTP call to Cloud Function
  private suspend fun callAnswerWithRag(question: String): String =
      withContext(Dispatchers.IO) {
        val data = hashMapOf("question" to question) // add "topK"/"model" if needed
        val result = functions.getHttpsCallable("answerWithRagFn").call(data).await()

        @Suppress("UNCHECKED_CAST")
        val map = result.getData() as? Map<String, Any?> ?: return@withContext "Invalid response"
        (map["reply"] as? String)?.ifBlank { null } ?: "No reply"
      }
}
