package com.android.sample.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.components.GuestProfileWarningModal

@Composable
fun SettingsPage(
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onProfileDisabledClick: () -> Unit = {},
    isProfileEnabled: Boolean = true,
    showProfileWarning: Boolean = false,
    onDismissProfileWarning: () -> Unit = {},
    onConnectorsClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
) {
  val background = Color(0xFF121212)
  val rowBackground = background
  val textPrimary = Color(0xFFECECEC)
  val textSecondary = Color(0xFF9E9E9E)
  val accentRed = Color(0xFFEB5757)
  val outline = Color.White.copy(alpha = 0.08f)

  // Bind to global AppSettings so changes update the whole app
  val language = remember { mutableStateOf(AppSettings.language) }

  Box(modifier = Modifier.fillMaxSize().background(background)) {
    Column(
        modifier =
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 48.dp)) {
          // Top bar
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                  Icon(
                      imageVector = Icons.Filled.Close,
                      contentDescription = Localization.t("close"),
                      tint = textPrimary)
                }
                Text(
                    text = Localization.t("settings_title"),
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center)
                IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
                  Icon(
                      imageVector = Icons.Filled.Info,
                      contentDescription = Localization.t("info"),
                      tint = textPrimary)
                }
              }

          Spacer(modifier = Modifier.height(8.dp))

          // Settings list
          Column(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsRow(
                    icon = Icons.Filled.Person,
                    title = Localization.t("profile"),
                    onClick = onProfileClick,
                    enabled = isProfileEnabled,
                    onDisabledClick = onProfileDisabledClick,
                    backgroundColor = rowBackground,
                    textColor = textPrimary,
                    secondaryTextColor = textSecondary)

                SettingsRow(
                    icon = Icons.Filled.Extension,
                    title = Localization.t("connectors"),
                    onClick = onConnectorsClick,
                    backgroundColor = rowBackground,
                    textColor = textPrimary,
                    secondaryTextColor = textSecondary)

                // Speech language with dropdown
                val languageMenuExpanded = remember { mutableStateOf(false) }
                Box {
                  SettingsRow(
                      icon = Icons.Filled.Language,
                      title = Localization.t("speech_language"),
                      backgroundColor = rowBackground,
                      textColor = textPrimary,
                      secondaryTextColor = textSecondary,
                      trailing = {
                        TrailingValue(
                            value = language.value.code,
                            textColor = textSecondary,
                            onClick = { languageMenuExpanded.value = true })
                      },
                      onClick = { languageMenuExpanded.value = true })

                  DropdownMenu(
                      expanded = languageMenuExpanded.value,
                      onDismissRequest = { languageMenuExpanded.value = false },
                      containerColor = rowBackground,
                  ) {
                    Language.entries.forEach { option ->
                      DropdownMenuItem(
                          text = { Text(option.code, color = textPrimary) },
                          onClick = {
                            language.value = option
                            AppSettings.setLanguage(option)
                            languageMenuExpanded.value = false
                          })
                    }
                  }
                }

                SectionDivider()

                // Log out row
                Surface(
                    modifier =
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                          onSignOut()
                        },
                    color = rowBackground) {
                      Row(
                          modifier = Modifier.fillMaxWidth().padding(16.dp),
                          verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "Log out",
                                tint = accentRed,
                                modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Localization.t("log_out"),
                                color = accentRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium)
                          }
                    }
              }
        }

    Text(
        text = Localization.t("by_epfl"),
        color = textSecondary,
        fontSize = 12.sp,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        textAlign = TextAlign.Center)

    if (showProfileWarning) {
      GuestProfileWarningModal(
          onContinueAsGuest = onDismissProfileWarning, onLogin = onProfileDisabledClick)
    }
  }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    backgroundColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onDisabledClick: (() -> Unit)? = null
) {
  Surface(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .alpha(if (enabled) 1f else 0.4f)
              .then(
                  when {
                    enabled -> Modifier.clickable { onClick() }
                    onDisabledClick != null -> Modifier.clickable { onDisabledClick() }
                    else -> Modifier
                  }),
      color = backgroundColor) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              Box(
                  modifier =
                      Modifier.size(36.dp)
                          .clip(CircleShape)
                          .background(Color.White.copy(alpha = 0.06f)),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = textColor,
                        modifier = Modifier.size(20.dp))
                  }
              Spacer(modifier = Modifier.width(14.dp))
              Text(
                  text = title, color = textColor, fontSize = 16.sp, modifier = Modifier.weight(1f))
              if (trailing != null) {
                trailing()
              } else {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = secondaryTextColor,
                    modifier = Modifier.size(20.dp))
              }
            }
      }
}

@Composable
private fun TrailingValue(value: String, textColor: Color, onClick: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
    Text(text = value, color = textColor, fontSize = 14.sp)
    Spacer(modifier = Modifier.width(6.dp))
    Icon(
        imageVector = Icons.Filled.ExpandMore,
        contentDescription = null,
        tint = textColor.copy(alpha = 0.9f),
        modifier = Modifier.size(18.dp))
  }
}

@Composable
private fun SectionDivider() {
  Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.0f))
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SettingsPagePreview() {
  MaterialTheme { SettingsPage() }
}
