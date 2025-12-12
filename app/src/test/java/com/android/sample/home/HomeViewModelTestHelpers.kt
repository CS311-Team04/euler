package com.android.sample.home

import com.android.sample.conversations.CachedResponseRepository
import com.android.sample.conversations.ConversationRepository
import com.android.sample.llm.FakeLlmClient
import com.google.firebase.auth.FirebaseAuth
import org.mockito.kotlin.mock

/**
 * Lightweight helpers for constructing HomeViewModel instances in unit tests without hitting real
 * Firebase dependencies.
 */
object HomeViewModelTestHelpers {
  fun createViewModelForPdfDetection(): HomeViewModel {
    val auth: FirebaseAuth = mock()
    val repo: ConversationRepository = mock()
    val cache: CachedResponseRepository = mock()

    // Use fake/mocked dependencies so the view model stays in guest mode and avoids Firestore work.
    return HomeViewModel(
        llmClient = FakeLlmClient(),
        auth = auth,
        repo = repo,
        profileRepository = FakeProfileRepository(),
        networkMonitor = null,
        cacheRepo = cache)
  }
}
