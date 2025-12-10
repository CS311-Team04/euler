package com.android.sample.llm

/** Test double for [LlmClient] that records prompts and returns a configurable reply. */
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

  /** Configure the fake to return an ED intent response */
  fun setEdIntentResponse(reply: String, intent: String) {
    nextReply = reply
    nextEdIntent = EdIntent(detected = true, intent = intent)
  }

  /** Configure the fake to return an ED intent response with formatted question and title */
  fun setEdIntentResponseWithFormatted(
      reply: String,
      intent: String,
      formattedQuestion: String,
      formattedTitle: String
  ) {
    nextReply = reply
    nextEdIntent =
        EdIntent(
            detected = true,
            intent = intent,
            formattedQuestion = formattedQuestion,
            formattedTitle = formattedTitle)
  }

  /** Reset to default non-ED response */
  fun resetToDefault() {
    nextReply = "test-reply"
    nextUrl = null
    nextSourceType = SourceType.NONE
    nextEdIntent = EdIntent()
    failure = null
  }
}
