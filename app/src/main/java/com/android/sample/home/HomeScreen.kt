package com.android.sample.home

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.Chat.ChatAttachment
import com.android.sample.Chat.ChatMessage
import com.android.sample.Chat.ChatType
import com.android.sample.R
import com.android.sample.settings.Localization
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.ui.components.EdPostConfirmationModal
import com.android.sample.ui.components.EdPostedCard
import com.android.sample.ui.components.GuestProfileWarningModal
import com.android.sample.ui.theme.Dimensions
import com.android.sample.ui.theme.Dimensions.ChatInputButtonRowPaddingEnd
import com.android.sample.ui.theme.Dimensions.ChatInputButtonSpacing
import com.android.sample.ui.theme.Dimensions.ChatInputCornerRadius
import com.android.sample.ui.theme.Dimensions.ChatInputListeningViewHeight
import com.android.sample.ui.theme.Dimensions.ChatInputListeningViewPaddingEnd
import com.android.sample.ui.theme.Dimensions.ChatInputListeningViewPaddingStart
import com.android.sample.ui.theme.Dimensions.ChatInputMaxLines
import com.android.sample.ui.theme.Dimensions.ChatInputMicBorderWidth
import com.android.sample.ui.theme.Dimensions.ChatInputMicButtonSize
import com.android.sample.ui.theme.Dimensions.ChatInputMicIconSize
import com.android.sample.ui.theme.Dimensions.ChatInputProgressIndicatorStrokeWidth
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarAnimationDelays
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarAnimationDurationMillis
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarAnimationInitialScale
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarAnimationTargetScale
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarColorAlpha
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarMaxHeight
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarMinHeight
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarSpacerWidth
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarSpacing
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceBarWidth
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceModeButtonAlpha
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceSendButtonOffsetY
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceSendButtonSize
import com.android.sample.ui.theme.Dimensions.ChatInputVoiceSendIconSize
import com.android.sample.ui.theme.Dimensions.InputHeight
import com.android.sample.ui.theme.Dimensions.InputHorizontal
import com.android.sample.ui.theme.EdPostDimensions
import com.android.sample.ui.theme.EulerRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

internal sealed class TimelineItem {
  abstract val timestamp: Long
  abstract val key: String

  data class MessageItem(
      val message: com.android.sample.Chat.ChatUIModel,
      override val timestamp: Long
  ) : TimelineItem() {
    override val key: String = message.id
  }

  data class CardItem(val card: EdPostCard, override val timestamp: Long) : TimelineItem() {
    override val key: String = card.id
  }

  data class PostsCardItem(val card: EdPostsCard, override val timestamp: Long) : TimelineItem() {
    override val key: String = card.id
  }
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
  val context = LocalContext.current
  var pdfViewerUrl by remember { mutableStateOf<String?>(null) }
  var isListening by remember { mutableStateOf(false) }

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

  // Synchronize ViewModel state <=> Drawer component
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
                    // Horizontal scrollable row of suggestion chips (Euler help questions)
                    val suggestions =
                        listOf(
                            Localization.t("suggestion_euler_capabilities"),
                            Localization.t("suggestion_new_conversation"),
                            Localization.t("suggestion_offline_mode"),
                            Localization.t("suggestion_previous_chats"),
                            Localization.t("suggestion_voice_input"),
                            Localization.t("suggestion_change_theme"),
                            Localization.t("suggestion_privacy"))

                    val scrollState = rememberScrollState()

                    // Determine suggestion chip visibility:
                    // - Always hide while actively sending
                    // - In offline mode, keep visible to allow cached responses
                    // - Online: hide after first user message
                    val shouldShowSuggestions =
                        !ui.isSending &&
                            (ui.isOffline || !ui.messages.any { it.type == ChatType.USER })

                    // Animate visibility of suggestions
                    AnimatedVisibility(
                        visible = shouldShowSuggestions,
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
                                        // Update draft and send message directly
                                        // Pass message directly to avoid state update timing issues
                                        viewModel.updateMessageDraft(suggestion)
                                        onSendMessage(suggestion)
                                        viewModel.sendMessage(suggestion)
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
                          if (!ui.isOffline && !isListening) {
                            isListening = true
                            speechHelper?.startListening(
                                onResult = { recognized ->
                                  viewModel.updateMessageDraft(recognized)
                                  isListening = false
                                },
                                onError = { _ -> isListening = false },
                                onComplete = { isListening = false })
                          } else if (!ui.isOffline) {
                            // User clicked mic while already listening - stop the recognition
                            speechHelper?.stopListening()
                            isListening = false
                          }
                        },
                        onVoiceModeClick = {
                          if (!ui.isOffline) {
                            onVoiceChatClick()
                          }
                        },
                        speechHelperAvailable = speechHelper != null && !ui.isOffline,
                        isListening = isListening,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        surfaceVariantColor = surfaceVariantColor,
                        modifier = Modifier)

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

                      val edPostAction =
                          ui.pendingAction as? com.android.sample.home.PendingAction.PostOnEd
                      val messagesToShow = ui.messages

                      // Merge messages, ED cards, and EdPostsCards into a single timeline
                      // EdPostsCards are placed immediately after their associated message
                      val timeline =
                          remember(messagesToShow, ui.edPostCards, ui.edPostsCards) {
                            val msgItems =
                                messagesToShow.map { TimelineItem.MessageItem(it, it.timestamp) }
                            val cardItems =
                                ui.edPostCards.map { TimelineItem.CardItem(it, it.createdAt) }

                            // Create a map of messageId -> EdPostsCard for quick lookup
                            val edPostsCardsByMessage = ui.edPostsCards.associateBy { it.messageId }

                            // Build timeline: for each message, add it, then add its EdPostsCard if
                            // it exists
                            val timelineItems = mutableListOf<TimelineItem>()
                            msgItems.forEach { msgItem ->
                              timelineItems.add(msgItem)
                              // Add EdPostsCard immediately after its message
                              edPostsCardsByMessage[msgItem.message.id]?.let { edPostsCard ->
                                // Ensure EdCard timestamp is after message timestamp for correct
                                // ordering
                                val cardTimestamp =
                                    maxOf(msgItem.timestamp + 1, edPostsCard.createdAt)
                                timelineItems.add(
                                    TimelineItem.PostsCardItem(edPostsCard, cardTimestamp))
                              }
                            }
                            // Add standalone EdPostCards (not associated with messages)
                            timelineItems.addAll(cardItems)
                            // Sort by timestamp to ensure correct order
                            timelineItems.sortBy { it.timestamp }
                            timelineItems
                          }

                      LazyColumn(
                          state = listState,
                          modifier = Modifier.fillMaxSize().padding(16.dp),
                          verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = timeline, key = { it.key }) { item ->
                              when (item) {
                                is TimelineItem.MessageItem -> {
                                  val msg = item.message
                                  val showLeadingDot =
                                      msg.id == ui.streamingMessageId && msg.text.isEmpty()
                                  val audioState =
                                      audioController.audioStateFor(msg, ui.streamingMessageId)

                                  // Render the chat bubble with source inline
                                  ChatMessage(
                                      message = msg,
                                      modifier = Modifier.fillMaxWidth(),
                                      isStreaming = showLeadingDot,
                                      audioState = audioState,
                                      aiText = textPrimary,
                                      onOpenAttachment = { attachment ->
                                        pdfViewerUrl = attachment.url
                                      },
                                      onDownloadAttachment = { attachment ->
                                        startPdfDownload(context, attachment)
                                      },
                                      onSourceClick = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                      })

                                  // Show compact source indicators for schedule/food (after the
                                  // message)
                                  if (msg.source != null &&
                                      !msg.isThinking &&
                                      msg.source.compactType != CompactSourceType.NONE) {
                                    Spacer(Modifier.height(8.dp))
                                    val sourceSiteLabel =
                                        msg.source.siteLabel.orEmpty()
                                    SourceCard(
                                        siteLabel = sourceSiteLabel,
                                        title = "",
                                        url = msg.source.url,
                                        retrievedAt = msg.source.retrievedAt,
                                        compactType = msg.source.compactType,
                                        onVisit = null)
                                  }
                                }
                                is TimelineItem.CardItem -> {
                                  EdPostedCard(item.card, modifier = Modifier.fillMaxWidth())
                                }
                                is TimelineItem.PostsCardItem -> {
                                  val context = LocalContext.current
                                  Spacer(Modifier.height(8.dp))
                                  EdPostsSection(
                                      state =
                                          EdPostsUiState(
                                              stage = item.card.stage,
                                              posts = item.card.posts,
                                              filters = item.card.filters,
                                              errorMessage = item.card.errorMessage),
                                      modifier = Modifier.fillMaxWidth(),
                                      onOpenPost = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                      },
                                      onRetry = { filters ->
                                        // Retry is not supported for persisted cards
                                      })
                                }
                              }
                            }

                            // Note: EdPostsSection is now displayed inline with each message via
                            // EdPostsCard
                            // No need for a global EdPostsSection here

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

                            // Inline ED post proposal card at the end of the list
                            if (edPostAction != null) {
                              item {
                                EdPostConfirmationModal(
                                    modifier = Modifier.fillMaxWidth(),
                                    title = edPostAction.draftTitle,
                                    body = edPostAction.draftBody,
                                    courses = ui.edCourses,
                                    selectedCourseId = edPostAction.selectedCourseId,
                                    isLoading = ui.isPostingToEd,
                                    isLoadingCourses = ui.isLoadingEdCourses,
                                    onPublish = { title, body, courseId, isAnonymous ->
                                      viewModel.publishEdPost(title, body, courseId, isAnonymous)
                                    },
                                    onCancel = { viewModel.cancelEdPost() })
                                Spacer(Modifier.height(8.dp))
                              }
                            }
                          }

                      pdfViewerUrl?.let { url ->
                        PdfViewerDialog(url = url, onDismiss = { pdfViewerUrl = null })
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

  // ED post status banner (published / cancelled / failed)
  val edPostResult = ui.edPostResult
  LaunchedEffect(edPostResult) {
    if (edPostResult != null) {
      // Auto-dismiss after a short delay
      delay(4_000)
      viewModel.clearEdPostResult()
    }
  }
  AnimatedVisibility(
      visible = edPostResult != null,
      enter = fadeIn(tween(150)),
      exit = fadeOut(tween(150)),
      modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          val result = edPostResult
          if (result != null) {
            EdPostResultBanner(result = result, onDismiss = { viewModel.clearEdPostResult() })
          }
        }
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
internal fun TopRightPanelPlaceholder(onDismiss: () -> Unit, onDeleteClick: () -> Unit) {
  DropdownMenuItem(
      text = { Text(Localization.t("share")) },
      onClick = onDismiss,
      modifier = Modifier.testTag("menu_share"))
  DropdownMenuItem(
      text = { Text(Localization.t("delete")) },
      onClick = {
        onDeleteClick()
        onDismiss()
      },
      modifier = Modifier.testTag("menu_delete"))
}

@Composable
internal fun EdPostResultBanner(result: EdPostResult, onDismiss: () -> Unit = {}) {
  val bg: Color
  val icon: ImageVector
  val title: String
  val subtitle: String
  when (result) {
    is EdPostResult.Published -> {
      bg = MaterialTheme.colorScheme.surfaceVariant
      icon = Icons.Default.CheckCircle
      title = Localization.t("ed_post_published_title")
      subtitle = Localization.t("ed_post_published_subtitle")
    }
    is EdPostResult.Cancelled -> {
      bg = MaterialTheme.colorScheme.surfaceVariant
      icon = Icons.Default.Close
      title = Localization.t("ed_post_cancelled_title")
      subtitle = Localization.t("ed_post_cancelled_subtitle")
    }
    is EdPostResult.Failed -> {
      bg = MaterialTheme.colorScheme.errorContainer
      icon = Icons.Default.Error
      title = Localization.t("ed_post_failed_title")
      subtitle = result.message
    }
    else -> {
      bg = MaterialTheme.colorScheme.surface
      icon = Icons.Default.Check
      title = ""
      subtitle = ""
    }
  }

  Surface(
      tonalElevation = EdPostDimensions.ResultCardElevation,
      shape = RoundedCornerShape(EdPostDimensions.ResultCardCornerRadius),
      color = bg,
      modifier = Modifier.padding(EdPostDimensions.ResultCardPadding)) {
        Row(
            modifier = Modifier.padding(EdPostDimensions.ResultCardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdPostDimensions.ResultCardRowSpacing)) {
              Icon(icon, contentDescription = null)
              Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) {
                  Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                }
              }
              IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = Localization.t("dismiss"))
              }
            }
      }
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
 * - Visual listening mode with animated voice bars
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
    isListening: Boolean = false,
    textPrimary: Color,
    textSecondary: Color,
    surfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
  // Helper composable to render the buttons (Mic + Send/Voice).
  // Extracted to avoid duplication since we need them in both 'Listening' and 'Text' modes.
  val buttonsUI: @Composable () -> Unit = {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ChatInputButtonSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = ChatInputButtonRowPaddingEnd)) {
          // Microphone button
          IconButton(
              onClick = onMicClick,
              enabled = speechHelperAvailable && enabled,
              modifier =
                  Modifier.size(ChatInputMicButtonSize)
                      // Apply a red border only when listening to give visual feedback
                      .then(
                          if (isListening) {
                            Modifier.border(
                                width = ChatInputMicBorderWidth,
                                color = EulerRed,
                                shape = CircleShape)
                          } else {
                            Modifier
                          })
                      .testTag(HomeTags.MicBtn)) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = Localization.t("dictate"),
                    tint = textSecondary,
                    modifier = Modifier.size(ChatInputMicIconSize))
              }

          // Voice mode / Send button - sized to match chatbox height with tiny margin (52dp -
          // 9dp margin = 43dp)
          Box(
              modifier =
                  Modifier.size(ChatInputVoiceSendButtonSize)
                      .offset(y = ChatInputVoiceSendButtonOffsetY),
              contentAlignment = Alignment.Center) {
                Crossfade(targetState = canSend, label = "voice-to-send-transition") { readyToSend
                  ->
                  if (!readyToSend) {
                    // Voice mode button - circular with waveform icon
                    Surface(
                        onClick = onVoiceModeClick,
                        modifier = Modifier.fillMaxSize().testTag(HomeTags.VoiceBtn),
                        shape = CircleShape,
                        color = textSecondary.copy(alpha = ChatInputVoiceModeButtonAlpha),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp) {
                          Box(
                              modifier = Modifier.fillMaxSize(),
                              contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = "Voice mode",
                                    tint = textSecondary,
                                    modifier = Modifier.size(ChatInputVoiceSendIconSize))
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
                                      strokeWidth = ChatInputProgressIndicatorStrokeWidth,
                                      modifier = Modifier.size(ChatInputVoiceSendIconSize),
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
                                      modifier = Modifier.size(ChatInputVoiceSendIconSize))
                                }
                              }
                        }
                  }
                }
              }
        }
  }

  // Main Container
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = InputHorizontal)
              .heightIn(min = InputHeight)) {
        if (isListening) {
          // --- LISTENING MODE (Visualizer) ---
          // Uses a Surface with the same shape/color as the TextField to prevent UI jumps
          Surface(
              modifier = Modifier.fillMaxWidth().height(ChatInputListeningViewHeight),
              shape = RoundedCornerShape(ChatInputCornerRadius),
              color = surfaceVariantColor) {
                Row(
                    modifier =
                        Modifier.fillMaxSize()
                            .padding(
                                start = ChatInputListeningViewPaddingStart,
                                end = ChatInputListeningViewPaddingEnd),
                    verticalAlignment = Alignment.CenterVertically) {

                      // Animated voice bars (simulated waveform)
                      val infiniteTransition = rememberInfiniteTransition(label = "voice-bars")
                      val barCount = 5
                      val delays =
                          ChatInputVoiceBarAnimationDelays // Staggered start for wave effect

                      Row(
                          horizontalArrangement = Arrangement.spacedBy(ChatInputVoiceBarSpacing),
                          verticalAlignment = Alignment.CenterVertically) {
                            repeat(barCount) { index ->
                              val scale by
                                  infiniteTransition.animateFloat(
                                      initialValue = ChatInputVoiceBarAnimationInitialScale,
                                      targetValue = ChatInputVoiceBarAnimationTargetScale,
                                      animationSpec =
                                          infiniteRepeatable(
                                              animation =
                                                  tween(
                                                      durationMillis =
                                                          ChatInputVoiceBarAnimationDurationMillis,
                                                      delayMillis = delays[index],
                                                      easing = LinearEasing),
                                              repeatMode = RepeatMode.Reverse),
                                      label = "bar-$index")

                              Box(
                                  modifier =
                                      Modifier.width(ChatInputVoiceBarWidth)
                                          .height(
                                              (ChatInputVoiceBarMaxHeight * scale).coerceAtLeast(
                                                  ChatInputVoiceBarMinHeight))
                                          .clip(CircleShape)
                                          .background(
                                              EulerRed.copy(alpha = ChatInputVoiceBarColorAlpha)))
                            }
                          }

                      Spacer(Modifier.width(ChatInputVoiceBarSpacerWidth))

                      Text(
                          text = "Listening...",
                          color = textSecondary,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.Normal)

                      // Push buttons to the end
                      Spacer(Modifier.weight(1f))

                      // Show buttons so user can stop listening or send
                      buttonsUI()
                    }
              }
        } else {
          // --- NORMAL TEXT MODE ---
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
              modifier =
                  Modifier.fillMaxWidth()
                      .heightIn(min = InputHeight)
                      .testTag(HomeTags.MessageField),
              enabled = enabled,
              singleLine = false,
              maxLines = ChatInputMaxLines, // Allow expansion up to 5 lines
              textStyle =
                  MaterialTheme.typography.bodyMedium.copy(
                      fontSize = 15.sp, fontWeight = FontWeight.Normal),
              trailingIcon = buttonsUI, // Reusing the same buttons
              shape = RoundedCornerShape(ChatInputCornerRadius),
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
      }
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

private fun startPdfDownload(context: Context, attachment: ChatAttachment) {
  val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
  val fileName = attachment.title.ifBlank { "document.pdf" }
  val request =
      DownloadManager.Request(Uri.parse(attachment.url))
          .setMimeType(attachment.mimeType)
          .setTitle(fileName)
          .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
  runCatching {
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
  }
  manager.enqueue(request)
  Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
}

@Composable
internal fun PdfViewerDialog(
    url: String,
    onDismiss: () -> Unit,
    testBitmaps: List<ImageBitmap>? = null,
    testForceLoading: Boolean = false
) {
  var loadFailed by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(true) }
  var pageBitmaps by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
  var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
  var parcel by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
  val context = LocalContext.current

  val openExternally: () -> Unit = {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure {
          Toast.makeText(context, "No app available to open the PDF", Toast.LENGTH_SHORT).show()
        }
  }

  DisposableEffect(url) {
    onDispose {
      renderer?.close()
      parcel?.close()
    }
  }

  LaunchedEffect(url, testBitmaps) {
    if (testForceLoading) {
      isLoading = true
      loadFailed = false
      pageBitmaps = emptyList()
      return@LaunchedEffect
    }
    if (testBitmaps != null) {
      pageBitmaps = testBitmaps
      isLoading = false
      loadFailed = testBitmaps.isEmpty()
      return@LaunchedEffect
    }
    isLoading = true
    loadFailed = false
    withContext(Dispatchers.IO) {
      runCatching {
            val file = downloadPdfToCache(context, url)
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            parcel = pfd
            renderer = pdfRenderer
            val all = renderAllPagesToBitmaps(pdfRenderer).map { it.asImageBitmap() }
            withContext(Dispatchers.Main) { pageBitmaps = all }
            withContext(Dispatchers.Main) { isLoading = false }
          }
          .onFailure {
            withContext(Dispatchers.Main) {
              loadFailed = true
              isLoading = false
            }
          }
    }
  }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = "Document",
                      style =
                          MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onBackground,
                      modifier = Modifier.weight(1f))
                  IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close PDF")
                  }
                }
            Divider()
            if (isLoading) {
              LinearProgressIndicator(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp))
              Text(
                  text = "Loading PDF...",
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                  style = MaterialTheme.typography.bodySmall)
              Button(
                  onClick = openExternally,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Open in another app")
                  }
            }
            if (loadFailed) {
              Column(
                  modifier = Modifier.fillMaxSize().padding(16.dp).testTag("pdf_error"),
                  verticalArrangement = Arrangement.Center,
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unable to display the PDF here.",
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openExternally() }) { Text("Open in another app") }
                  }
            } else {
              val scrollState = rememberScrollState()
              Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                pageBitmaps.forEachIndexed { idx, bmp ->
                  androidx.compose.foundation.Image(
                      bitmap = bmp,
                      contentDescription = "PDF page ${idx + 1}",
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(vertical = 8.dp)
                              .testTag("pdf_page_${idx + 1}"),
                      contentScale = ContentScale.FillWidth)
                }
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
internal fun BubbleSendButton(enabled: Boolean, isSending: Boolean, onClick: () -> Unit) {
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
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp).testTag("send_loading"),
                color = colorScheme.onPrimary)
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
                  modifier = Modifier.size(18.dp).testTag("send_icon"))
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
            Modifier.clip(RoundedCornerShape(Dimensions.CompactIndicatorCornerRadius))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(
                    horizontal = Dimensions.CompactIndicatorPaddingHorizontal,
                    vertical = Dimensions.CompactIndicatorPaddingVertical),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "$emoji $siteLabel",
              color = colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
        }
  }
  // Note: RAG sources (compactType == NONE) are now handled by RagSourceBadge inline with audio
  // button
}

@Preview(showBackground = true, backgroundColor = 0x000000)
@Composable
private fun HomeScreenPreview() {
  val previewViewModel = remember { HomeViewModel() }
  MaterialTheme { HomeScreen(viewModel = previewViewModel) }
}
