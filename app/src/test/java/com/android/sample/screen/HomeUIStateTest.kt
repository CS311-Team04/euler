package com.android.sample.home

import org.junit.Assert.*
import org.junit.Test

class HomeUIStateTest {

  @Test
  fun `default HomeUiState has correct values`() {
    val state = HomeUiState()

    assertEquals("Student", state.userName)
    assertTrue(state.systems.isEmpty())
    assertTrue(state.recent.isEmpty())
    assertEquals("", state.messageDraft)
    assertFalse(state.isDrawerOpen)
    assertFalse(state.isTopRightOpen)
    assertFalse(state.isLoading)
  }

  @Test
  fun `HomeUiState copy works correctly`() {
    val original = HomeUiState(userName = "John", messageDraft = "Hello")

    val copied = original.copy(messageDraft = "World")

    assertEquals("John", copied.userName)
    assertEquals("World", copied.messageDraft)
  }

  @Test
  fun `HomeUiState with systems`() {
    val systems =
        listOf(
            SystemItem("id1", "System 1", true),
            SystemItem("id2", "System 2", false),
        )

    val state = HomeUiState(systems = systems)

    assertEquals(2, state.systems.size)
    assertEquals("id1", state.systems[0].id)
    assertEquals("System 1", state.systems[0].name)
    assertTrue(state.systems[0].isConnected)
  }

  @Test
  fun `HomeUiState with recent actions`() {
    val actions =
        listOf(
            ActionItem("1", "Action 1", "1h ago"),
            ActionItem("2", "Action 2", "2h ago"),
        )

    val state = HomeUiState(recent = actions)

    assertEquals(2, state.recent.size)
    assertEquals("Action 1", state.recent[0].title)
    assertEquals("1h ago", state.recent[0].time)
  }

  @Test
  fun `SystemItem stores all properties correctly`() {
    val system = SystemItem(id = "test-id", name = "Test System", isConnected = true)

    assertEquals("test-id", system.id)
    assertEquals("Test System", system.name)
    assertTrue(system.isConnected)
  }

  @Test
  fun `SystemItem copy works correctly`() {
    val original = SystemItem("id1", "Name1", false)
    val copied = original.copy(isConnected = true)

    assertEquals("id1", copied.id)
    assertEquals("Name1", copied.name)
    assertTrue(copied.isConnected)
  }

  @Test
  fun `ActionItem stores all properties correctly`() {
    val action = ActionItem(id = "action-1", title = "Test Action", time = "Just now")

    assertEquals("action-1", action.id)
    assertEquals("Test Action", action.title)
    assertEquals("Just now", action.time)
  }

  @Test
  fun `ActionItem copy works correctly`() {
    val original = ActionItem("1", "Old Title", "1h")
    val copied = original.copy(title = "New Title", time = "2h")

    assertEquals("1", copied.id)
    assertEquals("New Title", copied.title)
    assertEquals("2h", copied.time)
  }

  @Test
  fun `HomeUiState equality works`() {
    val state1 =
        HomeUiState(
            userName = "User1",
            messageDraft = "Draft1",
            isDrawerOpen = true,
        )

    val state2 =
        HomeUiState(
            userName = "User1",
            messageDraft = "Draft1",
            isDrawerOpen = true,
        )

    assertEquals(state1, state2)
  }

  @Test
  fun `HomeUiState inequality works`() {
    val state1 = HomeUiState(userName = "User1")
    val state2 = HomeUiState(userName = "User2")

    assertNotEquals(state1, state2)
  }

  @Test
  fun `SystemItem equality works`() {
    val sys1 = SystemItem("id1", "Name", true)
    val sys2 = SystemItem("id1", "Name", true)

    assertEquals(sys1, sys2)
  }

  @Test
  fun `ActionItem equality works`() {
    val action1 = ActionItem("1", "Title", "Time")
    val action2 = ActionItem("1", "Title", "Time")

    assertEquals(action1, action2)
  }

  @Test
  fun `HomeUiState with all flags set`() {
    val state =
        HomeUiState(
            userName = "TestUser",
            messageDraft = "Test message",
            isDrawerOpen = true,
            isTopRightOpen = true,
            isLoading = true,
        )

    assertEquals("TestUser", state.userName)
    assertEquals("Test message", state.messageDraft)
    assertTrue(state.isDrawerOpen)
    assertTrue(state.isTopRightOpen)
    assertTrue(state.isLoading)
  }

  @Test
  fun `multiple copy operations preserve data`() {
    val initial = HomeUiState(userName = "User")
    val step1 = initial.copy(messageDraft = "Message")
    val step2 = step1.copy(isDrawerOpen = true)
    val step3 = step2.copy(isLoading = true)

    assertEquals("User", step3.userName)
    assertEquals("Message", step3.messageDraft)
    assertTrue(step3.isDrawerOpen)
    assertTrue(step3.isLoading)
  }
}
