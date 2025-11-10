package com.android.sample.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Abstraction so UI/test code can provide custom playback implementations if needed. */
interface SpeechPlayback {
    fun speak(
        text: String,
        utteranceId: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (Throwable?) -> Unit = {}
    )

    fun stop()

    fun shutdown()
}

/**
 * Lightweight wrapper around [TextToSpeech] to provide easy one-shot playback for chat messages.
 *
 * Responsibilities:
 * - Initialize TTS with the device locale (falling back gracefully when unavailable)
 * - Expose a simple [speak] method with callbacks for start/done/error events
 * - Allow clients to stop playback or release resources when no longer needed
 */
class TextToSpeechHelper(context: Context, private val preferredLocale: Locale = Locale.FRENCH) :
    SpeechPlayback {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class PendingSpeech(
        val text: String,
        val utteranceId: String,
        val callbacks: SpeechCallbacks
    )

    private data class SpeechCallbacks(
        val onStart: () -> Unit,
        val onDone: () -> Unit,
        val onError: (Throwable?) -> Unit
    )

    private val pendingQueue = mutableListOf<PendingSpeech>()
    private val callbacks = ConcurrentHashMap<String, SpeechCallbacks>()

    @Volatile private var isReady: Boolean = false

    private lateinit var textToSpeech: TextToSpeech

    init {
        textToSpeech =
            TextToSpeech(appContext) { status ->
                isReady =
                    if (status == TextToSpeech.SUCCESS) {
                        selectLanguage()
                    } else {
                        false
                    }

                synchronized(pendingQueue) {
                    if (isReady) {
                        // Replay queued requests now that initialization succeeded.
                        val requests = pendingQueue.toList()
                        pendingQueue.clear()
                        requests.forEach { speakInternal(it) }
                    } else {
                        // Notify queued callbacks that TTS is unavailable.
                        val failure =
                            IllegalStateException("TextToSpeech engine is not available on this device")
                        pendingQueue.forEach { pending ->
                            dispatchOnMain { pending.callbacks.onError(failure) }
                        }
                        pendingQueue.clear()
                    }
                }
            }

        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    callbacks[utteranceId]?.let { cb -> dispatchOnMain { cb.onStart() } }
                }

                override fun onDone(utteranceId: String) {
                    callbacks.remove(utteranceId)?.let { cb -> dispatchOnMain { cb.onDone() } }
                }

                @Deprecated("Deprecated in API 21")
                override fun onError(utteranceId: String) {
                    callbacks.remove(utteranceId)?.let { cb -> dispatchOnMain { cb.onError(null) } }
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    callbacks.remove(utteranceId)?.let { cb ->
                        val error = IllegalStateException("TTS error (code=$errorCode)")
                        dispatchOnMain { cb.onError(error) }
                    }
                }
            })
    }

    /**
     * Speaks the given [text]. If the TTS engine is still warming up, the request is queued and
     * played once initialization finishes.
     *
     * @param text Message that should be read aloud.
     * @param utteranceId Unique ID to track callbacks (use message ID when available).
     * @param onStart Invoked on the main thread when playback begins.
     * @param onDone Invoked on the main thread when playback completes.
     * @param onError Invoked on the main thread if playback fails (initialization or runtime).
     */
    override fun speak(
        text: String,
        utteranceId: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        val callbacks =
            SpeechCallbacks(
                onStart = onStart,
                onDone = onDone,
                onError = onError,
            )
        val request = PendingSpeech(text = text, utteranceId = utteranceId, callbacks = callbacks)

        if (!isReady) {
            synchronized(pendingQueue) { pendingQueue.add(request) }
            return
        }

        speakInternal(request)
    }

    /** Stops any ongoing playback immediately. */
    override fun stop() {
        callbacks.clear()
        textToSpeech.stop()
    }

    /** Releases the underlying TTS engine. Call when you no longer need speech output. */
    override fun shutdown() {
        callbacks.clear()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun speakInternal(request: PendingSpeech) {
        callbacks[request.utteranceId] = request.callbacks
        val result =
            textToSpeech.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, request.utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            callbacks.remove(request.utteranceId)
            val failure = IllegalStateException("Failed to enqueue text-to-speech (code=$result)")
            dispatchOnMain { request.callbacks.onError(failure) }
        }
    }

    private fun dispatchOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun selectLanguage(): Boolean {
        val candidates = buildList {
            add(preferredLocale)
            if (preferredLocale != Locale.FRANCE) add(Locale.FRANCE)
            val device = Locale.getDefault()
            if (!contains(device)) add(device)
        }

        for (locale in candidates) {
            val result = textToSpeech.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                return true
            }
        }
        return false
    }
}