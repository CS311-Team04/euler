package com.android.sample.Chat

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorsDimensions
import com.android.sample.ui.theme.EulerAudioButtonLoadingColor
import com.android.sample.ui.theme.EulerAudioButtonTint
import com.android.sample.ui.theme.EulerAudioButtonTintSemiTransparent
import com.android.sample.ui.theme.MoodleOrange

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
    onPdfClick: (String, String) -> Unit = { _, _ -> }
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

          // Display Moodle file attachment (PDF viewer)
          message.moodleFile?.let { file ->
            Spacer(modifier = Modifier.height(12.dp))
            val context = LocalContext.current
            MoodleFileViewer(
                file = file,
                onPdfClick = onPdfClick,
                onDownloadClick = { url, filename -> downloadPdf(context, url, filename) })
          }
        }
      }
    }
  }
}

/**
 * Professional PDF viewer component for Moodle files. Displays a card with PDF preview and download
 * option.
 *
 * @param file The Moodle file attachment to display
 * @param onPdfClick Callback when PDF card is clicked - should navigate to PDF viewer screen
 */
@Composable
fun MoodleFileViewer(
    file: MoodleFileAttachment,
    onPdfClick: (String, String) -> Unit = { _, _ -> },
    onDownloadClick: (String, String) -> Unit = { _, _ -> }
) {
  // Use helper function for file type display (moved out of composable for testability)
  val fileTypeWithNumber = formatMoodleFileTypeWithNumber(file.fileType, file.fileNumber)
  val courseName = file.courseName ?: Localization.t("moodle_default_course_name")

  // Use theme colors from Color.kt
  val accent = MoodleOrange
  val cardBg = Color(0xFF141414)
  val iconBg = Color(0xFF2A2A2A)
  val textPrimary = Color.White
  val textSecondary = Color(0xCCFFFFFF)

  // Use dimensions from ConnectorsDimensions
  val cardCornerRadius = ConnectorsDimensions.CardCornerRadius
  val cardPadding = ConnectorsDimensions.CardPadding
  val cardBorderWidth = ConnectorsDimensions.CardBorderWidth

  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
    Text(
        text =
            Localization.t("moodle_file_intro")
                .replace("%1\$s", fileTypeWithNumber)
                .replace("%2\$s", courseName),
        style = MaterialTheme.typography.bodyLarge.copy(color = textPrimary),
        modifier = Modifier.padding(bottom = 8.dp))

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .border(
                    width = cardBorderWidth,
                    color = accent,
                    shape = RoundedCornerShape(cardCornerRadius))
                .testTag("moodle_file_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(cardCornerRadius)) {
          Row(
              modifier = Modifier.padding(cardPadding),
              verticalAlignment = Alignment.CenterVertically) {
                // Moodle logo with rounded corners to hide white background
                Box(
                    modifier =
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(cardBg)) {
                      Image(
                          painter = painterResource(id = R.drawable.moodle_logo),
                          contentDescription = Localization.t("moodle_logo_description"),
                          modifier = Modifier.fillMaxSize(),
                          contentScale = ContentScale.Fit)
                    }

                Spacer(modifier = Modifier.width(12.dp))

                // Texts and pill
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(
                          text = file.filename,
                          style =
                              MaterialTheme.typography.titleMedium.copy(
                                  color = textPrimary, fontWeight = FontWeight.SemiBold),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis)
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier =
                                    Modifier.border(
                                            cardBorderWidth,
                                            accent,
                                            RoundedCornerShape(
                                                ConnectorsDimensions.LogoCornerRadius))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                  Text(
                                      text = Localization.t("moodle_file_pdf_label"),
                                      style =
                                          MaterialTheme.typography.labelSmall.copy(
                                              color = accent, fontWeight = FontWeight.Bold))
                                }
                            Text(
                                text = fileTypeWithNumber,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(color = textSecondary))
                          }
                    }

                Spacer(modifier = Modifier.width(10.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                      Box(
                          modifier =
                              Modifier.size(40.dp)
                                  .clip(RoundedCornerShape(cardCornerRadius))
                                  .background(iconBg)
                                  .clickable { onPdfClick(file.url, file.filename) }
                                  .testTag("moodle_file_view_button"),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = Localization.t("moodle_file_view_action"),
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp))
                          }
                      Box(
                          modifier =
                              Modifier.size(40.dp)
                                  .clip(RoundedCornerShape(cardCornerRadius))
                                  .background(iconBg)
                                  .clickable { onDownloadClick(file.url, file.filename) }
                                  .testTag("moodle_file_download_button"),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = Localization.t("moodle_file_download_action"),
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp))
                          }
                    }
              }
        }
  }
}

@Composable
fun LeadingThinkingDot(color: Color) {
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
fun AudioPlaybackButton(
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

/** Downloads a PDF file from a URL using Android's DownloadManager. */
@SuppressLint("Range")
private fun downloadPdf(context: Context, url: String, filename: String) {
  try {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val request =
        DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Downloading PDF")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

    downloadManager.enqueue(request)
    Toast.makeText(
            context,
            Localization.t("moodle_file_download_started").replace("%1\$s", filename),
            Toast.LENGTH_SHORT)
        .show()
  } catch (e: Exception) {
    Toast.makeText(context, "Failed to download: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}
