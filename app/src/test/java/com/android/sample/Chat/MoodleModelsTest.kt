package com.android.sample.Chat

import org.junit.Assert.*
import org.junit.Test

class MoodleModelsTest {

  @Test
  fun parseMoodleOverviewPayload_validJson_returnsObject() {
    val json =
        """
        {
          "type":"moodle_overview",
          "content":"- Topic: Details",
          "metadata":{
            "courseName":"Algebra",
            "weekLabel":"Week 2",
            "lastUpdated":"2025-12-11T04:15:00.000Z",
            "source":"Moodle"
          }
        }
        """
            .trimIndent()

    val result = parseMoodleOverviewPayload(json)

    assertNotNull(result)
    assertEquals("Algebra", result!!.metadata.courseName)
    assertEquals("Week 2", result.metadata.weekLabel)
    assertEquals("- Topic: Details", result.content)
  }

  @Test
  fun parseMoodleOverviewPayload_invalidJson_returnsNull() {
    val result = parseMoodleOverviewPayload("not json at all")
    assertNull(result)
  }

  @Test
  fun parseMoodleOverviewPayload_missingType_returnsNull() {
    val json =
        """
        {
          "content":"- Topic",
          "metadata":{"courseName":"Math","weekLabel":"Week 1","lastUpdated":"","source":"Moodle"}
        }
        """
            .trimIndent()

    val result = parseMoodleOverviewPayload(json)
    assertNull(result)
  }

  @Test
  fun parseMoodleOverviewPayload_null_returnsNull() {
    val result = parseMoodleOverviewPayload(null)
    assertNull(result)
  }

  @Test
  fun formatMoodleUpdatedDate_validIso_formatsNicely() {
    val formatted = formatMoodleUpdatedDate("2025-12-11T04:15:00.000Z")
    assertNotEquals("2025-12-11T04:15:00.000Z", formatted)
    assertTrue(formatted.matches(Regex("[A-Za-z]{3} \\d{1,2}, \\d{2}:\\d{2}")))
  }

  @Test
  fun formatMoodleUpdatedDate_invalid_returnsOriginal() {
    val input = "not-a-date"
    val formatted = formatMoodleUpdatedDate(input)
    assertEquals(input, formatted)
  }
}
