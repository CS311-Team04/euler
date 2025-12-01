package com.android.sample.llm

/** Test double for [LlmClient] that records prompts and returns a configurable reply. */
class FakeLlmClient : LlmClient {
  val prompts = mutableListOf<String>()
  var nextReply: String = "test-reply"
  var nextUrl: String? = null
  var nextEdIntentDetected: Boolean = false
  var nextEdIntent: String? = null
  var nextEdFormattedQuestion: String? = null
  var nextEdFormattedTitle: String? = null
  var failure: Throwable? = null

  override suspend fun generateReply(prompt: String): BotReply {
    prompts += prompt
    failure?.let { throw it }
    return BotReply(
        nextReply,
        nextUrl,
        nextEdIntentDetected,
        nextEdIntent,
        nextEdFormattedQuestion,
        nextEdFormattedTitle)
  }

  /** Configure the fake to return an ED intent response */
  fun setEdIntentResponse(reply: String, intent: String) {
    nextReply = reply
    nextEdIntentDetected = true
    nextEdIntent = intent
  }

  /** Configure the fake to return an ED intent response with formatted question and title */
  fun setEdIntentResponseWithFormatted(
      reply: String,
      intent: String,
      formattedQuestion: String? = null,
      formattedTitle: String? = null
  ) {
    nextReply = reply
    nextEdIntentDetected = true
    nextEdIntent = intent
    nextEdFormattedQuestion = formattedQuestion
    nextEdFormattedTitle = formattedTitle
  }

  /** Reset to default non-ED response */
  fun resetToDefault() {
    nextReply = "test-reply"
    nextUrl = null
    nextEdIntentDetected = false
    nextEdIntent = null
    nextEdFormattedQuestion = null
    nextEdFormattedTitle = null
    failure = null
  }
}
