package com.android.sample.conversations

/**
 * Fake implementation of CachedResponseRepository for testing. Allows configuring responses and
 * simulating errors.
 */
class FakeCachedResponseRepository : CachedResponseRepositoryInterface {
  /** Map of question hashes to responses. */
  private val cache = mutableMapOf<String, String>()

  /** If set, getCachedResponse will throw this exception. */
  var getException: Exception? = null

  /** If set, saveCachedResponse will throw this exception. */
  var saveException: Exception? = null

  /** Records of saved responses for verification. */
  val savedResponses = mutableListOf<Pair<String, String>>()

  /** Records of questions queried. */
  val queriedQuestions = mutableListOf<String>()

  /** Set a cached response for a question. */
  fun setCachedResponse(question: String, response: String) {
    cache[normalizeQuestion(question)] = response
  }

  /** Clear all cached responses. */
  fun clearCache() {
    cache.clear()
    savedResponses.clear()
    queriedQuestions.clear()
  }

  private fun normalizeQuestion(question: String): String = question.trim().lowercase()

  override suspend fun getCachedResponse(question: String, preferCache: Boolean): String? {
    queriedQuestions.add(question)
    getException?.let { throw it }
    return cache[normalizeQuestion(question)]
  }

  override suspend fun saveCachedResponse(question: String, response: String) {
    saveException?.let { throw it }
    savedResponses.add(Pair(question, response))
    cache[normalizeQuestion(question)] = response
  }

  override suspend fun hasCachedResponse(question: String): Boolean {
    return cache.containsKey(normalizeQuestion(question))
  }
}

/**
 * Interface for CachedResponseRepository to enable testing with fakes. This interface defines the
 * contract that both the real and fake implementations must follow.
 */
interface CachedResponseRepositoryInterface {
  suspend fun getCachedResponse(question: String, preferCache: Boolean = false): String?

  suspend fun saveCachedResponse(question: String, response: String)

  suspend fun hasCachedResponse(question: String): Boolean
}
