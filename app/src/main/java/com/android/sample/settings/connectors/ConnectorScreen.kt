package com.android.sample.settings.connectors

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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

    // Moodle redirect loading overlay
    if (uiState.isMoodleRedirecting) {
      MoodleRedirectingOverlay()
    }
  }
}

/** Full-screen overlay shown while "redirecting" to Moodle. */
@Composable
private fun MoodleRedirectingOverlay() {
  Box(
      modifier =
          Modifier.fillMaxSize().background(com.android.sample.ui.theme.MoodleLoginBackground),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          // Moodle logo
          AsyncImage(
              model =
                  "https://res.cloudinary.com/dw5ba0va3/image/upload/v1759937020/Moodle_Logo_zwznns.png",
              contentDescription = "Moodle Logo",
              modifier = Modifier.height(80.dp).widthIn(max = 260.dp),
              contentScale = androidx.compose.ui.layout.ContentScale.Fit)

          Spacer(modifier = Modifier.height(Dimens.MoodleDialogLargeSpacing))

          // Loading indicator
          CircularProgressIndicator(
              modifier = Modifier.size(Dimens.MoodleLoadingIndicatorSize),
              strokeWidth = 3.dp,
              color = com.android.sample.ui.theme.MoodleLoginButtonBlue)

          Spacer(modifier = Modifier.height(16.dp))

          // "Connecting..." text
          Text(
              text = Localization.t("settings_connectors_moodle_redirecting"),
              color = com.android.sample.ui.theme.MoodleLoginPlaceholder,
              fontSize = 15.sp)
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

/**
 * Moodle-styled login dialog that mimics the official Moodle login page. Features a white card on
 * gray background with Moodle branding.
 */
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

  // Full-screen dialog with Moodle-style background
  androidx.compose.ui.window.Dialog(
      onDismissRequest = { if (!isLoading) onDismiss() },
      properties =
          androidx.compose.ui.window.DialogProperties(
              usePlatformDefaultWidth = false, dismissOnBackPress = !isLoading)) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(com.android.sample.ui.theme.MoodleLoginBackground)
                    .padding(horizontal = Dimens.MoodleDialogHorizontalPadding, vertical = 40.dp),
            contentAlignment = Alignment.Center) {
              // White card container with shadow
              Column(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              com.android.sample.ui.theme.MoodleLoginCardBackground,
                              RoundedCornerShape(12.dp))
                          .padding(horizontal = 28.dp, vertical = 40.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Moodle Logo - Large and prominent
                    MoodleLogoHeader()

                    Spacer(modifier = Modifier.height(Dimens.MoodleDialogLargeSpacing))

                    // "Connexion" title - Moodle's serif-style typography
                    Text(
                        text = Localization.t("settings_connectors_moodle_login_title"),
                        color = com.android.sample.ui.theme.MoodleLoginTitleBlue,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        modifier = Modifier.align(Alignment.Start))

                    Spacer(modifier = Modifier.height(28.dp))

                    // Username field - Taller with more padding
                    MoodleStyledTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = Localization.t("settings_connectors_moodle_username_label"),
                        enabled = !isLoading,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next)

                    Spacer(modifier = Modifier.height(Dimens.MoodleDialogFieldSpacing))

                    // Password field - Taller with more padding
                    MoodleStyledPasswordField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = Localization.t("settings_connectors_moodle_password_label"),
                        enabled = !isLoading)

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Forgot password?" link - Right aligned
                    Text(
                        text = Localization.t("settings_connectors_moodle_forgot_password"),
                        color = com.android.sample.ui.theme.MoodleLoginLink,
                        fontSize = 15.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.align(Alignment.End).padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(Dimens.MoodleDialogFieldSpacing))

                    // Error message
                    if (!error.isNullOrBlank()) {
                      Text(
                          text = error,
                          color = com.android.sample.ui.theme.MoodleLoginError,
                          fontSize = 15.sp,
                          textAlign = TextAlign.Center,
                          lineHeight = 22.sp,
                          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                    }

                    // Connect button - Full width, Moodle blue
                    MoodleLoginButton(
                        text = Localization.t("settings_connectors_moodle_login_button"),
                        isLoading = isLoading,
                        enabled = isFormValid && !isLoading,
                        onClick = { onConfirm(baseUrl.trim(), username.trim(), password.trim()) })

                    Spacer(modifier = Modifier.height(Dimens.MoodleDialogFieldSpacing))

                    // Cancel button
                    TextButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier.fillMaxWidth()) {
                          Text(
                              text = Localization.t("cancel"),
                              color = com.android.sample.ui.theme.MoodleLoginPlaceholder,
                              fontSize = 15.sp,
                              fontWeight = FontWeight.Medium)
                        }
                  }
            }
      }
}

/** Moodle logo header - Large and prominent like the official Moodle login page. */
@Composable
private fun MoodleLogoHeader() {
  AsyncImage(
      model =
          "https://res.cloudinary.com/dw5ba0va3/image/upload/v1759937020/Moodle_Logo_zwznns.png",
      contentDescription = "Moodle Logo",
      modifier = Modifier.height(100.dp).widthIn(max = 320.dp),
      contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}

/** Returns the Moodle-styled colors for OutlinedTextField. */
@Composable
private fun moodleTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = com.android.sample.ui.theme.MoodleLoginInputBorderFocused,
        unfocusedBorderColor = com.android.sample.ui.theme.MoodleLoginInputBorder,
        focusedContainerColor = com.android.sample.ui.theme.MoodleLoginCardBackground,
        unfocusedContainerColor = com.android.sample.ui.theme.MoodleLoginCardBackground,
        cursorColor = com.android.sample.ui.theme.MoodleLoginButtonBlue,
        focusedTextColor = com.android.sample.ui.theme.MoodleLoginText,
        unfocusedTextColor = com.android.sample.ui.theme.MoodleLoginText)

/** Moodle-styled text field with clean borders and generous padding. */
@Composable
private fun MoodleStyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
            text = placeholder,
            color = com.android.sample.ui.theme.MoodleLoginPlaceholder,
            fontSize = 17.sp)
      },
      singleLine = true,
      enabled = enabled,
      textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
      keyboardOptions =
          KeyboardOptions(
              capitalization = KeyboardCapitalization.Unspecified,
              autoCorrectEnabled = false,
              keyboardType = keyboardType,
              imeAction = imeAction),
      colors = moodleTextFieldColors(),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().height(64.dp))
}

/** Moodle-styled password field with clean borders and generous padding. */
@Composable
private fun MoodleStyledPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
            text = placeholder,
            color = com.android.sample.ui.theme.MoodleLoginPlaceholder,
            fontSize = 17.sp)
      },
      singleLine = true,
      enabled = enabled,
      textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
      visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
      keyboardOptions =
          KeyboardOptions(
              autoCorrect = false,
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done),
      colors = moodleTextFieldColors(),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().height(64.dp))
}

/** Moodle-styled blue login button - Full width with proper height. */
@Composable
private fun MoodleLoginButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      enabled = enabled,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = com.android.sample.ui.theme.MoodleLoginButtonBlue,
              disabledContainerColor =
                  com.android.sample.ui.theme.MoodleLoginButtonBlue.copy(alpha = 0.6f)),
      shape = RoundedCornerShape(6.dp),
      modifier = Modifier.fillMaxWidth().height(56.dp)) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              strokeWidth = 2.dp,
              color = androidx.compose.ui.graphics.Color.White)
        } else {
          Text(
              text = text,
              color = ConnectorsLightSurface,
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium)
        }
      }
}

@Preview(showBackground = true, backgroundColor = previewBgColor)
@Composable
fun ConnectorsScreenPreview() {
  MaterialTheme { ConnectorsScreen() }
}
