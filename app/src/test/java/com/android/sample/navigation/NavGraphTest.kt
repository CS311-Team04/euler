package com.android.sample.navigation

import org.junit.Assert.*
import org.junit.Test

/** Unit tests for navigation routes and constants */
class NavGraphTest {

  @Test
  fun routes_object_contains_all_constants() {
    // Test that all route constants are defined
    assertNotNull("Opening route should be defined", Routes.Opening)
    assertNotNull("SignIn route should be defined", Routes.SignIn)
    assertNotNull("Home route should be defined", Routes.Home)
    assertNotNull("HomeWithDrawer route should be defined", Routes.HomeWithDrawer)
    assertNotNull("Settings route should be defined", Routes.Settings)
    assertNotNull("VoiceChat route should be defined", Routes.VoiceChat)
  }

  @Test
  fun routes_have_correct_values() {
    // Test that routes have the expected string values
    assertEquals("Opening route should match", "opening", Routes.Opening)
    assertEquals("SignIn route should match", "signin", Routes.SignIn)
    assertEquals("Home route should match", "home", Routes.Home)
    assertEquals("HomeWithDrawer route should match", "home_with_drawer", Routes.HomeWithDrawer)
    assertEquals("Settings route should match", "settings", Routes.Settings)
    assertEquals("VoiceChat route should match", "voice_chat", Routes.VoiceChat)
  }

  @Test
  fun all_routes_are_unique() {
    val routes =
        setOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)
    assertEquals("All routes should be unique", 6, routes.size)
  }

  @Test
  fun routes_follow_naming_convention() {
    // All routes should be lowercase with underscores
    val routes =
        listOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)

    for (route in routes) {
      assertTrue("Route should be lowercase: $route", route == route.lowercase())
      assertFalse("Route should not contain spaces: $route", route.contains(" "))
    }
  }
}
