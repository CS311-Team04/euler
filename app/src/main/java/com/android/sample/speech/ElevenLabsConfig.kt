package com.android.sample.speech

import com.android.sample.BuildConfig

object ElevenLabsConfig {
  private const val PLACEHOLDER_API_KEY: String = "KEY_PLACEHOLDER"
  private const val PLACEHOLDER_VOICE_ID: String = "VOICE_ID"

  /**
   * Returns the configured ElevenLabs API key.
   *
   * Values come from `BuildConfig.ELEVENLABS_API_KEY`, which is populated from either environment
   * variables (`ELEVENLABS_API_KEY`) or the optional `elevenlabs.properties` secrets file. When no
   * secret is provided we fall back to a placeholder so the app can still run in a degraded "no
   * TTS" mode.
   */
  val API_KEY: String
    get() = BuildConfig.ELEVENLABS_API_KEY.ifBlank { PLACEHOLDER_API_KEY }

  /**
   * Returns the configured ElevenLabs voice identifier.
   *
   * Same resolution strategy as [API_KEY]; resolves to a placeholder when no secret is configured
   * so callers can detect misconfiguration and surface an actionable error.
   */
  val VOICE_ID: String
    get() = BuildConfig.ELEVENLABS_VOICE_ID.ifBlank { PLACEHOLDER_VOICE_ID }
}
