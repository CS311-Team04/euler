package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.android.sample.authentification.AuthTags
import com.android.sample.home.DrawerTags
import com.android.sample.home.HomeTags
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.ConnectorCardTags
import com.android.sample.settings.connectors.EdConnectTags

/**
 * Robot pattern for Home screen navigation and interactions in E2E tests. Provides fluent API for
 * common navigation flows.
 */
@OptIn(ExperimentalTestApi::class)
class HomeRobot(private val composeRule: ComposeTestRule) {

  /**
   * Navigates to the home screen by waiting for Sign In screen and clicking guest button. Should be
   * called at the start of tests that require being on the home screen.
   */
  fun navigateToHome(): HomeRobot {
    // Wait for opening screen to navigate to Sign In screen
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.Root), timeoutMillis = 5_000)

    // Click guest button to navigate to home
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    composeRule.waitForIdle()

    // Wait for home screen
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 10_000)
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.waitForIdle()
    return this
  }

  /** Opens the drawer by clicking the menu button. */
  fun openDrawer(): DrawerRobot {
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()

    // Wait for drawer to open
    composeRule.waitUntilAtLeastOneExists(hasTestTag(DrawerTags.Root), timeoutMillis = 3_000)
    composeRule.waitForIdle()
    return DrawerRobot(composeRule)
  }

  /** Verifies that the home screen is displayed. */
  fun verifyHomeScreenDisplayed(): HomeRobot {
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    return this
  }
}

/** Robot pattern for Drawer navigation and interactions in E2E tests. */
@OptIn(ExperimentalTestApi::class)
class DrawerRobot(private val composeRule: ComposeTestRule) {

  /** Navigates to the Connectors screen by clicking on the Connectors row in the drawer. */
  fun goToConnectors(): ConnectorsRobot {
    // Click on Connectors row
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(DrawerTags.ConnectorsRow), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(DrawerTags.ConnectorsRow).performClick()
    composeRule.waitForIdle()

    // Wait for navigation to connectors screen
    composeRule.waitUntilAtLeastOneExists(
        hasText(Localization.t("connectors")), timeoutMillis = 5_000)
    return ConnectorsRobot(composeRule)
  }

  /** Navigates to Settings by clicking on the Settings icon in the drawer footer. */
  fun goToSettings(): DrawerRobot {
    // Wait for drawer content to be ready
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(DrawerTags.UserSettings), timeoutMillis = 3_000)

    composeRule.onNodeWithTag(DrawerTags.UserSettings).assertIsDisplayed()
    composeRule.onNodeWithTag(DrawerTags.UserSettings).performClick()
    composeRule.waitForIdle()
    return this
  }

  /** Verifies that the drawer is displayed. */
  fun verifyDrawerDisplayed(): DrawerRobot {
    composeRule.waitUntilAtLeastOneExists(hasTestTag(DrawerTags.Root), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()
    return this
  }
}

/** Robot pattern for Connectors screen navigation and interactions in E2E tests. */
@OptIn(ExperimentalTestApi::class)
class ConnectorsRobot(private val composeRule: ComposeTestRule) {

  /** Verifies that the Connectors screen is displayed. */
  fun verifyConnectorsScreenDisplayed(): ConnectorsRobot {
    composeRule.waitUntilAtLeastOneExists(
        hasText(Localization.t("connectors")), timeoutMillis = 5_000)
    composeRule.onNodeWithText(Localization.t("connectors")).assertIsDisplayed()
    return this
  }

  /** Clicks on the Ed connector card to navigate to Ed Connect screen. */
  fun clickEdConnector(): EdConnectRobot {
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(ConnectorCardTags.card("ed")), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(ConnectorCardTags.card("ed")).performClick()
    composeRule.waitForIdle()
    return EdConnectRobot(composeRule)
  }

  /** Clicks on the Moodle connector card to open Moodle connect dialog. */
  fun clickMoodleConnector(): MoodleConnectRobot {
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(ConnectorCardTags.card("moodle")), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(ConnectorCardTags.card("moodle")).performClick()
    composeRule.waitForIdle()
    return MoodleConnectRobot(composeRule)
  }
}

/** Robot pattern for Ed Connect screen interactions in E2E tests. */
@OptIn(ExperimentalTestApi::class)
class EdConnectRobot(private val composeRule: ComposeTestRule) {

  /** Verifies that the Ed Connect screen is displayed. */
  fun verifyEdConnectScreenDisplayed(): EdConnectRobot {
    composeRule.waitUntilAtLeastOneExists(hasTestTag(EdConnectTags.Title), timeoutMillis = 15_000)
    composeRule.waitForIdle()
    return this
  }
}

/** Robot pattern for Moodle Connect dialog interactions in E2E tests. */
@OptIn(ExperimentalTestApi::class)
class MoodleConnectRobot(private val composeRule: ComposeTestRule) {

  /** Verifies that the Moodle connect dialog is displayed. */
  fun verifyMoodleDialogDisplayed(): MoodleConnectRobot {
    val forgotPasswordText = Localization.t("settings_connectors_moodle_forgot_password")
    composeRule.waitUntilAtLeastOneExists(hasText(forgotPasswordText), timeoutMillis = 10_000)
    composeRule.waitForIdle()
    return this
  }
}
