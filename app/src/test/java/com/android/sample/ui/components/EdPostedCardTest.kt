package com.android.sample.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.home.EdPostCard
import com.android.sample.home.EdPostStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EdPostedCardTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun EdPostedCard_displays_published_card() {
    val card =
        EdPostCard(
            id = "card-1",
            title = "Published Question",
            body = "Body",
            status = EdPostStatus.Published,
            createdAt = System.currentTimeMillis())

    composeRule.setContent { EdPostedCard(card = card) }
    composeRule.onNodeWithText("Published Question").assertIsDisplayed()
    composeRule.onNodeWithText("Published").assertIsDisplayed()
    composeRule.onNodeWithText("Ed Discussion").assertIsDisplayed()
  }

  @Test
  fun EdPostedCard_displays_cancelled_card() {
    val card =
        EdPostCard(
            id = "card-2",
            title = "Cancelled Question",
            body = "Body",
            status = EdPostStatus.Cancelled,
            createdAt = System.currentTimeMillis())

    composeRule.setContent { EdPostedCard(card = card) }
    composeRule.onNodeWithText("Cancelled").assertIsDisplayed()
  }

  @Test
  fun EdPostedCard_handles_edge_cases() {
    val emptyTitleCard =
        EdPostCard(
            id = "card-1",
            title = "",
            body = "Body",
            status = EdPostStatus.Published,
            createdAt = 0L)
    composeRule.setContent { EdPostedCard(card = emptyTitleCard) }
    composeRule.onNodeWithText("ED post").assertIsDisplayed()
  }
}
