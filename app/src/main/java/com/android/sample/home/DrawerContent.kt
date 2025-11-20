package com.android.sample.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.android.sample.ui.theme.EulerDrawerBackground
import com.android.sample.ui.theme.EulerDrawerDivider
import com.android.sample.ui.theme.EulerDrawerEmptyText
import com.android.sample.ui.theme.EulerDrawerMutedIcon
import com.android.sample.ui.theme.EulerDrawerSectionLabel
import com.android.sample.ui.theme.EulerNewChatCircleRed
import com.android.sample.ui.theme.EulerNewChatTextRed
import com.android.sample.ui.theme.EulerRecentRowIconBackground
import com.android.sample.ui.theme.EulerRecentRowSelectedBg
import java.util.Locale

/** Test tags used to find drawer elements in UI tests. */
object DrawerTags {
  const val Root = "drawer_root"
  const val NewChatRow = "drawer_newchat_row"
  const val ConnectorsRow = "drawer_connectors_row"
  const val RecentsSection = "drawer_recents"
  const val ViewAllRow = "drawer_view_all"
  const val UserSettings = "drawer_user_settings"
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
 * @param onNewChat Called when the “New chat” row is pressed.
 * @param onPickConversation Called when a conversation entry is selected.
 */
@Composable
fun DrawerContent(
    ui: HomeUiState = HomeUiState(),
    onToggleSystem: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onProfileDisabledClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onPickConversation: (String) -> Unit = {}
) {
  // Controls RECENTS vs ALL CHATS; reset every time the drawer is reopened
  var showAllChats by remember(ui.isDrawerOpen) { mutableStateOf(false) }

  Column(
      modifier =
          Modifier.fillMaxHeight()
              .width(300.dp)
              .background(EulerDrawerBackground)
              .padding(horizontal = 20.dp, vertical = 16.dp)
              .testTag(DrawerTags.Root)) {
        DrawerHeader()

        Spacer(modifier = Modifier.height(35.dp))

        DrawerNewChatRow(onNewChat = onNewChat)

        Spacer(modifier = Modifier.height(12.dp))

        DrawerConnectorsRow(onSettingsClick = onSettingsClick)

        Spacer(modifier = Modifier.height(24.dp))

        DrawerConversationsSection(
            ui = ui,
            showAllChats = showAllChats,
            onShowAllChats = { showAllChats = true },
            onPickConversation = onPickConversation,
            modifier = Modifier.weight(1f),
        )

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
private fun DrawerConnectorsRow(onSettingsClick: () -> Unit) {
  Surface(
      color = Color.Transparent,
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSettingsClick() }
              .padding(vertical = 12.dp)
              .testTag(DrawerTags.ConnectorsRow)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
              Icons.Filled.Link,
              contentDescription = Localization.t("connectors"),
              tint = EulerDrawerMutedIcon)
          Spacer(Modifier.width(12.dp))
          Text(
              Localization.t("connectors"),
              color = Color.White,
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
 * @param onShowAllChats Called when the user taps the “View all chats” row.
 * @param onPickConversation Called when a conversation row is tapped.
 * @param modifier Modifier applied to the root column (e.g. weight + padding).
 */
@Composable
private fun DrawerConversationsSection(
    ui: HomeUiState,
    showAllChats: Boolean,
    onShowAllChats: () -> Unit,
    onPickConversation: (String) -> Unit,
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
 * @param onShowAllChats Called when the user taps the “View all chats” row.
 * @param onPickConversation Invoked when a conversation row is selected.
 */
@Composable
private fun DrawerConversationsList(
    ui: HomeUiState,
    showAllChats: Boolean,
    onShowAllChats: () -> Unit,
    onPickConversation: (String) -> Unit,
) {
  val hasMoreThanLimit = ui.conversations.size > RECENT_CONVERSATIONS_LIMIT
  val isShowingAll = !hasMoreThanLimit || showAllChats
  val sectionTitleKey = if (isShowingAll) "all_chats" else "recents"

  Text(
      text = Localization.t(sectionTitleKey),
      color = EulerDrawerSectionLabel,
      fontSize = 12.sp,
      modifier = Modifier.testTag(DrawerTags.RecentsSection))
  Spacer(modifier = Modifier.height(14.dp))

  val displayedConversations =
      if (isShowingAll) {
        ui.conversations
      } else {
        ui.conversations.take(RECENT_CONVERSATIONS_LIMIT)
      }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    displayedConversations.forEach { conv ->
      RecentRow(
          title = conv.title.ifBlank { Localization.t("untitled_conversation") },
          selected = conv.id == ui.currentConversationId,
          onClick = { onPickConversation(conv.id) })
    }

    if (hasMoreThanLimit && !showAllChats) {
      ViewAllChatsRow(onShowAllChats = onShowAllChats)
    }
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
              color = Color.White,
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
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f).clickable { onProfile() })
        Icon(
            Icons.Filled.Settings,
            contentDescription = Localization.t("settings"),
            tint = Color.White,
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
 * @param selected Whether this row represents the currently selected conversation.
 * @param onClick Invoked when the row is tapped.
 */
@Composable
private fun RecentRow(title: String, selected: Boolean = false, onClick: () -> Unit = {}) {
  val bg = if (selected) EulerRecentRowSelectedBg else Color.Transparent
  Surface(
      color = bg,
      shape = RoundedCornerShape(8.dp),
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onClick() }
              .padding(vertical = 6.dp, horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(24.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(EulerRecentRowIconBackground),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = EulerDrawerSectionLabel,
                    modifier = Modifier.size(15.dp))
              }
          Spacer(Modifier.width(12.dp))
          Text(
              text = title,
              color = Color.White,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
      }
}

/** Preview for the drawer in isolation with default state. */
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DrawerContentPreview() {
  MaterialTheme { DrawerContent() }
}
