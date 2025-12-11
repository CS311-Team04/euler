package com.android.sample.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeScreenMenuTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun topRightPanelPlaceholder_invokesDeleteAndDismiss() {
    var deleteCalled = false
    var dismissCalled = false

    composeRule.setContent {
      TopRightPanelPlaceholder(
          onDismiss = { dismissCalled = true },
          onDeleteClick = { deleteCalled = true })
    }

    composeRule.onNodeWithTag("menu_delete").assertIsDisplayed().performClick()

    assertTrue(deleteCalled)
    assertTrue(dismissCalled)
  }

  @Test
  fun topRightPanelPlaceholder_showsShare() {
    composeRule.setContent {
      TopRightPanelPlaceholder(onDismiss = {}, onDeleteClick = {})
    }

    composeRule.onNodeWithTag("menu_share").assertIsDisplayed()
  }
}

