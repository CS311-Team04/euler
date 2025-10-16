package com.android.sample.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

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

  // RAG:
  private val endpoint = BuildConfig.ENDPOINT
  private val apiKey = BuildConfig.API_KEY

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

      // add the user message to timeline immediately
      val userAction =
          ActionItem(
              id = UUID.randomUUID().toString(),
              title = "You: \"$msg\"",
              time = "Just now")

      _uiState.value =
          _uiState.value.copy(
              recent = listOf(userAction) + _uiState.value.recent,
              isLoading = true,
              messageDraft = "")

      viewModelScope.launch {
          try {
              val answer = queryAnswer(msg)
              val botAction =
                  ActionItem(
                      id = UUID.randomUUID().toString(),
                      title = "EULER: $answer",
                      time = "Just now")
              _uiState.value =
                  _uiState.value.copy(
                      recent = listOf(botAction) + _uiState.value.recent,
                      isLoading = false)
          } catch (e: Exception) {
              val errAction =
                  ActionItem(
                      id = UUID.randomUUID().toString(),
                      title = "Error: ${e.message ?: "request failed"}",
                      time = "Just now")
              _uiState.value =
                  _uiState.value.copy(
                      recent = listOf(errAction) + _uiState.value.recent,
                      isLoading = false)
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
  private suspend fun queryAnswer(question: String): String = withContext(Dispatchers.IO) {
    if (endpoint.isBlank() || apiKey.isBlank()) {
        return@withContext "Backend not configured (ENDPOINT/API_KEY missing)."
    }
    val url = URL(endpoint)
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        setRequestProperty("x-api-key", apiKey)
        setRequestProperty("Content-Type", "application/json")
        doOutput = true
        connectTimeout = 15000
        readTimeout = 30000
    }

    val body = JSONObject(mapOf("question" to question))
    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
    val text = stream.bufferedReader().readText()
    conn.disconnect()

    if (code !in 200..299) throw Exception("HTTP $code: $text")

    val json = runCatching { JSONObject(text) }.getOrNull()
    val reply = json?.optString("reply")
    reply?.takeIf { it.isNotBlank() } ?: text
  }

}
