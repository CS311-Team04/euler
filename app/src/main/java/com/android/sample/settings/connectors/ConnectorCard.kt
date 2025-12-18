package com.android.sample.settings.connectors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorsDimensions as Dimens

/** Test tags for connector cards. */
object ConnectorCardTags {
  fun card(connectorId: String): String = "connector_card_$connectorId"
}

/** Logo section of the connector card. */
@Composable
private fun ConnectorCardLogo(connectorId: String, isConnected: Boolean) {
  Box(
      modifier = Modifier.fillMaxWidth().height(Dimens.CardLogoHeight),
      contentAlignment = Alignment.Center) {
        ConnectorLogo(connectorId = connectorId, isConnected = isConnected)
      }
}

/** Title section of the connector card. */
@Composable
private fun ConnectorCardTitle(name: String, colors: ConnectorsColors) {
  Text(
      text = name,
      color = colors.textPrimary,
      fontSize = Dimens.CardTitleFontSize,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = Dimens.CardTitleLetterSpacing,
      maxLines = 1,
      textAlign = TextAlign.Center)
}

/** Subtitle section of the connector card. */
@Composable
private fun ConnectorCardSubtitle(description: String, colors: ConnectorsColors) {
  Text(
      text = description,
      color = colors.textSecondary,
      fontSize = Dimens.CardSubtitleFontSize,
      lineHeight = Dimens.CardSubtitleLineHeight,
      maxLines = 2,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = Dimens.CardSubtitlePadding))
}

/** Properties for connector card based on connection state. */
private data class ConnectorCardState(
    val statusText: String,
    val statusColor: Color,
    val buttonText: String,
    val buttonColor: Color,
    val buttonContentColor: Color
)

/** Computes connector card state properties based on connection status. */
private fun getConnectorCardState(
    isConnected: Boolean,
    colors: ConnectorsColors
): ConnectorCardState {
  return if (isConnected) {
    ConnectorCardState(
        statusText = Localization.t("connected"),
        statusColor = colors.connectedGreen,
        buttonText = Localization.t("disconnect"),
        buttonColor = colors.connectedGreenBackground,
        buttonContentColor = colors.textPrimary)
  } else {
    ConnectorCardState(
        statusText = Localization.t("not_connected"),
        statusColor = colors.accentRed,
        buttonText = Localization.t("connect"),
        buttonColor = colors.accentRed,
        buttonContentColor = colors.onPrimaryColor)
  }
}

/** Status text section of the connector card. */
@Composable
private fun ConnectorCardStatus(state: ConnectorCardState) {
  Text(
      text = state.statusText,
      color = state.statusColor,
      fontSize = Dimens.CardStatusFontSize,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center)
}

/** Action button section of the connector card. */
@Composable
private fun ConnectorCardActionButton(state: ConnectorCardState, onConnectClick: () -> Unit) {
  Surface(
      onClick = onConnectClick,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(Dimens.ButtonCornerRadius),
      color = state.buttonColor,
      contentColor = state.buttonContentColor) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.ButtonVerticalPadding),
            contentAlignment = Alignment.Center) {
              Text(
                  text = state.buttonText,
                  color = state.buttonContentColor,
                  fontSize = Dimens.ButtonTextFontSize,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = Dimens.ButtonTextLetterSpacing)
            }
      }
}

/** Computes the card elevation based on theme. */
private fun getCardElevation(isDark: Boolean): Dp {
  return if (isDark) Dimens.CardElevationDark else Dimens.CardElevationLight
}

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
                .testTag(ConnectorCardTags.card(connector.id))
                .shadow(
                    elevation = getCardElevation(isDark),
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
                ConnectorCardLogo(connectorId = connector.id, isConnected = connector.isConnected)

                Spacer(modifier = Modifier.height(Dimens.CardLogoSpacer))

                ConnectorCardTitle(name = connector.name, colors = colors)

                Spacer(modifier = Modifier.height(Dimens.CardTitleSubtitleSpacer))

                ConnectorCardSubtitle(description = connector.description, colors = colors)

                Spacer(modifier = Modifier.weight(1f))

                val cardState = getConnectorCardState(connector.isConnected, colors)
                ConnectorCardStatus(state = cardState)

                Spacer(modifier = Modifier.height(Dimens.CardStatusButtonSpacer))

                ConnectorCardActionButton(state = cardState, onConnectClick = onConnectClick)
              }
        }
  }
}
