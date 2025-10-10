package com.android.sample.viewmodel

import com.android.sample.auth.AuthState
import com.android.sample.auth.FakeAuthRepository
import com.android.sample.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour AuthViewModel
 *
 * Ces tests démontrent comment tester le ViewModel en utilisant le FakeAuthRepository au lieu de la
 * vraie implémentation MSAL.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

  private lateinit var fakeRepository: FakeAuthRepository
  private lateinit var viewModel: AuthViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeRepository = FakeAuthRepository()
    viewModel = AuthViewModel(fakeRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state should be NotInitialized`() = runTest {
    val state = viewModel.authState.first()
    assertEquals(AuthState.NotInitialized, state)
  }

  @Test
  fun `initializeAuth should change state to Ready`() = runTest {
    // Pour le FakeRepository, on peut passer null car il n'utilise pas le contexte
    viewModel.initializeAuth(null, 0)
    advanceUntilIdle()

    val state = viewModel.authState.first()
    assertEquals(AuthState.Ready, state)
  }

  @Test
  fun `signIn should update state to SignedIn on success`() = runTest {
    fakeRepository.initialize(null, 0)

    viewModel.signIn(null)
    advanceUntilIdle()

    val state = viewModel.authState.first()
    assertEquals(AuthState.SignedIn, state)

    val user = viewModel.currentUser.first()
    assertEquals("test@example.com", user?.username)
  }

  @Test
  fun `signIn should update state to Error on failure`() = runTest {
    fakeRepository.initialize(null, 0)
    fakeRepository.shouldFailSignIn = true

    viewModel.signIn(null)
    advanceUntilIdle()

    val state = viewModel.authState.first()
    assertTrue(state is AuthState.Error)

    val errorMessage = viewModel.errorMessage.first()
    assertEquals("Échec de connexion simulé", errorMessage)
  }

  @Test
  fun `signOut should clear user and change state to Ready`() = runTest {
    fakeRepository.initialize(null, 0)

    // D'abord se connecter
    viewModel.signIn(null)
    advanceUntilIdle()

    // Puis se déconnecter
    viewModel.signOut()
    advanceUntilIdle()

    val state = viewModel.authState.first()
    assertEquals(AuthState.Ready, state)

    val user = viewModel.currentUser.first()
    assertNull(user)
  }

  @Test
  fun `isSignedIn should be true when state is SignedIn`() = runTest {
    fakeRepository.initialize(null, 0)

    viewModel.signIn(null)
    advanceUntilIdle()

    val isSignedIn = viewModel.isSignedIn.first()
    assertTrue(isSignedIn)
  }

  @Test
  fun `isSignedIn should be false when state is Ready`() = runTest {
    fakeRepository.initialize(null, 0)
    advanceUntilIdle()

    val isSignedIn = viewModel.isSignedIn.first()
    assertFalse(isSignedIn)
  }

  @Test
  fun `isLoading should be true when state is SigningIn`() = runTest {
    fakeRepository.initialize(null, 0)
    fakeRepository.signInDelay = 100 // Simuler un délai

    viewModel.signIn(null)
    // Ne pas attendre la fin avec advanceUntilIdle()

    // Vérifier immédiatement l'état de chargement
    val isLoading = viewModel.isLoading.first()
    assertTrue(isLoading)
  }

  @Test
  fun `refreshToken should update user token`() = runTest {
    fakeRepository.initialize(null, 0)

    // Se connecter
    viewModel.signIn(null)
    advanceUntilIdle()

    val initialToken = viewModel.currentUser.first()?.accessToken

    // Rafraîchir le token
    viewModel.refreshToken()
    advanceUntilIdle()

    val newToken = viewModel.currentUser.first()?.accessToken

    // Le token devrait avoir changé
    assertTrue(initialToken != newToken)
  }

  @Test
  fun `checkSignedInUser should restore user if already signed in`() = runTest {
    val existingUser =
        User(
            username = "existing@example.com",
            accessToken = "existing-token",
            tenantId = "existing-tenant")

    fakeRepository.setSignedInUser(existingUser)

    viewModel.checkSignedInUser()
    advanceUntilIdle()

    val state = viewModel.authState.first()
    assertEquals(AuthState.SignedIn, state)

    val user = viewModel.currentUser.first()
    assertEquals("existing@example.com", user?.username)
  }
}
