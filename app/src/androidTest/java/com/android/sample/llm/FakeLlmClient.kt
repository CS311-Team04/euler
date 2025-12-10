package com.android.sample.llm

/** Instrumentation test double for [LlmClient] returning canned responses. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var nextUrl: String? = null
  var nextSourceType: SourceType = SourceType.NONE
  var nextEdIntent: EdIntent = EdIntent()
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): BotReply {
    prompts += prompt
    failure?.let { throw it }
    return BotReply(nextReply, nextUrl, nextSourceType, nextEdIntent, EdFetchIntent())
  }
}
