package com.android.sample.Chat

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.EulerAudioButtonLoadingColor
import com.android.sample.ui.theme.EulerAudioButtonTint
import com.android.sample.ui.theme.EulerAudioButtonTintSemiTransparent

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

          // Render file attachment if present
          message.fileAttachment?.let { attachment ->
            Spacer(modifier = Modifier.height(12.dp))
            FileAttachmentCard(
                attachment = attachment, modifier = Modifier.testTag("chat_file_attachment"))
          }

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
        }
      }
    }
  }
}

/**
 * File attachment card for displaying downloadable files in chat. Clicking opens the file in an
 * in-app PDF viewer for a seamless experience.
 */
@Composable
fun FileAttachmentCard(attachment: FileAttachment, modifier: Modifier = Modifier) {
  var showPdfViewer by remember { mutableStateOf(false) }

  // Colors for the card
  val cardBg = Color(0xFF1E3A5F) // Deep blue
  val iconBg = Color(0xFF2E5A8F) // Lighter blue
  val textColor = Color.White
  val subtitleColor = Color(0xFFB0C4DE) // Light steel blue

  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                // Open in-app PDF viewer
                showPdfViewer = true
              }
              .testTag("file_card"),
      colors = CardDefaults.cardColors(containerColor = cardBg),
      shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              // File icon
              Box(
                  modifier =
                      Modifier.size(48.dp)
                          .clip(RoundedCornerShape(8.dp))
                          .background(iconBg)
                          .testTag("file_icon_container"),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getFileIcon(attachment),
                        contentDescription = "File",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp))
                  }

              Spacer(modifier = Modifier.width(12.dp))

              // File info
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.filename,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("file_name"))

                attachment.courseName?.let { courseName ->
                  Text(
                      text = courseName,
                      color = subtitleColor,
                      style = MaterialTheme.typography.bodySmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.testTag("file_course"))
                }

                // Size if available
                attachment.formattedSize?.let { size ->
                  Text(
                      text = size,
                      color = subtitleColor.copy(alpha = 0.7f),
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.testTag("file_size"))
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              // View icon (indicates tap to open)
              Icon(
                  imageVector = Icons.Default.Visibility,
                  contentDescription = "View file",
                  tint = subtitleColor,
                  modifier = Modifier.size(24.dp).testTag("file_view_icon"))
            }
      }

  // Full-screen PDF viewer dialog
  if (showPdfViewer) {
    Dialog(
        onDismissRequest = { showPdfViewer = false },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
      PdfViewerScreen(
          fileUrl = attachment.downloadUrl,
          filename = attachment.filename,
          onDismiss = { showPdfViewer = false }
      )
    }
  }
}

/** Returns appropriate icon based on file type */
@Composable
private fun getFileIcon(attachment: FileAttachment): ImageVector {
  return when {
    attachment.isPdf -> Icons.Default.PictureAsPdf
    else -> Icons.Default.Description
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
