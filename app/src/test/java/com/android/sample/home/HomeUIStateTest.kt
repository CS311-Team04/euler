package com.android.sample.home

import org.junit.Assert.*
import org.junit.Test

class HomeUIStateTest {

  @Test
  fun default_HomeUiState_has_correct_values() {
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
  fun HomeUiState_copy_works_correctly() {
    val original = HomeUiState(userName = "John", messageDraft = "Hello")

    val copied = original.copy(messageDraft = "World")

    assertEquals("John", copied.userName)
    assertEquals("World", copied.messageDraft)
  }

  @Test
  fun HomeUiState_with_systems() {
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
  fun HomeUiState_with_recent_actions() {
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
  fun SystemItem_stores_all_properties_correctly() {
    val system = SystemItem(id = "test-id", name = "Test System", isConnected = true)

    assertEquals("test-id", system.id)
    assertEquals("Test System", system.name)
    assertTrue(system.isConnected)
  }

  @Test
  fun SystemItem_copy_works_correctly() {
    val original = SystemItem("id1", "Name1", false)
    val copied = original.copy(isConnected = true)

    assertEquals("id1", copied.id)
    assertEquals("Name1", copied.name)
    assertTrue(copied.isConnected)
  }

  @Test
  fun ActionItem_stores_all_properties_correctly() {
    val action = ActionItem(id = "action-1", title = "Test Action", time = "Just now")

    assertEquals("action-1", action.id)
    assertEquals("Test Action", action.title)
    assertEquals("Just now", action.time)
  }

  @Test
  fun ActionItem_copy_works_correctly() {
    val original = ActionItem("1", "Old Title", "1h")
    val copied = original.copy(title = "New Title", time = "2h")

    assertEquals("1", copied.id)
    assertEquals("New Title", copied.title)
    assertEquals("2h", copied.time)
  }

  @Test
  fun HomeUiState_equality_works() {
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
  fun HomeUiState_inequality_works() {
    val state1 = HomeUiState(userName = "User1")
    val state2 = HomeUiState(userName = "User2")

    assertNotEquals(state1, state2)
  }

  @Test
  fun SystemItem_equality_works() {
    val sys1 = SystemItem("id1", "Name", true)
    val sys2 = SystemItem("id1", "Name", true)

    assertEquals(sys1, sys2)
  }

  @Test
  fun ActionItem_equality_works() {
    val action1 = ActionItem("1", "Title", "Time")
    val action2 = ActionItem("1", "Title", "Time")

    assertEquals(action1, action2)
  }

  @Test
  fun HomeUiState_with_all_flags_set() {
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
  fun multiple_copy_operations_preserve_data() {
    val initial = HomeUiState(userName = "User")
    val step1 = initial.copy(messageDraft = "Message")
    val step2 = step1.copy(isDrawerOpen = true)
    val step3 = step2.copy(isLoading = true)

    assertEquals("User", step3.userName)
    assertEquals("Message", step3.messageDraft)
    assertTrue(step3.isDrawerOpen)
    assertTrue(step3.isLoading)
  }

  @Test
  fun HomeUiState_showDeleteConfirmation_flag() {
    val state = HomeUiState(showDeleteConfirmation = true)
    assertTrue(state.showDeleteConfirmation)
  }

  @Test
  fun HomeUiState_toString_includes_properties() {
    val state = HomeUiState(userName = "Test", messageDraft = "Draft")
    val toString = state.toString()
    assertTrue(toString.contains("Test"))
  }

  @Test
  fun SystemItem_toString_includes_properties() {
    val system = SystemItem("id1", "System1", true)
    val toString = system.toString()
    assertTrue(toString.contains("id1"))
    assertTrue(toString.contains("System1"))
  }

  @Test
  fun ActionItem_toString_includes_properties() {
    val action = ActionItem("id1", "Action1", "1h")
    val toString = action.toString()
    assertTrue(toString.contains("id1"))
    assertTrue(toString.contains("Action1"))
  }

  @Test
  fun SystemItem_data_class_hashcode() {
    val sys1 = SystemItem("id1", "Name1", true)
    val sys2 = SystemItem("id1", "Name1", true)
    val sys3 = SystemItem("id1", "Name1", false)

    assertEquals(sys1.hashCode(), sys2.hashCode())
    assertNotEquals(sys1.hashCode(), sys3.hashCode())
  }

  @Test
  fun ActionItem_data_class_hashcode() {
    val action1 = ActionItem("id1", "Title1", "Time1")
    val action2 = ActionItem("id1", "Title1", "Time1")
    val action3 = ActionItem("id2", "Title1", "Time1")

    assertEquals(action1.hashCode(), action2.hashCode())
    assertNotEquals(action1.hashCode(), action3.hashCode())
  }

  @Test
  fun SystemItem_component_functions() {
    val system = SystemItem("test-id", "Test System", true)
    val (id, name, isConnected) = system

    assertEquals("test-id", id)
    assertEquals("Test System", name)
    assertTrue(isConnected)
  }

  @Test
  fun ActionItem_component_functions() {
    val action = ActionItem("action-1", "Test Action", "Just now")
    val (id, title, time) = action

    assertEquals("action-1", id)
    assertEquals("Test Action", title)
    assertEquals("Just now", time)
  }

  @Test
  fun HomeUiState_with_empty_strings() {
    val state = HomeUiState(userName = "", messageDraft = "", systems = emptyList())
    assertEquals("", state.userName)
    assertEquals("", state.messageDraft)
    assertTrue(state.systems.isEmpty())
  }

  @Test
  fun SystemItem_not_equals_different_id() {
    val sys1 = SystemItem("id1", "Name", true)
    val sys2 = SystemItem("id2", "Name", true)
    assertNotEquals(sys1, sys2)
  }

  @Test
  fun ActionItem_not_equals_different_title() {
    val action1 = ActionItem("id1", "Title1", "Time")
    val action2 = ActionItem("id1", "Title2", "Time")
    assertNotEquals(action1, action2)
  }

  @Test
  fun HomeUiState_all_parameters_constructor() {
    val systems = listOf(SystemItem("id1", "System1", true))
    val recent = listOf(ActionItem("id1", "Action1", "1h"))

    val state =
        HomeUiState(
            userName = "User",
            systems = systems,
            recent = recent,
            messageDraft = "Draft",
            isDrawerOpen = true,
            isTopRightOpen = true,
            isLoading = true,
            showDeleteConfirmation = true)

    assertEquals("User", state.userName)
    assertEquals(1, state.systems.size)
    assertEquals(1, state.recent.size)
    assertEquals("Draft", state.messageDraft)
    assertTrue(state.isDrawerOpen)
    assertTrue(state.isTopRightOpen)
    assertTrue(state.isLoading)
    assertTrue(state.showDeleteConfirmation)
  }

  @Test
  fun SystemItem_component1_returns_id() {
    val system = SystemItem("test", "Name", true)
    assertEquals("test", system.component1())
  }

  @Test
  fun SystemItem_component2_returns_name() {
    val system = SystemItem("id", "TestName", true)
    assertEquals("TestName", system.component2())
  }

  @Test
  fun SystemItem_component3_returns_isConnected() {
    val system = SystemItem("id", "Name", true)
    assertTrue(system.component3())

    val systemFalse = SystemItem("id", "Name", false)
    assertFalse(systemFalse.component3())
  }

  @Test
  fun ActionItem_component1_returns_id() {
    val action = ActionItem("test-id", "Title", "Time")
    assertEquals("test-id", action.component1())
  }

  @Test
  fun ActionItem_component2_returns_title() {
    val action = ActionItem("id", "TestTitle", "Time")
    assertEquals("TestTitle", action.component2())
  }

  @Test
  fun ActionItem_component3_returns_time() {
    val action = ActionItem("id", "Title", "test-time")
    assertEquals("test-time", action.component3())
  }

  @Test
  fun HomeUiState_with_large_recent_list() {
    val recent = List(100) { ActionItem("$it", "Action $it", "${it}h ago") }
    val state = HomeUiState(recent = recent)
    assertEquals(100, state.recent.size)
  }

  @Test
  fun HomeUiState_with_large_systems_list() {
    val systems = List(50) { SystemItem("$it", "System $it", it % 2 == 0) }
    val state = HomeUiState(systems = systems)
    assertEquals(50, state.systems.size)
  }

  @Test
  fun SystemItem_with_null_name() {
    val system = SystemItem("id", "", true)
    assertEquals("", system.name)
  }

  @Test
  fun ActionItem_with_null_time() {
    val action = ActionItem("id", "Title", "")
    assertEquals("", action.time)
  }

  @Test
  fun HomeUiState_copy_with_different_parameters() {
    val state1 = HomeUiState(userName = "User1")
    val state2 = state1.copy(userName = "User2")

    assertEquals("User2", state2.userName)
    assertEquals("User1", state1.userName)
    assertNotEquals(state1, state2)
  }

  @Test
  fun SystemItem_inequality_different_name() {
    val sys1 = SystemItem("id", "Name1", true)
    val sys2 = SystemItem("id", "Name2", true)
    assertNotEquals(sys1, sys2)
  }

  @Test
  fun ActionItem_inequality_different_time() {
    val action1 = ActionItem("id", "Title", "Time1")
    val action2 = ActionItem("id", "Title", "Time2")
    assertNotEquals(action1, action2)
  }

  @Test
  fun HomeUiState_component_decomposition() {
    val state = HomeUiState(userName = "TestUser", messageDraft = "Draft", isDrawerOpen = true)

    val (
        userName,
        systems,
        recent,
        messageDraft,
        isDrawerOpen,
        isTopRightOpen,
        isLoading,
        showDeleteConfirmation) =
        state

    assertEquals("TestUser", userName)
    assertTrue(systems.isEmpty())
    assertTrue(recent.isEmpty())
    assertEquals("Draft", messageDraft)
    assertTrue(isDrawerOpen)
    assertFalse(isTopRightOpen)
    assertFalse(isLoading)
    assertFalse(showDeleteConfirmation)
  }

  @Test
  fun SystemItem_various_connection_states() {
    val connected = SystemItem("id", "Name", true)
    val disconnected = SystemItem("id", "Name", false)

    assertTrue(connected.isConnected)
    assertFalse(disconnected.isConnected)
    assertNotEquals(connected, disconnected)
  }

  @Test
  fun HomeUiState_edge_case_all_false() {
    val state =
        HomeUiState(
            userName = "User",
            isDrawerOpen = false,
            isTopRightOpen = false,
            isLoading = false,
            showDeleteConfirmation = false)

    assertFalse(state.isDrawerOpen)
    assertFalse(state.isTopRightOpen)
    assertFalse(state.isLoading)
    assertFalse(state.showDeleteConfirmation)
  }

  @Test
  fun HomeUiState_edge_case_all_true() {
    val state =
        HomeUiState(
            userName = "User",
            isDrawerOpen = true,
            isTopRightOpen = true,
            isLoading = true,
            showDeleteConfirmation = true)

    assertTrue(state.isDrawerOpen)
    assertTrue(state.isTopRightOpen)
    assertTrue(state.isLoading)
    assertTrue(state.showDeleteConfirmation)
  }

  @Test
  fun SystemItem_with_special_characters_in_id() {
    val system = SystemItem("id-with_special.chars-123", "Name", true)
    assertEquals("id-with_special.chars-123", system.id)
  }

  @Test
  fun ActionItem_with_long_strings() {
    val longTitle = "A".repeat(1000)
    val longTime = "B".repeat(500)
    val action = ActionItem("id", longTitle, longTime)

    assertEquals(1000, action.title.length)
    assertEquals(500, action.time.length)
  }

  @Test
  fun HomeUiState_toString_contains_userName() {
    val state = HomeUiState(userName = "TestUser123")
    val toString = state.toString()
    assertTrue(toString.contains("TestUser123"))
  }

  @Test
  fun HomeUiState_hashCode_consistency() {
    val state1 = HomeUiState(userName = "User", isDrawerOpen = true)
    val state2 = HomeUiState(userName = "User", isDrawerOpen = true)

    assertEquals(state1.hashCode(), state2.hashCode())
  }

  @Test
  fun SystemItem_hashCode_with_different_states() {
    val sys1 = SystemItem("id", "Name", true)
    val sys2 = SystemItem("id", "Name", false)

    assertNotEquals(sys1.hashCode(), sys2.hashCode())
  }

  @Test
  fun ActionItem_hashCode_with_different_titles() {
    val action1 = ActionItem("id", "Title1", "Time")
    val action2 = ActionItem("id", "Title2", "Time")

    assertNotEquals(action1.hashCode(), action2.hashCode())
  }

  @Test
  fun HomeUiState_with_multiple_systems_and_recent() {
    val systems =
        listOf(
            SystemItem("1", "S1", true), SystemItem("2", "S2", false), SystemItem("3", "S3", true))
    val recent =
        listOf(
            ActionItem("1", "A1", "1h"), ActionItem("2", "A2", "2h"), ActionItem("3", "A3", "3h"))

    val state = HomeUiState(systems = systems, recent = recent)

    assertEquals(3, state.systems.size)
    assertEquals(3, state.recent.size)
  }

  @Test
  fun SystemItem_copy_all_parameters() {
    val original = SystemItem("old-id", "Old Name", false)
    val copied = original.copy(id = "new-id", name = "New Name", isConnected = true)

    assertEquals("new-id", copied.id)
    assertEquals("New Name", copied.name)
    assertTrue(copied.isConnected)
  }

  @Test
  fun ActionItem_copy_all_parameters() {
    val original = ActionItem("old-id", "Old Title", "Old Time")
    val copied = original.copy(id = "new-id", title = "New Title", time = "New Time")

    assertEquals("new-id", copied.id)
    assertEquals("New Title", copied.title)
    assertEquals("New Time", copied.time)
  }

  @Test
  fun HomeUiState_componentN_functions() {
    val state =
        HomeUiState(
            userName = "User",
            systems = listOf(SystemItem("1", "S", true)),
            recent = listOf(ActionItem("1", "A", "1h")),
            messageDraft = "Draft",
            isDrawerOpen = true,
            isTopRightOpen = true,
            isLoading = true,
            showDeleteConfirmation = true)

    assertEquals("User", state.component1())
    assertEquals(1, state.component2().size)
    assertEquals(1, state.component3().size)
    assertEquals("Draft", state.component4())
    assertTrue(state.component5())
    assertTrue(state.component6())
    assertTrue(state.component7())
    assertTrue(state.component8())
  }
}
