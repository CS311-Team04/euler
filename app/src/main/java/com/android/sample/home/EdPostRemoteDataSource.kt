package com.android.sample.home

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class EdPostPublishResult(val threadId: Long?, val courseId: Long?, val threadNumber: Long?)

data class EdBrainSearchResult(
    val ok: Boolean,
    val posts: List<NormalizedEdPost>,
    val filters: EdSearchFilters,
    val error: EdBrainError?
)

data class NormalizedEdPost(
    val postId: String,
    val title: String,
    val snippet: String,
    val contentMarkdown: String,
    val author: String,
    val createdAt: String,
    val status: String,
    val course: String,
    val tags: List<String>,
    val url: String
)

data class EdSearchFilters(
    val course: String? = null,
    val status: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val limit: Int? = null
)

data class EdBrainError(val type: String, val message: String)

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

  suspend fun fetchPosts(query: String, limit: Int? = null): EdBrainSearchResult {
    val payload = hashMapOf<String, Any>("query" to query)
    if (limit != null) {
      payload["limit"] = limit
    }

    val result = functions.getHttpsCallable("edBrainSearchFn").call(payload).await()
    val data =
        parseResponseData(result.getData())
            ?: return EdBrainSearchResult(
                ok = false,
                posts = emptyList(),
                filters = EdSearchFilters(),
                error = EdBrainError("INVALID_RESPONSE", "Invalid response from server"))

    val ok = data["ok"] as? Boolean ?: false
    val postsList =
        (data["posts"] as? List<*>)?.mapNotNull { postData -> parseNormalizedEdPost(postData) }
            ?: emptyList()

    val filtersData = data["filters"] as? Map<*, *>
    val filters =
        EdSearchFilters(
            course = extractString(filtersData, "course"),
            status = extractString(filtersData, "status"),
            dateFrom = extractString(filtersData, "dateFrom"),
            dateTo = extractString(filtersData, "dateTo"),
            limit = extractInt(filtersData, "limit"))

    val errorData = data["error"] as? Map<*, *>
    val error =
        if (errorData != null) {
          EdBrainError(
              type = extractString(errorData, "type") ?: "UNKNOWN",
              message = extractString(errorData, "message") ?: "Unknown error")
        } else null

    return EdBrainSearchResult(ok = ok, posts = postsList, filters = filters, error = error)
  }

  private fun parseNormalizedEdPost(data: Any?): NormalizedEdPost? {
    if (data !is Map<*, *>) return null
    @Suppress("UNCHECKED_CAST") val map = data as? Map<String, Any?> ?: return null

    val tagsList = (map["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    return NormalizedEdPost(
        postId = extractString(map, "postId") ?: "",
        title = extractString(map, "title") ?: "",
        snippet = extractString(map, "snippet") ?: "",
        contentMarkdown = extractString(map, "contentMarkdown") ?: "",
        author = extractString(map, "author") ?: "",
        createdAt = extractString(map, "createdAt") ?: "",
        status = extractString(map, "status") ?: "",
        course = extractString(map, "course") ?: "",
        tags = tagsList,
        url = extractString(map, "url") ?: "")
  }

  private fun extractString(data: Map<*, *>?, key: String): String? {
    if (data == null) return null
    val value = data[key] ?: return null
    return when (value) {
      is String -> value
      is Number -> value.toString()
      else -> null
    }
  }

  private fun extractInt(data: Map<*, *>?, key: String): Int? {
    if (data == null) return null
    val value = data[key] ?: return null
    return when (value) {
      is Number -> value.toInt()
      is String -> value.toIntOrNull()
      else -> null
    }
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
