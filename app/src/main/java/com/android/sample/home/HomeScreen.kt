package com.android.sample.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.Chat.ChatMessage
import com.android.sample.Chat.ChatType
import com.android.sample.R
import com.android.sample.settings.Localization
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.ui.components.GuestProfileWarningModal
import com.android.sample.ui.theme.EulerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object HomeTags {
  const val Root = "home_root"
  const val MenuBtn = "home_menu_btn"
  const val TopRightBtn = "home_topright_btn"
  const val Action1Btn = "home_action1_btn"
  const val Action2Btn = "home_action2_btn"
  const val MessageField = "home_message_field"
  const val SendBtn = "home_send_btn"
  const val MicBtn = "home_mic_btn"
  const val VoiceBtn = "home_voice_btn"
  const val Drawer = "home_drawer"
  const val TopRightMenu = "home_topright_menu"
}

/**
 * Entry screen for the chat experience.
 *
 * Responsibilities:
 * - Hosts the top app bar, navigation drawer, message input, and the chat message list.
 * - Renders messages from [HomeViewModel.uiState] (see [HomeUiState.messages]).
 * - Shows a global “thinking” indicator after the last user message when [HomeUiState.isSending] is
 *   true.
 *
 * Key behaviors:
 * - Drawer open/close state is synchronized with the ViewModel.
 * - Send button animates between idle/sending/disabled states.
 * - Chat list uses spacing and stable keys for smooth updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onAction1Click: () -> Unit = {},
    onAction2Click: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onConnectorsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onVoiceChatClick: () -> Unit = {},
    openDrawerOnStart: Boolean = false,
    speechHelper: SpeechToTextHelper? = null,
    ttsHelper: SpeechPlayback? = null,
    forceNewChatOnFirstOpen: Boolean = false
) {
  val ui by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val colorScheme = MaterialTheme.colorScheme
  val backgroundColor = colorScheme.background
  val surfaceColor = colorScheme.surface
  val surfaceVariantColor = colorScheme.surfaceVariant
  val textPrimary = colorScheme.onBackground
  val textSecondary = colorScheme.onSurfaceVariant
  val accentColor = colorScheme.primary

  val audioController = remember(ttsHelper) { HomeAudioController(ttsHelper) }

  DisposableEffect(audioController) { onDispose { audioController.stop() } }

  LaunchedEffect(ui.messages) { audioController.handleMessagesChanged(ui.messages) }

  val ranNewChatOnce = remember { mutableStateOf(false) }
  LaunchedEffect(forceNewChatOnFirstOpen) {
    if (forceNewChatOnFirstOpen && !ranNewChatOnce.value) {
      ranNewChatOnce.value = true
      viewModel.startLocalNewChat()
    }
  }

  // Synchronize ViewModel state <-> Drawer component
  LaunchedEffect(ui.isDrawerOpen) {
    if (ui.isDrawerOpen && !drawerState.isOpen) {
      drawerState.open()
    } else if (!ui.isDrawerOpen && drawerState.isOpen) {
      drawerState.close()
    }
  }

  // Open drawer when returning from settings
  LaunchedEffect(openDrawerOnStart) {
    if (openDrawerOnStart) {
      drawerState.open()
      viewModel.toggleDrawer()
    }
  }

  ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        DrawerContent(
            ui = ui,
            onToggleSystem = { id -> viewModel.toggleSystemConnection(id) },
            onSignOut = {
              scope.launch {
                drawerState.close()
                if (ui.isDrawerOpen) viewModel.toggleDrawer()
                onSignOut()
              }
            },
            onSettingsClick = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              onSettingsClick()
            },
            onConnectorsClick = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              onConnectorsClick()
            },
            onProfileClick = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              if (ui.isGuest) {
                viewModel.showGuestProfileWarning()
              } else {
                onProfileClick()
              }
            },
            onProfileDisabledClick = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
              viewModel.showGuestProfileWarning()
            },
            onClose = {
              scope.launch { drawerState.close() }
              if (ui.isDrawerOpen) viewModel.toggleDrawer()
            },
            onNewChat = {
              scope.launch {
                drawerState.close()
                if (ui.isDrawerOpen) viewModel.toggleDrawer()
                viewModel.startLocalNewChat()
              }
            },
            onPickConversation = { cid ->
              scope.launch {
                viewModel.selectConversation(cid)
                drawerState.close()
                if (ui.isDrawerOpen) viewModel.toggleDrawer()
              }
            },
            onDeleteConversations = { ids -> viewModel.deleteConversations(ids) })
      }) {
        Scaffold(
            modifier = modifier.fillMaxSize().background(backgroundColor).testTag(HomeTags.Root),
            containerColor = backgroundColor,
            topBar = {
              CenterAlignedTopAppBar(
                  navigationIcon = {
                    IconButton(
                        onClick = {
                          viewModel.toggleDrawer()
                          scope.launch {
                            if (!drawerState.isOpen) drawerState.open() else drawerState.close()
                          }
                        },
                        modifier = Modifier.size(48.dp).testTag(HomeTags.MenuBtn)) {
                          Icon(
                              Icons.Default.Menu,
                              contentDescription = Localization.t("menu"),
                              tint = textPrimary,
                              modifier = Modifier.size(24.dp))
                        }
                  },
                  title = {
                    Image(
                        painter = painterResource(R.drawable.euler_logo),
                        contentDescription = Localization.t("euler"),
                        modifier = Modifier.height(25.dp),
                        contentScale = ContentScale.Fit)
                  },
                  actions = {
                    IconButton(
                        onClick = { viewModel.setTopRightOpen(true) },
                        modifier = Modifier.size(48.dp).testTag(HomeTags.TopRightBtn)) {
                          Icon(
                              Icons.Default.MoreVert,
                              contentDescription = Localization.t("more"),
                              tint = textPrimary,
                              modifier = Modifier.size(24.dp))
                        }

                    // Top-right menu with Delete and Share
                    DropdownMenu(
                        expanded = ui.isTopRightOpen,
                        onDismissRequest = { viewModel.setTopRightOpen(false) },
                        modifier = Modifier.testTag(HomeTags.TopRightMenu),
                        containerColor = MaterialTheme.colorScheme.surface) {
                          DeleteMenuItem(
                              onClick = {
                                viewModel.setTopRightOpen(false)
                                viewModel.showDeleteConfirmation()
                              })
                          DropdownMenuItem(
                              text = {
                                Text(
                                    Localization.t("share"),
                                    color = MaterialTheme.colorScheme.onSurface)
                              },
                              onClick = {
                                viewModel.setTopRightOpen(false)
                                // TODO: Implement share functionality
                              })
                        }
                  },
                  colors =
                      TopAppBarDefaults.topAppBarColors(
                          containerColor = backgroundColor,
                          titleContentColor = textPrimary,
                          navigationIconContentColor = textPrimary,
                          actionIconContentColor = textPrimary))
            },
            bottomBar = {
              Column(
                  Modifier.fillMaxWidth().background(backgroundColor).padding(bottom = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Horizontal scrollable row of suggestion chips
                    val suggestions =
                        listOf(
                            Localization.t("suggestion_what_is_epfl"),
                            Localization.t("suggestion_check_ed"),
                            Localization.t("suggestion_show_schedule"),
                            Localization.t("suggestion_library"),
                            Localization.t("suggestion_check_grades"),
                            Localization.t("suggestion_search_moodle"),
                            Localization.t("suggestion_whats_due"),
                            Localization.t("suggestion_study_help"))

                    val scrollState = rememberScrollState()

                    // Check if AI has already responded (at least one AI message exists)
                    val hasAiResponded = ui.messages.any { it.type == ChatType.AI }

                    // Animate visibility of suggestions - hide after first AI response
                    AnimatedVisibility(
                        visible = !hasAiResponded,
                        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { 20 }),
                        exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { -20 })) {
                          Row(
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .horizontalScroll(scrollState)
                                      .padding(horizontal = 16.dp),
                              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                suggestions.forEachIndexed { index, suggestion ->
                                  val testTag =
                                      when (index) {
                                        0 -> HomeTags.Action1Btn
                                        1 -> HomeTags.Action2Btn
                                        else -> null
                                      }

                                  SuggestionChip(
                                      text = suggestion,
                                      modifier =
                                          if (testTag != null) {
                                            Modifier.testTag(testTag)
                                          } else {
                                            Modifier
                                          },
                                      onClick = {
                                        // Call callbacks to maintain compatibility with tests
                                        when (index) {
                                          0 -> onAction1Click()
                                          1 -> onAction2Click()
                                        }
                                        // Update draft and send message
                                        viewModel.updateMessageDraft(suggestion)
                                        onSendMessage(suggestion)
                                        viewModel.sendMessage()
                                      })
                                }
                              }
                        }

                    Spacer(Modifier.height(16.dp))

                    // Offline message banner (dismissible)
                    if (ui.showOfflineMessage) {
                      OfflineMessageBanner(
                          onDismiss = { viewModel.dismissOfflineMessage() },
                          modifier = Modifier.padding(horizontal = 16.dp))
                      Spacer(Modifier.height(12.dp))
                    }

                    // Message input bar - perfectly aligned capsule design
                    ChatInputBar(
                        value = ui.messageDraft,
                        onValueChange = { viewModel.updateMessageDraft(it) },
                        placeholder = Localization.t("message_euler"),
                        enabled = !ui.isSending && !ui.isOffline,
                        isSending = ui.isSending,
                        canSend = ui.messageDraft.isNotBlank() && !ui.isSending && !ui.isOffline,
                        onSendClick = {
                          if (ui.messageDraft.isNotBlank() && !ui.isSending && !ui.isOffline) {
                            onSendMessage(ui.messageDraft)
                            viewModel.sendMessage()
                          }
                        },
                        onMicClick = {
                          if (!ui.isOffline) {
                            speechHelper?.startListening(
                                onResult = { recognized ->
                                  viewModel.updateMessageDraft(recognized)
                                })
                          }
                        },
                        onVoiceModeClick = {
                          if (!ui.isOffline) {
                            onVoiceChatClick()
                          }
                        },
                        speechHelperAvailable = speechHelper != null && !ui.isOffline,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        surfaceVariantColor = surfaceVariantColor,
                        modifier = Modifier.testTag(HomeTags.MessageField))

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = Localization.t("powered_by").uppercase(),
                        color = textSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp))
                  }
            }) { padding ->
              // Chat content: list of messages + thinking indicator at the end while sending.
              Box(
                  modifier = Modifier.fillMaxSize().padding(padding).background(backgroundColor),
                  contentAlignment = Alignment.Center) {
                    if (ui.messages.isEmpty() && !ui.isSending) {
                      // Show animated intro title when list is empty
                      AnimatedIntroTitle(
                          modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp))
                    } else {
                      val listState = rememberLazyListState()

                      // Auto-scroll to bottom when messages change or sending state changes
                      LaunchedEffect(ui.messages.size, ui.isSending, ui.streamingSequence) {
                        val lastIndex =
                            if (ui.messages.isEmpty()) {
                              0
                            } else {
                              // Scroll to last message index, or one more if showing thinking
                              // indicator
                              ui.messages.size - 1 + if (ui.isSending) 1 else 0
                            }
                        listState.animateScrollToItem(lastIndex)
                      }

                      LazyColumn(
                          state = listState,
                          modifier = Modifier.fillMaxSize().padding(16.dp),
                          verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = ui.messages, key = { it.id }) { item ->
                              val showLeadingDot =
                                  item.id == ui.streamingMessageId && item.text.isEmpty()
                              val audioState =
                                  audioController.audioStateFor(item, ui.streamingMessageId)

                              // If this message carries a source, draw the card first
                              if (item.source != null && !item.isThinking) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                SourceCard(
                                    siteLabel = item.source.siteLabel,
                                    title = item.source.title,
                                    url = item.source.url,
                                    retrievedAt = item.source.retrievedAt,
                                    compactType = item.source.compactType,
                                    onVisit =
                                        if (item.source.url != null &&
                                            item.source.compactType == CompactSourceType.NONE) {
                                          {
                                            val intent =
                                                Intent(
                                                    Intent.ACTION_VIEW, Uri.parse(item.source.url))
                                            context.startActivity(intent)
                                          }
                                        } else null)
                                Spacer(Modifier.height(8.dp))
                              }

                              // Then render the usual chat bubble (for USER/AI text)
                              ChatMessage(
                                  message = item,
                                  modifier = Modifier.fillMaxWidth(),
                                  isStreaming = showLeadingDot,
                                  audioState = audioState,
                                  aiText = textPrimary)
                            }

                            // Global thinking indicator shown AFTER the last user message.
                            if (ui.isSending && ui.streamingMessageId == null) {
                              item {
                                Spacer(Modifier.height(6.dp))
                                ThinkingIndicator(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .testTag("home_thinking_indicator"))
                              }
                            }
                          }
                    }
                  }
            }
      }

  // Delete confirmation dialog layered over the screen.
  AnimatedVisibility(
      visible = ui.showDeleteConfirmation,
      enter = fadeIn(tween(200)),
      exit = fadeOut(tween(200)),
      modifier = Modifier.fillMaxSize()) {
        DeleteConfirmationModal(
            onConfirm = { viewModel.deleteCurrentConversation() },
            onCancel = { viewModel.hideDeleteConfirmation() })
      }

  AnimatedVisibility(
      visible = ui.showGuestProfileWarning,
      enter = fadeIn(tween(200)),
      exit = fadeOut(tween(200)),
      modifier = Modifier.fillMaxSize()) {
        GuestProfileWarningModal(
            onContinueAsGuest = { viewModel.hideGuestProfileWarning() },
            onLogin = {
              viewModel.hideGuestProfileWarning()
              onSignOut()
            })
      }
}

/** Compact, rounded action button used in the bottom actions row. */
@Composable
private fun SuggestionChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(50.dp),
      color = colorScheme.surfaceVariant,
      modifier = modifier.height(50.dp)) {
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center) {
              Text(text = text, color = colorScheme.onSurface, fontSize = 14.sp)
            }
      }
}

/* ----- Placeholders for external components (drawer + top-right panel) ----- */

@Composable
private fun TopRightPanelPlaceholder(onDismiss: () -> Unit, onDeleteClick: () -> Unit) {
  DropdownMenuItem(text = { Text(Localization.t("share")) }, onClick = onDismiss)
  DropdownMenuItem(
      text = { Text(Localization.t("delete")) },
      onClick = {
        onDeleteClick()
        onDismiss()
      })
}

/** Delete menu item that turns red on hover/press. */
@Composable
private fun DeleteMenuItem(onClick: () -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isHovered by interactionSource.collectIsHoveredAsState()
  val colorScheme = MaterialTheme.colorScheme

  val textColor by
      animateColorAsState(
          targetValue = if (isPressed || isHovered) colorScheme.error else colorScheme.onSurface,
          label = "delete-text-color")

  DropdownMenuItem(
      text = { Text(Localization.t("delete"), color = textColor) },
      onClick = onClick,
      interactionSource = interactionSource)
}

/**
 * Perfectly aligned chat input bar with capsule shape, matching design specifications. Features:
 * - Capsule shape with large corner radius
 * - Vertically centered placeholder text
 * - Properly aligned microphone and voice/send buttons
 * - Smooth transition from voice mode to send button
 */
@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isSending: Boolean,
    canSend: Boolean,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onVoiceModeClick: () -> Unit,
    speechHelperAvailable: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = {
        Text(
            text = placeholder,
            color = textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal)
      },
      modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
      enabled = enabled,
      singleLine = true,
      textStyle =
          MaterialTheme.typography.bodyMedium.copy(
              fontSize = 15.sp, fontWeight = FontWeight.Normal),
      trailingIcon = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 4.dp)) {
              // Microphone button
              IconButton(
                  onClick = onMicClick,
                  enabled = speechHelperAvailable && enabled,
                  modifier = Modifier.size(40.dp).testTag(HomeTags.MicBtn)) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = Localization.t("dictate"),
                        tint = textSecondary,
                        modifier = Modifier.size(22.dp))
                  }

              // Voice mode / Send button - sized to match chatbox height with tiny margin (52dp -
              // 9dp margin = 43dp)
              Box(
                  modifier = Modifier.size(43.dp).offset(y = (-1).dp),
                  contentAlignment = Alignment.Center) {
                    Crossfade(targetState = canSend, label = "voice-to-send-transition") {
                        readyToSend ->
                      if (!readyToSend) {
                        // Voice mode button - circular with waveform icon
                        Surface(
                            onClick = onVoiceModeClick,
                            modifier = Modifier.fillMaxSize().testTag(HomeTags.VoiceBtn),
                            shape = CircleShape,
                            color = textSecondary.copy(alpha = 0.2f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp) {
                              Box(
                                  modifier = Modifier.fillMaxSize(),
                                  contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = "Voice mode",
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp))
                                  }
                            }
                      } else {
                        // Red send button - transitions from voice mode, matches chatbox height
                        Surface(
                            onClick = onSendClick,
                            modifier = Modifier.fillMaxSize().testTag(HomeTags.SendBtn),
                            shape = CircleShape,
                            color = EulerRed,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp) {
                              Box(
                                  modifier = Modifier.fillMaxSize(),
                                  contentAlignment = Alignment.Center) {
                                    if (isSending) {
                                      CircularProgressIndicator(
                                          strokeWidth = 2.dp,
                                          modifier = Modifier.size(18.dp),
                                          color = Color.White)
                                    } else {
                                      val icon =
                                          try {
                                            androidx.compose.material.icons.Icons.Rounded.Send
                                          } catch (_: Throwable) {
                                            androidx.compose.material.icons.Icons.Default.Send
                                          }
                                      Icon(
                                          imageVector = icon,
                                          contentDescription = Localization.t("send"),
                                          tint = Color.White,
                                          modifier = Modifier.size(18.dp))
                                    }
                                  }
                            }
                      }
                    }
                  }
            }
      },
      shape = RoundedCornerShape(50.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              focusedTextColor = textPrimary,
              unfocusedTextColor = textPrimary,
              disabledTextColor = textPrimary.copy(alpha = 0.6f),
              cursorColor = textPrimary,
              focusedPlaceholderColor = textSecondary,
              unfocusedPlaceholderColor = textSecondary,
              focusedBorderColor = textSecondary.copy(alpha = 0.5f),
              unfocusedBorderColor = textSecondary.copy(alpha = 0.35f),
              focusedContainerColor = surfaceVariantColor,
              unfocusedContainerColor = surfaceVariantColor,
              disabledContainerColor = surfaceVariantColor))
}

/** Simple delete confirmation modal with "Delete chat?" title and Cancel/Delete buttons. */
@Composable
private fun DeleteConfirmationModal(onConfirm: () -> Unit, onCancel: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  val textPrimary = colorScheme.onSurface
  val textSecondary = colorScheme.onSurfaceVariant
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.7f))
              .clickable(onClick = onCancel),
      contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(280.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(16.dp)) {
              Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Localization.t("clear_chat"),
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Localization.t("clear_chat_message"),
                        color = textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()) {
                          Button(
                              onClick = onCancel,
                              modifier = Modifier.weight(1f),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = colorScheme.surfaceVariant),
                              shape = RoundedCornerShape(8.dp)) {
                                Text(Localization.t("cancel"), color = textPrimary)
                              }

                          Button(
                              onClick = onConfirm,
                              modifier = Modifier.weight(1f),
                              colors =
                                  ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                              shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    Localization.t("delete"),
                                    color = colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold)
                              }
                        }
                  }
            }
      }
}

/**
 * Small inline indicator shown while awaiting an AI reply. Driven by HomeUiState.isSending and
 * rendered after the last message in the list.
 */
@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
  val colorScheme = MaterialTheme.colorScheme
  var dots by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(450)
      dots = (dots + 1) % 4
    }
  }
  val text = Localization.t("euler_thinking") + ".".repeat(dots)
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(12.dp),
      color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
      tonalElevation = 0.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp),
                  color = colorScheme.onSurfaceVariant)
              Spacer(Modifier.width(8.dp))
              Text(text = text, color = colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
      }
}

/**
 * Animated circular send button used inside the text field.
 * - Enlarges slightly when enabled, shows a spinner when sending.
 * - Disabled when the draft is blank or a send is in progress.
 */
@Composable
private fun BubbleSendButton(enabled: Boolean, isSending: Boolean, onClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  val accent = colorScheme.primary
  val size = 40.dp
  val interaction = remember { MutableInteractionSource() }

  val containerColor by
      animateColorAsState(
          targetValue =
              when {
                isSending -> accent.copy(alpha = 0.85f)
                enabled -> accent
                else -> colorScheme.onSurface.copy(alpha = 0.2f)
              },
          label = "bubble-color")

  Surface(
      modifier = Modifier.size(size).testTag(HomeTags.SendBtn),
      color = containerColor,
      shape = CircleShape,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
  ) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .testTag(HomeTags.SendBtn)
                .then(
                    if (enabled && !isSending)
                        Modifier.clickable(interactionSource = interaction, indication = null) {
                          onClick()
                        }
                    else Modifier),
        contentAlignment = Alignment.Center) {
          if (isSending) {
            CircularProgressIndicator(
                strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = colorScheme.onPrimary)
          } else {
            val icon =
                try {
                  androidx.compose.material.icons.Icons.Rounded.Send
                } catch (_: Throwable) {
                  androidx.compose.material.icons.Icons.Default.Send
                }
            Crossfade(targetState = enabled, label = "send-button-state") { canSend ->
              Icon(
                  imageVector = icon,
                  tint =
                      if (canSend) colorScheme.onPrimary
                      else colorScheme.onSurface.copy(alpha = 0.4f),
                  contentDescription = Localization.t("send"),
                  modifier = Modifier.size(18.dp))
            }
          }
        }
  }
}

/**
 * Animated intro title with rotating suggestions. Shows "Ask Euler Anything" in bold red, with
 * suggestions that fade in/out every 3 seconds.
 */
@Composable
internal fun AnimatedIntroTitle(modifier: Modifier = Modifier) {
  val colorScheme = MaterialTheme.colorScheme
  val suggestions = remember {
    listOf(
        Localization.t("intro_suggestion_1"),
        Localization.t("intro_suggestion_2"),
        Localization.t("intro_suggestion_3"),
        Localization.t("intro_suggestion_4"),
        Localization.t("intro_suggestion_5"))
  }

  var currentIndex by remember { mutableStateOf(0) }

  // Rotate suggestions every 3 seconds
  LaunchedEffect(Unit) {
    while (true) {
      delay(3000)
      currentIndex = (currentIndex + 1) % suggestions.size
    }
  }

  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        // Title: "Ask Euler Anything" in deep burgundy/plum
        Text(
            text = Localization.t("ask_euler_anything"),
            color = colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp))

        // Rotating suggestion text with crossfade animation
        Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
          AnimatedContent(
              targetState = currentIndex,
              transitionSpec = {
                // Crossfade with vertical slide: old slides down, new slides in from above
                (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetY = { -it / 4 } // Slide in from above
                        )) togetherWith
                    (fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                        slideOutVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            targetOffsetY = { it / 4 } // Slide out downward
                            ))
              },
              label = "suggestion-transition") { index ->
                Text(
                    text = suggestions[index],
                    color = colorScheme.onBackground,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center)
              }
        }
      }
}

/** Dismissible offline message banner shown at the top of the home screen when offline. */
@Composable
private fun OfflineMessageBanner(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
  val colorScheme = MaterialTheme.colorScheme
  Card(
      modifier = modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
      shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Text(
                  text = "You're not connected to the internet. Please try again.",
                  color = colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.weight(1f))
              IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp))
              }
            }
      }
}

@Composable
private fun SourceCard(
    siteLabel: String,
    title: String,
    url: String?,
    retrievedAt: Long,
    compactType: CompactSourceType = CompactSourceType.NONE,
    onVisit: (() -> Unit)? = null
) {
  val colorScheme = MaterialTheme.colorScheme

  if (compactType != CompactSourceType.NONE) {
    // Compact indicator - small inline badge with emoji based on type
    val emoji =
        when (compactType) {
          CompactSourceType.SCHEDULE -> "📅"
          CompactSourceType.FOOD -> "🍴"
          else -> ""
        }
    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = com.android.sample.ui.theme.EulerGreen,
              modifier = Modifier.size(12.dp))
          Spacer(Modifier.width(6.dp))
          Text(
              text = "$emoji $siteLabel",
              color = colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
        }
  } else {
    // Full RAG source card with Visit button
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
          // Top line: "Retrieved from EPFL.ch Website"
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = com.android.sample.ui.theme.EulerGreen,
                modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Retrieved from  $siteLabel",
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
          }

          Spacer(Modifier.height(4.dp))

          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // URL (ellipsized to one line)
            Text(
                text = url ?: "",
                color = colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))

            Spacer(Modifier.width(6.dp))

            if (onVisit != null && url != null) {
              Button(
                  onClick = onVisit,
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)) {
                    Text("Visit", color = colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = colorScheme.onPrimary)
                  }
            }
          }

          Spacer(Modifier.height(2.dp))

          // Retrieved date
          val dateStr =
              remember(retrievedAt) {
                val d =
                    java.time.Instant.ofEpochMilli(retrievedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                "%02d/%02d/%02d".format(d.dayOfMonth, d.monthValue, d.year % 100)
              }
          Text(
              text = "Retrieved on $dateStr",
              color = colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
        }
  }
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  val previewViewModel = remember { HomeViewModel() }
  MaterialTheme { HomeScreen(viewModel = previewViewModel) }
}
