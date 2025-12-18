package com.android.sample.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.settings.Localization
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeScreenEdPostResultTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun edPostResult_showsFailedBanner() {
    composeRule.setContent {
      EdPostResult.Failed("Oops").let { result -> EdPostResultBanner(result = result) }
    }

    composeRule.onNodeWithText(Localization.t("ed_post_failed_title")).assertIsDisplayed()
    composeRule.onNodeWithText("Oops").assertIsDisplayed()
  }

  @Test
  fun edPostResult_showsCancelledBanner() {
    composeRule.setContent { EdPostResultBanner(result = EdPostResult.Cancelled) }

    composeRule.onNodeWithText(Localization.t("ed_post_cancelled_title")).assertIsDisplayed()
  }

  @Test
  fun edPostResult_showsPublishedBanner() {
    composeRule.setContent { EdPostResultBanner(result = EdPostResult.Published("t", "b")) }

    composeRule.onNodeWithText(Localization.t("ed_post_published_title")).assertIsDisplayed()
  }

  @Test
  fun edPostResult_dismiss_invokesCallback() {
    var dismissed = false
    composeRule.setContent {
      EdPostResultBanner(result = EdPostResult.Cancelled, onDismiss = { dismissed = true })
    }
    composeRule.onNodeWithContentDescription(Localization.t("dismiss")).performClick()
    assertTrue(dismissed)
  }
}
