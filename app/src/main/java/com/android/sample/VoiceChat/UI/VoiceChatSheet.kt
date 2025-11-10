// VoiceChatSheet.kt
package com.android.sample.VoiceChat.UI

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Voice overlay entry point used from HomeScreen to render the voice UI. */
@Composable
fun VoiceOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  VoiceScreen(onClose = onDismiss, modifier = modifier)
}
