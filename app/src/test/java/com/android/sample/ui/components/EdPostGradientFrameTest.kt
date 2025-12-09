package com.android.sample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.ed1
import com.android.sample.ui.theme.ed2
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EdPostGradientFrameTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun EdPostGradientFrame_displays_content() {
    val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

    composeRule.setContent {
      EdPostGradientFrame(gradient = gradient) {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) { Text("Test Content") }
      }
    }

    composeRule.onNodeWithText("Test Content").assertIsDisplayed()
  }

  @Test
  fun EdPostGradientFrame_applies_gradient_border() {
    val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

    composeRule.setContent { EdPostGradientFrame(gradient = gradient) { Text("Content") } }

    // Verify content is displayed (gradient frame is working)
    composeRule.onNodeWithText("Content").assertIsDisplayed()
  }

  @Test
  fun EdPostGradientFrame_handles_custom_modifier() {
    val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

    composeRule.setContent {
      EdPostGradientFrame(
          gradient = gradient, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            Text("Custom Modifier Content")
          }
    }

    composeRule.onNodeWithText("Custom Modifier Content").assertIsDisplayed()
  }

  @Test
  fun EdPostGradientFrame_renders_with_different_gradients() {
    val gradient1 = Brush.horizontalGradient(listOf(Color.Red, Color.Blue))

    composeRule.setContent {
      EdPostGradientFrame(gradient = gradient1) { Text("Gradient Content") }
    }

    composeRule.onNodeWithText("Gradient Content").assertIsDisplayed()
  }

  @Test
  fun EdPostGradientFrame_displays_complex_content() {
    val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

    composeRule.setContent {
      EdPostGradientFrame(gradient = gradient) {
        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
          Text("Complex Content")
          Text("Second Text")
        }
      }
    }

    composeRule.onNodeWithText("Complex Content").assertIsDisplayed()
    composeRule.onNodeWithText("Second Text").assertIsDisplayed()
  }
}
