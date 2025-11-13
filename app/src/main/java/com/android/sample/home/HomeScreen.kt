package com.android.sample.home

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.Chat.ChatMessage
import com.android.sample.Chat.ChatType
import com.android.sample.R
import com.android.sample.ui.components.GuestProfileWarningModal
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
    onProfileClick: () -> Unit = {},
    onVoiceChatClick: () -> Unit = {},
    openDrawerOnStart: Boolean = false,
    speechHelper: com.android.sample.speech.SpeechToTextHelper? = null,
    forceNewChatOnFirstOpen: Boolean = false
) {
  val ui by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

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
            })
      }) {
        Scaffold(
            modifier = modifier.fillMaxSize().background(Color.Black).testTag(HomeTags.Root),
            containerColor = Color.Black,
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
                              contentDescription = "Menu",
                              tint = Color.White,
                              modifier = Modifier.size(24.dp))
                        }
                  },
                  title = {
                    Image(
                        painter = painterResource(R.drawable.euler_logo),
                        contentDescription = "Euler",
                        modifier = Modifier.height(25.dp),
                        contentScale = ContentScale.Fit)
                  },
                  actions = {
                    IconButton(
                        onClick = { viewModel.setTopRightOpen(true) },
                        modifier = Modifier.size(48.dp).testTag(HomeTags.TopRightBtn)) {
                          Icon(
                              Icons.Default.MoreVert,
                              contentDescription = "More",
                              tint = Color.White,
                              modifier = Modifier.size(24.dp))
                        }

                    // Top-right menu (placeholder)
                    DropdownMenu(
                        expanded = ui.isTopRightOpen,
                        onDismissRequest = { viewModel.setTopRightOpen(false) },
                        modifier = Modifier.testTag(HomeTags.TopRightMenu)) {
                          DropdownMenuItem(
                              text = { Text("Delete current chat") },
                              onClick = {
                                viewModel.setTopRightOpen(false)
                                viewModel.showDeleteConfirmation()
                              })
                        }
                  },
                  colors =
                      TopAppBarDefaults.topAppBarColors(
                          containerColor = Color.Black,
                          titleContentColor = Color.White,
                          navigationIconContentColor = Color.White,
                          actionIconContentColor = Color.White))
            },
            bottomBar = {
              Column(
                  Modifier.fillMaxWidth().background(Color.Black).padding(bottom = 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    // Horizontal scrollable row of suggestion chips
                    val suggestions =
                        listOf(
                            "What is EPFL",
                            "Check Ed Discussion",
                            "Show my schedule",
                            "Find library resources",
                            "Check grades on IS-Academia",
                            "Search Moodle courses",
                            "What's due this week?",
                            "Help me study for CS220")

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

                    // Message input bound to ViewModel state.
                    OutlinedTextField(
                        value = ui.messageDraft,
                        onValueChange = { viewModel.updateMessageDraft(it) },
                        placeholder = { Text("Message EULER", color = Color.Gray) },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(60.dp)
                                .testTag(HomeTags.MessageField),
                        enabled = !ui.isSending,
                        singleLine = true,
                        trailingIcon = {
                          Row(horizontalArrangement = Arrangement.spacedBy(0.2.dp)) {
                            // Voice chat button - opens voice visualizer
                            IconButton(
                                onClick = {
                                  speechHelper?.startListening(
                                      onResult = { recognized ->
                                        viewModel.updateMessageDraft(recognized)
                                      })
                                },
                                enabled = speechHelper != null,
                                modifier = Modifier.testTag(HomeTags.MicBtn)) {
                                  Icon(
                                      Icons.Default.Mic,
                                      contentDescription = "Dictate",
                                      tint = Color.Gray)
                                }

                            val canSend = ui.messageDraft.isNotBlank() && !ui.isSending

                            // Voice mode button (equalizer icon) - shown when there's no text
                            AnimatedVisibility(
                                visible = !canSend,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()) {
                                  IconButton(
                                      onClick = { onVoiceChatClick() },
                                      modifier = Modifier.testTag(HomeTags.VoiceBtn)) {
                                        Icon(
                                            Icons.Default.GraphicEq,
                                            contentDescription = "Voice mode",
                                            tint = Color.Gray)
                                      }
                                    },
                                    enabled = speechHelper != null,
                                    modifier = Modifier.size(36.dp).testTag(HomeTags.MicBtn)) {
                                      Icon(
                                          Icons.Default.Mic,
                                          contentDescription = "Dictate",
                                          tint = Color.Gray,
                                          modifier = Modifier.size(18.dp))
                                    }

                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center) {
                                      Crossfade(targetState = canSend, label = "voice-button") {
                                          readyToSend ->
                                        if (!readyToSend) {
                                          IconButton(
                                              onClick = onVoiceChatClick,
                                              modifier =
                                                  Modifier.fillMaxSize()
                                                      .testTag(HomeTags.VoiceBtn)) {
                                                Icon(
                                                    Icons.Default.GraphicEq,
                                                    contentDescription = "Voice mode",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(18.dp))
                                              }
                                        } else {
                                          Spacer(modifier = Modifier.size(18.dp))
                                        }
                                      }
                                    }

                                BubbleSendButton(
                                    enabled = canSend,
                                    isSending = ui.isSending,
                                    onClick = {
                                      if (canSend) {
                                        onSendMessage(ui.messageDraft)
                                        viewModel.sendMessage()
                                      }
                                    })
                              }
                        },
                        shape = RoundedCornerShape(50),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.LightGray,
                                cursorColor = Color.White,
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedBorderColor = Color.DarkGray,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF121212),
                                disabledContainerColor = Color(0xFF121212)))

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Powered by APERTUS",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp))
                  }
            }) { padding ->
              // Chat content: list of messages + thinking indicator at the end while sending.
              Box(
                  modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black),
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
                              ChatMessage(
                                  message = item,
                                  modifier = Modifier.fillMaxWidth(),
                                  isStreaming = showLeadingDot)
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
  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(50.dp),
      color = Color(0xFF1E1E1E),
      modifier = modifier.height(50.dp)) {
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center) {
              Text(text = text, color = Color.White, fontSize = 14.sp)
            }
      }
}

/**
 * Modal shown to confirm clearing the chat history. Exposes two testTags: "home_delete_cancel" and
 * "home_delete_confirm" on buttons.
 */
@Composable
private fun DeleteConfirmationModal(onConfirm: () -> Unit, onCancel: () -> Unit) {
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.7f))
              .clickable(onClick = onCancel),
      contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(280.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)) {
              Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Clear Chat?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This will delete all messages. This action cannot be undone.",
                        color = Color.Gray,
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
                                  ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                              shape = RoundedCornerShape(8.dp)) {
                                Text("Cancel", color = Color.White)
                              }

                          Button(
                              onClick = onConfirm,
                              modifier = Modifier.weight(1f),
                              colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                              shape = RoundedCornerShape(8.dp)) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
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
  var dots by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(450)
      dots = (dots + 1) % 4
    }
  }
  val text = "Euler is thinking" + ".".repeat(dots)
  Surface(
      modifier = modifier,
      shape = RoundedCornerShape(12.dp),
      color = Color(0x14FFFFFF),
      tonalElevation = 0.dp) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                  strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = Color.Gray)
              Spacer(Modifier.width(8.dp))
              Text(text = text, color = Color.LightGray, fontSize = 13.sp)
            }
      }
}

/**
 * Animated circular send button used inside the text field.
 * - Enlarges slightly when enabled, shows a spinner when sending.
 * - Disabled when the draft is blank or a send is in progress.
 */
@Composable
private fun BubbleSendButton(
    enabled: Boolean,
    isSending: Boolean,
    onClick: () -> Unit,
) {
  val size = 40.dp
  val interaction = remember { MutableInteractionSource() }

  val containerColor by
      animateColorAsState(
          targetValue =
              when {
                isSending -> Color(0xFFC62828)
                enabled -> Color(0xFFE53935)
                else -> Color(0xFF3C3C3C)
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
                strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = Color.White)
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
                  contentDescription = "Send",
                  tint = if (canSend) Color.White else Color.White.copy(alpha = 0.35f),
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
  val suggestions = remember {
    listOf(
        "Find CS220 past exams",
        "Check my Moodle assignments",
        "What's on Ed Discussion?",
        "Show my IS-Academia schedule",
        "Search EPFL Drive files")
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
            text = "Ask Euler Anything",
            color = Color(0xFF8B0000), // Deep burgundy red
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
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center)
              }
        }
      }
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  val previewViewModel = remember { HomeViewModel() }
  MaterialTheme { HomeScreen(viewModel = previewViewModel) }
}
