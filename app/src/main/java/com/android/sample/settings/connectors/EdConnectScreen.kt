package com.android.sample.settings.connectors

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorsDimensions as Dimens
import com.android.sample.ui.theme.containerColor
import com.android.sample.ui.theme.ed1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Screen for connecting to the ED connector by entering an API token. Displays instructions, a
 * token input field, and a clipboard suggestion banner when a valid token is detected in the
 * clipboard.
 *
 * @param onBackClick Callback invoked when the user navigates back
 * @param viewModel ViewModel managing the ED connector state and connection logic
 */
@Composable
fun EdConnectScreen(onBackClick: () -> Unit = {}, viewModel: ConnectorsViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val connectorState = rememberConnectorState()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var token by remember { mutableStateOf("") }

  // Check clipboard when screen is first shown
  LaunchedEffect(Unit) { viewModel.checkClipboardForEdToken(context, token) }

  // Check clipboard when screen resumes (e.g., user returns from browser)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        // Use coroutine scope to delay and check clipboard
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
          // Small delay to ensure clipboard is updated
          kotlinx.coroutines.delay(500)
          viewModel.checkClipboardForEdToken(context, token)
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // Also check when token changes (user might have pasted manually)
  LaunchedEffect(token) {
    if (token.isNotBlank() && uiState.showEdClipboardSuggestion) {
      viewModel.dismissEdClipboardSuggestion()
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(connectorState.colors.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top bar
      EdConnectTopBar(onBackClick = onBackClick, colors = connectorState.colors)

      // Content
      Column(
          modifier =
              Modifier.fillMaxSize()
                  .padding(horizontal = Dimens.ScreenHorizontalPadding)
                  .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            // Ed connector info card
            EdConnectorInfoCard(colors = connectorState.colors)

            Spacer(modifier = Modifier.height(8.dp))

            // How to connect card
            HowToConnectCard(colors = connectorState.colors)

            Spacer(modifier = Modifier.height(8.dp))

            // Open ED Token Page button
            Button(
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ED_API_TOKENS_URL))
                  context.startActivity(intent)
                },
                enabled = !uiState.isEdConnecting,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(containerColor = ed1, contentColor = Color.White),
                shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
                  Text(
                      Localization.t("settings_connectors_ed_get_token_button"),
                      fontSize = Dimens.DialogTextFontSize,
                      fontWeight = FontWeight.SemiBold)
                }

            Spacer(modifier = Modifier.height(8.dp))

            // Token input field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                  text = Localization.t("settings_connectors_ed_paste_token_label"),
                  color = connectorState.colors.textPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium)

              OutlinedTextField(
                  value = token,
                  onValueChange = { token = it },
                  placeholder = {
                    Text(
                        Localization.t("settings_connectors_ed_paste_token_placeholder"),
                        color = connectorState.colors.textSecondary)
                  },
                  leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = connectorState.colors.textSecondary)
                  },
                  singleLine = true,
                  enabled = !uiState.isEdConnecting,
                  keyboardOptions =
                      KeyboardOptions(
                          autoCorrect = false,
                          keyboardType = KeyboardType.Text,
                          imeAction = ImeAction.Done),
                  modifier = Modifier.fillMaxWidth(),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedTextColor = connectorState.colors.textPrimary,
                          unfocusedTextColor = connectorState.colors.textPrimary,
                          focusedBorderColor = connectorState.colors.glassBorder,
                          unfocusedBorderColor = connectorState.colors.glassBorder))
            }

            // Error message
            uiState.edConnectError?.let { error ->
              Text(
                  text = error,
                  color = connectorState.colors.accentRed,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connect button
            Button(
                onClick = {
                  val trimmedToken = token.trim()
                  viewModel.confirmEdConnect(trimmedToken, null)
                },
                enabled = token.isNotBlank() && !uiState.isEdConnecting,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            if (token.isNotBlank() && !uiState.isEdConnecting) {
                              ed1
                            } else {
                              connectorState.colors.accentRed
                            },
                        contentColor = Color.White),
                shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
                  if (uiState.isEdConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                  } else {
                    Text(
                        Localization.t("connect"),
                        fontSize = Dimens.DialogTextFontSize,
                        fontWeight = FontWeight.SemiBold)
                  }
                }

            Spacer(modifier = Modifier.height(24.dp))
          }
    }

    // ED clipboard suggestion banner at the bottom
    if (uiState.showEdClipboardSuggestion && uiState.detectedEdToken != null) {
      AnimatedVisibility(
          visible = uiState.showEdClipboardSuggestion,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter)) {
            EdClipboardSuggestionBanner(
                detectedToken = uiState.detectedEdToken ?: "",
                colors = connectorState.colors,
                onUseToken = { token = viewModel.acceptEdClipboardToken() },
                onDismiss = { viewModel.dismissEdClipboardSuggestion() })
          }
    }
  }

  // Handle successful connection - navigate back after state is updated
  LaunchedEffect(uiState.connectors, uiState.isEdConnecting) {
    val edConnector = uiState.connectors.find { it.id == ED_CONNECTOR_ID }
    // Wait for connection to complete
    if (edConnector?.isConnected == true &&
        !uiState.isEdConnecting &&
        uiState.edConnectError == null) {
      // Small delay to show success state, then navigate back
      // Status will be refreshed automatically when ConnectorsScreen becomes visible
      kotlinx.coroutines.delay(300)
      onBackClick()
    }
  }
}

@Composable
internal fun EdConnectTopBar(onBackClick: () -> Unit, colors: ConnectorsColors) {
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
        Text(
            text = Localization.t("settings_connectors_ed_title"),
            color = colors.textPrimary,
            fontSize = Dimens.TopBarTitleFontSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = Dimens.TopBarTitleLetterSpacing)
      }
}

@Composable
internal fun EdConnectorInfoCard(colors: ConnectorsColors) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colors.glassBackground.copy(alpha = 0.03f),
              contentColor = colors.textPrimary)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              // ED Logo with gradient (same as when connected)
              EdLogo(isConnected = true)

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ed",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.t("settings_connectors_ed_description"),
                    color = colors.textSecondary,
                    fontSize = 13.sp)
              }
            }
      }
}

@Composable
internal fun HowToConnectCard(colors: ConnectorsColors) {
  Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colors.glassBackground.copy(alpha = 0.03f),
              contentColor = colors.textPrimary)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                  text = Localization.t("settings_connectors_ed_how_to_connect_title"),
                  color = colors.textPrimary,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.SemiBold)

              // Step 1
              Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.Top) {
                    Text(
                        text = "1.",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                    Text(
                        text = Localization.t("settings_connectors_ed_step_1"),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                  }

              // Step 2
              Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.Top) {
                    Text(
                        text = "2.",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                          Icon(
                              imageVector = Icons.Default.ContentCopy,
                              contentDescription = null,
                              modifier = Modifier.size(16.dp),
                              tint = colors.textSecondary)
                          Text(
                              text = Localization.t("settings_connectors_ed_step_2"),
                              color = colors.textPrimary,
                              fontSize = 14.sp,
                              modifier = Modifier.weight(1f))
                        }
                  }

              // Step 3
              Row(
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.Top) {
                    Text(
                        text = "3.",
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                    Text(
                        text = Localization.t("settings_connectors_ed_step_3"),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f))
                  }
            }
      }
}

@Composable
internal fun EdClipboardSuggestionBanner(
    detectedToken: String,
    colors: ConnectorsColors,
    onUseToken: () -> Unit,
    onDismiss: () -> Unit
) {
  val tokenPreview =
      if (detectedToken.length > 40) {
        detectedToken.take(40) + "..."
      } else {
        detectedToken
      }

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = 16.dp),
      shape = RoundedCornerShape(22.dp),
      colors =
          CardDefaults.cardColors(
              containerColor = colors.background.copy(alpha = 0.95f),
              contentColor = colors.textPrimary)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = colors.connectedGreen,
                        modifier = Modifier.size(24.dp))
                    Text(
                        text = Localization.t("settings_connectors_ed_token_detected_title"),
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold)
                  }

              Text(
                  text = tokenPreview,
                  color = colors.textSecondary,
                  fontSize = 13.sp,
                  modifier = Modifier.fillMaxWidth())

              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // "Not now" button
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                      Text(
                          Localization.t("settings_connectors_ed_not_now_button"),
                          color = colors.textPrimary,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Medium)
                    }

                    // "Use this token" button
                    Button(
                        onClick = onUseToken,
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colors.connectedGreen,
                                contentColor = colors.onPrimaryColor),
                        shape = RoundedCornerShape(Dimens.DialogButtonCornerRadius)) {
                          Text(
                              Localization.t("settings_connectors_ed_use_token_button"),
                              fontSize = 14.sp,
                              fontWeight = FontWeight.SemiBold)
                        }
                  }
            }
      }
}
