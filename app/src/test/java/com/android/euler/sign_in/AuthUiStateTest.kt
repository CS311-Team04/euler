package com.android.euler.sign_in

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthUiStateTest {

    @Test
    fun `AuthUiState Idle should be accessible`() {
        // Test that Idle state is accessible
        assertNotNull("Idle state should not be null", AuthUiState.Idle)
        assertSame("Idle should be the same instance", AuthUiState.Idle, AuthUiState.Idle)
    }

    @Test
    fun `AuthUiState SignedIn should be accessible`() {
        // Test that SignedIn state is accessible
        assertNotNull("SignedIn state should not be null", AuthUiState.SignedIn)
        assertSame(
            "SignedIn should be the same instance",
            AuthUiState.SignedIn,
            AuthUiState.SignedIn
        )
    }

    @Test
    fun `AuthUiState Loading should contain provider`() {
        // Test Loading state with Microsoft provider
        val loadingMicrosoft = AuthUiState.Loading(AuthProvider.MICROSOFT)
        assertNotNull("Loading state should not be null", loadingMicrosoft)
        assertEquals(
            "Provider should be Microsoft",
            AuthProvider.MICROSOFT,
            loadingMicrosoft.provider
        )

        // Test Loading state with SwitchEdu provider
        val loadingSwitchEdu = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
        assertNotNull("Loading state should not be null", loadingSwitchEdu)
        assertEquals(
            "Provider should be SwitchEdu",
            AuthProvider.SWITCH_EDU,
            loadingSwitchEdu.provider
        )
    }

    @Test
    fun `AuthUiState Error should contain message`() {
        // Test Error state with message
        val errorMessage = "Test error message"
        val errorState = AuthUiState.Error(errorMessage)
        assertNotNull("Error state should not be null", errorState)
        assertEquals("Error message should match", errorMessage, errorState.message)

        // Test Error state with empty message
        val emptyErrorState = AuthUiState.Error("")
        assertNotNull("Empty error state should not be null", emptyErrorState)
        assertEquals("Empty error message should match", "", emptyErrorState.message)
    }

    @Test
    fun `AuthProvider enum should have correct values`() {
        // Test AuthProvider enum values
        assertEquals("Microsoft name should be correct", "MICROSOFT", AuthProvider.MICROSOFT.name)
        assertEquals("SwitchEdu name should be correct", "SWITCH_EDU", AuthProvider.SWITCH_EDU.name)

        // Test enum ordinal values
        assertEquals("Microsoft ordinal should be 0", 0, AuthProvider.MICROSOFT.ordinal)
        assertEquals("SwitchEdu ordinal should be 1", 1, AuthProvider.SWITCH_EDU.ordinal)
    }

    @Test
    fun `AuthProvider enum should have all values`() {
        // Test that all AuthProvider values exist
        val allProviders = AuthProvider.values()
        assertEquals("Should have exactly 2 providers", 2, allProviders.size)

        val providerNames = allProviders.map { it.name }
        assertTrue("Should contain MICROSOFT", providerNames.contains("MICROSOFT"))
        assertTrue("Should contain SWITCH_EDU", providerNames.contains("SWITCH_EDU"))
    }

    @Test
    fun `AuthProvider valueOf should work correctly`() {
        // Test valueOf method
        assertEquals(
            "valueOf should return Microsoft",
            AuthProvider.MICROSOFT,
            AuthProvider.valueOf("MICROSOFT")
        )
        assertEquals(
            "valueOf should return SwitchEdu",
            AuthProvider.SWITCH_EDU,
            AuthProvider.valueOf("SWITCH_EDU")
        )
    }

    @Test
    fun `Loading states with different providers should not be equal`() {
        // Test that Loading states with different providers are not equal
        val loadingMicrosoft = AuthUiState.Loading(AuthProvider.MICROSOFT)
        val loadingSwitchEdu = AuthUiState.Loading(AuthProvider.SWITCH_EDU)

        assertFalse(
            "Loading states with different providers should not be equal",
            loadingMicrosoft == loadingSwitchEdu
        )
    }

    @Test
    fun `Loading states with same provider should be equal`() {
        // Test that Loading states with same provider are equal
        val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
        val loading2 = AuthUiState.Loading(AuthProvider.MICROSOFT)

        assertEquals("Loading states with same provider should be equal", loading1, loading2)
        assertEquals(
            "Loading states should have same hash code",
            loading1.hashCode(),
            loading2.hashCode()
        )
    }

    @Test
    fun `Error states with different messages should not be equal`() {
        // Test that Error states with different messages are not equal
        val error1 = AuthUiState.Error("Error 1")
        val error2 = AuthUiState.Error("Error 2")

        assertFalse("Error states with different messages should not be equal", error1 == error2)
    }

    @Test
    fun `Error states with same message should be equal`() {
        // Test that Error states with same message are equal
        val error1 = AuthUiState.Error("Same error")
        val error2 = AuthUiState.Error("Same error")

        assertEquals("Error states with same message should be equal", error1, error2)
        assertEquals(
            "Error states should have same hash code",
            error1.hashCode(),
            error2.hashCode()
        )
    }

    @Test
    fun `state toString should work correctly`() {
        // Test toString methods
        val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
        val errorState = AuthUiState.Error("Test error")

        assertNotNull("Loading state toString should not be null", loadingState.toString())
        assertNotNull("Error state toString should not be null", errorState.toString())
        assertTrue(
            "Loading state toString should contain provider info",
            loadingState.toString().contains("Microsoft") ||
                loadingState.toString().contains("Loading")
        )
        assertTrue(
            "Error state toString should contain error info",
            errorState.toString().contains("Test error") || errorState.toString().contains("Error")
        )
    }

    @Test
    fun `AuthUiState should implement sealed interface correctly`() {
        // Test that all states implement AuthUiState interface
        val idle: AuthUiState = AuthUiState.Idle
        val signedIn: AuthUiState = AuthUiState.SignedIn
        val loading: AuthUiState = AuthUiState.Loading(AuthProvider.MICROSOFT)
        val error: AuthUiState = AuthUiState.Error("Test")

        assertNotNull("Idle should implement AuthUiState", idle)
        assertNotNull("SignedIn should implement AuthUiState", signedIn)
        assertNotNull("Loading should implement AuthUiState", loading)
        assertNotNull("Error should implement AuthUiState", error)
    }

    @Test
    fun `Loading state should handle edge cases`() {
        // Test Loading state with different provider combinations
        val loadingMicrosoft = AuthUiState.Loading(AuthProvider.MICROSOFT)
        val loadingSwitchEdu = AuthUiState.Loading(AuthProvider.SWITCH_EDU)

        // Test provider access
        assertEquals(
            "Microsoft provider should be accessible",
            AuthProvider.MICROSOFT,
            loadingMicrosoft.provider
        )
        assertEquals(
            "SwitchEdu provider should be accessible",
            AuthProvider.SWITCH_EDU,
            loadingSwitchEdu.provider
        )

        // Test that providers are different
        assertFalse(
            "Providers should be different",
            loadingMicrosoft.provider == loadingSwitchEdu.provider
        )
    }

    @Test
    fun `Error state should handle various message types`() {
        // Test Error state with various message types
        val emptyError = AuthUiState.Error("")
        val shortError = AuthUiState.Error("Error")
        val longError = AuthUiState.Error("This is a very long error message with lots of details")
        val specialCharsError = AuthUiState.Error("Error with special chars: !@#$%^&*()")

        assertEquals("Empty error message should be preserved", "", emptyError.message)
        assertEquals("Short error message should be preserved", "Error", shortError.message)
        assertEquals(
            "Long error message should be preserved",
            "This is a very long error message with lots of details",
            longError.message
        )
        assertEquals(
            "Special chars error message should be preserved",
            "Error with special chars: !@#$%^&*()",
            specialCharsError.message
        )
    }
}
