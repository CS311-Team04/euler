package com.android.sample.llm

/** Instrumentation test double for [LlmClient] returning canned responses. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): String {
    prompts += prompt
    failure?.let { throw it }
    return nextReply
  }
}
