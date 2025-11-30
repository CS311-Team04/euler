package com.android.sample.settings.connectors

import com.google.firebase.functions.FirebaseFunctions
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Mock EdConnectorRemoteDataSource for testing
// We pass null for functions since we override all methods that use it
private class MockEdConnectorRemoteDataSource : EdConnectorRemoteDataSource(null) {
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

  private fun createViewModel(): ConnectorsViewModel {
    // We don't need a real FirebaseFunctions instance since we override all methods
    // Mock FirebaseFunctions to avoid initialization issues in CI
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource()
    return ConnectorsViewModel(functions = mockFunctions, edRemoteDataSource = mockDataSource)
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
  fun `connectConnector sets connector to connected`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()

    viewModel.connectConnector("moodle")
    val uiState = viewModel.uiState.first()

    val moodle = uiState.connectors.find { it.id == "moodle" }
    assertNotNull(moodle)
    assertTrue(moodle!!.isConnected)

    // Other connectors should remain not connected
    val otherConnectors = uiState.connectors.filter { it.id != "moodle" }
    assertTrue(otherConnectors.all { !it.isConnected })
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
    // For ED, it opens a dialog, so we need to check the state differently
    val uiState = viewModel.uiState.first()

    val moodle = uiState.connectors.find { it.id == "moodle" }
    // ED should open dialog, not connect directly
    assertTrue(moodle!!.isConnected)
    assertTrue(uiState.isEdConnectDialogOpen)

    // Other connectors should remain not connected
    val otherConnectors = uiState.connectors.filter { it.id !in listOf("moodle", "ed") }
    assertTrue(otherConnectors.all { !it.isConnected })
  }

  @Test
  fun `connect then disconnect then connect again works`() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    viewModel.connectConnector("moodle")
    var uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
    viewModel.showDisconnectConfirmation(uiState.connectors.find { it.id == "moodle" }!!)
    viewModel.disconnectConnector("moodle")
    advanceUntilIdle()
    uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
    viewModel.connectConnector("moodle")
    assertTrue(viewModel.uiState.first().connectors.find { it.id == "moodle" }!!.isConnected)
  }

  @Test
  fun `disconnectConnector for ED calls remote data source`() = runTest {
    val mockFunctions = mockk<FirebaseFunctions>(relaxed = true)
    val mockDataSource = MockEdConnectorRemoteDataSource()
    val viewModel =
        ConnectorsViewModel(functions = mockFunctions, edRemoteDataSource = mockDataSource)
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
    val viewModel =
        ConnectorsViewModel(functions = mockFunctions, edRemoteDataSource = mockDataSource)
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
      val viewModel =
          ConnectorsViewModel(functions = mockFunctions, edRemoteDataSource = mockDataSource)
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
    val viewModel =
        ConnectorsViewModel(functions = mockFunctions, edRemoteDataSource = mockDataSource)
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
