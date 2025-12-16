package com.android.sample.Chat

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MoodleOverviewResponse(
    val type: String,
    val content: String,
    val metadata: MoodleMetadata
)

@Serializable
data class MoodleMetadata(
    val courseName: String,
    val weekLabel: String,
    val lastUpdated: String,
    val source: String
)

private val moodleJson = Json { ignoreUnknownKeys = true }

fun parseMoodleOverviewPayload(raw: String?): MoodleOverviewResponse? {
  val trimmed = raw?.trim() ?: return null
  if (!trimmed.startsWith("{") || !trimmed.contains("\"type\":\"moodle_overview\"")) return null
  return runCatching { moodleJson.decodeFromString<MoodleOverviewResponse>(trimmed) }.getOrNull()
}

fun formatMoodleUpdatedDate(isoString: String): String {
  return runCatching {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault())
        formatter.format(instant)
      }
      .getOrElse { isoString }
}

fun cleanMoodleMarkdown(content: String): String {
  val lines = content.lines()
  val cleaned =
      lines
          .map { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("- ")) {
              val withoutBullet = trimmed.removePrefix("- ").trim()
              val colonIdx = withoutBullet.indexOf(':')
              val topic =
                  if (colonIdx != -1) {
                    withoutBullet.substring(0, colonIdx).trim()
                  } else {
                    withoutBullet
                  }
              if (topic.isEmpty()) null else "- $topic"
            } else {
              trimmed
            }
          }
          .filterNotNull()
  return cleaned.joinToString("\n").trim()
}
