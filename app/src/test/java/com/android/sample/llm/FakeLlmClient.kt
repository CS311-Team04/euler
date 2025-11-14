package com.android.sample.llm

/** Test double for [LlmClient] that records prompts and returns a configurable reply. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): BotReply {
    prompts += prompt
    failure?.let { throw it }
    return BotReply(nextReply, null)
  }
}
