// VoiceChatSheet.kt
package com.android.sample.VoiceChat.UI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.speech.SpeechPlayback

/** Voice overlay entry point used from HomeScreen to render the voice UI. */
@Composable
fun VoiceOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    speechPlayback: SpeechPlayback? = null,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
  VoiceScreen(
      onClose = onDismiss,
      modifier = modifier,
      speechPlayback = speechPlayback,
      voiceChatViewModel = voiceChatViewModel)
}
