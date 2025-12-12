package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MoodleBadgeRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun badge_renders_title_date_and_icon() {
    val metadata =
        MoodleMetadata(
            courseName = "Algebra",
            weekLabel = "Week 3",
            lastUpdated = "2025-12-11T04:15:00.000Z",
            source = "Moodle")
    val formattedDate = formatMoodleUpdatedDate(metadata.lastUpdated)

    composeRule.setContent { MaterialTheme { MoodleSourceBadge(metadata = metadata) } }

    composeRule.onNodeWithText("Moodle Overview • ${metadata.weekLabel}").assertIsDisplayed()
    composeRule.onNodeWithText("Updated: $formattedDate").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Moodle Overview").assertIsDisplayed()
  }
}

