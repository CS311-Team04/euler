package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MoodleFileViewerTest {

  @get:Rule val composeRule = createComposeRule()

  private val sampleFile =
      MoodleFileAttachment(
          url = "https://example.com/file.pdf",
          filename = "lecture1.pdf",
          mimetype = "application/pdf",
          courseName = "Algebra",
          fileType = "lecture",
          fileNumber = "1",
          week = null)

  @Test
  fun displaysCard() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithTag("moodle_file_card").assertIsDisplayed()
  }

  @Test
  fun displaysHeaderText() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithText("Here is the Lecture 1 from Algebra").assertIsDisplayed()
  }

  @Test
  fun displaysFilename() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithText("lecture1.pdf").assertIsDisplayed()
  }

  @Test
  fun displaysFileTypeWithNumber() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithText("Lecture 1").assertIsDisplayed()
  }

  @Test
  fun displaysPdfPill() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithText("PDF").assertIsDisplayed()
  }

  @Test
  fun displaysViewButton() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithTag("moodle_file_view_button").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("View").assertIsDisplayed()
  }

  @Test
  fun displaysDownloadButton() {
    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = sampleFile) } }

    composeRule.onNodeWithTag("moodle_file_download_button").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Download").assertIsDisplayed()
  }

  @Test
  fun viewButtonCallsOnPdfClick() {
    var clickedUrl: String? = null
    var clickedFilename: String? = null

    composeRule.setContent {
      MaterialTheme {
        MoodleFileViewer(
            file = sampleFile,
            onPdfClick = { url, filename ->
              clickedUrl = url
              clickedFilename = filename
            })
      }
    }

    composeRule.onNodeWithTag("moodle_file_view_button").performClick()

    assert(clickedUrl == "https://example.com/file.pdf")
    assert(clickedFilename == "lecture1.pdf")
  }

  @Test
  fun downloadButtonCallsOnDownloadClick() {
    var clickedUrl: String? = null
    var clickedFilename: String? = null

    composeRule.setContent {
      MaterialTheme {
        MoodleFileViewer(
            file = sampleFile,
            onDownloadClick = { url, filename ->
              clickedUrl = url
              clickedFilename = filename
            })
      }
    }

    composeRule.onNodeWithTag("moodle_file_download_button").performClick()

    assert(clickedUrl == "https://example.com/file.pdf")
    assert(clickedFilename == "lecture1.pdf")
  }

  @Test
  fun handlesHomeworkFileType() {
    val homeworkFile =
        sampleFile.copy(fileType = "homework", fileNumber = "2", courseName = "Calculus")

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = homeworkFile) } }

    composeRule.onNodeWithText("Here is the Homework 2 from Calculus").assertIsDisplayed()
    composeRule.onNodeWithText("Homework 2").assertIsDisplayed()
  }

  @Test
  fun handlesHomeworkSolutionFileType() {
    val solutionFile =
        sampleFile.copy(fileType = "homework_solution", fileNumber = "3", courseName = "Physics")

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = solutionFile) } }

    composeRule.onNodeWithText("Here is the Homework solution 3 from Physics").assertIsDisplayed()
    composeRule.onNodeWithText("Homework solution 3").assertIsDisplayed()
  }

  @Test
  fun handlesFileWithoutNumber() {
    val fileWithoutNumber = sampleFile.copy(fileNumber = null)

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = fileWithoutNumber) } }

    composeRule.onNodeWithText("Here is the Lecture from Algebra").assertIsDisplayed()
    composeRule.onNodeWithText("Lecture").assertIsDisplayed()
  }

  @Test
  fun handlesFileWithoutCourseName() {
    val fileWithoutCourse = sampleFile.copy(courseName = null)

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = fileWithoutCourse) } }

    composeRule.onNodeWithText("Here is the Lecture 1 from Moodle").assertIsDisplayed()
  }

  @Test
  fun handlesUnknownFileType() {
    val unknownFile = sampleFile.copy(fileType = "unknown")

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = unknownFile) } }

    composeRule.onNodeWithText("Here is the file 1 from Algebra").assertIsDisplayed()
    composeRule.onNodeWithText("file 1").assertIsDisplayed()
  }

  @Test
  fun handlesLongFilename() {
    val longFilenameFile =
        sampleFile.copy(filename = "very-long-filename-that-should-be-truncated.pdf")

    composeRule.setContent { MaterialTheme { MoodleFileViewer(file = longFilenameFile) } }

    composeRule
        .onNodeWithText("very-long-filename-that-should-be-truncated.pdf", substring = true)
        .assertIsDisplayed()
  }
}
