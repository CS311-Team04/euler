package com.android.sample.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.settings.Localization
import com.android.sample.ui.theme.EulerDrawerAvatarBackground
import com.android.sample.ui.theme.EulerDrawerDivider
import com.android.sample.ui.theme.EulerDrawerEmptyText
import com.android.sample.ui.theme.EulerDrawerMutedIcon
import com.android.sample.ui.theme.EulerDrawerSectionLabel
import com.android.sample.ui.theme.EulerNewChatCircleRed
import com.android.sample.ui.theme.EulerNewChatTextRed
import java.util.Locale

/** Test tags used to find drawer elements in UI tests. */
object DrawerTags {
  const val Root = "drawer_root"
  const val NewChatRow = "drawer_newchat_row"
  const val ConnectorsRow = "drawer_connectors_row"
  const val RecentsSection = "drawer_recents"
  const val ViewAllRow = "drawer_view_all"
  const val UserSettings = "drawer_user_settings"
  const val DeleteButton = "drawer_delete_button"
  const val CancelButton = "drawer_cancel_button"
  const val ConversationRow = "drawer_conversation_row"
}

/**
 * Max number of conversations shown by default in "RECENTS" mode. If the user has more than this,
 * the "View all chats" row is displayed.
 */
private const val RECENT_CONVERSATIONS_LIMIT = 4

@Composable
fun DrawerContentPreviewable() {
  DrawerContent()
}

/**
 * Main drawer content used on the Home screen.
 *
 * Structure:
 * - Header: Euler logo
 * - Primary actions: "New chat", "Connectors"
 * - Body: conversations section (RECENTS / ALL CHATS, scrollable)
 * - Footer: user/profile area and Settings
 *
 * @param ui Current home UI state (user name, guest flag, conversations, drawer open/close, …).
 * @param onToggleSystem Unused for now, reserved for future system toggles.
 * @param onSignOut Unused for now, reserved for a future sign-out action from the drawer.
 * @param onSettingsClick Called when either the Connectors row or the Settings icon is tapped.
 * @param onProfileClick Called when the profile avatar/name is tapped in non-guest mode.
 * @param onProfileDisabledClick Called when the profile row is tapped while in guest mode.
 * @param onClose Unused for now, reserved for a “close drawer” action if needed later.
 * @param onNewChat Called when the "New chat" row is pressed.
 * @param onPickConversation Called when a conversation entry is selected.
 * @param onDeleteConversations Called when conversations should be deleted (with their IDs).
 * @param testLongPressConversationId For testing only: directly trigger long press on this
 *   conversation ID.
 */
@Composable
fun DrawerContent(
    ui: HomeUiState = HomeUiState(),
    onToggleSystem: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onConnectorsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onProfileDisabledClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onPickConversation: (String) -> Unit = {},
    onDeleteConversations: (List<String>) -> Unit = {},
    testLongPressConversationId: String? = null
) {
  val colorScheme = MaterialTheme.colorScheme
  val drawerBackground = colorScheme.surface
  val textPrimary = colorScheme.onSurface
  val textSecondary = colorScheme.onSurfaceVariant
  val accent = colorScheme.primary
  val dividerColor = colorScheme.outline.copy(alpha = 0.2f)
  // Controls RECENTS vs ALL CHATS; reset every time the drawer is reopened
  var showAllChats by remember(ui.isDrawerOpen) { mutableStateOf(false) }

  // Selection mode state
  var selectedConversationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
  var isSelectionMode by remember { mutableStateOf(false) }

  // Test helper: directly trigger long press for testing (must run before drawer close check)
  LaunchedEffect(testLongPressConversationId) {
    testLongPressConversationId?.let { id ->
      isSelectionMode = true
      selectedConversationIds = setOf(id)
    }
  }

  // Reset selection when drawer closes (but not if test helper is active)
  LaunchedEffect(ui.isDrawerOpen, testLongPressConversationId) {
    if (!ui.isDrawerOpen && testLongPressConversationId == null) {
      selectedConversationIds = emptySet()
      isSelectionMode = false
    }
  }

  Column(
      modifier =
          Modifier.fillMaxHeight()
              .width(300.dp)
              .background(drawerBackground)
              .padding(horizontal = 20.dp, vertical = 16.dp)
              .testTag(DrawerTags.Root)) {
        DrawerHeader()

        Spacer(modifier = Modifier.height(35.dp))

        DrawerNewChatRow(onNewChat = onNewChat)

        Spacer(modifier = Modifier.height(12.dp))

        DrawerConnectorsRow(onConnectorsClick = onConnectorsClick)

        Spacer(modifier = Modifier.height(24.dp))

        DrawerConversationsSection(
            ui = ui,
            showAllChats = showAllChats,
            onShowAllChats = { showAllChats = true },
            onPickConversation = onPickConversation,
            isSelectionMode = isSelectionMode,
            selectedConversationIds = selectedConversationIds,
            onToggleSelection = { id ->
              selectedConversationIds =
                  if (selectedConversationIds.contains(id)) {
                    val newSet = selectedConversationIds - id
                    if (newSet.isEmpty()) {
                      isSelectionMode = false
                    }
                    newSet
                  } else {
                    selectedConversationIds + id
                  }
            },
            onLongPressConversation = { id ->
              isSelectionMode = true
              selectedConversationIds = setOf(id)
            },
            modifier = Modifier.weight(1f),
        )

        // Selection mode actions
        if (isSelectionMode) {
          Spacer(modifier = Modifier.height(12.dp))
          DrawerSelectionActions(
              selectedCount = selectedConversationIds.size,
              onDelete = {
                onDeleteConversations(selectedConversationIds.toList())
                selectedConversationIds = emptySet()
                isSelectionMode = false
              },
              onCancel = {
                selectedConversationIds = emptySet()
                isSelectionMode = false
              })
        }

        DrawerFooter(
            isGuest = ui.isGuest,
            displayName = formatUserName(ui.userName),
            onProfileClick = onProfileClick,
            onProfileDisabledClick = onProfileDisabledClick,
            onSettingsClick = onSettingsClick,
        )
      }
}

/** Top header of the drawer containing only the Euler logo. */
@Composable
private fun DrawerHeader() {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Image(
        painter = painterResource(id = R.drawable.euler_logo),
        contentDescription = Localization.t("euler_logo"),
        modifier = Modifier.height(30.dp).offset(x = 1.dp, y = 5.dp),
        contentScale = ContentScale.Fit)
  }
}

/**
 * “New chat” primary action row.
 *
 * Visually appears as a red circular plus icon followed by the label. Tapping the whole row invokes
 * [onNewChat].
 */
@Composable
private fun DrawerNewChatRow(onNewChat: () -> Unit) {
  Surface(
      color = Color.Transparent,
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onNewChat() }
              .padding(vertical = 12.dp)
              .testTag(DrawerTags.NewChatRow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier = Modifier.size(28.dp).clip(CircleShape).background(EulerNewChatCircleRed),
              contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = Localization.t("new_chat"),
                    tint = Color.White)
              }
          Spacer(Modifier.width(12.dp))
          Text(
              Localization.t("new_chat"),
              color = EulerNewChatTextRed,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal)
        }
      }
}

/**
 * “Connectors” row, currently wired to the Settings screen.
 *
 * Tapping the row invokes [onSettingsClick]. A dedicated tag is exposed so tests can distinguish
 * this entry from the bottom Settings icon.
 */
@Composable
private fun DrawerConnectorsRow(onConnectorsClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  val primaryTextColor = colorScheme.onSurface
  val mutedIconColor = colorScheme.onSurfaceVariant

  Surface(
      color = Color.Transparent,
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onConnectorsClick() }
              .padding(vertical = 12.dp)
              .testTag(DrawerTags.ConnectorsRow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              Icons.Filled.Link,
              contentDescription = Localization.t("connectors"),
              tint = mutedIconColor)
          Spacer(Modifier.width(12.dp))
          Text(
              Localization.t("connectors"),
              color = primaryTextColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal)
          Spacer(Modifier.weight(1f))
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = mutedIconColor)
        }
      }
}

/**
 * Scrollable conversations section that sits between header and footer.
 *
 * This composable is responsible only for wiring:
 * - a vertical scroll container, and
 * - the choice between the empty state and the populated list.
 *
 * The actual UI for both states is delegated to [DrawerConversationsEmptyState] and
 * [DrawerConversationsList] to keep the control flow simple and testable.
 *
 * @param ui Current [HomeUiState] providing the conversations list.
 * @param showAllChats Whether ALL CHATS mode is currently active.
 * @param onShowAllChats Called when the user taps the "View all chats" row.
 * @param onPickConversation Called when a conversation row is tapped.
 * @param isSelectionMode Whether selection mode is active.
 * @param selectedConversationIds Set of selected conversation IDs.
 * @param onToggleSelection Called when a conversation's selection should be toggled.
 * @param onLongPressConversation Called when a conversation is long-pressed.
 * @param modifier Modifier applied to the root column (e.g. weight + padding).
 */
@Composable
private fun DrawerConversationsSection(
    ui: HomeUiState,
    showAllChats: Boolean,
    onShowAllChats: () -> Unit,
    onPickConversation: (String) -> Unit,
    isSelectionMode: Boolean,
    selectedConversationIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onLongPressConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()

  Column(modifier = modifier.verticalScroll(scrollState)) {
    if (ui.conversations.isEmpty()) {
      DrawerConversationsEmptyState()
    } else {
      DrawerConversationsList(
          ui = ui,
          showAllChats = showAllChats,
          onShowAllChats = onShowAllChats,
          onPickConversation = onPickConversation,
          selectionState =
              DrawerSelectionState(
                  isSelectionMode = isSelectionMode,
                  selectedConversationIds = selectedConversationIds,
                  onToggleSelection = onToggleSelection,
                  onLongPressConversation = onLongPressConversation),
      )
    }
  }
}

/**
 * Empty state shown when the user has no stored conversations yet.
 *
 * Renders the "RECENTS" section label followed by a short placeholder message. This keeps the
 * layout consistent with the populated state while clearly communicating that there is nothing to
 * list.
 */
@Composable
private fun DrawerConversationsEmptyState() {
  Text(
      text = Localization.t("recents"),
      color = EulerDrawerSectionLabel,
      fontSize = 12.sp,
      modifier = Modifier.testTag(DrawerTags.RecentsSection))
  Spacer(modifier = Modifier.height(14.dp))
  Text(Localization.t("no_conversations_yet"), color = EulerDrawerEmptyText, fontSize = 13.sp)
}

/**
 * State data class for selection mode parameters. Groups related selection parameters to reduce
 * function parameter count.
 */
private data class DrawerSelectionState(
    val isSelectionMode: Boolean,
    val selectedConversationIds: Set<String>,
    val onToggleSelection: (String) -> Unit,
    val onLongPressConversation: (String) -> Unit
)

/**
 * Populated conversations list for the drawer.
 *
 * Behavior:
 * - Computes whether we should show only the last [RECENT_CONVERSATIONS_LIMIT] items ("RECENTS"
 *   mode) or the full list ("ALL CHATS" mode).
 * - Shows the appropriate section title based on that mode.
 * - Renders each conversation as a [RecentRow].
 * - If there are more than [RECENT_CONVERSATIONS_LIMIT] conversations and we are still in "RECENTS"
 *   mode, appends a [ViewAllChatsRow] at the bottom.
 *
 * @param ui Current [HomeUiState] providing the ordered conversations.
 * @param showAllChats True when the user has expanded to "ALL CHATS".
 * @param onShowAllChats Called when the user taps the "View all chats" row.
 * @param onPickConversation Invoked when a conversation row is selected.
 * @param selectionState State and callbacks for selection mode.
 */
@Composable
private fun DrawerConversationsList(
    ui: HomeUiState,
    showAllChats: Boolean,
    onShowAllChats: () -> Unit,
    onPickConversation: (String) -> Unit,
    selectionState: DrawerSelectionState,
) {
  val displayedConversations = computeDisplayedConversations(ui.conversations, showAllChats)
  val sectionTitleKey =
      if (showAllChats || ui.conversations.size <= RECENT_CONVERSATIONS_LIMIT) "all_chats"
      else "recents"

  DrawerConversationsSectionHeader(sectionTitleKey)
  DrawerConversationsItems(
      conversations = displayedConversations,
      currentConversationId = ui.currentConversationId,
      selectionState = selectionState,
      onPickConversation = onPickConversation)

  if (ui.conversations.size > RECENT_CONVERSATIONS_LIMIT && !showAllChats) {
    ViewAllChatsRow(onShowAllChats = onShowAllChats)
  }
}

/** Computes which conversations should be displayed based on the current mode. */
private fun computeDisplayedConversations(
    conversations: List<com.android.sample.conversations.Conversation>,
    showAllChats: Boolean
): List<com.android.sample.conversations.Conversation> =
    if (showAllChats || conversations.size <= RECENT_CONVERSATIONS_LIMIT) {
      conversations
    } else {
      conversations.take(RECENT_CONVERSATIONS_LIMIT)
    }

/** Renders the section header (title) for the conversations list. */
@Composable
private fun DrawerConversationsSectionHeader(sectionTitleKey: String) {
  Text(
      text = Localization.t(sectionTitleKey),
      color = EulerDrawerSectionLabel,
      fontSize = 12.sp,
      modifier = Modifier.testTag(DrawerTags.RecentsSection))
  Spacer(modifier = Modifier.height(14.dp))
}

/** Renders the list of conversation items. */
@Composable
private fun DrawerConversationsItems(
    conversations: List<com.android.sample.conversations.Conversation>,
    currentConversationId: String?,
    selectionState: DrawerSelectionState,
    onPickConversation: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    conversations.forEach { conv ->
      DrawerConversationItem(
          conversation = conv,
          currentConversationId = currentConversationId,
          selectionState = selectionState,
          onPickConversation = onPickConversation)
    }
  }
}

/** Renders a single conversation item row. */
@Composable
private fun DrawerConversationItem(
    conversation: com.android.sample.conversations.Conversation,
    currentConversationId: String?,
    selectionState: DrawerSelectionState,
    onPickConversation: (String) -> Unit,
) {
  val isSelected =
      computeIsSelected(
          conversationId = conversation.id,
          currentConversationId = currentConversationId,
          isSelectionMode = selectionState.isSelectionMode,
          selectedConversationIds = selectionState.selectedConversationIds)

  val isItemSelected = selectionState.selectedConversationIds.contains(conversation.id)
  val title = conversation.title.ifBlank { Localization.t("untitled_conversation") }

  RecentRow(
      title = title,
      selected = isSelected,
      isSelectionMode = selectionState.isSelectionMode,
      isItemSelected = isItemSelected,
      onClick = {
        if (selectionState.isSelectionMode) {
          selectionState.onToggleSelection(conversation.id)
        } else {
          onPickConversation(conversation.id)
        }
      },
      onLongClick = { selectionState.onLongPressConversation(conversation.id) })
}

/** Computes whether a conversation should be visually marked as selected. */
private fun computeIsSelected(
    conversationId: String,
    currentConversationId: String?,
    isSelectionMode: Boolean,
    selectedConversationIds: Set<String>
): Boolean {
  return if (isSelectionMode) {
    selectedConversationIds.contains(conversationId)
  } else {
    conversationId == currentConversationId
  }
}

/**
 * Row displayed at the end of the RECENTS list when there are more conversations than
 * [RECENT_CONVERSATIONS_LIMIT].
 *
 * Shows a "View all chats" label with a chevron icon. Tapping the row promotes the drawer into "ALL
 * CHATS" mode by invoking [onShowAllChats].
 *
 * @param onShowAllChats Callback used to switch from RECENTS to ALL CHATS mode.
 */
@Composable
private fun ViewAllChatsRow(onShowAllChats: () -> Unit) {
  val primaryTextColor = MaterialTheme.colorScheme.onSurface

  Surface(
      color = Color.Transparent,
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onShowAllChats() }
              .padding(vertical = 4.dp)
              .testTag(DrawerTags.ViewAllRow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
              Localization.t("view_all_chats"),
              color = primaryTextColor,
              fontSize = 16.sp,
              fontWeight = FontWeight.Normal)
          Spacer(Modifier.weight(1f))
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = EulerDrawerMutedIcon)
        }
      }
}

/**
 * Drawer footer showing the current user and a Settings entry.
 * - The Settings icon always calls [onSettingsClick].
 */
@Composable
private fun DrawerFooter(
    isGuest: Boolean,
    displayName: String,
    onProfileClick: () -> Unit,
    onProfileDisabledClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme
  val primaryTextColor = colorScheme.onSurface

  Surface(color = EulerDrawerDivider, modifier = Modifier.fillMaxWidth().height(1.dp)) {}
  Spacer(Modifier.height(12.dp))

  val alpha = if (isGuest) 0.4f else 1f
  val onProfile = {
    if (isGuest) {
      onProfileDisabledClick()
    } else {
      onProfileClick()
    }
  }

  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        Box(
            modifier =
                Modifier.size(36.dp)
                    .clip(CircleShape)
                    .background(EulerDrawerAvatarBackground)
                    .clickable { onProfile() },
            contentAlignment = Alignment.Center) {
              Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }
        Spacer(Modifier.width(12.dp))
        Text(
            displayName,
            color = primaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f).clickable { onProfile() })
        Icon(
            Icons.Filled.Settings,
            contentDescription = Localization.t("settings"),
            tint = primaryTextColor,
            modifier =
                Modifier.size(20.dp)
                    .clickable { onSettingsClick() }
                    .testTag(DrawerTags.UserSettings))
      }

  Spacer(Modifier.height(12.dp))
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
    Text(Localization.t("powered_by"), color = Color.Gray, fontSize = 12.sp)
  }
}

/**
 * Formats the raw username into a display-friendly value.
 * - Falls back to "Student" when the name is blank.
 * - Normalizes whitespace and capitalizes each word.
 */
private fun formatUserName(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isEmpty()) return Localization.t("default_user_name")
  return trimmed.split("\\s+".toRegex()).joinToString(" ") { word ->
    word.replaceFirstChar { ch ->
      if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
    }
  }
}

/**
 * Single conversation row in the drawer.
 *
 * @param title Conversation title (already fallback-handled upstream).
 * @param selected Whether this row represents the currently selected conversation (in normal mode).
 * @param isSelectionMode Whether selection mode is active.
 * @param isItemSelected Whether this item is selected in selection mode.
 * @param onClick Invoked when the row is tapped.
 * @param onLongClick Invoked when the row is long-pressed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentRow(
    title: String,
    selected: Boolean = false,
    isSelectionMode: Boolean = false,
    isItemSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
  val colorScheme = MaterialTheme.colorScheme
  val bg =
      when {
        isSelectionMode && isItemSelected -> colorScheme.onSurface.copy(alpha = 0.08f)
        !isSelectionMode && selected -> colorScheme.onSurface.copy(alpha = 0.08f)
        else -> Color.Transparent
      }

  val iconBackground = colorScheme.surfaceVariant
  val iconTint = colorScheme.onSurfaceVariant
  val textColor = colorScheme.onSurface
  Surface(
      color = bg,
      shape = RoundedCornerShape(8.dp),
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .combinedClickable(onClick = onClick, onLongClick = onLongClick)
              .padding(vertical = 6.dp, horizontal = 2.dp)
              .testTag(DrawerTags.ConversationRow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(iconBackground),
              contentAlignment = Alignment.Center) {
                if (isSelectionMode && isItemSelected) {
                  Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = null,
                      // Using iconTint (defined in 'main') instead of Color.White
                      // so the icon is visible on the light background.
                      tint = iconTint,
                      modifier = Modifier.size(15.dp))
                } else {
                  Icon(
                      imageVector = Icons.Outlined.ChatBubbleOutline,
                      contentDescription = null,
                      // Using the 'main' theme color
                      tint = iconTint,
                      modifier = Modifier.size(15.dp))
                }
              }
          Spacer(Modifier.width(12.dp))
          Text(
              text = title,
              color = textColor,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
      }
}

/** Selection mode actions bar showing delete and cancel buttons. */
@Composable
private fun DrawerSelectionActions(selectedCount: Int, onDelete: () -> Unit, onCancel: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = EulerNewChatCircleRed,
            shape = RoundedCornerShape(8.dp),
            modifier =
                Modifier.weight(1f)
                    .clickable { onDelete() }
                    .padding(vertical = 12.dp)
                    .testTag(DrawerTags.DeleteButton)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete ($selectedCount)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }

        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier =
                Modifier.weight(1f)
                    .clickable { onCancel() }
                    .padding(vertical = 12.dp)
                    .testTag(DrawerTags.CancelButton)) {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cancel",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }
      }
}

/** Preview for the drawer in isolation with default state. */
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
