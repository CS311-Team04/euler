package com.android.sample

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Integration tests for the Jina / Vector Database service.
 *
 * Purpose:
 *  - Validate that the vector store can accept, store, and retrieve embeddings.
 *  - Ensure vector format, dimensionality, and similarity search consistency.
 *
 * Note:
 *  - Replace `BASE_URL` and endpoint paths once Jina or FastAPI routes are finalized.
 */
class VectorDatabaseIntegrationTest {

    // Replace with your real Jina or backend vector endpoint
    private val BASE_URL = "http://127.0.0.1:8000/api/v1/vectors"

    // --- Utility: GET request returning body as String ---
    private fun httpGet(path: String): String {
        val connection = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    // --- Utility: POST JSON payload ---
    private fun httpPost(path: String, payload: String): String {
        val connection = URL("$BASE_URL$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.write(payload.toByteArray())
        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    // --- Test: Check if vector endpoint is alive ---
    @Test
    fun testVectorDatabaseHealth() {
        val response = httpGet("/health")
        assertTrue("Vector DB should respond to /health",
            response.contains("ok", ignoreCase = true) ||
                    response.contains("healthy", ignoreCase = true))
    }

    // --- Test: Insert a simple embedding ---
    @Test
    fun testInsertVector() {
        val payload = """
            {
              "id": "test_vector_1",
              "embedding": [0.12, 0.45, 0.78, 0.91, 0.33],
              "metadata": {"source": "unit_test"}
            }
        """.trimIndent()

        val response = httpPost("/insert", payload)
        assertTrue("Insert response should mention success",
            response.contains("success", ignoreCase = true) ||
                    response.contains("inserted", ignoreCase = true))
    }

    // --- Test: Retrieve a vector by ID ---
    @Test
    fun testRetrieveVectorById() {
        val response = httpGet("/get?id=test_vector_1")

        try {
            val json = JSONObject(response)
            assertTrue("Response should contain embedding array", json.has("embedding"))
            val vectorArray = json.getJSONArray("embedding")
            assertTrue("Vector must contain at least one element", vectorArray.length() > 0)
        } catch (e: Exception) {
            fail("Invalid JSON returned for vector retrieval: $response")
        }
    }

    // --- Test: Perform a similarity query ---
    @Test
    fun testVectorSimilaritySearch() {
        val payload = """
            {
              "query_vector": [0.12, 0.45, 0.80, 0.90, 0.35],
              "top_k": 3
            }
        """.trimIndent()

        val response = httpPost("/search", payload)

        try {
            val json = JSONObject(response)
            assertTrue("Search response should contain results array", json.has("results"))
            val results = json.getJSONArray("results")
            assertTrue("Should return at least one similar vector", results.length() > 0)

            val firstResult = results.getJSONObject(0)
            assertTrue("Result should contain id and score",
                firstResult.has("id") && firstResult.has("score"))

            val score = firstResult.getDouble("score")
            assertTrue("Similarity score should be between 0 and 1", score in 0.0..1.0)
        } catch (e: Exception) {
            fail("Invalid JSON or structure from similarity search: $response")
        }
    }

    // --- Test: Dimensional consistency for inserted vectors ---
    @Test
    fun testVectorDimensionConsistency() {
        val payload = """{"query_vector": [0.12, 0.45, 0.78, 0.91, 0.33]}"""
        val response = httpPost("/dimension-check", payload)
        assertTrue(
            "Dimension check response should confirm consistent embedding size",
            response.contains("consistent", ignoreCase = true) ||
                    response.contains("true", ignoreCase = true)
        )
    }
}
