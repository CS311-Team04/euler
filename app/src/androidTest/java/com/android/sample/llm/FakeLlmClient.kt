package com.android.sample.llm

/** Instrumentation test double for [LlmClient] returning canned responses. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var nextUrl: String? = null
  var nextEdIntentDetected: Boolean = false
  var nextEdIntent: String? = null
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): BotReply {
    prompts += prompt
    failure?.let { throw it }
    return BotReply(nextReply, nextUrl, nextEdIntentDetected, nextEdIntent)
  }
}
