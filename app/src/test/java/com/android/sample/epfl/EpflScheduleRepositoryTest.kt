package com.android.sample.epfl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class EpflScheduleRepositoryTest {

  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFunctions: FirebaseFunctions
  private lateinit var mockUser: FirebaseUser
  private lateinit var repository: EpflScheduleRepository

  @Before
  fun setup() {
    mockAuth = mock()
    mockFunctions = mock()
    mockUser = mock()
    repository = EpflScheduleRepository(auth = mockAuth, functions = mockFunctions)
  }

  // ===== isAuthenticated tests =====

  @Test
  fun `isAuthenticated returns true when user is logged in`() {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    assertTrue(repository.isAuthenticated())
  }

  @Test
  fun `isAuthenticated returns false when user is not logged in`() {
    whenever(mockAuth.currentUser).thenReturn(null)

    assertFalse(repository.isAuthenticated())
  }

  // ===== isValidHttpUrl tests =====

  @Test
  fun `isValidHttpUrl returns true for https URL`() {
    assertTrue(repository.isValidHttpUrl("https://example.com/calendar.ics"))
  }

  @Test
  fun `isValidHttpUrl returns true for http URL`() {
    assertTrue(repository.isValidHttpUrl("http://example.com/calendar.ics"))
  }

  @Test
  fun `isValidHttpUrl returns false for empty string`() {
    assertFalse(repository.isValidHttpUrl(""))
  }

  @Test
  fun `isValidHttpUrl returns false for non-URL string`() {
    assertFalse(repository.isValidHttpUrl("not a url"))
  }

  @Test
  fun `isValidHttpUrl returns false for ftp URL`() {
    assertFalse(repository.isValidHttpUrl("ftp://example.com/calendar.ics"))
  }

  @Test
  fun `isValidHttpUrl trims whitespace`() {
    assertTrue(repository.isValidHttpUrl("  https://example.com/calendar.ics  "))
  }

  // ===== isLikelyEpflUrl tests =====

  @Test
  fun `isLikelyEpflUrl returns true for campus epfl ch URL`() {
    assertTrue(repository.isLikelyEpflUrl("https://campus.epfl.ch/deploy/something"))
  }

  @Test
  fun `isLikelyEpflUrl returns true for URL with raw-isacademia`() {
    assertTrue(repository.isLikelyEpflUrl("https://example.com/raw-isacademia?param=value"))
  }

  @Test
  fun `isLikelyEpflUrl returns true for URL with action=get_ics`() {
    assertTrue(repository.isLikelyEpflUrl("https://example.com/?action=get_ics&key=abc"))
  }

  @Test
  fun `isLikelyEpflUrl returns true for URL containing epfl ch domain`() {
    assertTrue(repository.isLikelyEpflUrl("https://schedule.epfl.ch/something"))
  }

  @Test
  fun `isLikelyEpflUrl returns true for URL containing isa epfl`() {
    assertTrue(repository.isLikelyEpflUrl("https://isa.epfl.ch/something"))
  }

  @Test
  fun `isLikelyEpflUrl returns true for URL containing isacademia`() {
    assertTrue(repository.isLikelyEpflUrl("https://example.com/isacademia/schedule"))
  }

  @Test
  fun `isLikelyEpflUrl returns false for generic ics URL`() {
    // Generic .ics URLs should NOT match - they could be from any calendar provider
    assertFalse(repository.isLikelyEpflUrl("https://calendar.google.com/calendar.ics"))
  }

  @Test
  fun `isLikelyEpflUrl returns false for generic URL`() {
    assertFalse(repository.isLikelyEpflUrl("https://google.com/calendar"))
  }

  @Test
  fun `isLikelyEpflUrl is case insensitive`() {
    assertTrue(repository.isLikelyEpflUrl("https://CAMPUS.EPFL.CH/deploy"))
    assertTrue(repository.isLikelyEpflUrl("https://example.com/RAW-ISACADEMIA"))
  }

  // ===== syncSchedule tests =====

  @Test
  fun `syncSchedule returns error when not authenticated`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    val result = repository.syncSchedule("https://example.com/calendar.ics")

    assertTrue(result is SyncResult.Error)
    assertEquals("User not authenticated", (result as SyncResult.Error).message)
  }

  @Test
  fun `syncSchedule returns success when Firebase call succeeds`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    val mockResult = mock<HttpsCallableResult>()
    val responseData =
        mapOf(
            "success" to true,
            "weeklySlots" to 15,
            "finalExams" to 3,
            "message" to "Schedule synced successfully")

    whenever(mockFunctions.getHttpsCallable("syncEpflScheduleFn")).thenReturn(mockCallable)
    whenever(mockCallable.call(any())).thenReturn(Tasks.forResult(mockResult))
    whenever(mockResult.getData()).thenReturn(responseData)

    val result = repository.syncSchedule("https://example.com/calendar.ics")

    assertTrue(result is SyncResult.Success)
    val success = result as SyncResult.Success
    assertEquals(15, success.weeklySlots)
    assertEquals(3, success.finalExams)
    assertEquals("Schedule synced successfully", success.message)
  }

  @Test
  fun `syncSchedule returns error when Firebase returns success=false`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    val mockResult = mock<HttpsCallableResult>()
    val responseData = mapOf("success" to false, "message" to "Invalid ICS format")

    whenever(mockFunctions.getHttpsCallable("syncEpflScheduleFn")).thenReturn(mockCallable)
    whenever(mockCallable.call(any())).thenReturn(Tasks.forResult(mockResult))
    whenever(mockResult.getData()).thenReturn(responseData)

    val result = repository.syncSchedule("https://example.com/calendar.ics")

    assertTrue(result is SyncResult.Error)
    assertEquals("Invalid ICS format", (result as SyncResult.Error).message)
  }

  @Test
  fun `syncSchedule returns error when Firebase call throws exception`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    whenever(mockFunctions.getHttpsCallable("syncEpflScheduleFn")).thenReturn(mockCallable)
    whenever(mockCallable.call(any())).thenReturn(Tasks.forException(Exception("Network error")))

    val result = repository.syncSchedule("https://example.com/calendar.ics")

    assertTrue(result is SyncResult.Error)
    assertTrue((result as SyncResult.Error).message.contains("Network error"))
  }

  // ===== getStatus tests =====

  @Test
  fun `getStatus returns NotConnected when not authenticated`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    val result = repository.getStatus()

    assertTrue(result is ScheduleStatus.NotConnected)
  }

  @Test
  fun `getStatus returns Connected when schedule is synced`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    val mockResult = mock<HttpsCallableResult>()
    val responseData =
        mapOf(
            "connected" to true,
            "weeklySlots" to 12,
            "finalExams" to 5,
            "lastSync" to "2024-01-15T10:30:00Z",
            "optimized" to true)

    whenever(mockFunctions.getHttpsCallable("getEpflScheduleStatusFn")).thenReturn(mockCallable)
    whenever(mockCallable.call()).thenReturn(Tasks.forResult(mockResult))
    whenever(mockResult.getData()).thenReturn(responseData)

    val result = repository.getStatus()

    assertTrue(result is ScheduleStatus.Connected)
    val connected = result as ScheduleStatus.Connected
    assertEquals(12, connected.weeklySlots)
    assertEquals(5, connected.finalExams)
    assertEquals("2024-01-15T10:30:00Z", connected.lastSync)
    assertTrue(connected.optimized)
  }

  @Test
  fun `getStatus returns NotConnected when not connected`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    val mockResult = mock<HttpsCallableResult>()
    val responseData = mapOf("connected" to false)

    whenever(mockFunctions.getHttpsCallable("getEpflScheduleStatusFn")).thenReturn(mockCallable)
    whenever(mockCallable.call()).thenReturn(Tasks.forResult(mockResult))
    whenever(mockResult.getData()).thenReturn(responseData)

    val result = repository.getStatus()

    assertTrue(result is ScheduleStatus.NotConnected)
  }

  @Test
  fun `getStatus returns Error when Firebase call throws exception`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    whenever(mockFunctions.getHttpsCallable("getEpflScheduleStatusFn")).thenReturn(mockCallable)
    whenever(mockCallable.call()).thenReturn(Tasks.forException(Exception("Connection failed")))

    val result = repository.getStatus()

    assertTrue(result is ScheduleStatus.Error)
    assertTrue((result as ScheduleStatus.Error).message.contains("Connection failed"))
  }

  // ===== disconnect tests =====

  @Test
  fun `disconnect returns false when not authenticated`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(null)

    val result = repository.disconnect()

    assertFalse(result)
  }

  @Test
  fun `disconnect returns true when Firebase call succeeds`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    val mockResult = mock<HttpsCallableResult>()

    whenever(mockFunctions.getHttpsCallable("disconnectEpflScheduleFn")).thenReturn(mockCallable)
    whenever(mockCallable.call()).thenReturn(Tasks.forResult(mockResult))

    val result = repository.disconnect()

    assertTrue(result)
  }

  @Test
  fun `disconnect returns false when Firebase call throws exception`() = runTest {
    whenever(mockAuth.currentUser).thenReturn(mockUser)

    val mockCallable = mock<HttpsCallableReference>()
    whenever(mockFunctions.getHttpsCallable("disconnectEpflScheduleFn")).thenReturn(mockCallable)
    whenever(mockCallable.call()).thenReturn(Tasks.forException(Exception("Disconnect failed")))

    val result = repository.disconnect()

    assertFalse(result)
  }

  // ===== SyncResult data class tests =====

  @Test
  fun `SyncResult Success contains correct values`() {
    val result = SyncResult.Success(weeklySlots = 10, finalExams = 2, message = "Done")

    assertEquals(10, result.weeklySlots)
    assertEquals(2, result.finalExams)
    assertEquals("Done", result.message)
  }

  @Test
  fun `SyncResult Error contains correct message`() {
    val result = SyncResult.Error("Something went wrong")

    assertEquals("Something went wrong", result.message)
  }

  // ===== ScheduleStatus data class tests =====

  @Test
  fun `ScheduleStatus Connected contains correct values`() {
    val status =
        ScheduleStatus.Connected(
            weeklySlots = 8, finalExams = 4, lastSync = "2024-01-20", optimized = false)

    assertEquals(8, status.weeklySlots)
    assertEquals(4, status.finalExams)
    assertEquals("2024-01-20", status.lastSync)
    assertFalse(status.optimized)
  }

  @Test
  fun `ScheduleStatus Connected has default optimized value of true`() {
    val status = ScheduleStatus.Connected(weeklySlots = 5, finalExams = 1, lastSync = null)

    assertTrue(status.optimized)
  }

  @Test
  fun `ScheduleStatus Error contains correct message`() {
    val status = ScheduleStatus.Error("Error message")

    assertEquals("Error message", status.message)
  }
}
