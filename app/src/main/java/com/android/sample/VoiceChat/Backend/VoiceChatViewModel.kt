package com.android.sample.VoiceChat.Backend

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.VoiceChat.Backend.ElevenLabs_LiveTTSClient.ServerEvent as LiveServerEvent
import com.android.sample.VoiceChat.Backend.ElevenLabs_LiveTTSClient.SessionState as LiveSessionState
import com.android.sample.VoiceChat.Backend.LiveTtsClient
import com.android.sample.speech.ElevenLabsConfig
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates the real-time ElevenLabs streaming client with UI state and audio playback.
 *
 * Responsibilities:
 * - Own a single instance of [ElevenLabs_LiveTTSClient] and surface its state to the UI.
 * - Pipe incoming PCM frames into an [AudioTrack] for low-latency playback.
 * - Provide high-level actions (connect, speak, pause, resume, disconnect) for composables.
 */
class VoiceChatViewModel(
    private val liveClient: LiveTtsClient =
        ElevenLabs_LiveTTSClient(
            apiKeyProvider = { ElevenLabsConfig.API_KEY },
            voiceIdProvider = { ElevenLabsConfig.VOICE_ID }
        ),
    private val audioPlayer: AudioPlayer = StreamingAudioPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceChatUiState())
    val uiState: StateFlow<VoiceChatUiState> = _uiState.asStateFlow()

    init {
        observeSessionState()
        observeServerEvents()
        observeAudioFrames()
    }

    /**
     * Ensures a WebSocket session is active. Safe to call repeatedly (no-ops while connected).
     */
    fun connect() {
        val apiKey = ElevenLabsConfig.API_KEY
        val voiceId = ElevenLabsConfig.VOICE_ID
        Log.d(
            TAG,
            "Connecting to ElevenLabs voice=$voiceId apiKeyPrefix=${apiKey.take(4)}***")
        val state = liveClient.connect()
        _uiState.update { it.copy(sessionState = state, lastError = extractError(state) ?: it.lastError) }
    }

    /**
     * Streams the given [text] to ElevenLabs so it can be spoken aloud.
     *
     * Blank messages are ignored to avoid unnecessary network calls.
     */
    fun speak(text: String, flush: Boolean = true, latencyHint: String = "normal") {
        if (text.isBlank()) return
        liveClient.speak(text = text, flush = flush, latencyOptimization = latencyHint)
        _uiState.update { it.copy(lastError = null) }
    }

    fun onUserTranscript(transcript: String) {
        _uiState.update { it.copy(lastTranscript = transcript, lastError = null) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(lastError = message) }
    }

    /** Temporarily pauses synthesis and local playback. */
    fun pause() {
        liveClient.pause()
        audioPlayer.pause()
        _uiState.update { it.copy(isSpeaking = false) }
    }

    /** Resumes a paused stream (both remote synthesis and local playback). */
    fun resume() {
        liveClient.resume()
        audioPlayer.play()
    }

    /** Disconnects the live session and clears audio buffers. */
    fun disconnect() {
        liveClient.disconnect()
        audioPlayer.stopAndRelease()
        _uiState.update { it.copy(sessionState = LiveSessionState.Disconnected, isSpeaking = false) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAndRelease()
        liveClient.close()
    }

    private fun observeSessionState() {
        viewModelScope.launch {
            liveClient.state.collect { state ->
                _uiState.update {
                    it.copy(
                        sessionState = state,
                        lastError = extractError(state),
                        isSpeaking = if (state is LiveSessionState.Disconnected) false else it.isSpeaking
                    )
                }
                if (state is LiveSessionState.Disconnected) {
                    audioPlayer.stopAndRelease()
                }
            }
        }
    }

    private fun observeServerEvents() {
        viewModelScope.launch {
            liveClient.events.collect { event ->
                when (event) {
                    is LiveServerEvent.Connected -> {
                        Log.i(TAG, "ElevenLabs connected session=${event.sessionId}")
                        _uiState.update { it.copy(sessionId = event.sessionId) }
                    }
                    is LiveServerEvent.EndOfTransmission -> {
                        Log.i(TAG, "ElevenLabs end of transmission")
                        audioPlayer.pause()
                        _uiState.update { it.copy(isSpeaking = false) }
                    }
                    is LiveServerEvent.RemoteError -> {
                        Log.w(TAG, "ElevenLabs remote error ${event.code}: ${event.message}")
                        audioPlayer.pause()
                        _uiState.update { it.copy(lastError = event.message, isSpeaking = false) }
                    }
                    is LiveServerEvent.ProtocolError -> {
                        Log.w(TAG, "ElevenLabs protocol error: ${event.message}")
                        _uiState.update { it.copy(lastError = event.message) }
                    }
                    is LiveServerEvent.SocketFailure -> {
                        Log.e(TAG, "ElevenLabs socket failure", event.throwable)
                        audioPlayer.stopAndRelease()
                        val reason = event.throwable.message ?: "Socket failure"
                        _uiState.update { it.copy(lastError = reason, isSpeaking = false) }
                    }
                    is LiveServerEvent.SocketClosed -> {
                        Log.i(TAG, "ElevenLabs socket closed code=${event.code} reason=${event.reason}")
                        audioPlayer.stopAndRelease()
                        _uiState.update { it.copy(isSpeaking = false) }
                    }
                    is LiveServerEvent.FirstAudio -> {
                        _uiState.update {
                            it.copy(
                                lastFirstAudioMs = event.ttfbMs,
                                lastFirstAudioBytes = event.bytes)
                        }
                    }
                    is LiveServerEvent.Unhandled -> {
                        Log.w(TAG, "ElevenLabs unhandled event=${event.event}")
                        _uiState.update { it.copy(lastError = "Unhandled event: ${event.event}") }
                    }
                    is LiveServerEvent.SocketOpen -> {
                        // No UI change required; keep awaiting the "connected" payload.
                    }
                }
            }
        }
    }

    private fun observeAudioFrames() {
        viewModelScope.launch {
            liveClient.audioFrames.collect { frame ->
                audioPlayer.write(frame)
                _uiState.update { it.copy(isSpeaking = true) }
            }
        }
    }

    private fun extractError(state: LiveSessionState): String? {
        return when (state) {
            is LiveSessionState.Failed -> state.reason
            else -> null
        }
    }

    private companion object {
        private const val TAG = "VoiceChatViewModel"
    }

    data class VoiceChatUiState(
        val sessionState: LiveSessionState = LiveSessionState.Disconnected,
        val sessionId: String? = null,
        val isSpeaking: Boolean = false,
        val lastError: String? = null,
        val lastTranscript: String? = null,
        val lastFirstAudioMs: Long? = null,
        val lastFirstAudioBytes: Int? = null
    )

    /**
     * Abstraction over the audio output sink so tests can inject a fake implementation.
     */
    interface AudioPlayer : Closeable {
        fun write(data: ByteArray)
        fun pause()
        fun play()
        fun stopAndRelease()
    }

    /**
     * Lightweight PCM player that wraps [AudioTrack] for streaming playback.
     */
    class StreamingAudioPlayer(
        private val sampleRateHz: Int = 16_000,
        private val channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
        private val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
        private val playbackGain: Float = DEFAULT_GAIN,
    ) : AudioPlayer {

        private var audioTrack: AudioTrack? = null
        private var loudnessEnhancer: LoudnessEnhancer? = null

        override fun write(data: ByteArray) {
            if (data.isEmpty()) return
            val track =
                audioTrack
                    ?: createTrack().also {
                        audioTrack = it
                        ensureLoudnessEnhancer(it)
                    }
            ensureLoudnessEnhancer(track)
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            val processed = applyGainIfNeeded(data)
            track.write(processed, 0, processed.size)
        }

        override fun pause() {
            audioTrack?.pause()
        }

        override fun play() {
            audioTrack?.play()
        }

        override fun stopAndRelease() {
            audioTrack?.let { track ->
                try {
                    track.stop()
                } catch (_: IllegalStateException) {
                    // Ignored: stop() can throw if track was never started.
                }
                track.flush()
                track.release()
            }
            audioTrack = null
            loudnessEnhancer?.let {
                try {
                    it.release()
                } catch (_: Throwable) {}
            }
            loudnessEnhancer = null
        }

        override fun close() {
            stopAndRelease()
        }

        @VisibleForTesting
        fun isActive(): Boolean = audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

        private fun createTrack(): AudioTrack {
            val attributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

            val format =
                AudioFormat.Builder()
                    .setSampleRate(sampleRateHz)
                    .setEncoding(encoding)
                    .setChannelMask(channelConfig)
                    .build()

            val minBufferSize = AudioTrack.getMinBufferSize(sampleRateHz, channelConfig, encoding)
            return AudioTrack(
                attributes,
                format,
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        }

        private fun applyGainIfNeeded(source: ByteArray): ByteArray {
            val gain = playbackGain
            if (gain <= 1.001f) {
                return source
            }
            val input = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN)
            val output = ByteBuffer.allocate(source.size).order(ByteOrder.LITTLE_ENDIAN)
            while (input.remaining() >= 2) {
                val sample = input.short.toInt()
                val scaled =
                    (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                output.putShort(scaled.toShort())
            }
            while (input.hasRemaining()) {
                output.put(input.get())
            }
            return output.array()
        }

        private fun ensureLoudnessEnhancer(track: AudioTrack) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
            if (loudnessEnhancer != null) return
            try {
                loudnessEnhancer =
                    LoudnessEnhancer(track.audioSessionId).apply {
                        setTargetGain(DEFAULT_LOUDNESS_GAIN_MB)
                        enabled = true
                    }
                Log.d(TAG, "LoudnessEnhancer enabled (gain=${DEFAULT_LOUDNESS_GAIN_MB} mB)")
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to enable LoudnessEnhancer", t)
                loudnessEnhancer = null
            }
        }

        companion object {
            private const val DEFAULT_GAIN = 10f
            private const val DEFAULT_LOUDNESS_GAIN_MB = 1000 // +20 dB
        }
    }
}
