package com.android.sample.settings.connectors

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EdConnectorRemoteDataSourceTest {

  private fun createMockResult(data: Map<String, Any?>): HttpsCallableResult {
    return mockk<HttpsCallableResult>().apply { every { getData() } returns data }
  }

  private fun setupMockFunctions(
      functionName: String,
      result: HttpsCallableResult
  ): FirebaseFunctions {
    val mockFunctions = mockk<FirebaseFunctions>()
    val mockCallable = mockk<HttpsCallableReference>()
    every { mockFunctions.getHttpsCallable(functionName) } returns mockCallable
    every { mockCallable.call(any()) } returns Tasks.forResult(result)
    every { mockCallable.call() } returns Tasks.forResult(result)
    return mockFunctions
  }

  @Test
  fun `getStatus returns correct config`() = runTest {
    val responseData =
        mapOf<String, Any?>(
            "status" to "connected",
            "baseUrl" to "https://example.com",
            "lastTestAt" to "2024-01-01",
            "lastError" to null)
    val dataSource =
        EdConnectorRemoteDataSource(
            setupMockFunctions("edConnectorStatusFn", createMockResult(responseData)))
    val result = dataSource.getStatus()
    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
    assertEquals("https://example.com", result.baseUrl)
  }

  @Test
  fun `connect handles baseUrl variations`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "connected")
    val functions = setupMockFunctions("edConnectorConnectFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    assertEquals(
        EdConnectorStatusRemote.CONNECTED,
        dataSource.connect("token", "https://example.com").status)
    assertEquals(EdConnectorStatusRemote.CONNECTED, dataSource.connect("token", null).status)
    assertEquals(EdConnectorStatusRemote.CONNECTED, dataSource.connect("token", "   ").status)
  }

  @Test
  fun `disconnect and test return correct config`() = runTest {
    val disconnectData = mapOf<String, Any?>("status" to "not_connected")
    val testData = mapOf<String, Any?>("status" to "connected")
    val disconnectSource =
        EdConnectorRemoteDataSource(
            setupMockFunctions("edConnectorDisconnectFn", createMockResult(disconnectData)))
    val testSource =
        EdConnectorRemoteDataSource(
            setupMockFunctions("edConnectorTestFn", createMockResult(testData)))
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, disconnectSource.disconnect().status)
    assertEquals(EdConnectorStatusRemote.CONNECTED, testSource.test().status)
  }

  @Test
  fun `mapEdConnectorConfig handles invalid input`() = runTest {
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, mapEdConnectorConfig(null).status)
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, mapEdConnectorConfig("not a map").status)
  }

  @Test
  fun `mapEdConnectorConfig handles valid map with all fields`() = runTest {
    val input =
        mapOf<String, Any?>(
            "status" to "error",
            "baseUrl" to "https://test.com",
            "lastTestAt" to "2024-01-01",
            "lastError" to "test error")
    val result = mapEdConnectorConfig(input)
    assertEquals(EdConnectorStatusRemote.ERROR, result.status)
    assertEquals("https://test.com", result.baseUrl)
    assertEquals("2024-01-01", result.lastTestAt)
    assertEquals("test error", result.lastError)
  }

  @Test
  fun `EdConnectorStatusRemote fromRaw handles all cases`() {
    assertEquals(EdConnectorStatusRemote.CONNECTED, EdConnectorStatusRemote.fromRaw("connected"))
    assertEquals(EdConnectorStatusRemote.ERROR, EdConnectorStatusRemote.fromRaw("error"))
    assertEquals(
        EdConnectorStatusRemote.NOT_CONNECTED, EdConnectorStatusRemote.fromRaw("not_connected"))
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, EdConnectorStatusRemote.fromRaw(null))
  }
}
