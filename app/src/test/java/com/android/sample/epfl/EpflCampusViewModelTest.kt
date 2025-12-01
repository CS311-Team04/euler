package com.android.sample.epfl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
  private lateinit var mockContext: Context
  private lateinit var mockClipboardManager: ClipboardManager
  private lateinit var mockPackageManager: PackageManager

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mock()
    mockContext = mock()
    mockClipboardManager = mock()
    mockPackageManager = mock()

    whenever(mockRepository.isAuthenticated()).thenReturn(true)
    whenever(mockContext.getSystemService(Context.CLIPBOARD_SERVICE))
        .thenReturn(mockClipboardManager)
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
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
  }

  @Test
  fun `initial state is connected when repository returns Connected`() = runTest {
    val connectedStatus =
        ScheduleStatus.Connected(
            weeklySlots = 15, finalExams = 5, lastSync = "2024-01-15", optimized = true)
    val viewModel = createViewModel(connectedStatus)
    val state = viewModel.uiState.first()

    assertTrue(state.isConnected)
    assertEquals(15, state.weeklySlots)
    assertEquals(5, state.finalExams)
  }

  @Test
  fun `initial state has error when repository returns Error`() = runTest {
    val viewModel = createViewModel(ScheduleStatus.Error("Connection failed"))
    assertEquals("Connection failed", viewModel.uiState.first().error)
  }

  // ===== updateIcsUrl tests =====

  @Test
  fun `updateIcsUrl updates input value`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://example.com/calendar.ics")

    assertEquals("https://example.com/calendar.ics", viewModel.uiState.first().icsUrlInput)
  }

  @Test
  fun `updateIcsUrl sets isValidUrl based on repository`() = runTest {
    whenever(mockRepository.isValidIcsUrl("valid")).thenReturn(true)
    whenever(mockRepository.isValidIcsUrl("invalid")).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("valid")
    assertTrue(viewModel.uiState.first().isValidUrl)

    viewModel.updateIcsUrl("invalid")
    assertFalse(viewModel.uiState.first().isValidUrl)
  }

  // ===== syncSchedule tests =====

  @Test
  fun `syncSchedule sets error when URL is invalid`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.syncSchedule()

    assertEquals("Invalid URL", viewModel.uiState.first().error)
  }

  @Test
  fun `syncSchedule updates state on error`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)
    whenever(mockRepository.syncSchedule(any())).thenReturn(SyncResult.Error("Parse error"))

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://example.com/bad.ics")
    viewModel.syncSchedule()

    assertEquals("Parse error", viewModel.uiState.first().error)
  }

  @Test
  fun `syncSchedule calls repository on success`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)
    whenever(mockRepository.syncSchedule(any()))
        .thenReturn(SyncResult.Success(weeklySlots = 12, finalExams = 4, message = "Done"))

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://example.com/cal.ics")
    viewModel.syncSchedule()

    verify(mockRepository).syncSchedule("https://example.com/cal.ics")
  }

  // ===== disconnect tests =====

  @Test
  fun `disconnect resets state on success`() = runTest {
    val connected = ScheduleStatus.Connected(10, 2, "now", true)
    whenever(mockRepository.getStatus()).thenReturn(connected)
    whenever(mockRepository.disconnect()).thenReturn(true)

    val viewModel = createViewModel(connected)
    viewModel.disconnect()

    assertFalse(viewModel.uiState.first().isConnected)
  }

  @Test
  fun `disconnect shows error on failure`() = runTest {
    val connected = ScheduleStatus.Connected(10, 2, "now", true)
    whenever(mockRepository.getStatus()).thenReturn(connected)
    whenever(mockRepository.disconnect()).thenReturn(false)

    val viewModel = createViewModel(connected)
    viewModel.disconnect()

    assertEquals("Failed to disconnect", viewModel.uiState.first().error)
  }

  // ===== checkClipboard tests =====

  @Test
  fun `checkClipboard shows suggestion for valid EPFL URL`() = runTest {
    val clipData = mock<ClipData>()
    val clipItem = mock<ClipData.Item>()
    val url = "https://campus.epfl.ch/calendar.ics"

    whenever(mockClipboardManager.primaryClip).thenReturn(clipData)
    whenever(clipData.getItemAt(0)).thenReturn(clipItem)
    whenever(clipItem.text).thenReturn(url)
    whenever(mockRepository.isValidIcsUrl(url)).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(url)).thenReturn(true)

    val viewModel = createViewModel()
    viewModel.checkClipboard(mockContext)
    val state = viewModel.uiState.first()

    assertTrue(state.showClipboardSuggestion)
    assertEquals(url, state.detectedClipboardUrl)
  }

  @Test
  fun `checkClipboard ignores non-EPFL URL`() = runTest {
    val clipData = mock<ClipData>()
    val clipItem = mock<ClipData.Item>()

    whenever(mockClipboardManager.primaryClip).thenReturn(clipData)
    whenever(clipData.getItemAt(0)).thenReturn(clipItem)
    whenever(clipItem.text).thenReturn("https://google.com")
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.checkClipboard(mockContext)

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
  }

  @Test
  fun `checkClipboard ignores invalid URL`() = runTest {
    val clipData = mock<ClipData>()
    val clipItem = mock<ClipData.Item>()

    whenever(mockClipboardManager.primaryClip).thenReturn(clipData)
    whenever(clipData.getItemAt(0)).thenReturn(clipItem)
    whenever(clipItem.text).thenReturn("not a url")
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.checkClipboard(mockContext)

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
  }

  @Test
  fun `checkClipboard handles null clipboard`() = runTest {
    whenever(mockClipboardManager.primaryClip).thenReturn(null)

    val viewModel = createViewModel()
    viewModel.checkClipboard(mockContext)

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
  }

  @Test
  fun `checkClipboard ignores URL already in input`() = runTest {
    val clipData = mock<ClipData>()
    val clipItem = mock<ClipData.Item>()
    val url = "https://campus.epfl.ch/cal.ics"

    whenever(mockClipboardManager.primaryClip).thenReturn(clipData)
    whenever(clipData.getItemAt(0)).thenReturn(clipItem)
    whenever(clipItem.text).thenReturn(url)
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)

    val viewModel = createViewModel()
    viewModel.updateIcsUrl(url) // Set as current input
    viewModel.checkClipboard(mockContext)

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
  }

  @Test
  fun `checkClipboard handles exception gracefully`() = runTest {
    whenever(mockContext.getSystemService(any<String>())).thenThrow(RuntimeException("Error"))

    val viewModel = createViewModel()
    viewModel.checkClipboard(mockContext) // Should not throw

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
  }

  // ===== acceptClipboardUrl tests =====

  @Test
  fun `acceptClipboardUrl sets URL and dismisses`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)

    val viewModel = createViewModel()
    // Set detected URL via reflection
    val field = viewModel.javaClass.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = field.get(viewModel) as MutableStateFlow<EpflCampusUiState>
    stateFlow.value =
        stateFlow.value.copy(
            detectedClipboardUrl = "https://epfl.ch/cal.ics", showClipboardSuggestion = true)

    viewModel.acceptClipboardUrl()
    val state = viewModel.uiState.first()

    assertEquals("https://epfl.ch/cal.ics", state.icsUrlInput)
    assertFalse(state.showClipboardSuggestion)
  }

  @Test
  fun `acceptClipboardUrl does nothing when no URL`() = runTest {
    val viewModel = createViewModel()
    viewModel.acceptClipboardUrl()

    assertEquals("", viewModel.uiState.first().icsUrlInput)
  }

  @Test
  fun `dismissClipboardSuggestion clears state`() = runTest {
    val viewModel = createViewModel()
    viewModel.dismissClipboardSuggestion()

    assertFalse(viewModel.uiState.first().showClipboardSuggestion)
    assertNull(viewModel.uiState.first().detectedClipboardUrl)
  }

  // ===== openEpflCampus tests =====

  @Test
  fun `openEpflCampus opens app when installed`() = runTest {
    val mockIntent = mock<Intent>()
    // Mock getPackageInfo to not throw (app is installed)
    whenever(mockPackageManager.getPackageInfo(eq("org.pocketcampus"), any<Int>()))
        .thenReturn(mock())
    whenever(mockPackageManager.getLaunchIntentForPackage("org.pocketcampus"))
        .thenReturn(mockIntent)

    val viewModel = createViewModel()
    viewModel.openEpflCampus(mockContext)

    // Use any() since the intent's flags are modified inside the method
    verify(mockContext).startActivity(any())
  }

  @Test
  fun `openEpflCampus falls back to web when app not installed`() = runTest {
    // Mock getPackageInfo to throw NameNotFoundException (app not installed)
    whenever(mockPackageManager.getPackageInfo(eq("org.pocketcampus"), any<Int>()))
        .thenThrow(android.content.pm.PackageManager.NameNotFoundException())

    val viewModel = createViewModel()
    viewModel.openEpflCampus(mockContext)

    verify(mockContext).startActivity(any())
  }

  @Test
  fun `openEpflCampus falls back to web on app exception`() = runTest {
    // Mock getPackageInfo to throw a generic exception
    whenever(mockPackageManager.getPackageInfo(eq("org.pocketcampus"), any<Int>()))
        .thenThrow(RuntimeException("Error"))

    val viewModel = createViewModel()
    viewModel.openEpflCampus(mockContext)

    verify(mockContext).startActivity(any())
  }

  @Test
  fun `openEpflCampus sets error when all fails`() = runTest {
    // App not installed
    whenever(mockPackageManager.getPackageInfo(eq("org.pocketcampus"), any<Int>()))
        .thenThrow(android.content.pm.PackageManager.NameNotFoundException())
    // Web fallback also fails
    whenever(mockContext.startActivity(any())).thenThrow(RuntimeException("Cannot start"))

    val viewModel = createViewModel()
    viewModel.openEpflCampus(mockContext)

    assertEquals("Could not open EPFL Campus", viewModel.uiState.first().error)
  }

  // ===== clearError and clearSuccessMessage tests =====

  @Test
  fun `clearError clears error`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(false)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(false)

    val viewModel = createViewModel()
    viewModel.syncSchedule()
    assertNotNull(viewModel.uiState.first().error)

    viewModel.clearError()
    assertNull(viewModel.uiState.first().error)
  }

  @Test
  fun `clearSuccessMessage clears message`() = runTest {
    whenever(mockRepository.isValidIcsUrl(any())).thenReturn(true)
    whenever(mockRepository.isLikelyEpflUrl(any())).thenReturn(true)
    whenever(mockRepository.syncSchedule(any())).thenReturn(SyncResult.Success(5, 1, "Done!"))

    val viewModel = createViewModel()
    viewModel.updateIcsUrl("https://epfl.ch/cal.ics")
    viewModel.syncSchedule()
    assertEquals("Done!", viewModel.uiState.first().successMessage)

    viewModel.clearSuccessMessage()
    assertNull(viewModel.uiState.first().successMessage)
  }

  // ===== EpflCampusUiState tests =====

  @Test
  fun `EpflCampusUiState has correct defaults`() {
    val state = EpflCampusUiState()
    assertFalse(state.isLoading)
    assertFalse(state.isSyncing)
    assertFalse(state.isConnected)
    assertEquals(0, state.weeklySlots)
  }

  @Test
  fun `EpflCampusUiState copy works`() {
    val copied = EpflCampusUiState().copy(isConnected = true, weeklySlots = 10)
    assertTrue(copied.isConnected)
    assertEquals(10, copied.weeklySlots)
  }
}
