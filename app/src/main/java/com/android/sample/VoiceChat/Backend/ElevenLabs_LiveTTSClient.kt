package com.android.sample.VoiceChat.Backend

import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject

/** Abstraction for a streaming TTS client to facilitate testing. */
interface LiveTtsClient : Closeable {
    val state: StateFlow<ElevenLabs_LiveTTSClient.SessionState>
    val events: SharedFlow<ElevenLabs_LiveTTSClient.ServerEvent>
    val audioFrames: SharedFlow<ByteArray>

    fun connect(): ElevenLabs_LiveTTSClient.SessionState
    fun speak(text: String, flush: Boolean, latencyOptimization: String)
    fun pause()
    fun resume()
    fun disconnect()
}

/**
 * Real-time ElevenLabs Text-to-Speech client based on the streaming WebSocket API.
 *
 * This component establishes a persistent WebSocket session, publishes decoded PCM audio frames,
 * and exposes a simple command surface (`speak`, `pause`, `resume`, `disconnect`) so higher layers
 * can orchestrate an interactive, low-latency voice chat loop.
 *
 * The ElevenLabs streaming pipeline (as of November 2025) works roughly as follows:
 * 1. Open a WebSocket to `wss://api.elevenlabs.io/v1/text-to-speech/{voiceId}/stream`.
 * 2. Authenticate by sending the API key in the `xi-api-key` header.
 * 3. Send JSON control messages (`"event": "synthesize"`, `"event": "stop"`, etc.).
 * 4. Receive JSON envelopes containing Base64-encoded PCM audio chunks (16 kHz mono by default).
 *
 * The client focuses on:
 * - Connection lifecycle and reconnection resilience.
 * - Emitting audio frames as soon as they arrive (no intermediate caching).
 * - Surface callbacks for session state and server side events (errors, acknowledgements, etc.).
 *
 * Playback is intentionally left out of scope so UI/Audio layers can decide whether to route audio
 * through `AudioTrack`, `ExoPlayer`, or any custom renderer.
 */
class ElevenLabs_LiveTTSClient(
    private val apiKeyProvider: () -> String,
    private val voiceIdProvider: () -> String,
    private val modelId: String = "eleven_turbo_v2",
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val outputFormat: String = DEFAULT_OUTPUT_FORMAT,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : LiveTtsClient {

    // High-level session state inspected by UI/state machines to know if we can send audio.
    private val _state = MutableStateFlow(SessionState.Disconnected as SessionState)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    // Emits lifecycle + protocol notifications (connected, remote error, etc.).
    private val _events =
        MutableSharedFlow<ServerEvent>(replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val events: SharedFlow<ServerEvent> = _events.asSharedFlow()

    // Emits raw PCM16 audio frames in arrival order; downstream layer pipes them to AudioTrack.
    private val _audioFrames =
        MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val audioFrames: SharedFlow<ByteArray> = _audioFrames.asSharedFlow()

    // Single listener reused for the lifetime of this client instance.
    private val webSocketListener = ElevenLabsWebSocketListener()
    // Active WebSocket connection (null => disconnected).
    private var webSocket: WebSocket? = null
    // Prevents multiple concurrent connect calls from racing while the handshake is in-flight.
    private val isConnecting = AtomicBoolean(false)
    @Volatile private var lastSpeakTs: Long = 0L
    private val firstAudioSeen = AtomicBoolean(false)

    /**
     * Initiates the WebSocket connection if we are currently disconnected.
     *
     * The call is idempotent: repeated invocations while connected are ignored. Returns the
     * resulting [SessionState] immediately (use [state] flow to observe transitions).
     */
    override fun connect(): SessionState {
        val current = _state.value
        if (current is SessionState.Connected || isConnecting.get()) {
            return current
        }

        // Fail fast with explicit state if the API key/voice ID are not configured.
        val apiKey = apiKeyProvider().takeIf { it.isNotBlank() }
        if (apiKey == null) {
            val failure = SessionState.Failed("Missing ElevenLabs API key")
            _state.value = failure
            return failure
        }
        val voiceId = voiceIdProvider().takeIf { it.isNotBlank() }
        if (voiceId == null) {
            val failure = SessionState.Failed("Missing ElevenLabs voice id")
            _state.value = failure
            return failure
        }

        isConnecting.set(true)
        _state.value = SessionState.Connecting

        // Build the ElevenLabs streaming endpoint request with auth header.
        val request =
            Request.Builder()
                .url(
                    "$baseUrl/${voiceIdProvider()}/stream-input" +
                            "?model_id=$modelId&output_format=$outputFormat&inactivity_timeout=3600&auto_mode=true"
                )
                .header("xi-api-key", apiKey)
                .build()

        val socket = httpClient.newWebSocket(request, webSocketListener)
        webSocket = socket
        return _state.value
    }

    /**
     * Sends a synthesis request with the given [text]. The connection must be in a Connected state.
     *
     * @param text natural language string the TTS engine should vocalize.
     * @param flush when `true`, instructs the server to cancel any previous synthesis and start fresh.
     * @param latencyOptimization optional hint: "normal" (default), "low", or "super_low".
     */
    override fun speak(text: String, flush: Boolean, latencyOptimization: String) {
        val socket = webSocket ?: return
        if (_state.value !is SessionState.Connected) return

        lastSpeakTs = SystemClock.elapsedRealtime()
        firstAudioSeen.set(false)

        val payload = JSONObject().apply {
            put("text", text)
            put("try_trigger_generation", true)
        }
        val queued = socket.send(payload.toString())
        if (!queued) {
            emitEvent(ServerEvent.ProtocolError("WebSocket send() returned false"))
        }
    }

    private fun endTurn() {
        webSocket?.send(JSONObject().put("text", "").toString())
    }



    /**
     * Request the server to pause audio generation. Audio that already arrived stays in the buffer.
     */
    override fun pause() {
        sendControlEvent("pause")
    }

    /** Resumes audio streaming after a [pause] call. */
    override fun resume() {
        sendControlEvent("resume")
    }

    /**
     * Gracefully closes the WebSocket and clears internal state.
     */
    override fun close() {
        disconnect()
        // Cancel any coroutines that may still be emitting audio/events.
        coroutineScope.cancel()
    }

    /** Closes the WebSocket (if active). Safe to call multiple times. */
    override fun disconnect() {
        webSocket?.close(NORMAL_CLOSURE_CODE, "client_shutdown")
        webSocket = null
        isConnecting.set(false)
        _state.value = SessionState.Disconnected
    }

    // Helper that wraps small control events (pause/resume/stop) into JSON and sends them.
    private fun sendControlEvent(event: String) {
        val socket = webSocket ?: return
        if (_state.value !is SessionState.Connected) return

        val payload = JSONObject().put("event", event).toString()
        socket.send(payload)
    }

    private fun defaultVoiceSettings(): JSONObject =
        JSONObject().apply {
            put("speed", 1.0)
            put("stability", 0.3)
            put("similarity_boost", 0.7)
        }

    @VisibleForTesting
    internal fun handleServerMessage(message: String) {
        val json = try { JSONObject(message) } catch (t: JSONException) {
            emitEvent(ServerEvent.ProtocolError("Malformed JSON: ${t.message}"))
            return
        }

        // Cas 1: chunk audio
        if (json.has("audio")) {
            val encoded = json.optString("audio", "")
            if (encoded.isEmpty()) {
                emitEvent(ServerEvent.ProtocolError("audio chunk missing 'audio'"))
                return
            }
            val bytes = decodeAudioChunk(encoded) ?: run {
                emitEvent(ServerEvent.ProtocolError("Invalid base64 audio"))
                return
            }
            Log.d(TAG, "Decoded audio chunk bytes=${bytes.size} encodedLen=${encoded.length}")
            emitFirstAudioIfNeeded(bytes.size)
            emitAudioFrame(bytes)
            if (json.optBoolean("isFinal", false)) {
                emitEvent(ServerEvent.EndOfTransmission)
            }
            return
        }

        // Cas 2: message final sans audio (rare)
        if (json.has("final")) {
            emitEvent(ServerEvent.EndOfTransmission)
            return
        }

        // Cas 3: erreur standardisée?
        if (json.has("error") || json.has("message")) {
            val code = json.optInt("code", -1)
            val msg  = json.optString("message", "Unknown ElevenLabs error")
            emitEvent(ServerEvent.RemoteError(code, msg))
            return
        }

        // Sinon : log
        emitEvent(ServerEvent.Unhandled("unknown", json))
    }


    @VisibleForTesting
    internal fun onConnected(sessionId: String?) {
        isConnecting.set(false)
        val connected = SessionState.Connected(sessionId = sessionId)
        _state.value = connected
        emitEvent(ServerEvent.Connected(sessionId.orEmpty()))
    }

    @VisibleForTesting
    internal fun onAudioChunk(encoded: String) {
        if (encoded.isEmpty()) {
            emitEvent(ServerEvent.ProtocolError("audio_chunk without audio_base64"))
            return
        }
        val bytes = decodeAudioChunk(encoded) ?: run {
            emitEvent(ServerEvent.ProtocolError("Invalid base64 audio: decode returned null"))
            return
        }
        emitFirstAudioIfNeeded(bytes.size)
        emitAudioFrame(bytes)
    }

    @VisibleForTesting
    internal fun decodeAudioChunk(encoded: String): ByteArray? =
        try {
            Base64.decode(encoded, Base64.DEFAULT)
        } catch (t: IllegalArgumentException) {
            null
        }

    private fun emitFirstAudioIfNeeded(byteCount: Int) {
        if (firstAudioSeen.compareAndSet(false, true)) {
            val ttfb = SystemClock.elapsedRealtime() - lastSpeakTs
            emitEvent(ServerEvent.FirstAudio(ttfbMs = ttfb, bytes = byteCount))
        }
    }

    private fun emitAudioFrame(bytes: ByteArray) {
        if (!_audioFrames.tryEmit(bytes)) {
            coroutineScope.launch { _audioFrames.emit(bytes) }
        }
    }

    private fun emitEvent(event: ServerEvent) {
        if (!_events.tryEmit(event)) {
            coroutineScope.launch { _events.emit(event) }
        }
    }

    private inner class ElevenLabsWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _state.value = SessionState.Connecting
            emitEvent(ServerEvent.SocketOpen(response.headers))

            // Message d'init recommandé par la doc
            val init = JSONObject().apply {
                put("text", " ") // keep-alive + arm le pipeline
                put("voice_settings", defaultVoiceSettings())
            }
            webSocket.send(init.toString())

            // On considère la session prête côté client
            _state.value = SessionState.Connected(sessionId = null)
            emitEvent(ServerEvent.Connected(sessionId = ""))
        }


        override fun onMessage(webSocket: WebSocket, text: String) {
            handleServerMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // According to the docs audio should arrive inside JSON envelopes, but we handle binary too.
            // The payload is already PCM16 mono; emit directly for consumers that support raw frames.
            val data = bytes.toByteArray()
            emitFirstAudioIfNeeded(data.size)
            emitAudioFrame(data)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // Remote or client initiated close -> reset state so users can reconnect.
            _state.value = SessionState.Disconnected
            emitEvent(ServerEvent.SocketClosed(code, reason))
            isConnecting.set(false)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // Socket failure (network drop, auth issue, server crash) -> propagate as Failed.
            _state.value = SessionState.Failed(t.message ?: "Unknown socket error")
            emitEvent(ServerEvent.SocketFailure(t, response))
            isConnecting.set(false)
            val bodyPreview =
                try {
                    response?.body?.string()?.take(256)
                } catch (ignored: Exception) {
                    null
                }
            Log.e(
                TAG,
                "WebSocket failure. code=${response?.code} reason=${response?.message} body=${bodyPreview ?: "<none>"}",
                t)
        }
    }

    // Public facing session states so UI/viewmodels can react to connectivity changes.
    sealed interface SessionState {
        object Disconnected : SessionState
        object Connecting : SessionState
        data class Connected(val sessionId: String? = null) : SessionState
        data class Failed(val reason: String) : SessionState
    }

    /**
     * Events emitted by the live session. Consumers can surface them to logs/analytics/UI as needed.
     */
    sealed interface ServerEvent {
        data class SocketOpen(val headers: okhttp3.Headers) : ServerEvent
        data class Connected(val sessionId: String) : ServerEvent
        data class RemoteError(val code: Int, val message: String) : ServerEvent
        data class ProtocolError(val message: String) : ServerEvent
        data class SocketClosed(val code: Int, val reason: String) : ServerEvent
        data class SocketFailure(val throwable: Throwable, val response: Response?) : ServerEvent
        object EndOfTransmission : ServerEvent
        data class FirstAudio(val ttfbMs: Long, val bytes: Int) : ServerEvent
        data class Unhandled(val event: String, val raw: JSONObject) : ServerEvent
    }

    companion object {
        private const val NORMAL_CLOSURE_CODE = 1000
        private const val DEFAULT_BASE_URL =
            "wss://api.elevenlabs.io/v1/text-to-speech"
        private const val TAG = "ElevenLabs_LiveTTS"
        private const val DEFAULT_OUTPUT_FORMAT = "pcm_16000"
    }
}


