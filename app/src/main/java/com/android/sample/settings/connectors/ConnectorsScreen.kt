package com.android.sample.settings.connectors

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.settings.AppSettings
import com.android.sample.settings.AppearanceMode
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorsDimensions as Dimens
import com.android.sample.ui.theme.ConnectorsLightSurface
import com.android.sample.ui.theme.DarkSurface
import com.android.sample.ui.theme.containerColor
import com.android.sample.ui.theme.previewBgColor

/** Data class representing a connector with its connection status. */
data class Connector(
    val id: String,
    val name: String,
    val description: String,
    val isConnected: Boolean = false
)

/** Mock data for connectors. In the future, this will come from a ViewModel or repository. */
private val mockConnectors =
    listOf(
        Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = false),
        Connector(id = "ed", name = "Ed", description = "Q&A platform", isConnected = false),
        Connector(
            id = "epfl_campus",
            name = "EPFL Campus",
            description = "EPFL services",
            isConnected = false),
        Connector(
            id = "is_academia",
            name = "IS Academia",
            description = "Pers. services",
            isConnected = false))

/**
 * Premium Connectors Screen - Apple/Linear style design
 *
 * Design specifications:
 * - Background: #0D0D0D
 * - Glassmorphism cards: rgba(255,255,255,0.03) with 1dp border rgba(255,255,255,0.07)
 * - Rounded corners: 22dp
 * - Typography: Inter-style (SemiBold 18sp title, Normal 13sp subtitle 70% opacity)
 * - Icons: Minimalist logos, white 80% opacity, 30-32dp
 * - Status chips: Rounded 100dp with bullet point for connected state
 */
@Composable
fun ConnectorsScreen(onBackClick: () -> Unit = {}, onConnectorClick: (String) -> Unit = {}) {

  // Local state to simulate connection status (mock data)
  var connectors by remember { mutableStateOf(mockConnectors) }

  // State for disconnect confirmation dialog
  var connectorToDisconnect by remember { mutableStateOf<Connector?>(null) }

  // Use AppSettings.appearanceMode to determine dark mode (same logic as Theme.kt)
  val appearanceMode by AppSettings.appearanceState
  val isSystemDark = isSystemInDarkTheme()
  val isDark =
      when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemDark
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
      }

  val colors = ConnectorsTheme.colors(isDark)

  Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Premium top bar
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(
                      horizontal = Dimens.ScreenHorizontalPadding,
                      vertical = Dimens.ScreenTopPadding),
          verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(Dimens.TopBarIconButtonSize),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = containerColor, contentColor = colors.textPrimary)) {
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                      contentDescription = Localization.t("close"),
                      modifier = Modifier.size(Dimens.TopBarIconSize),
                      tint = colors.textPrimary)
                }
            Spacer(modifier = Modifier.width(Dimens.TopBarSpacerWidth))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                  text = Localization.t("connectors"),
                  color = colors.textPrimary,
                  fontSize = Dimens.TopBarTitleFontSize,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = Dimens.TopBarTitleLetterSpacing)
              Spacer(modifier = Modifier.height(Dimens.TopBarSubtitleSpacer))
              Text(
                  text = "Connect your academic services",
                  color = colors.textSecondary,
                  fontSize = Dimens.TopBarSubtitleFontSize,
                  fontWeight = FontWeight.Normal)
            }
          }

      Spacer(modifier = Modifier.height(Dimens.ScreenContentSpacing))

      // Premium grid - 2x2 with exact spacing (18dp)
      LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenHorizontalPadding),
          horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
          verticalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
          contentPadding = PaddingValues(bottom = Dimens.GridBottomPadding)) {
            items(connectors) { connector ->
              ConnectorCard(
                  connector = connector,
                  onConnectClick = {
                    if (connector.isConnected) {
                      // Show confirmation dialog before disconnecting
                      connectorToDisconnect = connector
                    } else {
                      // Connect directly (no confirmation needed)
                      connectors =
                          connectors.map {
                            if (it.id == connector.id) {
                              it.copy(isConnected = true)
                            } else {
                              it
                            }
                          }
                      onConnectorClick(connector.id)
                    }
                  },
                  colors = colors,
                  isDark = isDark)
            }
          }
    }

    // Footer
    Text(
        text = Localization.t("by_epfl"),
        color = colors.textSecondary50,
        fontSize = Dimens.FooterFontSize,
        fontWeight = FontWeight.Light,
        letterSpacing = Dimens.FooterLetterSpacing,
        modifier =
            Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.ScreenBottomPadding),
        textAlign = TextAlign.Center)

    // Disconnect confirmation dialog
    connectorToDisconnect?.let { connector ->
      DisconnectConfirmationDialog(
          connectorName = connector.name,
          colors = colors,
          isDark = isDark,
          onConfirm = {
            connectors =
                connectors.map {
                  if (it.id == connector.id) {
                    it.copy(isConnected = false)
                  } else {
                    it
                  }
                }
            onConnectorClick(connector.id)
            connectorToDisconnect = null
          },
          onDismiss = { connectorToDisconnect = null })
    }
  }
}

/** Clean and minimal disconnect confirmation dialog */
@Composable
private fun DisconnectConfirmationDialog(
    connectorName: String,
    colors: ConnectorsColors,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  val surfaceColor = if (isDark) DarkSurface else ConnectorsLightSurface

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            text = Localization.t("disconnect_confirm_title"),
            color = colors.textPrimary,
            fontSize = Dimens.DialogTitleFontSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
      },
      text = {
        Text(
            text = Localization.t("disconnect_confirm_message").replace("%s", connectorName),
            color = colors.textSecondary,
            fontSize = Dimens.DialogTextFontSize,
            textAlign = TextAlign.Center,
            lineHeight = Dimens.DialogTextLineHeight)
      },
      confirmButton = {
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
            shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
              Text(
                  Localization.t("disconnect"),
                  color = colors.onPrimaryColor,
                  fontSize = Dimens.DialogTextFontSize,
                  fontWeight = FontWeight.SemiBold)
            }
      },
      dismissButton = {
        TextButton(
            onClick = onDismiss, shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
              Text(
                  Localization.t("cancel"),
                  color = colors.textPrimary,
                  fontSize = Dimens.DialogTextFontSize,
                  fontWeight = FontWeight.Medium)
            }
      },
      containerColor = surfaceColor,
      shape = RoundedCornerShape(Dimens.DialogCornerRadius))
}

@Preview(showBackground = true, backgroundColor = previewBgColor)
@Composable
fun ConnectorsScreenPreview() {
  MaterialTheme { ConnectorsScreen() }
}
