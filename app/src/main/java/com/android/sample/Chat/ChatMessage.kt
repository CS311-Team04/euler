package com.android.sample.Chat

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.EulerAudioButtonLoadingColor
import com.android.sample.ui.theme.EulerAudioButtonTint
import com.android.sample.ui.theme.EulerAudioButtonTintSemiTransparent
import com.android.sample.R

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
    userBubbleBg: Color = Color(0xFF2B2B2B),
    userBubbleText: Color = Color.White,
    aiText: Color = Color(0xFFEDEDED),
    maxUserBubbleWidthFraction: Float = 0.78f,
    onOpenAttachment: (ChatAttachment) -> Unit = {},
    onDownloadAttachment: (ChatAttachment) -> Unit = {}
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
    // AI: full-width plain text
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
      if (isStreaming && message.text.isEmpty()) {
        LeadingThinkingDot(color = aiText)
      } else {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
              text = message.text,
              color = aiText,
              style = MaterialTheme.typography.bodyMedium,
              lineHeight = 20.sp,
              modifier = Modifier.fillMaxWidth().testTag("chat_ai_text"))

          if (audioState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                  AudioPlaybackButton(
                      state = audioState, tint = EulerAudioButtonTintSemiTransparent)
                }
          }

          message.attachment?.let { attachment ->
            Spacer(modifier = Modifier.height(10.dp))
            AttachmentCard(
                attachment = attachment,
                onOpen = { onOpenAttachment(attachment) },
                onDownload = { onDownloadAttachment(attachment) })
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
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = EulerAudioButtonTint
) {
  when {
    state.isLoading -> {
      CircularProgressIndicator(
          modifier = Modifier.size(14.dp).testTag("chat_audio_btn_loading"),
          strokeWidth = 2.dp,
          color = EulerAudioButtonLoadingColor)
    }
    state.isPlaying -> {
      IconButton(modifier = modifier.testTag("chat_audio_btn_stop"), onClick = state.onStop) {
        Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = "Stop audio",
            tint = tint,
            modifier = Modifier.size(14.dp))
      }
    }
    else -> {
      IconButton(modifier = modifier.testTag("chat_audio_btn_play"), onClick = state.onPlay) {
        Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = "Play audio",
            tint = tint,
            modifier = Modifier.size(16.dp))
      }
    }
  }
}

@Composable
private fun AttachmentCard(
    attachment: ChatAttachment,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
  val bg = Color(0xFF121212)
  val border = Color(0xFF2A2A2A)
  val accent = Color(0xFFE65100)
  Surface(
      shape = RoundedCornerShape(16.dp),
      color = bg,
      border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(border)),
      modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = Color(0xFF1F1F1F),
                  modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Image(
                          painter = painterResource(id = R.drawable.moodle_logo),
                          contentDescription = "Moodle",
                          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                          contentScale = ContentScale.Crop)
                    }
                  }

              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = attachment.title.ifBlank { "Document PDF" },
                    color = Color.White,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp))
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)) {
                      Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = accent,
                                modifier = Modifier.size(16.dp))
                            Text("PDF", color = accent, fontSize = 12.sp)
                          }
                    }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(40.dp)) {
                      Icon(
                          imageVector = Icons.Outlined.Visibility,
                          contentDescription = "Open PDF",
                          tint = Color.White,
                          modifier = Modifier.size(22.dp))
                    }
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(40.dp)) {
                      Icon(
                          imageVector = Icons.Outlined.Download,
                          contentDescription = "Download PDF",
                          tint = Color.White,
                          modifier = Modifier.size(22.dp))
                    }
              }
            }
      }
}
