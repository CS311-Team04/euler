package com.android.sample.VoiceChat

import com.android.sample.VoiceChat.Backend.ElevenLabs_LiveTTSClient
import com.android.sample.VoiceChat.Backend.ElevenLabs_LiveTTSClient.ServerEvent as LiveServerEvent
import com.android.sample.VoiceChat.Backend.ElevenLabs_LiveTTSClient.SessionState as LiveSessionState
import com.android.sample.VoiceChat.Backend.LiveTtsClient
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceChatStreamingTest {

    @get:Rule val dispatcherRule = MainDispatcherRule()
    @Test
    fun liveClient_onConnected_updatesState() = runTest {
        val client =
            ElevenLabs_LiveTTSClient(
                apiKeyProvider = { "api_key" },
                voiceIdProvider = { "voice_id" },
            )

        client.onConnected("session-123")

        val state = client.state.value
        assertTrue(state is LiveSessionState.Connected)
        assertEquals("session-123", (state as LiveSessionState.Connected).sessionId)
    }

    @Test
    fun liveClient_decodeAudioChunk_returnsNullForInvalidBase64() {
        val client =
            ElevenLabs_LiveTTSClient(
                apiKeyProvider = { "api_key" },
                voiceIdProvider = { "voice_id" },
            )

        assertEquals(null, client.decodeAudioChunk("!invalid!"))
    }

    @Test
    fun liveClient_failsWhenApiKeyMissing() {
        val client =
            ElevenLabs_LiveTTSClient(
                apiKeyProvider = { "" },
                voiceIdProvider = { "voice" }
            )
        val state = client.connect()
        assertTrue(state is LiveSessionState.Failed)
        assertEquals("Missing ElevenLabs API key", (state as LiveSessionState.Failed).reason)
    }

    @Test
    fun viewModel_connectUpdatesStateAndDelegates() = runTest {
        val fakeClient = FakeLiveTtsClient()
        val audioPlayer = FakeAudioPlayer()
        val viewModel = VoiceChatViewModel(fakeClient, audioPlayer)

        fakeClient.stateFlow.value = LiveSessionState.Connecting
        viewModel.connect()

        assertEquals(1, fakeClient.connectInvocations)
        assertEquals(LiveSessionState.Connecting, viewModel.uiState.value.sessionState)
    }

    @Test
    fun viewModel_writesAudioFramesAndMarksSpeaking() = runTest {
        val fakeClient = FakeLiveTtsClient()
        val audioPlayer = FakeAudioPlayer()
        val viewModel = VoiceChatViewModel(fakeClient, audioPlayer)

        val job = launch { fakeClient.audioFramesFlow.emit(byteArrayOf(42)) }
        job.join()
        assertEquals(listOf(listOf(42.toByte())), audioPlayer.writtenFrames.map { it.toList() })
        assertTrue(viewModel.uiState.value.isSpeaking)
    }

    @Test
    fun viewModel_handlesRemoteError() = runTest {
        val fakeClient = FakeLiveTtsClient()
        val audioPlayer = FakeAudioPlayer()
        val viewModel = VoiceChatViewModel(fakeClient, audioPlayer)

        val job =
            launch {
                fakeClient.eventsFlow.emit(LiveServerEvent.RemoteError(code = 500, message = "boom"))
            }
        job.join()

        assertEquals("boom", viewModel.uiState.value.lastError)
        assertTrue(audioPlayer.pauseCalls > 0)
    }

    @Test
    fun viewModel_disconnectStopsAudio() = runTest {
        val fakeClient = FakeLiveTtsClient()
        val audioPlayer = FakeAudioPlayer()
        val viewModel = VoiceChatViewModel(fakeClient, audioPlayer)

        viewModel.disconnect()

        assertEquals(1, fakeClient.disconnectInvocations)
        assertEquals(1, audioPlayer.stopCalls)
        assertEquals(LiveSessionState.Disconnected, viewModel.uiState.value.sessionState)
        assertFalse(viewModel.uiState.value.isSpeaking)
    }

}

class FakeLiveTtsClient : LiveTtsClient {
    val stateFlow = MutableStateFlow<LiveSessionState>(LiveSessionState.Disconnected)
    val eventsFlow =
        MutableSharedFlow<LiveServerEvent>(replay = 0, extraBufferCapacity = 8)
    val audioFramesFlow =
        MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 8)

    var connectInvocations = 0
    var pauseInvocations = 0
    var resumeInvocations = 0
    var disconnectInvocations = 0
    val spokenTexts = mutableListOf<String>()

    override val state = stateFlow
    override val events = eventsFlow
    override val audioFrames = audioFramesFlow

    override fun connect(): LiveSessionState {
        connectInvocations++
        return stateFlow.value
    }

    override fun speak(text: String, flush: Boolean, latencyOptimization: String) {
        spokenTexts += text
    }

    override fun pause() {
        pauseInvocations++
    }

    override fun resume() {
        resumeInvocations++
    }

    override fun disconnect() {
        disconnectInvocations++
        stateFlow.value = LiveSessionState.Disconnected
    }

    override fun close() {}
}

class FakeAudioPlayer : VoiceChatViewModel.AudioPlayer {
    val writtenFrames = mutableListOf<ByteArray>()
    var pauseCalls = 0
    var playCalls = 0
    var stopCalls = 0

    override fun write(data: ByteArray) {
        writtenFrames += data
    }

    override fun pause() {
        pauseCalls++
    }

    override fun play() {
        playCalls++
    }

    override fun stopAndRelease() {
        stopCalls++
    }

    override fun close() {
        // no-op
    }
}


