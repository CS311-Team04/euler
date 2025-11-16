package com.android.sample.home

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class HomeScreenTest {

  @Test
  fun HomeTags_has_correct_tag_constants() {
    assertEquals("home_root", HomeTags.Root)
    assertEquals("home_menu_btn", HomeTags.MenuBtn)
    assertEquals("home_topright_btn", HomeTags.TopRightBtn)
    assertEquals("home_action1_btn", HomeTags.Action1Btn)
    assertEquals("home_action2_btn", HomeTags.Action2Btn)
    assertEquals("home_message_field", HomeTags.MessageField)
    assertEquals("home_send_btn", HomeTags.SendBtn)
    assertEquals("home_drawer", HomeTags.Drawer)
    assertEquals("home_topright_menu", HomeTags.TopRightMenu)
  }

  @Test
  fun HomeTags_constants_are_not_empty() {
    assertTrue(HomeTags.Root.isNotEmpty())
    assertTrue(HomeTags.MenuBtn.isNotEmpty())
    assertTrue(HomeTags.TopRightBtn.isNotEmpty())
    assertTrue(HomeTags.Action1Btn.isNotEmpty())
    assertTrue(HomeTags.Action2Btn.isNotEmpty())
    assertTrue(HomeTags.MessageField.isNotEmpty())
    assertTrue(HomeTags.SendBtn.isNotEmpty())
    assertTrue(HomeTags.Drawer.isNotEmpty())
    assertTrue(HomeTags.TopRightMenu.isNotEmpty())
  }

  @Test
  fun HomeTags_constants_are_unique() {
    val tags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)

    assertEquals(tags.size, tags.distinct().size)
  }

  @Test
  fun HomeTags_object_is_not_null() {
    assertNotNull(HomeTags)
  }

  @Test
  fun HomeTags_constants_follow_naming_convention() {
    assertTrue(HomeTags.Root.startsWith("home_"))
    assertTrue(HomeTags.MenuBtn.startsWith("home_"))
    assertTrue(HomeTags.TopRightBtn.startsWith("home_"))
    assertTrue(HomeTags.Action1Btn.startsWith("home_"))
    assertTrue(HomeTags.Action2Btn.startsWith("home_"))
    assertTrue(HomeTags.MessageField.startsWith("home_"))
    assertTrue(HomeTags.SendBtn.startsWith("home_"))
    assertTrue(HomeTags.Drawer.startsWith("home_"))
    assertTrue(HomeTags.TopRightMenu.startsWith("home_"))
  }

  @Test
  fun HomeTags_does_not_contain_whitespace() {
    assertFalse(HomeTags.Root.contains(" "))
    assertFalse(HomeTags.MenuBtn.contains(" "))
    assertFalse(HomeTags.TopRightBtn.contains(" "))
    assertFalse(HomeTags.Action1Btn.contains(" "))
    assertFalse(HomeTags.Action2Btn.contains(" "))
    assertFalse(HomeTags.MessageField.contains(" "))
    assertFalse(HomeTags.SendBtn.contains(" "))
    assertFalse(HomeTags.Drawer.contains(" "))
    assertFalse(HomeTags.TopRightMenu.contains(" "))
  }

  @Test
  fun HomeTags_constants_length_check() {
    assertTrue(HomeTags.Root.length > 5)
    assertTrue(HomeTags.MenuBtn.length > 5)
    assertTrue(HomeTags.TopRightBtn.length > 5)
    assertTrue(HomeTags.Action1Btn.length > 5)
    assertTrue(HomeTags.Action2Btn.length > 5)
    assertTrue(HomeTags.MessageField.length > 5)
    assertTrue(HomeTags.SendBtn.length > 5)
    assertTrue(HomeTags.Drawer.length > 5)
    assertTrue(HomeTags.TopRightMenu.length > 5)
  }

  @Test
  fun HomeTags_is_singleton_object() {
    val instance1 = HomeTags
    val instance2 = HomeTags
    assertSame(instance1, instance2)
  }

  @Test
  fun HomeTags_object_class_name() {
    assertEquals("HomeTags", HomeTags::class.java.simpleName)
  }

  @Test
  fun HomeTags_root_tag_is_consistent() {
    val root = HomeTags.Root
    assertTrue(root.startsWith("home_"))
    assertTrue(root.endsWith("root"))
  }

  @Test
  fun HomeTags_menu_btn_tag_is_consistent() {
    val menuBtn = HomeTags.MenuBtn
    assertTrue(menuBtn.contains("menu"))
    assertTrue(menuBtn.contains("btn"))
  }

  @Test
  fun HomeTags_topright_btn_tag_is_consistent() {
    val topRightBtn = HomeTags.TopRightBtn
    assertTrue(topRightBtn.contains("topright"))
    assertTrue(topRightBtn.contains("btn"))
  }

  @Test
  fun HomeTags_message_field_tag_is_consistent() {
    val messageField = HomeTags.MessageField
    assertTrue(messageField.contains("message"))
    assertTrue(messageField.contains("field"))
  }

  @Test
  fun HomeTags_send_btn_tag_is_consistent() {
    val sendBtn = HomeTags.SendBtn
    assertTrue(sendBtn.contains("send"))
    assertTrue(sendBtn.contains("btn"))
  }

  @Test
  fun HomeTags_constants_have_expected_prefix() {
    val expectedPrefix = "home_"
    assertTrue(HomeTags.Root.startsWith(expectedPrefix))
    assertTrue(HomeTags.MenuBtn.startsWith(expectedPrefix))
    assertTrue(HomeTags.TopRightBtn.startsWith(expectedPrefix))
    assertTrue(HomeTags.Action1Btn.startsWith(expectedPrefix))
    assertTrue(HomeTags.Action2Btn.startsWith(expectedPrefix))
    assertTrue(HomeTags.MessageField.startsWith(expectedPrefix))
    assertTrue(HomeTags.SendBtn.startsWith(expectedPrefix))
    assertTrue(HomeTags.Drawer.startsWith(expectedPrefix))
    assertTrue(HomeTags.TopRightMenu.startsWith(expectedPrefix))
  }

  @Test
  fun HomeTags_action_buttons_have_btn_suffix() {
    assertTrue(HomeTags.MenuBtn.endsWith("btn"))
    assertTrue(HomeTags.TopRightBtn.endsWith("btn"))
    assertTrue(HomeTags.Action1Btn.endsWith("btn"))
    assertTrue(HomeTags.Action2Btn.endsWith("btn"))
    assertTrue(HomeTags.SendBtn.endsWith("btn"))
  }

  @Test
  fun HomeTags_constants_are_immutable() {
    val root1 = HomeTags.Root
    val root2 = HomeTags.Root
    assertSame(root1, root2)
    assertEquals(root1, root2)
  }

  @Test
  fun HomeTags_drawer_tag_contains_drawer() {
    assertTrue(HomeTags.Drawer.contains("drawer"))
  }

  @Test
  fun HomeTags_topright_menu_tag_contains_menu() {
    assertTrue(HomeTags.TopRightMenu.contains("menu"))
    assertTrue(HomeTags.TopRightMenu.contains("topright"))
  }

  @Test
  fun HomeTags_message_field_tag_contains_field() {
    assertTrue(HomeTags.MessageField.contains("field"))
    assertTrue(HomeTags.MessageField.contains("message"))
  }

  @Test
  fun HomeTags_action_buttons_are_distinct() {
    assertNotEquals(HomeTags.Action1Btn, HomeTags.Action2Btn)
    assertNotEquals(HomeTags.MenuBtn, HomeTags.TopRightBtn)
    assertNotEquals(HomeTags.Action1Btn, HomeTags.SendBtn)
  }

  @Test
  fun HomeTags_all_tags_are_accessible() {
    // Vérifier qu'on peut accéder à toutes les constantes sans exception
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    assertEquals(9, allTags.size)
  }

  @Test
  fun HomeTags_tags_do_not_contain_special_chars() {
    val specialChars = listOf("@", "#", "$", "%", "&", "*", "(", ")", "+", "=")
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      specialChars.forEach { char ->
        assertFalse("Tag $tag should not contain $char", tag.contains(char))
      }
    }
  }

  @Test
  fun HomeTags_tags_use_underscore_separator() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      assertTrue("Tag $tag should use underscore separator", tag.contains("_"))
    }
  }

  @Test
  fun HomeTags_tags_are_lowercase() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag -> assertEquals("Tag $tag should be lowercase", tag.lowercase(), tag) }
  }

  @Test
  fun HomeTags_constants_have_reasonable_length() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      assertTrue("Tag $tag should be at least 8 chars", tag.length >= 8)
      assertTrue("Tag $tag should not exceed 30 chars", tag.length <= 30)
    }
  }

  @Test
  fun HomeTags_root_is_base_tag() {
    assertEquals("home_root", HomeTags.Root)
  }

  @Test
  fun HomeTags_menu_button_tag_is_correct() {
    assertEquals("home_menu_btn", HomeTags.MenuBtn)
  }

  @Test
  fun HomeTags_topright_button_tag_is_correct() {
    assertEquals("home_topright_btn", HomeTags.TopRightBtn)
  }

  @Test
  fun HomeTags_action1_button_tag_is_correct() {
    assertEquals("home_action1_btn", HomeTags.Action1Btn)
  }

  @Test
  fun HomeTags_action2_button_tag_is_correct() {
    assertEquals("home_action2_btn", HomeTags.Action2Btn)
  }

  @Test
  fun HomeTags_message_field_tag_is_correct() {
    assertEquals("home_message_field", HomeTags.MessageField)
  }

  @Test
  fun HomeTags_send_button_tag_is_correct() {
    assertEquals("home_send_btn", HomeTags.SendBtn)
  }

  @Test
  fun HomeTags_drawer_tag_is_correct() {
    assertEquals("home_drawer", HomeTags.Drawer)
  }

  @Test
  fun HomeTags_topright_menu_tag_is_correct() {
    assertEquals("home_topright_menu", HomeTags.TopRightMenu)
  }

  @Test
  fun HomeTags_all_tags_match_expected_values() {
    val expectedTags =
        mapOf(
            "Root" to "home_root",
            "MenuBtn" to "home_menu_btn",
            "TopRightBtn" to "home_topright_btn",
            "Action1Btn" to "home_action1_btn",
            "Action2Btn" to "home_action2_btn",
            "MessageField" to "home_message_field",
            "SendBtn" to "home_send_btn",
            "Drawer" to "home_drawer",
            "TopRightMenu" to "home_topright_menu")
    assertEquals(expectedTags["Root"], HomeTags.Root)
    assertEquals(expectedTags["MenuBtn"], HomeTags.MenuBtn)
    assertEquals(expectedTags["TopRightBtn"], HomeTags.TopRightBtn)
    assertEquals(expectedTags["Action1Btn"], HomeTags.Action1Btn)
    assertEquals(expectedTags["Action2Btn"], HomeTags.Action2Btn)
    assertEquals(expectedTags["MessageField"], HomeTags.MessageField)
    assertEquals(expectedTags["SendBtn"], HomeTags.SendBtn)
    assertEquals(expectedTags["Drawer"], HomeTags.Drawer)
    assertEquals(expectedTags["TopRightMenu"], HomeTags.TopRightMenu)
  }

  @Test
  fun HomeTags_object_is_accessible() {
    assertNotNull(HomeTags)
    val tags = HomeTags
    assertSame(tags, HomeTags)
  }

  @Test
  fun HomeTags_all_constants_are_public() {
    // Toutes les constantes doivent être accessibles
    assertNotNull(HomeTags.Root)
    assertNotNull(HomeTags.MenuBtn)
    assertNotNull(HomeTags.TopRightBtn)
    assertNotNull(HomeTags.Action1Btn)
    assertNotNull(HomeTags.Action2Btn)
    assertNotNull(HomeTags.MessageField)
    assertNotNull(HomeTags.SendBtn)
    assertNotNull(HomeTags.Drawer)
    assertNotNull(HomeTags.TopRightMenu)
  }

  @Test
  fun HomeTags_strings_are_not_blank() {
    assertTrue(HomeTags.Root.isNotBlank())
    assertTrue(HomeTags.MenuBtn.isNotBlank())
    assertTrue(HomeTags.TopRightBtn.isNotBlank())
    assertTrue(HomeTags.Action1Btn.isNotBlank())
    assertTrue(HomeTags.Action2Btn.isNotBlank())
    assertTrue(HomeTags.MessageField.isNotBlank())
    assertTrue(HomeTags.SendBtn.isNotBlank())
    assertTrue(HomeTags.Drawer.isNotBlank())
    assertTrue(HomeTags.TopRightMenu.isNotBlank())
  }

  @Test
  fun HomeTags_tags_do_not_start_with_underscore() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      assertFalse("Tag should not start with underscore: $tag", tag.startsWith("_"))
    }
  }

  @Test
  fun HomeTags_tags_do_not_end_with_underscore() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      assertFalse("Tag should not end with underscore: $tag", tag.endsWith("_"))
    }
  }

  @Test
  fun HomeTags_no_consecutive_underscores() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      assertFalse("Tag should not contain consecutive underscores: $tag", tag.contains("__"))
    }
  }

  @Test
  fun HomeTags_tags_have_minimum_word_count() {
    // Chaque tag devrait avoir au moins 2 mots (home_xxx)
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    allTags.forEach { tag ->
      val wordCount = tag.split("_").size
      assertTrue("Tag should have at least 2 words: $tag", wordCount >= 2)
    }
  }

  @Test
  fun HomeTags_constants_can_be_used_in_equality() {
    assertEquals(HomeTags.Root, "home_root")
    assertEquals(HomeTags.MenuBtn, "home_menu_btn")
    assertNotEquals(HomeTags.Root, HomeTags.MenuBtn)
  }

  @Test
  fun HomeTags_tags_hashcode_consistency() {
    val root1 = HomeTags.Root.hashCode()
    val root2 = HomeTags.Root.hashCode()
    assertEquals(root1, root2)
  }

  @Test
  fun HomeTags_tags_toString_returns_value() {
    assertEquals("home_root", HomeTags.Root.toString())
    assertEquals("home_menu_btn", HomeTags.MenuBtn.toString())
  }

  @Test
  fun HomeTags_object_hashcode_consistency() {
    val hash1 = HomeTags.hashCode()
    val hash2 = HomeTags.hashCode()
    assertEquals(hash1, hash2)
  }

  @Test
  fun HomeTags_object_toString_contains_class_name() {
    val toString = HomeTags.toString()
    assertTrue(toString.contains("HomeTags"))
  }

  @Test
  fun HomeTags_all_button_tags_share_btn_pattern() {
    val buttonTags =
        listOf(
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.SendBtn)
    buttonTags.forEach { tag ->
      assertTrue("Button tag should contain 'btn': $tag", tag.contains("btn"))
    }
  }

  @Test
  fun HomeTags_field_tags_contain_field() {
    assertTrue(HomeTags.MessageField.contains("field"))
  }

  @Test
  fun HomeTags_menu_tags_contain_menu() {
    val menuTags = listOf(HomeTags.MenuBtn, HomeTags.TopRightMenu)
    menuTags.forEach { tag ->
      assertTrue("Menu tag should contain 'menu': $tag", tag.contains("menu"))
    }
  }

  @Test
  fun HomeTags_action_tags_contain_action() {
    assertTrue(HomeTags.Action1Btn.contains("action"))
    assertTrue(HomeTags.Action2Btn.contains("action"))
  }

  @Test
  fun HomeTags_send_tag_contains_send() {
    assertTrue(HomeTags.SendBtn.contains("send"))
  }

  @Test
  fun HomeTags_drawer_tag_is_simple() {
    // Drawer est le plus simple après Root
    assertTrue(HomeTags.Drawer.split("_").size == 2)
  }

  @Test
  fun HomeTags_topright_menu_is_longest() {
    val allTags =
        listOf(
            HomeTags.Root,
            HomeTags.MenuBtn,
            HomeTags.TopRightBtn,
            HomeTags.Action1Btn,
            HomeTags.Action2Btn,
            HomeTags.MessageField,
            HomeTags.SendBtn,
            HomeTags.Drawer,
            HomeTags.TopRightMenu)
    val maxLength = allTags.maxOfOrNull { it.length } ?: 0
    assertEquals(HomeTags.TopRightMenu.length, maxLength)
  }

  @Test
  fun HomeTags_constants_are_stable_across_access() {
    val firstAccess = listOf(HomeTags.Root, HomeTags.MenuBtn, HomeTags.TopRightBtn)
    val secondAccess = listOf(HomeTags.Root, HomeTags.MenuBtn, HomeTags.TopRightBtn)
    assertEquals(firstAccess, secondAccess)
  }

  @Test
  fun SourceMeta_default_retrievedAt_is_recent() {
    val before = System.currentTimeMillis()

    val meta =
        SourceMeta(
            siteLabel = "EPFL.ch Website",
            title = "Projet de Semestre – Bachelor",
            url = "https://www.epfl.ch/education/projects")

    val after = System.currentTimeMillis()

    assertTrue(meta.retrievedAt in before..after)
  }

  @Test
  fun HomeUiState_preserves_source_meta_in_messages() {
    val meta =
        SourceMeta(
            siteLabel = "EPFL.ch Website",
            title = "Projet de Semestre – Bachelor",
            url = "https://www.epfl.ch/education/projects",
            retrievedAt = 123456789L)
    val state =
        HomeUiState(
            messages =
                listOf(
                    ChatUIModel(
                        id = "ai-source",
                        text = "",
                        timestamp = 0L,
                        type = ChatType.AI,
                        source = meta)))

    assertEquals(meta, state.messages.single().source)
  }

  @Test
  fun SourceMeta_copy_allows_timestamp_override() {
    val original =
        SourceMeta(
            siteLabel = "EPFL.ch Website",
            title = "Projet de Semestre – Bachelor",
            url = "https://www.epfl.ch/education/projects")

    val overridden = original.copy(retrievedAt = 99L)

    assertEquals(99L, overridden.retrievedAt)
    assertEquals(original.siteLabel, overridden.siteLabel)
    assertEquals(original.title, overridden.title)
    assertEquals(original.url, overridden.url)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeScreenComposeInteractionsTest {

  @get:Rule val composeRule = createComposeRule()

  @OptIn(ExperimentalCoroutinesApi::class)
  @get:Rule
  val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
    FirebaseAuth.getInstance().signOut()
  }

  private fun createViewModel(): HomeViewModel = HomeViewModel()

  @After
  fun tearDownMocks() {
    unmockkAll()
    FirebaseAuth.getInstance().signOut()
  }

  private fun HomeViewModel.editState(transform: (HomeUiState) -> HomeUiState) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val flow = field.get(this) as MutableStateFlow<HomeUiState>
    flow.value = transform(flow.value)
  }

  private fun userMessage(id: String = "user-${System.nanoTime()}", text: String = "Hello") =
      ChatUIModel(id = id, text = text, timestamp = 0L, type = ChatType.USER)

  @Test
  fun deleteConfirmation_cancel_hides_modal() {
    val viewModel = createViewModel()
    viewModel.showDeleteConfirmation()

    composeRule.setContent { HomeScreen(viewModel = viewModel) }

    composeRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()

    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showDeleteConfirmation) }
  }

  @Test
  fun deleteConfirmation_confirm_hides_modal() {
    val viewModel = createViewModel()
    viewModel.showDeleteConfirmation()

    composeRule.setContent { HomeScreen(viewModel = viewModel) }

    composeRule.onNodeWithText("Delete").assertIsDisplayed().performClick()

    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showDeleteConfirmation) }
  }

  @Test
  fun drawerSettings_click_triggers_callback_and_closes_drawer() {
    val viewModel = createViewModel()
    // Open the drawer before composition so drawer content is visible
    viewModel.toggleDrawer()
    var settingsInvoked = false

    composeRule.setContent {
      HomeScreen(viewModel = viewModel, onSettingsClick = { settingsInvoked = true })
    }

    // Allow drawer state transitions to run
    composeRule.mainClock.advanceTimeByFrame()

    composeRule
        .onNodeWithTag(DrawerTags.ConnectorsRow, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeRule.runOnIdle {
      assertTrue(settingsInvoked)
      assertFalse(viewModel.uiState.value.isDrawerOpen)
    }
  }

  @Test
  fun thinkingIndicator_visible_when_sending_without_streaming_id() {
    val viewModel = createViewModel()
    viewModel.editState { state ->
      state.copy(messages = listOf(userMessage()), isSending = true, streamingMessageId = null)
    }

    composeRule.setContent { HomeScreen(viewModel = viewModel) }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("home_thinking_indicator", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun sendButton_click_dispatches_message_and_clears_draft() {
    val viewModel = createViewModel()
    viewModel.editState { state ->
      state.copy(messageDraft = "Ping Euler", isSending = false, streamingMessageId = null)
    }
    var sent: String? = null

    composeRule.setContent { HomeScreen(viewModel = viewModel, onSendMessage = { sent = it }) }

    composeRule.waitForIdle()

    composeRule
        .onNode(hasTestTag(HomeTags.SendBtn) and hasClickAction(), useUnmergedTree = true)
        .performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals("", viewModel.uiState.value.messageDraft)
      assertEquals("Ping Euler", sent)
      assertTrue(
          viewModel.uiState.value.messages.any {
            it.type == ChatType.USER && it.text == "Ping Euler"
          })
    }
  }

  @Test
  fun micButton_click_invokes_speech_helper_and_updates_draft() {
    val viewModel = createViewModel()
    val speechHelper = mockk<SpeechToTextHelper>()
    val resultSlot = slot<(String) -> Unit>()
    every { speechHelper.startListening(capture(resultSlot), any(), any(), any(), any()) } answers
        {
          resultSlot.captured.invoke("Bonjour Euler")
        }

    composeRule.setContent { HomeScreen(viewModel = viewModel, speechHelper = speechHelper) }

    composeRule.onNodeWithTag(HomeTags.MicBtn, useUnmergedTree = true).performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals("Bonjour Euler", viewModel.uiState.value.messageDraft) }
  }
}
