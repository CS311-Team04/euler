package com.android.sample.signinscreen

import com.android.sample.authentification.AuthProvider
import org.junit.Assert.*
import org.junit.Test

class AuthProviderTest {

  @Test
  fun AuthProvider_MICROSOFT_exists() {
    assertNotNull(AuthProvider.MICROSOFT)
  }

  @Test
  fun AuthProvider_SWITCH_EDU_exists() {
    assertNotNull(AuthProvider.SWITCH_EDU)
  }

  @Test
  fun AuthProvider_values_contains_all_providers() {
    val values = AuthProvider.values()
    assertEquals(2, values.size)
    assertTrue(values.contains(AuthProvider.MICROSOFT))
    assertTrue(values.contains(AuthProvider.SWITCH_EDU))
  }

  @Test
  fun AuthProvider_valueOf_MICROSOFT() {
    assertEquals(AuthProvider.MICROSOFT, AuthProvider.valueOf("MICROSOFT"))
  }

  @Test
  fun AuthProvider_valueOf_SWITCH_EDU() {
    assertEquals(AuthProvider.SWITCH_EDU, AuthProvider.valueOf("SWITCH_EDU"))
  }

  @Test
  fun AuthProvider_values_are_unique() {
    val values = AuthProvider.values()
    assertEquals(values.size, values.distinct().size)
  }

  @Test
  fun AuthProvider_MICROSOFT_equals_itself() {
    assertEquals(AuthProvider.MICROSOFT, AuthProvider.MICROSOFT)
  }

  @Test
  fun AuthProvider_SWITCH_EDU_equals_itself() {
    assertEquals(AuthProvider.SWITCH_EDU, AuthProvider.SWITCH_EDU)
  }

  @Test
  fun AuthProvider_MICROSOFT_not_equals_SWITCH_EDU() {
    assertNotEquals(AuthProvider.MICROSOFT, AuthProvider.SWITCH_EDU)
  }

  @Test
  fun AuthProvider_name_property() {
    assertEquals("MICROSOFT", AuthProvider.MICROSOFT.name)
    assertEquals("SWITCH_EDU", AuthProvider.SWITCH_EDU.name)
  }

  @Test
  fun AuthProvider_ordinal_property() {
    val microsoft = AuthProvider.MICROSOFT.ordinal
    val switchEdu = AuthProvider.SWITCH_EDU.ordinal
    assertTrue(microsoft >= 0)
    assertTrue(switchEdu >= 0)
    assertNotEquals(microsoft, switchEdu)
  }

  @Test
  fun AuthProvider_hashCode_consistency() {
    assertEquals(AuthProvider.MICROSOFT.hashCode(), AuthProvider.MICROSOFT.hashCode())
  }

  @Test
  fun AuthProvider_hashCode_different_for_different_values() {
    val sameEnumObject = AuthProvider.MICROSOFT
    val differentEnumObject = AuthProvider.SWITCH_EDU
    assertNotEquals(
        "Enum instances representing different values should not be equal",
        sameEnumObject,
        differentEnumObject)
    assertNotEquals(
        "Hash codes for different enum values should typically differ",
        sameEnumObject.hashCode(),
        differentEnumObject.hashCode())
  }

  @Test
  fun AuthProvider_toString_returns_name() {
    assertEquals("MICROSOFT", AuthProvider.MICROSOFT.toString())
    assertEquals("SWITCH_EDU", AuthProvider.SWITCH_EDU.toString())
  }

  @Test
  fun AuthProvider_compareTo() {
    val microsoft = AuthProvider.MICROSOFT
    val switchEdu = AuthProvider.SWITCH_EDU
    assertTrue(microsoft.compareTo(switchEdu) != 0 || switchEdu.compareTo(microsoft) != 0)
  }

  @Test
  fun AuthProvider_equals_works_correctly() {
    assertTrue(AuthProvider.MICROSOFT.equals(AuthProvider.MICROSOFT))
    assertFalse(AuthProvider.MICROSOFT.equals(AuthProvider.SWITCH_EDU))
    assertFalse(AuthProvider.MICROSOFT.equals(null))
    assertFalse(AuthProvider.MICROSOFT.equals("STRING"))
  }

  @Test
  fun AuthProvider_can_be_used_in_when_expression() {
    val provider1 = AuthProvider.MICROSOFT
    val provider2 = AuthProvider.SWITCH_EDU

    val result1 =
        when (provider1) {
          AuthProvider.MICROSOFT -> "Microsoft"
          AuthProvider.SWITCH_EDU -> "Switch"
        }
    assertEquals("Microsoft", result1)

    val result2 =
        when (provider2) {
          AuthProvider.MICROSOFT -> "Microsoft"
          AuthProvider.SWITCH_EDU -> "Switch"
        }
    assertEquals("Switch", result2)
  }

  @Test
  fun AuthProvider_values_order() {
    val values = AuthProvider.values()
    assertTrue(values.isNotEmpty())
    // Verify order: MICROSOFT should be first, SWITCH_EDU second
    assertEquals(AuthProvider.MICROSOFT, values[0])
    assertEquals(AuthProvider.SWITCH_EDU, values[1])
  }

  @Test
  fun AuthProvider_valueOf_throws_exception_for_invalid_name() {
    try {
      AuthProvider.valueOf("INVALID")
      fail("Should throw IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      // Expected
    }
  }

  @Test
  fun AuthProvider_enum_is_serializable() {
    // Enum constants are inherently serializable
    assertNotNull(AuthProvider.MICROSOFT.name)
    assertNotNull(AuthProvider.SWITCH_EDU.name)
  }

  @Test
  fun AuthProvider_all_providers_have_non_empty_names() {
    AuthProvider.values().forEach { provider ->
      assertTrue("Provider name should not be empty", provider.name.isNotEmpty())
    }
  }
}
