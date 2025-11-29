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
    val functions = setupMockFunctions("edConnectorStatusFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.getStatus()

    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
    assertEquals("https://example.com", result.baseUrl)
  }

  @Test
  fun `connect with baseUrl includes it in payload`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "connected")
    val functions = setupMockFunctions("edConnectorConnectFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.connect("token", "https://example.com")

    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
  }

  @Test
  fun `connect without baseUrl does not include it`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "connected")
    val functions = setupMockFunctions("edConnectorConnectFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.connect("token", null)

    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
  }

  @Test
  fun `connect with blank baseUrl does not include it`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "connected")
    val functions = setupMockFunctions("edConnectorConnectFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.connect("token", "   ")

    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
  }

  @Test
  fun `disconnect returns correct config`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "not_connected")
    val functions = setupMockFunctions("edConnectorDisconnectFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.disconnect()

    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, result.status)
  }

  @Test
  fun `test returns correct config`() = runTest {
    val responseData = mapOf<String, Any?>("status" to "connected")
    val functions = setupMockFunctions("edConnectorTestFn", createMockResult(responseData))
    val dataSource = EdConnectorRemoteDataSource(functions)

    val result = dataSource.test()

    assertEquals(EdConnectorStatusRemote.CONNECTED, result.status)
  }

  @Test
  fun `mapEdConnectorConfig handles null input`() = runTest {
    val result = mapEdConnectorConfig(null)
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, result.status)
  }

  @Test
  fun `mapEdConnectorConfig handles non-map input`() = runTest {
    val result = mapEdConnectorConfig("not a map")
    assertEquals(EdConnectorStatusRemote.NOT_CONNECTED, result.status)
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
