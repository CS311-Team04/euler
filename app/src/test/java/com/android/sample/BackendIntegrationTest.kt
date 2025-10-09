package com.android.sample

import org.junit.Assert.*
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import org.json.JSONObject

/**
 * Integration tests for the FastAPI backend and Jina vector service.
 *
 * Purpose:
 *  - Ensure the FastAPI endpoints are reachable.
 *  - Validate JSON response structure.
 *  - Confirm vector set operations (retrieval, query, etc.) return correct format.
 *
 * Note:
 *  - Replace placeholder URLs once the backend is deployed.
 *  - Tests will automatically validate real network responses.
 */
class BackendIntegrationTest {

    // --- Configurable base URL (replace with your FastAPI endpoint) ---
    private val BASE_URL = "http://127.0.0.1:8000" //TODO: update with deployed backend

    // --- Utility: perform GET request and return response as string ---
    private fun httpGet(path: String): String {
        val connection = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    // --- Utility: perform POST request with JSON payload ---
    private fun httpPost(path: String, payload: String): String {
        val connection = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.write(payload.toByteArray())
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    // --- Test: FastAPI backend is alive ---
    @Test
    fun testHealthEndpoint() {
        val response = httpGet("/health")
        assertTrue("Response should contain 'healthy' or status OK",
            response.contains("healthy", ignoreCase = true) ||
                    response.contains("ok", ignoreCase = true))
    }

    // --- Test: chat/query endpoint returns valid JSON ---
    @Test
    fun testChatEndpointReturnsJSON() {
        val prompt = """{"query": "What is EPFL?"}"""
        val response = httpPost("/api/v1/chat", prompt)

        try {
            val json = JSONObject(response)
            assertTrue("Response should contain 'response' or 'answer' field",
                json.has("response") || json.has("answer"))
        } catch (e: Exception) {
            fail("Response is not valid JSON: $response")
        }
    }

    // --- Test: response contains vector or embedding data ---
    @Test
    fun testVectorRetrievalStructure() {
        val response = httpGet("/api/v1/vectors/sample") // optional endpoint for debugging
        try {
            val json = JSONObject(response)
            assertTrue("Vector response should contain embeddings or vectors array",
                json.has("vectors") || json.has("embeddings"))
        } catch (e: Exception) {
            fail("Invalid JSON structure for vector retrieval: $response")
        }
    }

    // --- Test: backend returns consistent format for multiple queries ---
    @Test
    fun testMultipleQueriesConsistency() {
        val queries = listOf("EPFL", "AI research", "admissions")
        val results = queries.map { q ->
            val payload = """{"query": "$q"}"""
            JSONObject(httpPost("/api/v1/chat", payload))
        }

        val keys = results.first().keys().asSequence().toSet()
        for (res in results.drop(1)) {
            val resKeys = res.keys().asSequence().toSet()
            assertEquals("All responses should share the same JSON structure", keys, resKeys)
        }
    }
}
