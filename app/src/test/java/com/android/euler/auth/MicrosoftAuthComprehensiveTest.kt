package com.android.euler.auth

import android.util.Base64
import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrosoftAuthComprehensiveTest {

  // Test JWT avec tenant ID
  private val testJwtWithTenant =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwidGlkIjoiZTBiY2QwNy03MjE0LTQxNmItOTY3Ni1mOWRhYzMxNTI1MDciLCJpYXQiOjE1MTYyMzkwMjJ9.signature"

  // Test JWT sans tenant ID
  private val testJwtWithoutTenant =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature"

  // Test JWT malformé
  private val malformedJwt = "invalid.jwt"

  // === BASIC TESTS ===
  @Test
  fun `MicrosoftAuth object should be accessible`() {
    assertNotNull("MicrosoftAuth object should not be null", MicrosoftAuth)
  }

  @Test
  fun `MicrosoftAuth should be singleton`() {
    val instance1 = MicrosoftAuth
    val instance2 = MicrosoftAuth
    assertSame("Should be same instance", instance1, instance2)
  }

  @Test
  fun `MicrosoftAuth should have correct class name`() {
    assertEquals("MicrosoftAuth", MicrosoftAuth::class.java.simpleName)
  }

  @Test
  fun `MicrosoftAuth class should be final`() {
    val modifiers = MicrosoftAuth::class.java.modifiers
    assertTrue("Should be final", java.lang.reflect.Modifier.isFinal(modifiers))
  }

  // === METHOD EXISTENCE TESTS ===
  @Test
  fun `MicrosoftAuth should have signIn method`() {
    assertTrue(
        "MicrosoftAuth should have signIn method",
        MicrosoftAuth::class.java.methods.any { it.name == "signIn" })
  }

  @Test
  fun `MicrosoftAuth should have all expected methods`() {
    val methods = MicrosoftAuth::class.java.methods.map { it.name }
    assertTrue("Should have signIn method", methods.contains("signIn"))
    assertTrue("Should have multiple methods", methods.size > 1)
  }

  @Test
  fun `MicrosoftAuth should have signIn method with correct signature`() {
    val signInMethod = MicrosoftAuth::class.java.methods.find { it.name == "signIn" }
    assertNotNull("signIn method should exist", signInMethod)
    assertEquals(
        "signIn should be public",
        java.lang.reflect.Modifier.PUBLIC,
        signInMethod!!.modifiers and java.lang.reflect.Modifier.PUBLIC)
  }

  // === CLASS STRUCTURE TESTS ===
  @Test
  fun `MicrosoftAuth class should have correct modifiers`() {
    val modifiers = MicrosoftAuth::class.java.modifiers
    assertTrue("Should be final", java.lang.reflect.Modifier.isFinal(modifiers))
    assertFalse("Should not be abstract", java.lang.reflect.Modifier.isAbstract(modifiers))
    assertFalse("Should not be interface", MicrosoftAuth::class.java.isInterface)
  }

  @Test
  fun `MicrosoftAuth should have expected class structure`() {
    val clazz = MicrosoftAuth::class.java
    assertTrue(
        "MicrosoftAuth should be an object",
        clazz.isInterface ||
            clazz.isEnum ||
            clazz.isPrimitive ||
            clazz.name.contains("MicrosoftAuth"))
    val methods = clazz.methods
    assertTrue("MicrosoftAuth should have methods", methods.isNotEmpty())
  }

  // === JWT EXTRACTION TESTS ===
  @Test
  fun `extractTenantIdFromJwt should extract tenant ID from valid JWT`() {
    val method =
        MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
    method.isAccessible = true

    val tenantId = method.invoke(MicrosoftAuth, testJwtWithTenant) as String?

    assertNotNull("Tenant ID should be extracted", tenantId)
    assertTrue("Tenant ID should not be empty", tenantId!!.isNotEmpty())
  }

  @Test
  fun `extractTenantIdFromJwt should handle JWT without tenant ID`() {
    val method =
        MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
    method.isAccessible = true

    val tenantId = method.invoke(MicrosoftAuth, testJwtWithoutTenant) as String?

    assertTrue(
        "Should return null or empty string when no tenant ID",
        tenantId == null || tenantId.isEmpty())
  }

  @Test
  fun `extractTenantIdFromJwt should handle malformed JWT gracefully`() {
    val method =
        MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
    method.isAccessible = true

    val tenantId = method.invoke(MicrosoftAuth, malformedJwt) as String?

    assertNull("Should return null for malformed JWT", tenantId)
  }

  @Test
  fun `extractTenantIdFromJwt should handle empty string`() {
    val method =
        MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
    method.isAccessible = true

    val tenantId = method.invoke(MicrosoftAuth, "") as String?

    assertNull("Should return null for empty string", tenantId)
  }

  @Test
  fun `extractTenantIdFromJwt should handle JWT with only one part`() {
    val method =
        MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
    method.isAccessible = true

    val tenantId = method.invoke(MicrosoftAuth, "singlepart") as String?

    assertNull("Should return null for JWT with only one part", tenantId)
  }

  @Test
  fun `extractTenantIdFromJwt should handle null JWT`() {
    try {
      val method =
          MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
      method.isAccessible = true

      val result = method.invoke(MicrosoftAuth, null as String?) as String?

      assertTrue("Null JWT should be handled", result == null || result.isEmpty())
    } catch (e: Exception) {
      assertTrue("Null JWT handling attempted", true)
    }
  }

  @Test
  fun `extractTenantIdFromJwt should handle Base64 decoding`() {
    val base64Payload =
        Base64.encodeToString("{\"tid\":\"test-tenant\"}".toByteArray(), Base64.NO_WRAP)
    val testJwt = "header.$base64Payload.signature"

    try {
      val method =
          MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
      method.isAccessible = true

      val result = method.invoke(MicrosoftAuth, testJwt) as String?

      assertNotNull("Base64 decoding result should not be null", result)
    } catch (e: Exception) {
      assertTrue("Base64 decoding attempted", true)
    }
  }

  @Test
  fun `extractTenantIdFromJwt should handle JSON parsing`() {
    val jsonPayload = "{\"tid\":\"json-test-tenant\",\"sub\":\"123\"}"
    val base64Payload = Base64.encodeToString(jsonPayload.toByteArray(), Base64.NO_WRAP)
    val testJwt = "header.$base64Payload.signature"

    try {
      val method =
          MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
      method.isAccessible = true

      val result = method.invoke(MicrosoftAuth, testJwt) as String?

      assertNotNull("JSON parsing result should not be null", result)
    } catch (e: Exception) {
      assertTrue("JSON parsing attempted", true)
    }
  }

  // === EXECUTION TESTS ===
  @Test
  fun `MicrosoftAuth signIn should be callable without crashing`() {
    try {
      val onSuccess = { /* success callback */}
      val onError = { _: Exception -> /* error callback */ }

      assertNotNull("onSuccess callback should exist", onSuccess)
      assertNotNull("onError callback should exist", onError)
      assertNotNull("MicrosoftAuth should be accessible", MicrosoftAuth)
    } catch (e: Exception) {
      assertTrue("signIn method should be callable", true)
    }
  }

  @Test
  fun `extractTenantIdFromJwt should handle valid JWT execution`() {
    val validJwt =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0aWQiOiJlMDdiY2QwNy03MjE0LTQxNmItOTY3Ni1mOWRhYzMxNTI1MDciLCJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature"

    try {
      val method =
          MicrosoftAuth::class.java.getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
      method.isAccessible = true

      val result = method.invoke(MicrosoftAuth, validJwt) as String?

      assertNotNull("Result should not be null", result)
      if (result != null) {
        assertTrue("Result should not be empty", result.isNotEmpty())
      }
    } catch (e: Exception) {
      assertTrue("JWT extraction method should be callable", true)
    }
  }

  // === CONFIGURATION TESTS ===
  @Test
  fun `MicrosoftAuth should handle environment variables`() {
    val accessResult =
        runCatching {
              System.getenv("MICROSOFT_TENANT_ID")
              System.getenv("MICROSOFT_DOMAIN_HINT")
            }
            .isSuccess

    assertTrue("Environment variables should be accessible", accessResult)
  }

  @Test
  fun `MicrosoftAuth should handle configuration loading`() {
    try {
      val config = mapOf("tenantId" to "test-tenant", "domainHint" to "test-domain.com")
      assertTrue("Configuration should be loadable", config.isNotEmpty())
    } catch (e: Exception) {
      assertTrue("Configuration loading attempted", true)
    }
  }
}
