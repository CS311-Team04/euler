package com.android.euler.sign_in

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.android.sample.authentification.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Idle`() {
        // Test that AuthViewModel starts with Idle state
        assertEquals("Initial state should be Idle", AuthUiState.Idle, viewModel.state.value)
    }

    @Test
    fun `onMicrosoftLoginClick should change state to Loading with Microsoft provider`() {
        // Test Microsoft login click
        viewModel.onMicrosoftLoginClick()

        // Verify state changed to Loading with Microsoft provider
        val currentState = viewModel.state.value
        assertTrue("State should be Loading", currentState is AuthUiState.Loading)
        assertEquals(
            "Provider should be Microsoft",
            AuthProvider.MICROSOFT,
            (currentState as AuthUiState.Loading).provider
        )
    }

    @Test
    fun `onSwitchEduLoginClick should change state to Loading with SwitchEdu provider`() {
        // Test SwitchEdu login click
        viewModel.onSwitchEduLoginClick()

        // Verify state changed to Loading with SwitchEdu provider
        val currentState = viewModel.state.value
        assertTrue("State should be Loading", currentState is AuthUiState.Loading)
        assertEquals(
            "Provider should be SwitchEdu",
            AuthProvider.SWITCH_EDU,
            (currentState as AuthUiState.Loading).provider
        )
    }

    @Test
    fun `multiple login clicks while loading should not change state`() = runTest {
        // Test that multiple clicks while loading don't change state
        viewModel.onMicrosoftLoginClick()
        val firstLoadingState = viewModel.state.value

        // Click again while loading
        viewModel.onMicrosoftLoginClick()
        val secondLoadingState = viewModel.state.value

        // State should remain the same
        assertEquals("State should remain the same", firstLoadingState, secondLoadingState)
    }

    @Test
    fun `sign in flow should complete successfully`() = runTest {
        // Test complete sign in flow
        viewModel.onMicrosoftLoginClick()

        // Verify loading state
        assertTrue("Should be in loading state", viewModel.state.value is AuthUiState.Loading)

        // Advance time to complete the async operation
        advanceUntilIdle()

        // Verify success state
        assertEquals("Should be signed in", AuthUiState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `sign in flow with SwitchEdu should complete successfully`() = runTest {
        // Test SwitchEdu sign in flow
        viewModel.onSwitchEduLoginClick()

        // Verify loading state with correct provider
        val loadingState = viewModel.state.value as AuthUiState.Loading
        assertEquals("Provider should be SwitchEdu", AuthProvider.SWITCH_EDU, loadingState.provider)

        // Advance time to complete the async operation
        advanceUntilIdle()

        // Verify success state
        assertEquals("Should be signed in", AuthUiState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `state flow should be observable`() = runTest {
        // Test that state flow is observable and emits values
        val states = mutableListOf<AuthUiState>()

        // Collect initial state
        states.add(viewModel.state.value)

        // Trigger state change
        viewModel.onMicrosoftLoginClick()
        states.add(viewModel.state.value)

        // Complete the flow
        advanceUntilIdle()
        states.add(viewModel.state.value)

        // Verify we collected all expected states
        assertEquals("Should have collected 3 states", 3, states.size)
        assertEquals("First state should be Idle", AuthUiState.Idle, states[0])
        assertTrue("Second state should be Loading", states[1] is AuthUiState.Loading)
        assertEquals("Third state should be SignedIn", AuthUiState.SignedIn, states[2])
    }

    @Test
    fun `AuthProvider enum should have correct values`() {
        // Test AuthProvider enum values
        assertEquals("Microsoft provider should exist", "MICROSOFT", AuthProvider.MICROSOFT.name)
        assertEquals("SwitchEdu provider should exist", "SWITCH_EDU", AuthProvider.SWITCH_EDU.name)

        // Test enum values
        val allProviders = AuthProvider.values()
        assertEquals("Should have 2 providers", 2, allProviders.size)
        assertTrue("Should contain Microsoft", allProviders.contains(AuthProvider.MICROSOFT))
        assertTrue("Should contain SwitchEdu", allProviders.contains(AuthProvider.SWITCH_EDU))
    }

    @Test
    fun `AuthUiState Loading should contain provider`() {
        // Test Loading state with provider
        val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
        assertEquals(
            "Loading state should contain Microsoft provider",
            AuthProvider.MICROSOFT,
            loadingState.provider
        )

        val loadingStateEdu = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
        assertEquals(
            "Loading state should contain SwitchEdu provider",
            AuthProvider.SWITCH_EDU,
            loadingStateEdu.provider
        )
    }

    @Test
    fun `AuthUiState Error should contain message`() {
        // Test Error state with message
        val errorMessage = "Test error message"
        val errorState = AuthUiState.Error(errorMessage)
        assertEquals("Error state should contain message", errorMessage, errorState.message)
    }

    @Test
    fun `AuthUiState objects should be accessible`() {
        // Test that all AuthUiState objects are accessible
        assertNotNull("Idle state should not be null", AuthUiState.Idle)
        assertNotNull("SignedIn state should not be null", AuthUiState.SignedIn)

        // Test Loading state creation
        val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
        assertNotNull("Loading state should not be null", loadingState)

        // Test Error state creation
        val errorState = AuthUiState.Error("Test")
        assertNotNull("Error state should not be null", errorState)
    }

    @Test
    fun `state transitions should work correctly`() = runTest {
        // Test complete state transition flow
        assertEquals("Initial state should be Idle", AuthUiState.Idle, viewModel.state.value)

        // Click Microsoft login
        viewModel.onMicrosoftLoginClick()
        val loadingState = viewModel.state.value as AuthUiState.Loading
        assertEquals(
            "Should be loading with Microsoft",
            AuthProvider.MICROSOFT,
            loadingState.provider
        )

        // Complete the flow
        advanceUntilIdle()
        assertEquals("Should be signed in", AuthUiState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `switch edu login should work correctly`() = runTest {
        // Test SwitchEdu login flow
        viewModel.onSwitchEduLoginClick()

        val loadingState = viewModel.state.value as AuthUiState.Loading
        assertEquals(
            "Should be loading with SwitchEdu",
            AuthProvider.SWITCH_EDU,
            loadingState.provider
        )

        advanceUntilIdle()
        assertEquals("Should be signed in", AuthUiState.SignedIn, viewModel.state.value)
    }

    @Test
    fun `viewModel should handle rapid state changes`() = runTest {
        // Test rapid state changes
        viewModel.onMicrosoftLoginClick()
        val firstLoadingState = viewModel.state.value as AuthUiState.Loading
        assertEquals(
            "Should be loading with Microsoft",
            AuthProvider.MICROSOFT,
            firstLoadingState.provider
        )

        // Don't wait, click SwitchEdu immediately - should be ignored due to loading protection
        viewModel.onSwitchEduLoginClick()
        val currentState = viewModel.state.value as AuthUiState.Loading
        assertEquals(
            "Should still be loading with Microsoft (SwitchEdu click ignored)",
            AuthProvider.MICROSOFT,
            currentState.provider
        )

        advanceUntilIdle()
        assertEquals("Should be signed in", AuthUiState.SignedIn, viewModel.state.value)
    }
}
