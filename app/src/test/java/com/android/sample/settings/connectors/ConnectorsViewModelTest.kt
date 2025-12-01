package com.android.sample.settings.connectors

import com.android.sample.epfl.EpflScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectorsViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(): ConnectorsViewModel {
    val mockRepository = mock<EpflScheduleRepository>()
    whenever(mockRepository.isAuthenticated()).thenReturn(false)
    return ConnectorsViewModel(epflScheduleRepository = mockRepository)
  }

  @Test
  fun `initial state has 4 connectors all not connected`() = runTest {
    val viewModel = createViewModel()
    val uiState = viewModel.uiState.first()

    assertEquals(4, uiState.connectors.size)
    assertTrue(uiState.connectors.all { !it.isConnected })
    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `connectConnector sets connector to connected`() = runTest {
    val viewModel = createViewModel()

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
    val initialState = viewModel.uiState.first()

    viewModel.connectConnector("non_existent")
    val newState = viewModel.uiState.first()

    assertEquals(initialState.connectors, newState.connectors)
  }

  @Test
  fun `showDisconnectConfirmation sets pending connector`() = runTest {
    val viewModel = createViewModel()
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)

    viewModel.showDisconnectConfirmation(connector)
    val uiState = viewModel.uiState.first()

    assertNotNull(uiState.pendingConnectorForDisconnect)
    assertEquals(connector, uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `disconnectConnector sets connector to not connected and clears pending`() = runTest {
    val viewModel = createViewModel()

    // First connect a connector
    viewModel.connectConnector("moodle")
    val connectedState = viewModel.uiState.first()
    val moodle = connectedState.connectors.find { it.id == "moodle" }!!

    // Show disconnect confirmation
    viewModel.showDisconnectConfirmation(moodle)

    // Disconnect
    viewModel.disconnectConnector("moodle")
    val disconnectedState = viewModel.uiState.first()

    val disconnectedMoodle = disconnectedState.connectors.find { it.id == "moodle" }
    assertNotNull(disconnectedMoodle)
    assertFalse(disconnectedMoodle!!.isConnected)
    assertNull(disconnectedState.pendingConnectorForDisconnect)
  }

  @Test
  fun `disconnectConnector with non-existent id only clears pending`() = runTest {
    val viewModel = createViewModel()
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)

    viewModel.showDisconnectConfirmation(connector)
    viewModel.disconnectConnector("non_existent")
    val uiState = viewModel.uiState.first()

    assertNull(uiState.pendingConnectorForDisconnect)
  }

  @Test
  fun `dismissDisconnectConfirmation clears pending connector`() = runTest {
    val viewModel = createViewModel()
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

    viewModel.connectConnector("moodle")
    viewModel.connectConnector("ed")
    val uiState = viewModel.uiState.first()

    val moodle = uiState.connectors.find { it.id == "moodle" }
    val ed = uiState.connectors.find { it.id == "ed" }

    assertTrue(moodle!!.isConnected)
    assertTrue(ed!!.isConnected)

    // Other connectors should remain not connected
    val otherConnectors = uiState.connectors.filter { it.id !in listOf("moodle", "ed") }
    assertTrue(otherConnectors.all { !it.isConnected })
  }

  @Test
  fun `connect then disconnect then connect again works`() = runTest {
    val viewModel = createViewModel()

    viewModel.connectConnector("moodle")
    var uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)

    val moodle = uiState.connectors.find { it.id == "moodle" }!!
    viewModel.showDisconnectConfirmation(moodle)
    viewModel.disconnectConnector("moodle")
    uiState = viewModel.uiState.first()
    assertFalse(uiState.connectors.find { it.id == "moodle" }!!.isConnected)

    viewModel.connectConnector("moodle")
    uiState = viewModel.uiState.first()
    assertTrue(uiState.connectors.find { it.id == "moodle" }!!.isConnected)
  }
}
