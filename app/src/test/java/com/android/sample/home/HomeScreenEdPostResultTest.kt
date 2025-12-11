package com.android.sample.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
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
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeRule.setContent {
      EdPostResult.Failed("Oops").let { result -> EdPostResultBanner(result = result) }
    }

    composeRule.onNodeWithText(ctx.getString(R.string.ed_post_failed_title)).assertIsDisplayed()
    composeRule.onNodeWithText("Oops").assertIsDisplayed()
  }

  @Test
  fun edPostResult_showsCancelledBanner() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeRule.setContent { EdPostResultBanner(result = EdPostResult.Cancelled) }

    composeRule.onNodeWithText(ctx.getString(R.string.ed_post_cancelled_title)).assertIsDisplayed()
  }

  @Test
  fun edPostResult_showsPublishedBanner() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeRule.setContent { EdPostResultBanner(result = EdPostResult.Published("t", "b")) }

    composeRule.onNodeWithText(ctx.getString(R.string.ed_post_published_title)).assertIsDisplayed()
  }
}
