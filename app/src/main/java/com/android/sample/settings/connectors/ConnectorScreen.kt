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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

/** State holder for connector screen theme and appearance logic */
data class ConnectorState(val isDark: Boolean, val colors: ConnectorsColors)

/** Remembers and manages connector screen state including theme logic. */
@Composable
fun rememberConnectorState(): ConnectorState {
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

  return remember(isDark) { ConnectorState(isDark = isDark, colors = colors) }
}

/** Top bar composable for Connectors screen. */
@Composable
private fun ConnectorsTopBar(onBackClick: () -> Unit, colors: ConnectorsColors) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(
                  horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ScreenTopPadding),
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
              text = Localization.t("Connect_your_academic_services"),
              color = colors.textSecondary,
              fontSize = Dimens.TopBarSubtitleFontSize,
              fontWeight = FontWeight.Normal)
        }
      }
}

/** Grid composable displaying connector cards. */
@Composable
private fun ConnectorsGrid(
    connectors: List<Connector>,
    colors: ConnectorsColors,
    isDark: Boolean,
    onConnectorClick: (Connector) -> Unit,
    onShowDisconnectConfirmation: (Connector) -> Unit
) {
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
                  onShowDisconnectConfirmation(connector)
                } else {
                  // Connect directly (no confirmation needed)
                  onConnectorClick(connector)
                }
              },
              colors = colors,
              isDark = isDark)
        }
      }
}

/** Footer composable for Connectors screen. */
@Composable
private fun ConnectorsFooter(colors: ConnectorsColors, modifier: Modifier = Modifier) {
  Text(
      text = Localization.t("by_epfl"),
      color = colors.textSecondary50,
      fontSize = Dimens.FooterFontSize,
      fontWeight = FontWeight.Light,
      letterSpacing = Dimens.FooterLetterSpacing,
      modifier = modifier.padding(bottom = Dimens.ScreenBottomPadding),
      textAlign = TextAlign.Center)
}

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
fun ConnectorsScreen(
    onBackClick: () -> Unit = {},
    onConnectorClick: (String) -> Unit = {},
    viewModel: ConnectorsViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val connectorState = rememberConnectorState()

  Box(modifier = Modifier.fillMaxSize().background(connectorState.colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      ConnectorsTopBar(onBackClick = onBackClick, colors = connectorState.colors)

      // Small indicator while testing/loading ED or Moodle status
      if (uiState.isLoadingEd || uiState.isLoadingMoodle) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenHorizontalPadding))
        Spacer(modifier = Modifier.height(Dimens.ScreenContentSpacing))
      } else {
        Spacer(modifier = Modifier.height(Dimens.ScreenContentSpacing))
      }

      // ED error message (e.g.: "Failed to load ED connector status")
      uiState.edError?.let { errorText ->
        Text(
            text = errorText,
            color = connectorState.colors.accentRed,
            fontSize = Dimens.CardStatusFontSize,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardStatusButtonSpacer))
      }

      // Moodle error message (e.g.: "Failed to load Moodle connector status")
      uiState.moodleError?.let { errorText ->
        Text(
            text = errorText,
            color = connectorState.colors.accentRed,
            fontSize = Dimens.CardStatusFontSize,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = Dimens.ScreenHorizontalPadding,
                        vertical = Dimens.CardStatusButtonSpacer))
      }

      ConnectorsGrid(
          connectors = uiState.connectors,
          colors = connectorState.colors,
          isDark = connectorState.isDark,
          onConnectorClick = { connector ->
            viewModel.connectConnector(connector.id)
            onConnectorClick(connector.id)
          },
          onShowDisconnectConfirmation = viewModel::showDisconnectConfirmation)
    }

    ConnectorsFooter(
        colors = connectorState.colors, modifier = Modifier.align(Alignment.BottomCenter))

    // Disconnect confirmation dialog (unchanged)
    uiState.pendingConnectorForDisconnect?.let { connector ->
      DisconnectConfirmationDialog(
          connectorName = connector.name,
          colors = connectorState.colors,
          isDark = connectorState.isDark,
          onConfirm = {
            viewModel.disconnectConnector(connector.id)
            onConnectorClick(connector.id)
          },
          onDismiss = { viewModel.dismissDisconnectConfirmation() })
    }

    if (uiState.isEdConnectDialogOpen) {
      EdConnectDialog(
          colors = connectorState.colors,
          isDark = connectorState.isDark,
          isLoading = uiState.isEdConnecting,
          error = uiState.edConnectError,
          onConfirm = { token, baseUrl -> viewModel.confirmEdConnect(token, baseUrl) },
          onDismiss = { viewModel.dismissEdConnectDialog() },
      )
    }

    if (uiState.isMoodleConnectDialogOpen) {
      MoodleConnectDialog(
          colors = connectorState.colors,
          isDark = connectorState.isDark,
          isLoading = uiState.isMoodleConnecting,
          error = uiState.moodleConnectError,
          onConfirm = { baseUrl, username, password ->
            viewModel.connectMoodleWithCredentials(baseUrl, username, password)
          },
          onDismiss = { viewModel.dismissMoodleConnectDialog() },
      )
    }
  }
}

/** Computes dialog surface color based on theme. */
internal fun getDialogSurfaceColor(isDark: Boolean) =
    if (isDark) DarkSurface else ConnectorsLightSurface

/** Reusable dialog title component. */
@Composable
internal fun DialogTitle(text: String, colors: ConnectorsColors) {
  Text(
      text = text,
      color = colors.textPrimary,
      fontSize = Dimens.DialogTitleFontSize,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth())
}

/** Reusable dialog dismiss button component. */
@Composable
internal fun DialogDismissButton(
    colors: ConnectorsColors,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
  TextButton(
      onClick = { if (!isLoading) onDismiss() },
      shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
        Text(
            Localization.t("cancel"),
            color = colors.textPrimary,
            fontSize = Dimens.DialogTextFontSize,
            fontWeight = FontWeight.Medium)
      }
}

/** Reusable dialog confirm button with loading indicator. */
@Composable
internal fun DialogConfirmButton(
    colors: ConnectorsColors,
    isLoading: Boolean,
    enabled: Boolean,
    buttonText: String,
    onConfirm: () -> Unit,
) {
  Button(
      onClick = onConfirm,
      enabled = enabled && !isLoading,
      shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(18.dp), strokeWidth = Dimens.CardBorderWidth)
        } else {
          Text(
              buttonText,
              color = colors.onPrimaryColor,
              fontSize = Dimens.DialogTextFontSize,
              fontWeight = FontWeight.SemiBold)
        }
      }
}

/** Reusable error text component for dialogs. */
@Composable
internal fun DialogErrorText(error: String?, colors: ConnectorsColors) {
  if (!error.isNullOrBlank()) {
    Text(
        text = error,
        color = colors.accentRed,
        fontSize = Dimens.DialogTextFontSize,
        textAlign = TextAlign.Start)
  }
}

/** Clean and minimal disconnect confirmation dialog */
@Composable
internal fun DisconnectConfirmationDialog(
    connectorName: String,
    colors: ConnectorsColors,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { DialogTitle(Localization.t("disconnect_confirm_title"), colors) },
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
      dismissButton = { DialogDismissButton(colors, isLoading = false, onDismiss) },
      containerColor = getDialogSurfaceColor(isDark),
      shape = RoundedCornerShape(Dimens.DialogCornerRadius))
}

/** Dialog for connecting to ED with API token and optional base URL. */
@Composable
internal fun EdConnectDialog(
    colors: ConnectorsColors,
    isDark: Boolean,
    isLoading: Boolean,
    error: String?,
    onConfirm: (apiToken: String, baseUrl: String?) -> Unit,
    onDismiss: () -> Unit,
) {
  var token by remember { mutableStateOf("") }
  var baseUrl by remember { mutableStateOf("") }

  AlertDialog(
      onDismissRequest = { if (!isLoading) onDismiss() },
      title = { DialogTitle(Localization.t("settings_connectors_ed_title"), colors) },
      text = {
        Column {
          OutlinedTextField(
              value = token,
              onValueChange = { token = it },
              label = { Text(Localization.t("settings_connectors_ed_api_token_label")) },
              singleLine = true,
              enabled = !isLoading,
              keyboardOptions =
                  KeyboardOptions(
                      autoCorrect = false,
                      keyboardType = KeyboardType.Text,
                      imeAction = ImeAction.Done),
          )
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
              value = baseUrl,
              onValueChange = { baseUrl = it },
              label = { Text(Localization.t("settings_connectors_ed_base_url_label")) },
              singleLine = true,
              enabled = !isLoading,
              keyboardOptions =
                  KeyboardOptions(
                      autoCorrect = false,
                      keyboardType = KeyboardType.Uri,
                      imeAction = ImeAction.Done),
          )

          if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            DialogErrorText(error, colors)
          }
        }
      },
      confirmButton = {
        DialogConfirmButton(
            colors = colors,
            isLoading = isLoading,
            enabled = token.isNotBlank(),
            buttonText = Localization.t("connect"),
            onConfirm = {
              val trimmedToken = token.trim()
              val trimmedBaseUrl = baseUrl.trim().ifBlank { null }
              onConfirm(trimmedToken, trimmedBaseUrl)
            })
      },
      dismissButton = { DialogDismissButton(colors, isLoading, onDismiss) },
      containerColor = getDialogSurfaceColor(isDark),
      shape = RoundedCornerShape(Dimens.DialogCornerRadius))
}

/** Simple dialog for Moodle connection with username/password form. */
@Composable
internal fun MoodleConnectDialog(
    colors: ConnectorsColors,
    isDark: Boolean,
    isLoading: Boolean,
    error: String?,
    onConfirm: (baseUrl: String, username: String, password: String) -> Unit,
    onDismiss: () -> Unit,
) {
  var baseUrl by remember { mutableStateOf("https://euler-swent.moodlecloud.com") }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  val isFormValid = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

  AlertDialog(
      onDismissRequest = { if (!isLoading) onDismiss() },
      title = { MoodleDialogTitle(colors) },
      text = {
        MoodleDialogContent(
            baseUrl = baseUrl,
            username = username,
            password = password,
            error = error,
            colors = colors,
            isLoading = isLoading,
            onBaseUrlChange = { baseUrl = it },
            onUsernameChange = { username = it },
            onPasswordChange = { password = it })
      },
      confirmButton = {
        MoodleDialogConfirmButton(
            colors = colors,
            isLoading = isLoading,
            isFormValid = isFormValid,
            onConfirm = { onConfirm(baseUrl.trim(), username.trim(), password.trim()) })
      },
      dismissButton = {
        MoodleDialogDismissButton(colors = colors, isLoading = isLoading, onDismiss = onDismiss)
      },
      containerColor = getDialogSurfaceColor(isDark),
      shape = RoundedCornerShape(Dimens.DialogCornerRadius))
}

@Composable
private fun MoodleDialogTitle(colors: ConnectorsColors) {
  DialogTitle(Localization.t("settings_connectors_moodle_title"), colors)
}

@Composable
private fun MoodleDialogContent(
    baseUrl: String,
    username: String,
    password: String,
    error: String?,
    colors: ConnectorsColors,
    isLoading: Boolean,
    onBaseUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    MoodleTextField(
        value = baseUrl,
        onValueChange = onBaseUrlChange,
        label = Localization.t("settings_connectors_moodle_base_url_label"),
        enabled = !isLoading)

    MoodleTextField(
        value = username,
        onValueChange = onUsernameChange,
        label = Localization.t("settings_connectors_moodle_username_label"),
        enabled = !isLoading)

    MoodlePasswordField(
        value = password,
        onValueChange = onPasswordChange,
        label = Localization.t("settings_connectors_moodle_password_label"),
        enabled = !isLoading)

    Text(
        text = Localization.t("settings_connectors_moodle_login_instructions"),
        color = colors.textSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 4.dp))

    DialogErrorText(error, colors)
  }
}

@Composable
private fun MoodleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      singleLine = true,
      enabled = enabled,
      keyboardOptions =
          KeyboardOptions(
              autoCorrect = false, keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MoodlePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      singleLine = true,
      enabled = enabled,
      visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
      keyboardOptions =
          KeyboardOptions(
              autoCorrect = false,
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done),
      modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MoodleDialogConfirmButton(
    colors: ConnectorsColors,
    isLoading: Boolean,
    isFormValid: Boolean,
    onConfirm: () -> Unit,
) {
  DialogConfirmButton(
      colors = colors,
      isLoading = isLoading,
      enabled = isFormValid,
      buttonText = Localization.t("connect"),
      onConfirm = onConfirm)
}

@Composable
private fun MoodleDialogDismissButton(
    colors: ConnectorsColors,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
  DialogDismissButton(colors, isLoading, onDismiss)
}

@Preview(showBackground = true, backgroundColor = previewBgColor)
@Composable
fun ConnectorsScreenPreview() {
  MaterialTheme { ConnectorsScreen() }
}
