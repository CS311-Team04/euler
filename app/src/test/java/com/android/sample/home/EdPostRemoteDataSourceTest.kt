package com.android.sample.home

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class EdPostRemoteDataSourceTest {

  @Test
  fun publish_parses_camelCase_ids_and_sends_default_course() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { data } doReturn
              mapOf(
                  "threadId" to 10,
                  "courseId" to 1153,
                  "threadNumber" to 7,
              )
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> {
          on { getHttpsCallable("edConnectorPostFn") } doReturn callableRef
        }

    val dataSource = EdPostRemoteDataSource(functions)
    val result = dataSource.publish("T", "B")

    // ids parsed
    assertEquals(10L, result.threadId)
    assertEquals(1153L, result.courseId)
    assertEquals(7L, result.threadNumber)

    // payload contains default courseId
    val captor: ArgumentCaptor<Map<String, Any>> = argumentCaptor()
    verify(callableRef).call(captor.capture())
    assertEquals(1153L, captor.firstValue["courseId"])
  }

  @Test
  fun publish_parses_snake_case_ids() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { data } doReturn
              mapOf(
                  "thread_id" to 5,
                  "course_id" to 222,
                  "number" to 9,
              )
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> {
          on { getHttpsCallable("edConnectorPostFn") } doReturn callableRef
        }

    val dataSource = EdPostRemoteDataSource(functions)
    val result = dataSource.publish("T", "B")

    assertEquals(5L, result.threadId)
    assertEquals(222L, result.courseId)
    assertEquals(9L, result.threadNumber)
  }
}
