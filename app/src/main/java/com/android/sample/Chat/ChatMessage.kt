package com.android.sample.Chat

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders a single chat message as either:
 * - a right-aligned grey "bubble" for user messages, or
 * - a full-width, background-less paragraph for AI messages.
 *
 * This composable is presentation-only: it consumes a [ChatUIModel] that must already encode the
 * message role (see [ChatType]) and the display text.
 *
 * ### Testing
 * - User bubble node is tagged with `chat_user_bubble`
 * - User text node is tagged with `chat_user_text`
 * - AI text node is tagged with `chat_ai_text`
 *
 * @param message The UI model for the message (text + role).
 * @param modifier Optional [Modifier] for the outer layout node of this message.
 * @param userBubbleBg Background color for the user bubble.
 * @param userBubbleText Text color for the user bubble content.
 * @param aiText Text color for the AI paragraph.
 * @param maxUserBubbleWidthFraction Maximum width fraction for the user bubble (0 < f ≤ 1).
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ChatMessage(
    message: ChatUIModel,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    audioState: MessageAudioState? = null,
    onRetry: (() -> Unit)? = null,
    isLastAiMessage: Boolean = false,
    userBubbleBg: Color = Color(0xFF2B2B2B),
    userBubbleText: Color = Color.White,
    aiText: Color = Color(0xFFEDEDED),
    maxUserBubbleWidthFraction: Float = 0.78f
) {
  val isUser = message.type == ChatType.USER

  if (isUser) {
    // RIGHT-ALIGNED row; bubble wraps content (no fillMaxWidth on bubble or text)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      BoxWithConstraints {
        val maxBubbleWidth = maxWidth * maxUserBubbleWidthFraction

        Surface(
            color = userBubbleBg,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier =
                Modifier.widthIn(max = maxBubbleWidth) // cap width
                    .testTag("chat_user_bubble")) {
              Text(
                  text = message.text,
                  color = userBubbleText,
                  style = MaterialTheme.typography.bodyMedium,
                  lineHeight = 18.sp,
                  textAlign = TextAlign.Start,
                  // IMPORTANT: no fillMaxWidth here → wrap content
                  modifier =
                      Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                          .testTag("chat_user_text"))
            }
      }
    }
  } else {
    // AI: full-width Markdown-rendered text
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
      if (isStreaming && message.text.isEmpty()) {
        LeadingThinkingDot(color = aiText)
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          MarkdownText(
              markdown = message.text,
              color = aiText,
              modifier = Modifier.fillMaxWidth().testTag("chat_ai_text"))

          // Show action buttons row (retry + audio)
          val showRetry = isLastAiMessage && onRetry != null && !isStreaming
          if (audioState != null || showRetry) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                  // Retry button - only show for the last AI message
                  if (showRetry) {
                    RetryButton(onClick = onRetry!!)
                    if (audioState != null) {
                      Spacer(modifier = Modifier.size(4.dp))
                    }
                  }
                  if (audioState != null) {
                    AudioPlaybackButton(state = audioState)
                  }
                }
          }
        }
      }
    }
  }
}

@Composable
private fun LeadingThinkingDot(color: Color) {
  val transition = rememberInfiniteTransition(label = "cursor")
  val alpha by
      transition.animateFloat(
          initialValue = 0.25f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "cursorAlpha")

  Surface(
      modifier = Modifier.size(10.dp).testTag("chat_ai_cursor"),
      color = color.copy(alpha = alpha),
      shape = CircleShape,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp) {}
}

@Immutable
data class MessageAudioState(
    val isLoading: Boolean,
    val isPlaying: Boolean,
    val onPlay: () -> Unit,
    val onStop: () -> Unit
)

@Composable
private fun AudioPlaybackButton(
    state: MessageAudioState,
    modifier: Modifier = Modifier.size(24.dp)
) {
  val colorScheme = MaterialTheme.colorScheme
  // Use onSurfaceVariant which adapts to light/dark mode
  val buttonTint = colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
  val loadingColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

  when {
    state.isLoading -> {
      CircularProgressIndicator(
          modifier = Modifier.size(14.dp).testTag("chat_audio_btn_loading"),
          strokeWidth = 2.dp,
          color = loadingColor)
    }
    state.isPlaying -> {
      IconButton(modifier = modifier.testTag("chat_audio_btn_stop"), onClick = state.onStop) {
        Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = "Stop audio",
            tint = buttonTint,
            modifier = Modifier.size(14.dp))
      }
    }
    else -> {
      IconButton(modifier = modifier.testTag("chat_audio_btn_play"), onClick = state.onPlay) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = "Play audio",
            tint = buttonTint,
            modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
private fun RetryButton(onClick: () -> Unit, modifier: Modifier = Modifier.size(24.dp)) {
  val colorScheme = MaterialTheme.colorScheme
  // Use onSurfaceVariant which adapts to light/dark mode
  val buttonTint = colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

  IconButton(modifier = modifier.testTag("chat_retry_btn"), onClick = onClick) {
    Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = "Retry",
        tint = buttonTint,
        modifier = Modifier.size(16.dp))
  }
}
