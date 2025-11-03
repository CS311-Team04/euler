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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DrawerTags {
  const val Root = "drawer_root"
  const val CloseBtn = "drawer_close_btn"
  const val SettingsRow = "drawer_settings_row"
  const val SignOutRow = "drawer_signout_row"
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
        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Menu",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.weight(1f))
          Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "Close",
              tint = Color.White,
              modifier = Modifier.size(24.dp).clickable { onClose() }.testTag(DrawerTags.CloseBtn))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile card (embedded)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
            shape = RoundedCornerShape(14.dp)) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Box(
                        modifier =
                            Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center) {
                          Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                      Text("Student", color = Color.White, fontWeight = FontWeight.SemiBold)
                      Text("EPFL • 15,000 students", color = Color.Gray, fontSize = 12.sp)
                    }
                  }
            }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings row
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .testTag(DrawerTags.SettingsRow),
            color = Color.Transparent) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", color = Color.White, fontSize = 16.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected systems quick view (compact chips)
        Text("Connected systems", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Chip(label = "Moodle", color = Color(0xFFE6B422)) { onToggleSystem("moodle") }
          Chip(label = "IS-Academia", color = Color(0xFF5386E4)) { onToggleSystem("is-academia") }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign out row (red)
        Surface(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSignOut() }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .testTag(DrawerTags.SignOutRow),
            color = Color.Transparent) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ExitToApp, contentDescription = "Log out", tint = Color(0xFFE53935))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Log out", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
}

@Composable
private fun Chip(label: String, color: Color, onClick: () -> Unit) {
  Surface(
      color = Color(0xFF1B1B1B), shape = RoundedCornerShape(20.dp), modifier = Modifier.clickable { onClick() }) {
    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(10.dp).clip(CircleShape).background(color))
      Spacer(Modifier.width(6.dp))
      Text(label, color = Color.White, fontSize = 12.sp)
    }
  }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
