package com.android.sample.authentification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial_state_is_Idle`() {
        val vm = AuthViewModel()
        assertEquals(AuthUiState.Idle, vm.state.value)
    }

    @Test
    fun `onMicrosoftLoginClick_sets_Loading_state_with_Microsoft_provider`() = runTest {
        val vm = AuthViewModel()

        vm.onMicrosoftLoginClick()

        // Juste après le clic, on devrait être en Loading
        val state = vm.state.value
        assertTrue(state is AuthUiState.Loading)
        assertEquals(AuthProvider.MICROSOFT, (state as AuthUiState.Loading).provider)
    }

    @Test
    fun `onSwitchEduLoginClick_sets_Loading_state_with_SwitchEdu_provider`() = runTest {
        val vm = AuthViewModel()

        vm.onSwitchEduLoginClick()

        // Juste après le clic, on devrait être en Loading
        val state = vm.state.value
        assertTrue(state is AuthUiState.Loading)
        assertEquals(AuthProvider.SWITCH_EDU, (state as AuthUiState.Loading).provider)
    }

    @Test
    fun `Microsoft_login_eventually_becomes_SignedIn_after_delay`() = runTest {
        val vm = AuthViewModel()

        vm.onMicrosoftLoginClick()

        // État initial: Loading
        assertTrue(vm.state.value is AuthUiState.Loading)

        // Avance le temps de 1200ms (la durée du delay dans startSignIn)
        advanceTimeBy(1200)
        advanceUntilIdle()

        // Après le délai, devrait être SignedIn
        assertEquals(AuthUiState.SignedIn, vm.state.value)
    }

    @Test
    fun `SwitchEdu_login_eventually_becomes_SignedIn_after_delay`() = runTest {
        val vm = AuthViewModel()

        vm.onSwitchEduLoginClick()

        // État initial: Loading
        assertTrue(vm.state.value is AuthUiState.Loading)

        // Avance le temps de 1200ms
        advanceTimeBy(1200)
        advanceUntilIdle()

        // Après le délai, devrait être SignedIn
        assertEquals(AuthUiState.SignedIn, vm.state.value)
    }

    @Test
    fun `cannot_trigger_second_login_while_already_loading`() = runTest {
        val vm = AuthViewModel()

        // Lance un login Microsoft
        vm.onMicrosoftLoginClick()
        val firstState = vm.state.value
        assertTrue(firstState is AuthUiState.Loading)
        assertEquals(AuthProvider.MICROSOFT, (firstState as AuthUiState.Loading).provider)

        // Tente un login SwitchEdu pendant le loading
        vm.onSwitchEduLoginClick()

        // L'état ne devrait pas changer, reste sur Microsoft Loading
        val afterState = vm.state.value
        assertTrue(afterState is AuthUiState.Loading)
        assertEquals(AuthProvider.MICROSOFT, (afterState as AuthUiState.Loading).provider)
    }

    @Test
    fun `multiple_clicks_on_same_provider_while_loading_are_ignored`() = runTest {
        val vm = AuthViewModel()

        vm.onMicrosoftLoginClick()
        val state1 = vm.state.value

        // Clique encore plusieurs fois
        vm.onMicrosoftLoginClick()
        vm.onMicrosoftLoginClick()
        val state2 = vm.state.value

        // L'état reste le même Loading
        assertEquals(state1, state2)
        assertTrue(state2 is AuthUiState.Loading)
    }

    @Test
    fun `state_sequence_is_Idle_to_Loading_to_SignedIn`() = runTest {
        val vm = AuthViewModel()
        val states = mutableListOf<AuthUiState>()

        // Collecte l'état initial
        states.add(vm.state.value)

        vm.onMicrosoftLoginClick()
        states.add(vm.state.value)

        advanceTimeBy(1200)
        advanceUntilIdle()
        states.add(vm.state.value)

        // Vérifie la séquence
        assertEquals(3, states.size)
        assertTrue(states[0] is AuthUiState.Idle)
        assertTrue(states[1] is AuthUiState.Loading)
        assertTrue(states[2] is AuthUiState.SignedIn)
    }
}
