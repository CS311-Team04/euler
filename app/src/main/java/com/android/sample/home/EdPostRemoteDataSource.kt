package com.android.sample.home

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class EdPostPublishResult(val threadId: Long?, val courseId: Long?, val threadNumber: Long?)

class EdPostRemoteDataSource(private val functions: FirebaseFunctions) {

  suspend fun publish(title: String, body: String): EdPostPublishResult {
    val payload =
        hashMapOf<String, Any>(
            "title" to title,
            "body" to body,
            "courseId" to 1153L, // default course/channel for initial rollout
        )

    val result = functions.getHttpsCallable("edConnectorPostFn").call(payload).await()
    val data = parseResponseData(result.getData())

    fun num(key: String): Long? = extractLong(data, key)

    return EdPostPublishResult(
        threadId = num("threadId") ?: num("thread_id"),
        courseId = num("courseId") ?: num("course_id"),
        threadNumber = num("threadNumber") ?: num("number"))
  }

  /**
   * Safely parses the response data from Firebase Functions result. Returns a Map if the data is
   * valid, null otherwise.
   */
  private fun parseResponseData(data: Any?): Map<String, Any?>? {
    if (data == null) return null
    if (data !is Map<*, *>) return null

    // Convert to Map<String, Any?> for type safety
    return try {
      @Suppress("UNCHECKED_CAST")
      data as? Map<String, Any?>
    } catch (e: ClassCastException) {
      null
    }
  }

  /**
   * Safely extracts a Long value from the response data map. Handles different number types and
   * null values.
   */
  private fun extractLong(data: Map<String, Any?>?, key: String): Long? {
    if (data == null) return null
    val value = data[key] ?: return null

    return when (value) {
      is Number -> value.toLong()
      is String -> value.toLongOrNull()
      else -> null
    }
  }
}
