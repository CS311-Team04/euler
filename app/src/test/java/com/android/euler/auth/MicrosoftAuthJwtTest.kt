package com.android.euler.auth

import com.android.sample.auth.MicrosoftAuth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MicrosoftAuthJwtTest {

    // Test JWT avec tenant ID
    private val testJwtWithTenant =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwidGlkIjoiZTBiY2QwNy03MjE0LTQxNmItOTY3Ni1mOWRhYzMxNTI1MDciLCJpYXQiOjE1MTYyMzkwMjJ9.signature"

    // Test JWT sans tenant ID
    private val testJwtWithoutTenant =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature"

    // Test JWT malformé
    private val malformedJwt = "invalid.jwt"

    @Test
    fun `extractTenantIdFromJwt should extract tenant ID from valid JWT`() {
        // Accéder à la méthode privée via reflection
        val method =
            MicrosoftAuth::class
                .java
                .getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
        method.isAccessible = true

        val tenantId = method.invoke(MicrosoftAuth, testJwtWithTenant) as String?

        // Vérifier que le tenant ID est extrait (peut varier selon l'implémentation)
        assertNotNull("Tenant ID should be extracted", tenantId)
        assertTrue("Tenant ID should not be empty", tenantId!!.isNotEmpty())
    }

    @Test
    fun `extractTenantIdFromJwt should handle JWT without tenant ID`() {
        val method =
            MicrosoftAuth::class
                .java
                .getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
        method.isAccessible = true

        val tenantId = method.invoke(MicrosoftAuth, testJwtWithoutTenant) as String?

        // Peut retourner null ou une chaîne vide selon l'implémentation
        assertTrue(
            "Should return null or empty string when no tenant ID",
            tenantId == null || tenantId.isEmpty()
        )
    }

    @Test
    fun `extractTenantIdFromJwt should handle malformed JWT gracefully`() {
        val method =
            MicrosoftAuth::class
                .java
                .getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
        method.isAccessible = true

        val tenantId = method.invoke(MicrosoftAuth, malformedJwt) as String?

        assertNull("Should return null for malformed JWT", tenantId)
    }

    @Test
    fun `extractTenantIdFromJwt should handle empty string`() {
        val method =
            MicrosoftAuth::class
                .java
                .getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
        method.isAccessible = true

        val tenantId = method.invoke(MicrosoftAuth, "") as String?

        assertNull("Should return null for empty string", tenantId)
    }

    @Test
    fun `extractTenantIdFromJwt should handle JWT with only one part`() {
        val method =
            MicrosoftAuth::class
                .java
                .getDeclaredMethod("extractTenantIdFromJwt", String::class.java)
        method.isAccessible = true

        val tenantId = method.invoke(MicrosoftAuth, "singlepart") as String?

        assertNull("Should return null for JWT with only one part", tenantId)
    }
}
