package com.android.sample.llm

/** Test double for [LlmClient] that records prompts and returns a configurable reply. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var nextUrl: String? = null
  var nextSourceType: SourceType = SourceType.NONE
  var nextEdIntentDetected: Boolean = false
  var nextEdIntent: String? = null
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): BotReply {
    prompts += prompt
    failure?.let { throw it }
    return BotReply(nextReply, nextUrl, nextSourceType, nextEdIntentDetected, nextEdIntent)
  }

  /** Configure the fake to return an ED intent response */
  fun setEdIntentResponse(reply: String, intent: String) {
    nextReply = reply
    nextEdIntentDetected = true
    nextEdIntent = intent
  }

  /** Reset to default non-ED response */
  fun resetToDefault() {
    nextReply = "test-reply"
    nextUrl = null
    nextSourceType = SourceType.NONE
    nextEdIntentDetected = false
    nextEdIntent = null
    failure = null
  }
}
