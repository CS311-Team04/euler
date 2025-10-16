package com.android.sample.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
              .width(280.dp)
              .background(Color(0xFF1E1E1E))
              .padding(horizontal = 20.dp, vertical = 24.dp)) {
        // --- User Profile Section ---
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.Red),
              contentAlignment = Alignment.Center) {
                Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
              }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Student", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("EPFL • 15,000 students", color = Color.Gray, fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Out Button
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(45.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
            shape = RoundedCornerShape(10.dp)) {
              Text("Sign Out", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Connected Systems Section ---
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              Icons.Default.Star,
              contentDescription = "Connected Systems",
              tint = Color.White,
              modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              "CONNECTED SYSTEMS",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connected Systems Grid (2x3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Row 1
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()) {
                SystemCard("Moodle", Color(0xFFE6B422), isConnected = true) {
                  onToggleSystem("moodle")
                }
                SystemCard("IS-Academia", Color(0xFF5386E4), isConnected = true) {
                  onToggleSystem("is-academia")
                }
                SystemCard("Ed-Discussion", Color(0xFF9C27B0), isConnected = true) {
                  onToggleSystem("ed-discussion")
                }
              }
          // Row 2
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()) {
                SystemCard("Library", Color(0xFF4CAF50), isConnected = true) {
                  onToggleSystem("library")
                }
                SystemCard("Drive EPFL", Color(0xFFBA68C8), isConnected = true) {
                  onToggleSystem("drive-epfl")
                }
                SystemCard("Associations", Color(0xFF26A69A), isConnected = true) {
                  onToggleSystem("associations")
                }
              }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Recent Actions Section ---
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              Icons.Default.Refresh,
              contentDescription = "Recent Actions",
              tint = Color.White,
              modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              "RECENT ACTIONS",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recent Actions List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ActionCard("Posted question on Ed Discussion", "Ed", "30m ago")
          ActionCard("Retrieved exam papers from Drive", "Drive EPFL", "1h ago")
          ActionCard("Checked course deadlines", "IS-Academia", "5h ago")
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Settings Section ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth().clickable { onSettingsClick() }.padding(vertical = 8.dp)) {
              Icon(
                  Icons.Default.Settings,
                  contentDescription = "Settings",
                  tint = Color.White,
                  modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text("Settings", color = Color.White, fontSize = 16.sp)
            }
      }
}

@Composable
private fun SystemCard(
    label: String,
    color: Color,
    isConnected: Boolean = true,
    onClick: () -> Unit = {}
) {
  Surface(
      modifier = Modifier.width(78.dp).height(68.dp).clickable { onClick() },
      shape = RoundedCornerShape(10.dp),
      color = Color(0xFF2A2A2A)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Box(
                  modifier = Modifier.size(22.dp).clip(CircleShape).background(color),
                  contentAlignment = Alignment.Center) {}

              Spacer(modifier = Modifier.height(5.dp))

              Text(
                  text = label,
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1)
            }
      }
}

@Composable
private fun ActionCard(title: String, tag: String, time: String) {
  Surface(
      shape = RoundedCornerShape(10.dp),
      color = Color(0xFF121212),
      modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(title, color = Color.White, fontSize = 14.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color.Red) {
                  Text(
                      tag,
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Text(time, color = Color.Gray, fontSize = 12.sp)
              }
        }
      }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
