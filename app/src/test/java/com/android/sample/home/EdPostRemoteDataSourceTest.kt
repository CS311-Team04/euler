package com.android.sample.home

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class EdPostRemoteDataSourceTest {

  @Test
  fun publish_parses_camelCase_ids_and_sends_courseId_and_isAnonymous() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
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
    val result = dataSource.publish("T", "B", 2000L, true)

    // ids parsed
    assertEquals(10L, result.threadId)
    assertEquals(1153L, result.courseId)
    assertEquals(7L, result.threadNumber)

    // payload contains courseId and isAnonymous
    val captor = argumentCaptor<Map<String, Any>>()
    verify(callableRef).call(captor.capture())
    assertEquals(2000L, captor.firstValue["courseId"])
    assertEquals(true, captor.firstValue["isAnonymous"])
  }

  @Test
  fun publish_defaults_isAnonymous_to_false() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
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
    dataSource.publish("T", "B", 2000L)

    val captor = argumentCaptor<Map<String, Any>>()
    verify(callableRef).call(captor.capture())
    assertEquals(false, captor.firstValue["isAnonymous"])
  }

  @Test
  fun publish_omits_courseId_when_not_provided() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
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
    dataSource.publish("T", "B")

    val captor = argumentCaptor<Map<String, Any>>()
    verify(callableRef).call(captor.capture())
    // Verify that courseId key is omitted entirely (not present in the map)
    assertFalse(
        "courseId should be omitted from payload when not provided",
        captor.firstValue.containsKey("courseId"))
    assertEquals(false, captor.firstValue["isAnonymous"])
  }

  @Test
  fun publish_parses_snake_case_ids() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
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

  @Test
  fun fetchPosts_returns_error_when_response_invalid() = runTest {
    val callableResult = mock<HttpsCallableResult> { on { getData() } doReturn null }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> { on { getHttpsCallable("edBrainSearchFn") } doReturn callableRef }

    val result = EdPostRemoteDataSource(functions).fetchPosts("test")

    assertFalse(result.ok)
    assertTrue(result.posts.isEmpty())
    assertEquals("INVALID_RESPONSE", result.error?.type)
  }

  @Test
  fun fetchPosts_parses_valid_response_with_all_fields() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf(
                  "ok" to true,
                  "posts" to
                      listOf(
                          mapOf(
                              "postId" to "123",
                              "title" to "Test",
                              "snippet" to "Snip",
                              "contentMarkdown" to "Content",
                              "author" to "Author",
                              "createdAt" to "2024-01-01",
                              "status" to "active",
                              "course" to "CS-101",
                              "tags" to listOf("tag1", "tag2"),
                              "url" to "https://test.com")),
                  "filters" to
                      mapOf(
                          "course" to "CS-101",
                          "status" to "active",
                          "dateFrom" to "2024-01-01",
                          "dateTo" to "2024-12-31",
                          "limit" to 10),
                  "error" to mapOf("type" to "WARN", "message" to "Test warning"))
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> { on { getHttpsCallable("edBrainSearchFn") } doReturn callableRef }

    val result = EdPostRemoteDataSource(functions).fetchPosts("query", 5)

    assertTrue(result.ok)
    assertEquals(1, result.posts.size)
    assertEquals("123", result.posts[0].postId)
    assertEquals(2, result.posts[0].tags.size)
    assertEquals("CS-101", result.filters.course)
    assertEquals(10, result.filters.limit)
    assertNotNull(result.error)
    assertEquals("WARN", result.error?.type)

    val captor = argumentCaptor<Map<String, Any>>()
    verify(callableRef).call(captor.capture())
    assertEquals(5, captor.firstValue["limit"])
  }

  @Test
  fun fetchPosts_handles_missing_optional_fields() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf(
                  "ok" to false,
                  "posts" to
                      listOf(
                          mapOf("postId" to 456), // Number as postId, missing most fields
                          "invalid", // Not a map
                          mapOf<String, Any?>()), // Empty map
                  "filters" to mapOf<String, Any?>()) // Empty filters
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> { on { getHttpsCallable("edBrainSearchFn") } doReturn callableRef }

    val result = EdPostRemoteDataSource(functions).fetchPosts("test")

    assertFalse(result.ok)
    assertTrue(result.posts.size >= 1) // At least one valid map parsed
    assertEquals("456", result.posts.find { it.postId == "456" }?.postId)
    assertNull(result.filters.course)
    assertNull(result.error)
  }

  @Test
  fun extractString_handles_number_and_other_types() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf(
                  "ok" to true,
                  "filters" to
                      mapOf(
                          "course" to 123, // Number should convert to string
                          "status" to listOf("invalid"))) // Non-string/number should be null
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> { on { getHttpsCallable("edBrainSearchFn") } doReturn callableRef }

    val result = EdPostRemoteDataSource(functions).fetchPosts("test")

    assertEquals("123", result.filters.course)
    assertNull(result.filters.status)
  }

  @Test
  fun extractInt_handles_string_and_invalid_types() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf("ok" to true, "filters" to mapOf("limit" to "42")) // String number
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> { on { getHttpsCallable("edBrainSearchFn") } doReturn callableRef }

    val result = EdPostRemoteDataSource(functions).fetchPosts("test")

    assertEquals(42, result.filters.limit)
  }

  @Test
  fun getCourses_parses_courses_correctly() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf(
                  "courses" to
                      listOf(
                          mapOf("id" to 1, "code" to "CS-101", "name" to "Intro CS"),
                          mapOf("id" to 2, "code" to null, "name" to "No Code Course"),
                          mapOf("id" to 3, "code" to "MATH-200", "name" to "Calculus")))
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> {
          on { getHttpsCallable("edConnectorGetCoursesFn") } doReturn callableRef
        }

    val dataSource = EdPostRemoteDataSource(functions)
    val result = dataSource.getCourses()

    assertEquals(3, result.courses.size)
    assertEquals(1L, result.courses[0].id)
    assertEquals("CS-101", result.courses[0].code)
    assertEquals("Intro CS", result.courses[0].name)
    assertEquals(2L, result.courses[1].id)
    assertNull(result.courses[1].code)
    assertEquals("No Code Course", result.courses[1].name)
  }

  @Test
  fun getCourses_handles_empty_courses_list() = runTest {
    val callableResult =
        mock<HttpsCallableResult> { on { getData() } doReturn mapOf("courses" to emptyList<Any>()) }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> {
          on { getHttpsCallable("edConnectorGetCoursesFn") } doReturn callableRef
        }

    val dataSource = EdPostRemoteDataSource(functions)
    val result = dataSource.getCourses()

    assertTrue(result.courses.isEmpty())
  }

  @Test
  fun getCourses_handles_invalid_course_data() = runTest {
    val callableResult =
        mock<HttpsCallableResult> {
          on { getData() } doReturn
              mapOf(
                  "courses" to
                      listOf(
                          mapOf("id" to 1, "code" to "CS-101", "name" to "Valid"),
                          "invalid", // Not a map
                          mapOf<String, Any?>(), // Empty map - should be filtered
                          mapOf("id" to "not-a-number"))) // Invalid id
        }
    val callableRef =
        mock<HttpsCallableReference> {
          on { call(any<Map<String, Any>>()) } doReturn Tasks.forResult(callableResult)
        }
    val functions =
        mock<FirebaseFunctions> {
          on { getHttpsCallable("edConnectorGetCoursesFn") } doReturn callableRef
        }

    val dataSource = EdPostRemoteDataSource(functions)
    val result = dataSource.getCourses()

    // Only valid courses should be parsed
    assertTrue(result.courses.size >= 1)
    assertEquals("CS-101", result.courses.find { it.code == "CS-101" }?.code)
  }
}
