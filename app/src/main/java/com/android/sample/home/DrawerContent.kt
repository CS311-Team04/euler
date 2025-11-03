package com.android.sample.home

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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.android.sample.R

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
    onClose: () -> Unit = {}
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
              contentDescription = "Euler Logo",
              modifier = Modifier.height(30.dp).offset(x = 1.dp,y=5.dp),
              contentScale = ContentScale.Fit)
        }

        Spacer(modifier = Modifier.height(35.dp))

        // New chat
        Surface(
            color = Color.Transparent,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { /* TODO new chat */}
                    .padding(vertical = 12.dp)
                    .testTag(DrawerTags.NewChatRow)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center) {
                      Icon(Icons.Filled.Add, contentDescription = "New chat", tint = Color.White)
                    }
                Spacer(Modifier.width(12.dp))
                Text(
                    "New chat",
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
                Icon(Icons.Filled.Link, contentDescription = "Connectors", tint = Color(0xFFB0B0B0))
                Spacer(Modifier.width(12.dp))
                Text("Connectors", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFB0B0B0))
              }
            }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "RECENTS",
            color = Color(0xFF8A8A8A),
            fontSize = 12.sp,
            modifier = Modifier.testTag(DrawerTags.RecentsSection))
        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
          RecentRow(title = "CS220 Final Exam retrieval")
          RecentRow(title = "Linear Algebra help")
          RecentRow(title = "Project deadline query")
          RecentRow(title = "Course registration info")

          Surface(
              color = Color.Transparent,
              modifier =
                  Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {}
                      .padding(vertical = 4.dp)
                      .testTag(DrawerTags.ViewAllRow)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("View all chats", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                  Spacer(Modifier.weight(1f))
                  Icon(
                      Icons.Filled.KeyboardArrowRight,
                      contentDescription = null,
                      tint = Color(0xFFB0B0B0))
                }
              }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(color = Color(0x22FFFFFF), modifier = Modifier.fillMaxWidth().height(1.dp)) {}
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
          Box(
              modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
              contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
              }
          Spacer(Modifier.width(12.dp))
          Text(ui.userName.lowercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Normal)
          Spacer(Modifier.weight(1f))
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
              Text("Powered by Apertus", color = Color.Gray, fontSize = 12.sp)
            }
      }
}

@Composable
private fun RecentRow(title: String) {
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
    Text(title, color = Color.White, fontSize = 13.sp)
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
