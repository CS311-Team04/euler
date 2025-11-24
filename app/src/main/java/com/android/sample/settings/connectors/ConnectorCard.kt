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
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorsDimensions as Dimens

/** Premium connector card with glassmorphism effect */
@Composable
fun ConnectorCard(
    connector: Connector,
    onConnectClick: () -> Unit,
    colors: ConnectorsColors,
    isDark: Boolean
) {

  Box(modifier = Modifier.fillMaxWidth().aspectRatio(Dimens.CardAspectRatio)) {
    Surface(
        onClick = onConnectClick,
        modifier =
            Modifier.fillMaxSize()
                .shadow(
                    elevation = if (isDark) Dimens.CardElevationDark else Dimens.CardElevationLight,
                    shape = RoundedCornerShape(Dimens.CardCornerRadius),
                    spotColor = colors.shadowSpot,
                    ambientColor = colors.shadowAmbient),
        shape = RoundedCornerShape(Dimens.CardCornerRadius),
        color = colors.glassBackground,
        border = BorderStroke(width = Dimens.CardBorderWidth, color = colors.glassBorder)) {
          Column(
              modifier = Modifier.fillMaxSize().padding(Dimens.CardPadding),
              verticalArrangement = Arrangement.spacedBy(Dimens.CardContentSpacing),
              horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo petit et compact
                Box(
                    modifier = Modifier.fillMaxWidth().height(Dimens.CardLogoHeight),
                    contentAlignment = Alignment.Center) {
                      ConnectorLogo(connectorId = connector.id, isConnected = connector.isConnected)
                    }

                Spacer(modifier = Modifier.height(Dimens.CardLogoSpacer))

                // Title - Inter-style: SemiBold 18sp
                Text(
                    text = connector.name,
                    color = colors.textPrimary,
                    fontSize = Dimens.CardTitleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = Dimens.CardTitleLetterSpacing,
                    maxLines = 1,
                    textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(Dimens.CardTitleSubtitleSpacer))

                // Subtitle - Normal 13sp, 70% opacity
                Text(
                    text = connector.description,
                    color = colors.textSecondary,
                    fontSize = Dimens.CardSubtitleFontSize,
                    lineHeight = Dimens.CardSubtitleLineHeight,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Dimens.CardSubtitlePadding))

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
                    fontSize = Dimens.CardStatusFontSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(Dimens.CardStatusButtonSpacer))

                // Action button - premium style
                Surface(
                    onClick = onConnectClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.ButtonCornerRadius),
                    color =
                        if (connector.isConnected) {
                          colors.connectedGreenBackground
                        } else {
                          colors.accentRed
                        },
                    contentColor =
                        if (connector.isConnected) colors.textPrimary else colors.onPrimaryColor) {
                      Box(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(vertical = Dimens.ButtonVerticalPadding),
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
                                fontSize = Dimens.ButtonTextFontSize,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = Dimens.ButtonTextLetterSpacing)
                          }
                    }
              }
        }
  }
}
