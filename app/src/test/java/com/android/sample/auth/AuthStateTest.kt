package com.android.sample.auth

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive tests for AuthState sealed class
 * 
 * Tests all possible states and their properties to achieve 100% coverage
 */
class AuthStateTest {

    @Test
    fun `NotInitialized should be singleton object`() {
        val state1 = AuthState.NotInitialized
        val state2 = AuthState.NotInitialized
        
        assertSame("NotInitialized should be singleton", state1, state2)
    }


    @Test
    fun `Error should be data class with message`() {
        val errorMessage = "Test error message"
        val errorState = AuthState.Error(errorMessage)
        
        assertEquals("Error message should match", errorMessage, errorState.message)
        assertEquals("Error(message=Test error message)", errorState.toString())
    }

    @Test
    fun `Error should support empty message`() {
        val emptyError = AuthState.Error("")
        
        assertEquals("Empty error message should be preserved", "", emptyError.message)
        assertEquals("Error(message=)", emptyError.toString())
    }


    @Test
    fun `Error should support long message`() {
        val longMessage = "This is a very long error message that contains multiple words and should be properly handled by the AuthState.Error data class"
        val longError = AuthState.Error(longMessage)
        
        assertEquals("Long error message should be preserved", longMessage, longError.message)
        assertTrue("Long error message should contain original text", longError.toString().contains(longMessage))
    }

    @Test
    fun `Error should support special characters in message`() {
        val specialMessage = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialError = AuthState.Error(specialMessage)
        
        assertEquals("Special characters should be preserved", specialMessage, specialError.message)
    }

    @Test
    fun `Error should support unicode characters in message`() {
        val unicodeMessage = "Erreur avec caractères spéciaux: éàçùñ中文日本語한국어"
        val unicodeError = AuthState.Error(unicodeMessage)
        
        assertEquals("Unicode characters should be preserved", unicodeMessage, unicodeError.message)
    }

    @Test
    fun `Error instances with same message should be equal`() {
        val message = "Same error message"
        val error1 = AuthState.Error(message)
        val error2 = AuthState.Error(message)
        
        assertEquals("Error instances with same message should be equal", error1, error2)
        assertEquals("Error instances with same message should have same hash", error1.hashCode(), error2.hashCode())
    }

    @Test
    fun `Error instances with different messages should not be equal`() {
        val error1 = AuthState.Error("First error")
        val error2 = AuthState.Error("Second error")
        
        assertNotEquals("Error instances with different messages should not be equal", error1, error2)
        assertNotEquals("Error instances with different messages should have different hash", error1.hashCode(), error2.hashCode())
    }

    @Test
    fun `Error should not be equal to other AuthState types`() {
        val error = AuthState.Error("Test error")
        
        assertNotEquals("Error should not equal NotInitialized", error, AuthState.NotInitialized)
        assertNotEquals("Error should not equal Ready", error, AuthState.Ready)
        assertNotEquals("Error should not equal SigningIn", error, AuthState.SigningIn)
        assertNotEquals("Error should not equal SignedIn", error, AuthState.SignedIn)
    }

    @Test
    fun `All singleton states should be equal to themselves`() {
        assertEquals("NotInitialized should equal itself", AuthState.NotInitialized, AuthState.NotInitialized)
        assertEquals("Ready should equal itself", AuthState.Ready, AuthState.Ready)
        assertEquals("SigningIn should equal itself", AuthState.SigningIn, AuthState.SigningIn)
        assertEquals("SignedIn should equal itself", AuthState.SignedIn, AuthState.SignedIn)
    }

    @Test
    fun `Different singleton states should not be equal`() {
        assertNotEquals("NotInitialized should not equal Ready", AuthState.NotInitialized, AuthState.Ready)
        assertNotEquals("NotInitialized should not equal SigningIn", AuthState.NotInitialized, AuthState.SigningIn)
        assertNotEquals("NotInitialized should not equal SignedIn", AuthState.NotInitialized, AuthState.SignedIn)
        assertNotEquals("Ready should not equal SigningIn", AuthState.Ready, AuthState.SigningIn)
        assertNotEquals("Ready should not equal SignedIn", AuthState.Ready, AuthState.SignedIn)
        assertNotEquals("SigningIn should not equal SignedIn", AuthState.SigningIn, AuthState.SignedIn)
    }

    @Test
    fun `AuthState should be sealed class`() {
        // Test that AuthState is indeed a sealed class by checking its subclasses
        val authStateClass = AuthState::class.java
        
        // Verify it's a sealed class (this is a structural test)
        assertTrue("AuthState should be a sealed class", authStateClass.isSealed)
        
        // Verify all expected subclasses exist
        val subclasses = authStateClass.permittedSubclasses
        assertTrue("Should have NotInitialized subclass", subclasses.any { it.simpleName == "NotInitialized" })
        assertTrue("Should have Ready subclass", subclasses.any { it.simpleName == "Ready" })
        assertTrue("Should have SigningIn subclass", subclasses.any { it.simpleName == "SigningIn" })
        assertTrue("Should have SignedIn subclass", subclasses.any { it.simpleName == "SignedIn" })
        assertTrue("Should have Error subclass", subclasses.any { it.simpleName == "Error" })
    }

    @Test
    fun `Error should support copy operation`() {
        val originalError = AuthState.Error("Original message")
        val copiedError = originalError.copy(message = "Copied message")
        
        assertEquals("Copied error should have new message", "Copied message", copiedError.message)
        assertNotEquals("Original and copied errors should be different", originalError, copiedError)
    }

    @Test
    fun `Error should support component1 destructuring`() {
        val error = AuthState.Error("Test message")
        val (message) = error
        
        assertEquals("Destructured message should match", "Test message", message)
    }

    @Test
    fun `AuthState should work in when expressions`() {
        fun getStateDescription(state: AuthState): String = when (state) {
            is AuthState.NotInitialized -> "Not initialized"
            is AuthState.Ready -> "Ready"
            is AuthState.SigningIn -> "Signing in"
            is AuthState.SignedIn -> "Signed in"
            is AuthState.Error -> "Error: ${state.message}"
        }
        
        assertEquals("NotInitialized description", "Not initialized", getStateDescription(AuthState.NotInitialized))
        assertEquals("Ready description", "Ready", getStateDescription(AuthState.Ready))
        assertEquals("SigningIn description", "Signing in", getStateDescription(AuthState.SigningIn))
        assertEquals("SignedIn description", "Signed in", getStateDescription(AuthState.SignedIn))
        assertEquals("Error description", "Error: Test error", getStateDescription(AuthState.Error("Test error")))
    }

    @Test
    fun `AuthState should work in collections`() {
        val states = listOf(
            AuthState.NotInitialized,
            AuthState.Ready,
            AuthState.SigningIn,
            AuthState.SignedIn,
            AuthState.Error("Test error")
        )
        
        assertEquals("Should have 5 states", 5, states.size)
        assertTrue("Should contain NotInitialized", states.contains(AuthState.NotInitialized))
        assertTrue("Should contain Ready", states.contains(AuthState.Ready))
        assertTrue("Should contain SigningIn", states.contains(AuthState.SigningIn))
        assertTrue("Should contain SignedIn", states.contains(AuthState.SignedIn))
        assertTrue("Should contain Error", states.contains(AuthState.Error("Test error")))
    }

    @Test
    fun `AuthState should work in sets`() {
        val stateSet = setOf(
            AuthState.NotInitialized,
            AuthState.Ready,
            AuthState.SigningIn,
            AuthState.SignedIn,
            AuthState.Error("Test error")
        )
        
        assertEquals("Should have 5 unique states", 5, stateSet.size)
        assertTrue("Should contain NotInitialized", stateSet.contains(AuthState.NotInitialized))
        assertTrue("Should contain Ready", stateSet.contains(AuthState.Ready))
        assertTrue("Should contain SigningIn", stateSet.contains(AuthState.SigningIn))
        assertTrue("Should contain SignedIn", stateSet.contains(AuthState.SignedIn))
        assertTrue("Should contain Error", stateSet.contains(AuthState.Error("Test error")))
    }
}
