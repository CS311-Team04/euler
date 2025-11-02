package com.android.sample.Chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ChatMessage(
    message: ChatUIModel,
    modifier: Modifier = Modifier,
    userBubbleBg: Color = Color(0xFF2B2B2B),
    userBubbleText: Color = Color(0xFFFFFFFF),
    aiText: Color = Color(0xFFEDEDED),
    maxUserBubbleWidthFraction: Float = 0.78f,
) {
  val isUser = message.type == ChatType.USER

  if (isUser) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      Box(modifier = Modifier.fillMaxWidth(maxUserBubbleWidthFraction)) {
        Surface(
            color = userBubbleBg,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.testTag("chat_user_bubble")) {
              Text(
                  text = message.text,
                  color = userBubbleText,
                  style = MaterialTheme.typography.bodyMedium,
                  lineHeight = 18.sp,
                  textAlign = TextAlign.Start,
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(horizontal = 14.dp, vertical = 10.dp)
                          .testTag("chat_user_text"))
            }
      }
    }
  } else {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
      Text(
          text = message.text,
          color = aiText,
          style = MaterialTheme.typography.bodyMedium,
          lineHeight = 20.sp,
          modifier = Modifier.fillMaxWidth().testTag("chat_ai_text"))
    }
  }
}
