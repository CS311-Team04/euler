package com.android.sample.epfl

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
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EpflCampusViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var mockRepository: EpflScheduleRepository

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mock()
    whenever(mockRepository.isAuthenticated()).thenReturn(true)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  private suspend fun createViewModel(
      initialStatus: ScheduleStatus = ScheduleStatus.NotConnected
  ): EpflCampusViewModel {
    whenever(mockRepository.getStatus()).thenReturn(initialStatus)
    return EpflCampusViewModel(repository = mockRepository)
  }

  // ===== Initial state tests =====

  @Test
  fun `initial state is not connected when repository returns NotConnected`() = runTest {
    val viewModel = createViewModel(ScheduleStatus.NotConnected)
    val state = viewModel.uiState.first()

    assertFalse(state.isConnected)
    assertFalse(state.isLoading)
    assertEquals(0, state.weeklySlots)
    assertEquals(0, state.finalExams)
    assertNull(state.lastSync)
  }

  @Test
  fun `initial state is connected when repository returns Connected`() = runTest {
    val connectedStatus =
        ScheduleStatus.Connected(
            weeklySlots = 15, finalExams = 5, lastSync = "2024-01-15T10:00:00Z", optimized = true)

    val viewModel = createViewModel(connectedStatus)
    val state = viewModel.uiState.first()

    assertTrue(state.isConnected)
    assertFalse(state.isLoading)
    assertEquals(15, state.weeklySlots)
    assertEquals(5, state.finalExams)
    assertEquals("2024-01-15T10:00:00Z", state.lastSync)
  }

  @Test
  fun `initial state has error when repository returns Error`() = runTest {
    val errorStatus = ScheduleStatus.Error("Connection failed")

    val viewModel = createViewModel(errorStatus)
    val state = viewModel.uiState.first()

    assertFalse(state.isLoading)
    assertEquals("Connection failed", state.error)
  }

  // ===== updateIcsUrl tests =====

  @Test
  fun `updateIcsUrl updates input value`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://example.com/calendar.ics")
    val state = viewModel.uiState.first()

    assertEquals("https://example.com/calendar.ics", state.icsUrlInput)
  }

  @Test
  fun `updateIcsUrl sets isValidUrl based on repository validation`() = runTest {
    whenever(mockRepository.isValidIcsUrl("https://valid.com")).thenReturn(true)
    whenever(mockRepository.isValidIcsUrl("invalid")).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()

    viewModel.updateIcsUrl("https://valid.com")
    assertTrue(viewModel.uiState.first().isValidUrl)

    viewModel.updateIcsUrl("invalid")
    assertFalse(viewModel.uiState.first().isValidUrl)
  }

  @Test
  fun `updateIcsUrl sets isLikelyEpflUrl based on repository check`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl("https://campus.epfl.ch/cal")).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl("https://google.com/cal")).thenReturn(false)

    val viewModel = createViewModel()

    viewModel.updateIcsUrl("https://campus.epfl.ch/cal")
    assertTrue(viewModel.uiState.first().isLikelyEpflUrl)

    viewModel.updateIcsUrl("https://google.com/cal")
    assertFalse(viewModel.uiState.first().isLikelyEpflUrl)
  }

  // ===== syncSchedule tests =====

  @Test
  fun `syncSchedule sets error when URL is invalid`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("invalid-url")
    viewModel.syncSchedule()
    val state = viewModel.uiState.first()

    assertEquals("Invalid URL", state.error)
    assertFalse(state.isSyncing)
  }

  @Test
  fun `syncSchedule updates state on error`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)
    whenever(mockRepository.syncSchedule(any())).thenReturn(SyncResult.Error("Failed to parse ICS"))

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://example.com/bad.ics")
    viewModel.syncSchedule()
    val state = viewModel.uiState.first()

    assertFalse(state.isConnected)
    assertEquals("Failed to parse ICS", state.error)
    assertFalse(state.isSyncing)
  }

  // ===== disconnect tests =====

  @Test
  fun `disconnect resets state on success`() = runTest {
    val connectedStatus =
        ScheduleStatus.Connected(
            weeklySlots = 10, finalExams = 2, lastSync = "now", optimized = true)
    whenever(mockRepository.getStatus()).thenReturn(connectedStatus)
    whenever(mockRepository.disconnect()).thenReturn(true)

    val viewModel = createViewModel(connectedStatus)
    assertTrue(viewModel.uiState.first().isConnected)

    viewModel.disconnect()
    val state = viewModel.uiState.first()

    assertFalse(state.isConnected)
    assertEquals(0, state.weeklySlots)
    assertEquals(0, state.finalExams)
  }

  @Test
  fun `disconnect shows error on failure`() = runTest {
    val connectedStatus =
        ScheduleStatus.Connected(
            weeklySlots = 10, finalExams = 2, lastSync = "now", optimized = true)
    whenever(mockRepository.getStatus()).thenReturn(connectedStatus)
    whenever(mockRepository.disconnect()).thenReturn(false)

    val viewModel = createViewModel(connectedStatus)
    viewModel.disconnect()
    val state = viewModel.uiState.first()

    assertEquals("Failed to disconnect", state.error)
  }

  // ===== clipboard tests =====

  @Test
  fun `dismissClipboardSuggestion clears clipboard state`() = runTest {
    val viewModel = createViewModel()
    viewModel.dismissClipboardSuggestion()
    val state = viewModel.uiState.first()

    assertFalse(state.showClipboardSuggestion)
    assertNull(state.detectedClipboardUrl)
  }

  // ===== clearError and clearSuccessMessage tests =====

  @Test
  fun `clearError clears error message`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("invalid")
    viewModel.syncSchedule()

    assertNotNull(viewModel.uiState.first().error)

    viewModel.clearError()
    assertNull(viewModel.uiState.first().error)
  }

  @Test
  fun `clearSuccessMessage clears success message`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)
    whenever(mockRepository.syncSchedule(any()))
        .thenReturn(SyncResult.Success(weeklySlots = 5, finalExams = 1, message = "Done!"))

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://epfl.ch/cal.ics")
    viewModel.syncSchedule()

    assertEquals("Done!", viewModel.uiState.first().successMessage)

    viewModel.clearSuccessMessage()
    assertNull(viewModel.uiState.first().successMessage)
  }

  // ===== EpflCampusUiState tests =====

  @Test
  fun `EpflCampusUiState has correct default values`() {
    val state = EpflCampusUiState()

    assertFalse(state.isLoading)
    assertFalse(state.isSyncing)
    assertFalse(state.isConnected)
    assertEquals(0, state.weeklySlots)
    assertEquals(0, state.finalExams)
    assertNull(state.lastSync)
    assertEquals("", state.icsUrlInput)
    assertFalse(state.isValidUrl)
    assertFalse(state.isLikelyEpflUrl)
    assertNull(state.detectedClipboardUrl)
    assertFalse(state.showClipboardSuggestion)
    assertNull(state.error)
    assertNull(state.successMessage)
  }

  @Test
  fun `EpflCampusUiState copy works correctly`() {
    val original = EpflCampusUiState()
    val copied =
        original.copy(isConnected = true, weeklySlots = 10, finalExams = 3, lastSync = "2024-01-01")

    assertFalse(original.isConnected)
    assertTrue(copied.isConnected)
    assertEquals(10, copied.weeklySlots)
    assertEquals(3, copied.finalExams)
    assertEquals("2024-01-01", copied.lastSync)
  }
}
