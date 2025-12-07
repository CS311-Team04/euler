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
    @Suppress("UNCHECKED_CAST") val data = result.getData() as? Map<*, *>

    fun num(key: String): Long? = (data?.get(key) as? Number)?.toLong()

    return EdPostPublishResult(
        threadId = num("threadId") ?: num("thread_id"),
        courseId = num("courseId") ?: num("course_id"),
        threadNumber = num("threadNumber") ?: num("number"))
  }
}
