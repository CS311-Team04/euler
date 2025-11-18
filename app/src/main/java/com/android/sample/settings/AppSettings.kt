package com.android.sample.settings

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Type-safe app settings manager with persistent storage using DataStore. Settings are stored on
 * disk and automatically loaded on app start.
 */
object AppSettings {
  // DataStore instance (lazily created via delegate)
  private val Context.dataStore: DataStore<Preferences> by
      preferencesDataStore(name = "app_settings")

  // DataStore key
  private val LANGUAGE_KEY = stringPreferencesKey("language")

  // Observable state using Compose's mutableStateOf - use internal state holder
  private val _languageState = mutableStateOf(Language.EN)

  // Public read-only access to language
  val language: Language
    get() = _languageState.value

  private var dataStore: DataStore<Preferences>? = null

  // Allow dispatcher injection for testing
  private var dispatcher: CoroutineDispatcher = Dispatchers.IO
  private var scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
  private var initializationJob: Job? = null

  /**
   * Initialize AppSettings with the app context. This loads saved settings from disk. Call this
   * once during app initialization (e.g., in Application.onCreate()).
   */
  fun initialize(context: Context) {
    dataStore = context.dataStore
    // Cancel any previous initialization job
    initializationJob?.cancel()
    // Load settings from DataStore
    initializationJob = scope.launch { loadSettings() }
  }

  /** Load settings from DataStore into memory. */
  private suspend fun loadSettings() {
    dataStore
        ?.data
        ?.map { preferences ->
          val languageCode = preferences[LANGUAGE_KEY] ?: Language.EN.code
          _languageState.value = Language.fromCode(languageCode)
        }
        ?.first()
  }

  /** Update the language and persist to disk. */
  fun setLanguage(value: Language) {
    _languageState.value = value
    scope.launch { dataStore?.edit { preferences -> preferences[LANGUAGE_KEY] = value.code } }
  }

  /**
   * Set the dispatcher to use for coroutines. This is primarily for testing purposes. Call this
   * before initialize() in test setup. Cancels any pending coroutines from the old dispatcher.
   */
  internal fun setDispatcher(dispatcher: CoroutineDispatcher) {
    // Cancel any pending jobs and old scope
    initializationJob?.cancel()
    scope.cancel()
    this.dispatcher = dispatcher
    // Recreate scope with new dispatcher to ensure new coroutines use the test dispatcher
    scope = CoroutineScope(dispatcher)
    initializationJob = null
  }

  /**
   * Reset the dispatcher to the default (Dispatchers.IO). This is primarily for testing purposes to
   * reset state after tests. Cancels any pending coroutines.
   */
  internal fun resetDispatcher() {
    // Cancel any pending jobs
    initializationJob?.cancel()
    scope.cancel()
    this.dispatcher = Dispatchers.IO
    // Recreate scope with default dispatcher
    scope = CoroutineScope(Dispatchers.IO)
    initializationJob = null
  }
}
