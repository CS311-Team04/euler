package com.android.sample.Chat

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    userBubbleBg: Color = Color(0xFF2B2B2B),
    userBubbleText: Color = Color.White,
    aiText: Color = Color(0xFFEDEDED),
    maxUserBubbleWidthFraction: Float = 0.78f,
    isSpeaking: Boolean = false,
    isLoadingSpeech: Boolean = false,
    onSpeakClick: ((ChatUIModel) -> Unit)? = null
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
      Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message.text,
            color = aiText,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth().padding(end = 36.dp).testTag("chat_ai_text"))

        if (onSpeakClick != null) {
          val buttonModifier =
              Modifier.align(Alignment.BottomEnd).testTag("chat_ai_voice_btn_${message.id}")

          if (isLoadingSpeech) {
            Surface(
                shape = CircleShape,
                color = Color(0x1AFFFFFF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = buttonModifier.size(28.dp)) {
                  Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(16.dp).testTag("chat_ai_voice_btn_${message.id}_loading"),
                        strokeWidth = 2.dp,
                        color = Color.Gray)
                  }
                }
          } else {
            Surface(
                onClick = { onSpeakClick(message) },
                shape = CircleShape,
                color = if (isSpeaking) Color(0xFFE53935) else Color(0x1AFFFFFF),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = buttonModifier.size(28.dp)) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector =
                            if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription =
                            if (isSpeaking) "Arrêter la lecture" else "Lire le message",
                        tint = if (isSpeaking) Color.White else Color.LightGray,
                        modifier = Modifier.size(16.dp))
                  }
                }
          }
        }
      }
    }
  }
}
