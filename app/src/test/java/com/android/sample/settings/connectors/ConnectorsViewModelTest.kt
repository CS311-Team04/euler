package com.android.sample.settings.connectors

import com.android.sample.epfl.EpflScheduleRepository
import com.android.sample.epfl.ScheduleStatus
import com.android.sample.util.MainDispatcherRule
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Mock EdConnectorRemoteDataSource for testing
// We pass a mock FirebaseFunctions since requireNotNull now validates it
private class MockEdConnectorRemoteDataSource :
    EdConnectorRemoteDataSource(mockk<FirebaseFunctions>(relaxed = true)) {
  var statusToReturn: EdConnectorStatusRemote = EdConnectorStatusRemote.NOT_CONNECTED
  var connectShouldSucceed: Boolean = true
  var connectError: String? = null
  var shouldThrowException: Boolean = false

  override suspend fun getStatus(): EdConnectorConfigRemote {
    return EdConnectorConfigRemote(
        status = statusToReturn, baseUrl = null, lastTestAt = null, lastError = null)
  }

  override suspend fun connect(apiToken: String, baseUrl: String?): EdConnectorConfigRemote {
    if (shouldThrowException) {
      throw Exception("Network error")
    }
    return EdConnectorConfigRemote(
        status =
            if (connectShouldSucceed) EdConnectorStatusRemote.CONNECTED
            else EdConnectorStatusRemote.ERROR,
        baseUrl = baseUrl,
        lastTestAt = null,
        lastError = connectError)
  }

  override suspend fun disconnect(): EdConnectorConfigRemote {
    return EdConnectorConfigRemote(
        status = EdConnectorStatusRemote.NOT_CONNECTED,
        baseUrl = null,
        lastTestAt = null,
        lastError = null)
  }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectorsViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  private fun createViewModel(): ConnectorsViewModel {
    // We don't need a real FirebaseFunctions instance since we override all methods
    // Mock FirebaseFunctions to avoid initialization issues in CI
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource()
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    return ConnectorsViewModel(
        functions = mockFunctions,
        edRemoteDataSource = mockDataSource,
        epflScheduleRepository = mockEpflRepo)
  }

  @Test
  fun `initial state has 4 connectors all not connected`() = runTest {
    val viewModel = createViewModel()
    // Wait for the init coroutine to complete
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()

    assertEquals(4, uiState.connectors.size)
    assertTrue(uiState.connectors.all { !it.isConnected })
    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `connectConnector for moodle opens dialog`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.connectConnector("moodle")
    val uiState = viewModel.uiState.first()

    // Moodle should open dialog, not connect directly
    assertTrue(uiState.isMoodleConnectDialogOpen)
    val moodle = uiState.connectors.find { it.id == "moodle" }
    assertNotNull(moodle)
    assertFalse(moodle!!.isConnected) // Not connected yet, dialog is open
  }

  @Test
  fun `connectConnector with non-existent id does nothing`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    val initialState = viewModel.uiState.first()

    viewModel.connectConnector("non_existent")
    val newState = viewModel.uiState.first()

    assertEquals(initialState.connectors, newState.connectors)
  }

  @Test
  fun `showDisconnectConfirmation sets pending connector`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)

    viewModel.showDisconnectConfirmation(connector)
    val uiState = viewModel.uiState.first()

    assertNotNull(uiState.pendingConnectorForDisconnect)
    assertEquals(connector, uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `disconnectConnector sets connector to not connected and clears pending`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    viewModel.connectConnector("moodle")
    val moodle = viewModel.uiState.first().connectors.find { it.id == "moodle" }!!
    viewModel.showDisconnectConfirmation(moodle)
    viewModel.disconnectConnector("moodle")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `disconnectConnector with non-existent id only clears pending`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)

    viewModel.showDisconnectConfirmation(connector)
    viewModel.disconnectConnector("non_existent")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()

    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `dismissDisconnectConfirmation clears pending connector`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)

    viewModel.showDisconnectConfirmation(connector)
    val stateWithPending = viewModel.uiState.first()
    assertNotNull(stateWithPending.pendingConnectorForDisconnect)

    viewModel.dismissDisconnectConfirmation()
    val stateAfterDismiss = viewModel.uiState.first()
    assertNull(stateAfterDismiss.pendingConnectorForDisconnect)
  }

  @Test
  fun `multiple connect operations work correctly`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.connectConnector("moodle")
    viewModel.connectConnector("ed")
    // Both Moodle and ED open dialogs, not connect directly
    val uiState = viewModel.uiState.first()

    // Both should open dialogs
    assertTrue(uiState.isMoodleConnectDialogOpen)
    assertTrue(uiState.isEdConnectDialogOpen)

    // Neither should be connected yet
    val moodle = uiState.connectors.find { it.id == "moodle" }
    val ed = uiState.connectors.find { it.id == "ed" }
    assertFalse(moodle!!.isConnected)
    assertFalse(ed!!.isConnected)

    // Other connectors should remain not connected
    val otherConnectors = uiState.connectors.filter { it.id !in listOf("moodle", "ed") }
    assertTrue(otherConnectors.all { !it.isConnected })
  }

  @Test
  fun `connect then disconnect then connect again works`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockEdDataSource = MockEdConnectorRemoteDataSource()
    val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockEdDataSource,
            epflScheduleRepository = mockEpflRepo,
            moodleRemoteDataSource = mockMoodleDataSource)
    advanceUntilIdle()

    // Connect moodle (opens dialog, then we need to confirm with credentials)
    viewModel.connectConnector("moodle")
    var uiState = viewModel.uiState.first()
    assertTrue(uiState.isMoodleConnectDialogOpen)

    // Mock successful connection
    coEvery { mockMoodleDataSource.connect(any(), any()) } returns
        MoodleConnectorConfigRemote(
            status = MoodleConnectorStatusRemote.CONNECTED, lastTestAt = null, lastError = null)

    // Confirm connection with token
    viewModel.confirmMoodleConnect("https://test.com", "test-token")
    advanceUntilIdle()
    uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)

    // Disconnect
    viewModel.showDisconnectConfirmation(uiState.connectors.find { it.id == "moodle" }!!)
    coEvery { mockMoodleDataSource.disconnect() } returns
        MoodleConnectorConfigRemote(
            status = MoodleConnectorStatusRemote.NOT_CONNECTED, lastTestAt = null, lastError = null)
    viewModel.disconnectConnector("moodle")
    advanceUntilIdle()
    uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)

    // Connect again (opens dialog)
    viewModel.connectConnector("moodle")
    assertTrue(viewModel.uiState.first().isMoodleConnectDialogOpen)
  }

  @Test
  fun `disconnectConnector for ED calls remote data source`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource()
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockDataSource,
            epflScheduleRepository = mockEpflRepo)
    advanceUntilIdle()
    viewModel.connectConnector("ed")
    viewModel.confirmEdConnect("test-token", null)
    advanceUntilIdle()
    val edConnector = viewModel.uiState.first().connectors.find { it.id == "ed" }!!
    assertTrue(edConnector.isConnected)
    viewModel.showDisconnectConfirmation(edConnector)
    viewModel.disconnectConnector("ed")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "ed" }!!.isConnected)
    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `confirmEdConnect with success updates connector state`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource().apply { connectShouldSucceed = true }
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockDataSource,
            epflScheduleRepository = mockEpflRepo)
    advanceUntilIdle()
    viewModel.connectConnector("ed")
    viewModel.confirmEdConnect("test-token", "https://test.com")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "ed" }!!.isConnected)
    assertFalse(uiState.isEdConnectDialogOpen)
    assertFalse(uiState.isEdConnecting)
    assertNull(uiState.edConnectError)
  }

  @Test
  fun `confirmEdConnect with errors shows error message`() = runTest {
    val errors = listOf("invalid_credentials", "api_unreachable", "unknown_error")
    errors.forEach { error ->
      val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
      val mockDataSource =
          MockEdConnectorRemoteDataSource().apply {
            connectShouldSucceed = false
            connectError = error
          }
      val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
      coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
      val viewModel =
          ConnectorsViewModel(
              functions = mockFunctions,
              edRemoteDataSource = mockDataSource,
              epflScheduleRepository = mockEpflRepo)
      advanceUntilIdle()
      viewModel.connectConnector("ed")
      viewModel.confirmEdConnect("test-token", null)
      advanceUntilIdle()
      val uiState = viewModel.uiState.first()
      assertFalse(uiState.connectors.find { it.id == "ed" }!!.isConnected)
      assertFalse(uiState.isEdConnecting)
      assertNotNull(uiState.edConnectError)
    }
  }

  @Test
  fun `confirmEdConnect with network exception shows generic error`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource().apply { shouldThrowException = true }
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockDataSource,
            epflScheduleRepository = mockEpflRepo)
    advanceUntilIdle()
    viewModel.connectConnector("ed")
    viewModel.confirmEdConnect("test-token", null)
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "ed" }!!.isConnected)
    assertFalse(uiState.isEdConnecting)
    assertNotNull(uiState.edConnectError)
  }

  @Test
  fun `connectMoodleWithCredentials fetches token and connects successfully`() =
      runTest(dispatcherRule.dispatcher) {
        val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
        val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
        val mockEdDataSource = MockEdConnectorRemoteDataSource()
        val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
        coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected

        val mockCallable = mockk<HttpsCallableReference>(relaxed = true)
        val mockResult = mockk<HttpsCallableResult>(relaxed = true)
        val mockData = mapOf("token" to "test-moodle-token-123")

        every { mockFunctions.getHttpsCallable("connectorsMoodleFetchTokenFn") } returns
            mockCallable
        every { mockCallable.call(any()) } returns Tasks.forResult(mockResult)
        every { mockResult.getData() } returns mockData

        // Mock successful connection
        coEvery { mockMoodleDataSource.connect(any(), any()) } returns
            MoodleConnectorConfigRemote(
                status = MoodleConnectorStatusRemote.CONNECTED, lastTestAt = null, lastError = null)

        val viewModel =
            ConnectorsViewModel(
                functions = mockFunctions,
                edRemoteDataSource = mockEdDataSource,
                epflScheduleRepository = mockEpflRepo,
                moodleRemoteDataSource = mockMoodleDataSource)
        advanceUntilIdle()

        viewModel.connectMoodleWithCredentials(
            baseUrl = "https://euler-swent.moodlecloud.com",
            username = "testuser",
            password = "testpass")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
        assertFalse(uiState.isMoodleConnecting)
        assertFalse(uiState.isMoodleConnectDialogOpen)
        assertNull(uiState.moodleConnectError)
      }

  @Test
  fun `connectMoodleWithCredentials handles token fetch failure`() =
      runTest(dispatcherRule.dispatcher) {
        val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
        val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
        val mockEdDataSource = MockEdConnectorRemoteDataSource()
        val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
        coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected

        // Mock Firebase Function call to throw exception
        val mockCallable = mockk<HttpsCallableReference>(relaxed = true)
        every { mockFunctions.getHttpsCallable("connectorsMoodleFetchTokenFn") } returns
            mockCallable
        every { mockCallable.call(any()) } returns
            Tasks.forException(Exception("Failed to fetch token"))

        val viewModel =
            ConnectorsViewModel(
                functions = mockFunctions,
                edRemoteDataSource = mockEdDataSource,
                epflScheduleRepository = mockEpflRepo,
                moodleRemoteDataSource = mockMoodleDataSource)
        advanceUntilIdle()

        viewModel.connectMoodleWithCredentials(
            baseUrl = "https://euler-swent.moodlecloud.com",
            username = "testuser",
            password = "wrongpass")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
        assertFalse(uiState.isMoodleConnecting)
        assertNotNull(uiState.moodleConnectError)
      }

  @Test
  fun `connectMoodleWithCredentials handles invalid credentials error`() =
      runTest(dispatcherRule.dispatcher) {
        val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
        val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
        val mockEdDataSource = MockEdConnectorRemoteDataSource()
        val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
        coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected

        // Mock Firebase Function call to return error
        val mockCallable = mockk<HttpsCallableReference>(relaxed = true)
        val mockException = Exception("invalid-argument: Invalid credentials")

        every { mockFunctions.getHttpsCallable("connectorsMoodleFetchTokenFn") } returns
            mockCallable
        every { mockCallable.call(any()) } returns Tasks.forException(mockException)

        val viewModel =
            ConnectorsViewModel(
                functions = mockFunctions,
                edRemoteDataSource = mockEdDataSource,
                epflScheduleRepository = mockEpflRepo,
                moodleRemoteDataSource = mockMoodleDataSource)
        advanceUntilIdle()

        viewModel.connectMoodleWithCredentials(
            baseUrl = "https://euler-swent.moodlecloud.com",
            username = "wronguser",
            password = "wrongpass")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
        assertFalse(uiState.isMoodleConnecting)
        assertNotNull(uiState.moodleConnectError)
        // Should show user-friendly error message
        assertTrue(uiState.moodleConnectError!!.isNotBlank())
      }

  @Test
  fun `confirmMoodleConnect with success updates connector state`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
    val mockEdDataSource = MockEdConnectorRemoteDataSource()
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockEdDataSource,
            epflScheduleRepository = mockEpflRepo,
            moodleRemoteDataSource = mockMoodleDataSource)
    advanceUntilIdle()

    // Mock successful connection
    coEvery { mockMoodleDataSource.connect(any(), any()) } returns
        MoodleConnectorConfigRemote(
            status = MoodleConnectorStatusRemote.CONNECTED, lastTestAt = null, lastError = null)

    viewModel.connectConnector("moodle")
    viewModel.confirmMoodleConnect("https://euler-swent.moodlecloud.com", "test-token")
    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
    assertFalse(uiState.isMoodleConnectDialogOpen)
    assertFalse(uiState.isMoodleConnecting)
    assertNull(uiState.moodleConnectError)
  }

  @Test
  fun `confirmMoodleConnect with error shows error message`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
    val mockEdDataSource = MockEdConnectorRemoteDataSource()
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockEdDataSource,
            epflScheduleRepository = mockEpflRepo,
            moodleRemoteDataSource = mockMoodleDataSource)
    advanceUntilIdle()

    // Mock connection failure
    coEvery { mockMoodleDataSource.connect(any(), any()) } returns
        MoodleConnectorConfigRemote(
            status = MoodleConnectorStatusRemote.ERROR,
            lastTestAt = null,
            lastError = "invalid_credentials")

    viewModel.connectConnector("moodle")
    viewModel.confirmMoodleConnect("https://euler-swent.moodlecloud.com", "invalid-token")
    advanceUntilIdle()

    val uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
    assertFalse(uiState.isMoodleConnecting)
    assertNotNull(uiState.moodleConnectError)
  }

  @Test
  fun `dismissMoodleConnectDialog closes dialog and clears error`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockMoodleDataSource = mockk<MoodleConnectorRemoteDataSource>(relaxed = true)
    val mockEdDataSource = MockEdConnectorRemoteDataSource()
    val mockEpflRepo = mockk<EpflScheduleRepository>(relaxed = true)
    coEvery { mockEpflRepo.getStatus() } returns ScheduleStatus.NotConnected
    val viewModel =
        ConnectorsViewModel(
            functions = mockFunctions,
            edRemoteDataSource = mockEdDataSource,
            epflScheduleRepository = mockEpflRepo,
            moodleRemoteDataSource = mockMoodleDataSource)
    advanceUntilIdle()

    viewModel.connectConnector("moodle")
    var uiState = viewModel.uiState.first()
    assertTrue(uiState.isMoodleConnectDialogOpen)

    viewModel.dismissMoodleConnectDialog()
    uiState = viewModel.uiState.first()
    assertFalse(uiState.isMoodleConnectDialogOpen)
    assertNull(uiState.moodleConnectError)
  }
}
