package com.android.sample.settings.connectors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.settings.Localization

/** Premium connector card with glassmorphism effect */
@Composable
fun ConnectorCard(
    connector: Connector,
    onConnectClick: () -> Unit,
    colors: ConnectorsColors,
    isDark: Boolean
) {

  Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.95f)) {
    Surface(
        onClick = onConnectClick,
        modifier =
            Modifier.fillMaxSize()
                .shadow(
                    elevation = if (isDark) 0.dp else 2.dp, // Add elevation in light mode
                    shape = RoundedCornerShape(22.dp),
                    spotColor = colors.shadowSpot,
                    ambientColor = colors.shadowAmbient),
        shape = RoundedCornerShape(22.dp),
        color = colors.glassBackground,
        border = BorderStroke(width = 1.dp, color = colors.glassBorder)) {
          Column(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              verticalArrangement = Arrangement.spacedBy(0.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo petit et compact
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center) {
                      ConnectorLogo(connectorId = connector.id, isConnected = connector.isConnected)
                    }

                Spacer(modifier = Modifier.height(22.dp))

                // Title - Inter-style: SemiBold 18sp
                Text(
                    text = connector.name,
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle - Normal 13sp, 70% opacity
                Text(
                    text = connector.description,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp))

                Spacer(modifier = Modifier.weight(1f))

                // Status text - Connected/Not connected
                Text(
                    text =
                        if (connector.isConnected) {
                          Localization.t("connected")
                        } else {
                          Localization.t("not_connected")
                        },
                    color = if (connector.isConnected) colors.connectedGreen else colors.accentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(16.dp))

                // Action button - premium style
                Surface(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color =
                        if (connector.isConnected) {
                          colors.connectedGreenBackground
                        } else {
                          colors.accentRed
                        },
                    contentColor =
                        if (connector.isConnected) colors.textPrimary else colors.onPrimaryColor) {
                      Box(
                          modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                          contentAlignment = Alignment.Center) {
                            Text(
                                text =
                                    if (connector.isConnected) {
                                      Localization.t("disconnect")
                                    } else {
                                      Localization.t("connect")
                                    },
                                color =
                                    if (connector.isConnected) colors.textPrimary
                                    else colors.onPrimaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.1.sp)
                          }
                    }
              }
        }
  }
}
