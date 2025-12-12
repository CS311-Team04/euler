package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoodleBadgeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun badge_displays_title_week_and_date_and_icon() {
    val metadata =
        MoodleMetadata(
            courseName = "Algebra",
            weekLabel = "Week 3",
            lastUpdated = "2025-12-11T04:15:00.000Z",
            source = "Moodle")
    val formattedDate = formatMoodleUpdatedDate(metadata.lastUpdated)

    composeRule.setContent { MaterialTheme { MoodleSourceBadge(metadata = metadata) } }

    composeRule.onNodeWithText("Moodle Overview • Week 3").assertIsDisplayed()
    composeRule.onNodeWithText("Updated: $formattedDate").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Moodle Overview").assertIsDisplayed()
  }
}
