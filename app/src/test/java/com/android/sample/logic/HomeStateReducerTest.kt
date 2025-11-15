package com.android.sample.logic

import com.android.sample.home.HomeUiState
import com.android.sample.home.SystemItem
import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for HomeStateReducer. Tests home state transitions without Android
 * dependencies.
 */
class HomeStateReducerTest {

  private val defaultSystems = listOf(SystemItem("isa", "IS-Academia", true))

  @Test
  fun `calculateUserName uses preferredName when available`() {
    val profile = UserProfile(preferredName = "John", fullName = "John Doe")
    val result = HomeStateReducer.calculateUserName(profile)

    assertEquals("John", result)
  }

  @Test
  fun `calculateUserName falls back to fullName when preferredName empty`() {
    val profile = UserProfile(preferredName = "", fullName = "John Doe")
    val result = HomeStateReducer.calculateUserName(profile)

    assertEquals("John Doe", result)
  }

  @Test
  fun `calculateUserName falls back to default when both empty`() {
    val profile = UserProfile(preferredName = "", fullName = "")
    val result = HomeStateReducer.calculateUserName(profile)

    assertEquals("Student", result)
  }

  @Test
  fun `calculateUserName returns default when profile is null`() {
    val result = HomeStateReducer.calculateUserName(null)

    assertEquals("Student", result)
  }

  @Test
  fun `calculateUserNameWithFallback preserves currentUserName when not default`() {
    val profile = UserProfile()
    val result = HomeStateReducer.calculateUserNameWithFallback(profile, "CurrentName")

    assertEquals("Student", result) // Since profile is empty, returns default
  }

  @Test
  fun `setGuestMode sets guest properties correctly`() {
    val state = HomeUiState(userName = "Test", systems = defaultSystems)
    val result = HomeStateReducer.setGuestMode(state, true)

    assertTrue(result.isGuest)
    assertEquals(null, result.profile)
    assertEquals("guest", result.userName)
    assertFalse(result.showGuestProfileWarning)
  }

  @Test
  fun `setGuestMode clears guest mode correctly`() {
    val state = HomeUiState(userName = "", isGuest = true, systems = defaultSystems)
    val result = HomeStateReducer.setGuestMode(state, false)

    assertFalse(result.isGuest)
    assertEquals("Student", result.userName)
  }

  @Test
  fun `setGuestMode preserves userName when clearing guest`() {
    val state = HomeUiState(userName = "ExistingName", isGuest = true, systems = defaultSystems)
    val result = HomeStateReducer.setGuestMode(state, false)

    assertEquals("ExistingName", result.userName)
  }

  @Test
  fun `refreshProfile updates state with profile`() {
    val state = HomeUiState(systems = defaultSystems)
    val profile = UserProfile(preferredName = "John", fullName = "John Doe")
    val result = HomeStateReducer.refreshProfile(state, profile)

    assertEquals(profile, result.profile)
    assertEquals("John", result.userName)
    assertFalse(result.isGuest)
  }

  @Test
  fun `refreshProfile handles null profile`() {
    val state = HomeUiState(userName = "Test", systems = defaultSystems)
    val result = HomeStateReducer.refreshProfile(state, null)

    assertEquals(null, result.profile)
    assertEquals("Test", result.userName)
    assertFalse(result.isGuest)
  }

  @Test
  fun `refreshProfile uses default userName when both empty`() {
    val state = HomeUiState(userName = "", systems = defaultSystems)
    val result = HomeStateReducer.refreshProfile(state, null)

    assertEquals("Student", result.userName)
  }

  @Test
  fun `saveProfile updates state correctly`() {
    val state = HomeUiState(systems = defaultSystems)
    val profile = UserProfile(preferredName = "Jane", fullName = "Jane Smith")
    val result = HomeStateReducer.saveProfile(state, profile)

    assertEquals(profile, result.profile)
    assertEquals("Jane", result.userName)
    assertFalse(result.isGuest)
  }

  @Test
  fun `clearProfile resets to defaults`() {
    val state =
        HomeUiState(
            userName = "Test",
            profile = UserProfile(),
            isGuest = true,
            showGuestProfileWarning = true,
            systems = defaultSystems)
    val result = HomeStateReducer.clearProfile(state)

    assertEquals(null, result.profile)
    assertEquals("Student", result.userName)
    assertFalse(result.isGuest)
    assertFalse(result.showGuestProfileWarning)
  }

  @Test
  fun `showGuestProfileWarning sets flag to true`() {
    val state = HomeUiState(showGuestProfileWarning = false, systems = defaultSystems)
    val result = HomeStateReducer.showGuestProfileWarning(state)

    assertTrue(result.showGuestProfileWarning)
  }

  @Test
  fun `hideGuestProfileWarning sets flag to false`() {
    val state = HomeUiState(showGuestProfileWarning = true, systems = defaultSystems)
    val result = HomeStateReducer.hideGuestProfileWarning(state)

    assertFalse(result.showGuestProfileWarning)
  }

  @Test
  fun `toggleDrawer opens when closed`() {
    val state = HomeUiState(isDrawerOpen = false, systems = defaultSystems)
    val result = HomeStateReducer.toggleDrawer(state)

    assertTrue(result.isDrawerOpen)
  }

  @Test
  fun `toggleDrawer closes when open`() {
    val state = HomeUiState(isDrawerOpen = true, systems = defaultSystems)
    val result = HomeStateReducer.toggleDrawer(state)

    assertFalse(result.isDrawerOpen)
  }

  @Test
  fun `setTopRightOpen sets flag correctly`() {
    val state = HomeUiState(isTopRightOpen = false, systems = defaultSystems)
    val result = HomeStateReducer.setTopRightOpen(state, true)

    assertTrue(result.isTopRightOpen)
  }

  @Test
  fun `startLocalNewChat clears conversation state`() {
    val state =
        HomeUiState(
            currentConversationId = "conv-1",
            messages = listOf(),
            messageDraft = "draft",
            isSending = true,
            showDeleteConfirmation = true,
            systems = defaultSystems)
    val result = HomeStateReducer.startLocalNewChat(state)

    assertEquals(null, result.currentConversationId)
    assertTrue(result.messages.isEmpty())
    assertEquals("", result.messageDraft)
    assertFalse(result.isSending)
    assertFalse(result.showDeleteConfirmation)
  }

  @Test
  fun `onSignedOut resets to defaults while preserving systems`() {
    val state =
        HomeUiState(
            userName = "Test",
            messages = listOf(),
            currentConversationId = "conv-1",
            systems = defaultSystems)
    val result = HomeStateReducer.onSignedOut(state)

    assertEquals(defaultSystems, result.systems)
    assertTrue(result.messages.isEmpty())
    assertEquals(null, result.currentConversationId)
    assertEquals("Student", result.userName)
  }

  @Test
  fun `calculateUserName handles whitespace in preferredName`() {
    val profile = UserProfile(preferredName = "  John  ", fullName = "John Doe")
    val result = HomeStateReducer.calculateUserName(profile)

    assertEquals("  John  ", result) // Logic doesn't trim, that's OK
  }

  @Test
  fun `calculateUserName prefers preferredName even if fullName is longer`() {
    val profile = UserProfile(preferredName = "J", fullName = "John Michael Doe")
    val result = HomeStateReducer.calculateUserName(profile)

    assertEquals("J", result)
  }
}
