package com.android.sample.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeTags
import com.android.sample.home.HomeViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: HomeViewModel

  @Before
  fun setUp() {
    initFirebase()
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDown() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun suggestions_visible_in_guest_mode() {
    launchScreen()

    composeRule.onNodeWithText("What is EPFL").assertIsDisplayed()
    composeRule.onNodeWithText("Check Ed Discussion").assertIsDisplayed()
  }

  @Test
  fun top_right_menu_opens_when_requested() {
    launchScreen()

    composeRule.runOnIdle { viewModel.setTopRightOpen(true) }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HomeTags.TopRightMenu, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun message_field_updates_draft_text() {
    launchScreen()

    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hello Euler")
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals("Hello Euler", viewModel.uiState.value.messageDraft) }
  }

  @Test
  fun drawer_state_reflects_viewmodel_toggle() {
    launchScreen()

    composeRule.runOnIdle { viewModel.toggleDrawer() }
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertTrue(viewModel.uiState.value.isDrawerOpen) }
  }

  private fun launchScreen() {
    viewModel = HomeViewModel()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
  }

  private fun initFirebase() {
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
  }
}
