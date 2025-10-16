package com.android.sample.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SettingsOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsPage(onBackClick: () -> Unit = {}, onSignOut: () -> Unit = {}) {
  val settingsOptions =
      listOf(
          SettingsOption(
              title = "Profile & Account",
              subtitle = "Manage your personal information",
              icon = Icons.Default.Person,
              iconColor = Color(0xFFFF6B35)),
          SettingsOption(
              title = "Notifications",
              subtitle = "Configure alert preferences",
              icon = Icons.Default.Notifications,
              iconColor = Color(0xFF2196F3)),
          SettingsOption(
              title = "Privacy & Security",
              subtitle = "Control your data and security",
              icon = Icons.Default.Star,
              iconColor = Color(0xFF4CAF50)),
          SettingsOption(
              title = "Appearance",
              subtitle = "Customize your experience",
              icon = Icons.Default.Star,
              iconColor = Color(0xFF9C27B0)),
          SettingsOption(
              title = "Connected Systems",
              subtitle = "Manage EPFL integrations",
              icon = Icons.Default.Star,
              iconColor = Color(0xFFFFB100)),
          SettingsOption(
              title = "Language & Region",
              subtitle = "Change language settings",
              icon = Icons.Default.Star,
              iconColor = Color(0xFFE91E63)),
          SettingsOption(
              title = "Help & Support",
              subtitle = "Get assistance and documentation",
              icon = Icons.Default.Star,
              iconColor = Color(0xFF00BCD4)))

  Column(
      modifier =
          Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White)
              }

              Spacer(modifier = Modifier.width(8.dp))

              Text(
                  text = "Settings",
                  color = Color.White,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f))
            }

        Spacer(modifier = Modifier.height(16.dp))

        // User Profile Card
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFC82828)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(20.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Box(
                        modifier =
                            Modifier.size(60.dp).clip(CircleShape).background(Color(0xFFE53E3E)),
                        contentAlignment = Alignment.Center) {
                          Text(
                              text = "S",
                              color = Color.White,
                              fontSize = 24.sp,
                              fontWeight = FontWeight.Bold)
                        }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                          text = "Student Name",
                          color = Color.White,
                          fontSize = 18.sp,
                          fontWeight = FontWeight.Bold)

                      Text(
                          text = "student.name@epfl.ch",
                          color = Color.White.copy(alpha = 0.8f),
                          fontSize = 14.sp)

                      Spacer(modifier = Modifier.height(8.dp))

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Link",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "EPFL Student • Section Informatique",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp)
                      }
                    }
                  }
            }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Options
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              settingsOptions.forEach { option ->
                SettingsOptionCard(option = option, onClick = option.onClick)
              }
            }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Surface(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onSignOut() },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C2C)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFFF6B35),
                        modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        color = Color(0xFFFF6B35),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }

        Spacer(modifier = Modifier.height(32.dp))

        // Footer
        Text(
            text = "Powered by APERTUS Swiss LLM - Secure & GDPR Compliant",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(modifier = Modifier.height(24.dp))

        // Extra bottom padding to ensure all content is scrollable
        Spacer(modifier = Modifier.height(40.dp))
      }
}

@Composable
private fun SettingsOptionCard(option: SettingsOption, onClick: () -> Unit) {
  Surface(
      modifier = Modifier.fillMaxWidth().clickable { onClick() },
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFF2C2C2C)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Icon
              Box(
                  modifier =
                      Modifier.size(40.dp)
                          .clip(CircleShape)
                          .background(option.iconColor.copy(alpha = 0.1f)),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = option.iconColor,
                        modifier = Modifier.size(20.dp))
                  }

              Spacer(modifier = Modifier.width(16.dp))

              // Text content
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium)
                Text(text = option.subtitle, color = Color.Gray, fontSize = 14.sp)
              }

              // Arrow
              Icon(
                  imageVector = Icons.Default.KeyboardArrowRight,
                  contentDescription = "Navigate",
                  tint = Color.Gray,
                  modifier = Modifier.size(20.dp))
            }
      }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SettingsPagePreview() {
  MaterialTheme { SettingsPage() }
}
