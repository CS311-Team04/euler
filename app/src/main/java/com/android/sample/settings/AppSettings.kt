package com.android.sample.settings

import android.content.Context
import androidx.compose.runtime.State
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
  private val APPEARANCE_KEY = stringPreferencesKey("appearance_mode")

  // Observable state using Compose's mutableStateOf - use internal state holder
  private val _languageState = mutableStateOf(Language.EN)

  // Public read-only access to language
  val language: Language
    get() = _languageState.value

  val languageState: State<Language>
    get() = _languageState

  private val _appearanceState = mutableStateOf(AppearanceMode.SYSTEM)
  val appearanceMode: AppearanceMode
    get() = _appearanceState.value

  val appearanceState: State<AppearanceMode>
    get() = _appearanceState

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
          val appearanceValue = preferences[APPEARANCE_KEY]
          _appearanceState.value = AppearanceMode.fromPreference(appearanceValue)
        }
        ?.first()
  }

  /** Update the language and persist to disk. */
  fun setLanguage(value: Language) {
    _languageState.value = value
    scope.launch { dataStore?.edit { preferences -> preferences[LANGUAGE_KEY] = value.code } }
  }

  /** Update the appearance mode and persist to disk. */
  fun setAppearanceMode(value: AppearanceMode) {
    _appearanceState.value = value
    scope.launch {
      dataStore?.edit { preferences -> preferences[APPEARANCE_KEY] = value.prefValue }
    }
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

  /**
   * Test-only helper to clear state and avoid reusing DataStore between tests. Resets in-memory
   * settings and drops the DataStore reference so a fresh instance can be created in the next test.
   */
  internal fun clearForTests() {
    initializationJob?.cancel()
    scope.cancel()
    dataStore = null
    _languageState.value = Language.EN
    _appearanceState.value = AppearanceMode.SYSTEM
    // recreate scope with current dispatcher
    scope = CoroutineScope(dispatcher)
    initializationJob = null
  }
}
