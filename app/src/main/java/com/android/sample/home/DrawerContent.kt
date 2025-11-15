package com.android.sample.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.settings.Localization
import java.util.Locale

object DrawerTags {
  const val Root = "drawer_root"
  const val NewChatRow = "drawer_newchat_row"
  const val ConnectorsRow = "drawer_connectors_row"
  const val RecentsSection = "drawer_recents"
  const val ViewAllRow = "drawer_view_all"
  const val UserSettings = "drawer_user_settings"
}

@Composable
fun DrawerContentPreviewable() {
  DrawerContent()
}

@Composable
fun DrawerContent(
    ui: HomeUiState = HomeUiState(),
    onToggleSystem: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onProfileDisabledClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onPickConversation: (String) -> Unit = {}
) {
  Column(
      modifier =
          Modifier.fillMaxHeight()
              .width(300.dp)
              .background(Color(0xFF121212))
              .padding(horizontal = 20.dp, vertical = 16.dp)
              .testTag(DrawerTags.Root)) {
        // Logo header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Image(
              painter = painterResource(id = R.drawable.euler_logo),
              contentDescription = Localization.t("euler_logo"),
              modifier = Modifier.height(30.dp).offset(x = 1.dp, y = 5.dp),
              contentScale = ContentScale.Fit)
        }

        Spacer(modifier = Modifier.height(35.dp))

        // New chat
        Surface(
            color = Color.Transparent,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNewChat() }
                    .padding(vertical = 12.dp)
                    .testTag(DrawerTags.NewChatRow)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center) {
                      Icon(
                          Icons.Filled.Add,
                          contentDescription = Localization.t("new_chat"),
                          tint = Color.White)
                    }
                Spacer(Modifier.width(12.dp))
                Text(
                    Localization.t("new_chat"),
                    color = Color(0xFFFF6E6E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal)
              }
            }

        Spacer(modifier = Modifier.height(12.dp))

        // Connectors row
        Surface(
            color = Color.Transparent,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .padding(vertical = 12.dp)
                    .testTag(DrawerTags.ConnectorsRow)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = Localization.t("connectors"),
                    tint = Color(0xFFB0B0B0))
                Spacer(Modifier.width(12.dp))
                Text(
                    Localization.t("connectors"),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFB0B0B0))
              }
            }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            Localization.t("recents"),
            color = Color(0xFF8A8A8A),
            fontSize = 12.sp,
            modifier = Modifier.testTag(DrawerTags.RecentsSection))
        Spacer(modifier = Modifier.height(14.dp))

        if (ui.conversations.isEmpty()) {
          Text("No conversations yet", color = Color.Gray, fontSize = 13.sp)
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ui.conversations.take(12).forEach { conv ->
              RecentRow(
                  title = conv.title.ifBlank { "Untitled" },
                  selected = conv.id == ui.currentConversationId,
                  onClick = { onPickConversation(conv.id) })
            }

            Surface(
                color = Color.Transparent,
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* navigate to “All chats” screen later */}
                        .padding(vertical = 4.dp)
                        .testTag(DrawerTags.ViewAllRow)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "View all chats",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB0B0B0))
                  }
                }
          }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(color = Color(0x22FFFFFF), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
        Spacer(Modifier.height(12.dp))
        val displayName = formatUserName(ui.userName)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().alpha(if (ui.isGuest) 0.4f else 1f)) {
              Box(
                  modifier =
                      Modifier.size(36.dp)
                          .clip(CircleShape)
                          .background(Color(0xFF2A2A2A))
                          .clickable {
                            if (ui.isGuest) {
                              onProfileDisabledClick()
                            } else {
                              onProfileClick()
                            }
                          },
                  contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                  }
              Spacer(Modifier.width(12.dp))
              Text(
                  displayName,
                  color = Color.White,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Normal,
                  modifier =
                      Modifier.weight(1f).clickable {
                        if (ui.isGuest) {
                          onProfileDisabledClick()
                        } else {
                          onProfileClick()
                        }
                      })
              Icon(
                  Icons.Filled.Settings,
                  contentDescription = "Settings",
                  tint = Color.White,
                  modifier =
                      Modifier.size(20.dp)
                          .clickable { onSettingsClick() }
                          .testTag(DrawerTags.UserSettings))
            }
        Spacer(Modifier.height(12.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {
              Text(Localization.t("powered_by"), color = Color.Gray, fontSize = 12.sp)
            }
      }
}

private fun formatUserName(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isEmpty()) return "Student"
  return trimmed.split("\\s+".toRegex()).joinToString(" ") { word ->
    word.replaceFirstChar { ch ->
      if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }
  }
}

@Composable
private fun RecentRow(title: String, selected: Boolean = false, onClick: () -> Unit = {}) {
  val bg = if (selected) Color(0x22FFFFFF) else Color.Transparent
  Surface(
      color = bg,
      shape = RoundedCornerShape(8.dp),
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onClick() }
              .padding(vertical = 6.dp, horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2A2A2A)),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = Color(0xFF8A8A8A),
                    modifier = Modifier.size(15.dp))
              }
          Spacer(Modifier.width(12.dp))
          Text(
              text = title,
              color = Color.White,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
      }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
