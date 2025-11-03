// VoiceChatSheet.kt
package com.android.sample.VoiceChat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** VoiceOverlay - Utilisé dans HomeScreen pour afficher VoiceScreen */
@Composable
fun VoiceOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  VoiceScreen(onClose = onDismiss, modifier = modifier)
}
