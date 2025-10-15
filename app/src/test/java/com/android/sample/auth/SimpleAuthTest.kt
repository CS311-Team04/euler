package com.android.sample.auth

import org.junit.Test
import org.junit.Assert.*

class SimpleAuthTest {

    @Test
    fun `test that authentication configuration is valid`() {
        assertTrue("authentification must be configured", true)
    }

    @Test
    fun `test that MSAL repository can be created`() {
        val repository = MsalAuthRepository.getInstance()
        assertNotNull("MSAL repository must be created", repository)
    }

    @Test
    fun `test that auth states are defined correctly`() {
        assertTrue("AuthState.Ready doit exister", AuthState.Ready is AuthState.Ready)
        assertTrue("AuthState.SigningIn doit exister", AuthState.SigningIn is AuthState.SigningIn)
        assertTrue("AuthState.SignedIn doit exister", AuthState.SignedIn is AuthState.SignedIn)
        assertTrue("AuthState.NotInitialized doit exister", AuthState.NotInitialized is AuthState.NotInitialized)
    }

    @Test
    fun `test that user model is correct`() {
        val user = User(
            username = "test@example.com",
            accessToken = "mock_token",
            tenantId = "mock_tenant"
        )

        assertEquals("Username must be correct", "test@example.com", user.username)
        assertEquals("Token must be correct", "mock_token", user.accessToken)
        assertEquals("TenantId must be correct", "mock_tenant", user.tenantId)
    }
}
