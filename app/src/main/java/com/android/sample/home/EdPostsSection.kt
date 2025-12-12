package com.android.sample.home

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Converts HTML/XML content to plain text by:
 * - Extracting text from paragraph tags
 * - Removing all HTML/XML tags
 * - Converting paragraph breaks to newlines
 * - Cleaning up extra whitespace
 */
private fun htmlToPlainText(html: String): String {
  if (html.isBlank()) return ""

  var text = html
  // Replace paragraph tags with newlines
  text = text.replace(Regex("<paragraph>", RegexOption.IGNORE_CASE), "")
  text = text.replace(Regex("</paragraph>", RegexOption.IGNORE_CASE), "\n")

  // Replace other common block elements with newlines
  text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
  text = text.replace(Regex("<div>", RegexOption.IGNORE_CASE), "")
  text = text.replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
  text = text.replace(Regex("<p>", RegexOption.IGNORE_CASE), "")
  text = text.replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")

  // Remove all remaining HTML/XML tags
  text = text.replace(Regex("<[^>]+>"), "")

  // Decode common HTML entities
  text = text.replace("&amp;", "&")
  text = text.replace("&lt;", "<")
  text = text.replace("&gt;", ">")
  text = text.replace("&quot;", "\"")
  text = text.replace("&apos;", "'")
  text = text.replace("&nbsp;", " ")

  // Clean up whitespace: multiple newlines -> single newline, trim each line
  text = text.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n")

  // Remove excessive newlines (more than 2 consecutive)
  text = text.replace(Regex("\n{3,}"), "\n\n")

  return text.trim()
}

@Composable
fun EdPostsSection(
    state: EdPostsUiState,
    modifier: Modifier = Modifier,
    onOpenPost: (String) -> Unit = {},
    onRetry: (EdIntentFilters) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
  if (state.stage == EdPostsStage.IDLE) return

  val colors = MaterialTheme.colorScheme
  val panelBg = Color(0xFF050509)
  val panelBorder = Color(0xFF8B5CFF)

  val configuration = LocalConfiguration.current
  val maxScrollableHeight = (configuration.screenHeightDp * 0.40f).dp // ⬅️ hauteur max

  Card(
      modifier = modifier.fillMaxWidth().wrapContentHeight(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = panelBg),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, panelBorder.copy(alpha = 0.95f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          EdHeader(state)

          Spacer(Modifier.height(8.dp))

          FiltersRow(state = state)

          Spacer(Modifier.height(12.dp))

          val scrollState = rememberScrollState()

          // Limit scrollable content height here
          Column(
              modifier =
                  Modifier.fillMaxWidth()
                      .heightIn(max = maxScrollableHeight) // 👈 important
                      .verticalScroll(scrollState),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.stage) {
                  EdPostsStage.LOADING -> LoadingBlock()
                  EdPostsStage.EMPTY -> EmptyBlock()
                  EdPostsStage.ERROR -> ErrorBlock(state, onRetry)
                  EdPostsStage.SUCCESS -> PostsBlock(state.posts, onOpenPost)
                  EdPostsStage.IDLE -> Unit
                }

                if (state.stage == EdPostsStage.SUCCESS) {
                  Spacer(Modifier.height(4.dp))
                  Text(
                      text =
                          "Found ${state.posts.size} result${if (state.posts.size == 1) "" else "s"}",
                      style = MaterialTheme.typography.labelSmall,
                      color = colors.onSurfaceVariant)
                }
              }
        }
      }
}

@Composable
private fun EdHeader(state: EdPostsUiState) {
  val colors = MaterialTheme.colorScheme
  val courseLabel = state.filters.course ?: "ED"

  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                  text = "ED Discussion Posts",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.SemiBold)
              Icon(
                  imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                  contentDescription = null,
                  tint = colors.onSurfaceVariant,
                  modifier = Modifier.size(16.dp))
            }

        // pill course chip type "COM-301"
        Box(
            modifier =
                Modifier.background(color = Color(0xFF8B5CFF), shape = RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)) {
              Text(
                  text = courseLabel,
                  color = Color.White,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold)
            }
      }
}

@Composable
private fun FiltersRow(state: EdPostsUiState) {
  val courseLabel = state.filters.course
  if (courseLabel != null) {
    Box(
        modifier =
            Modifier.background(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)) {
          Text(
              text = "Course: $courseLabel",
              color = Color.White,
              style = MaterialTheme.typography.labelSmall)
        }
  }
}

/** Friendly loading indicator shown while searching ED Discussion posts. */
@Composable
private fun LoadingBlock() {
  val edPurple = Color(0xFF8B5CFF)

  val transition = rememberInfiniteTransition(label = "edLoading")
  val alpha by
      transition.animateFloat(
          initialValue = 0.4f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "edAlpha")

  Column(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Animated purple dot
        Box(
            modifier =
                Modifier.size(12.dp)
                    .background(color = edPurple.copy(alpha = alpha), shape = RoundedCornerShape(6.dp)))

        Text(
            text = "Searching ED Discussion…",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = alpha),
            fontWeight = FontWeight.Medium)
      }
}

@Composable
private fun EmptyBlock() {
  Column(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp))
        Text(
            text = "No posts found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
}

@Composable
private fun ErrorBlock(state: EdPostsUiState, onRetry: (EdIntentFilters) -> Unit) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(
                  imageVector = Icons.Filled.Error,
                  contentDescription = null,
                  tint = Color(0xFFFF4444),
                  modifier = Modifier.size(24.dp))
              Text(
                  text = state.errorMessage ?: "An error occurred",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFFF4444))
            }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(onClick = { onRetry(state.filters) }) {
            Text("Retry", color = Color(0xFF8B5CFF))
          }
        }
      }
}

@Composable
private fun PostsBlock(posts: List<EdPost>, onOpenPost: (String) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    posts.forEach { post -> EdCard(post = post, onOpenPost = onOpenPost) }
  }
}

@Composable
private fun EdCard(post: EdPost, onOpenPost: (String) -> Unit) {

  val postBg = Color(0xFF101014)

  Card(
      modifier =
          Modifier.fillMaxWidth().wrapContentHeight().clickable( // Clickable on entire card
              interactionSource = remember { MutableInteractionSource() },
              indication = null // 👈 pas de ripple / petit rond
              ) {
                onOpenPost(post.url)
              },
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = postBg),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
              // Title + date (same as marquee)
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier =
                            Modifier.weight(1f)
                                .padding(end = 8.dp)
                                .basicMarquee(iterations = Int.MAX_VALUE))

                    Text(
                        text = formatDate(post.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }

              // Content preview (slightly shorter)
              Text(
                  text = htmlToPlainText(post.content),
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis)

              // Meta footer like the mock (author on the left)
              Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    // “Chip” auteur
                    Box(
                        modifier =
                            Modifier.background(
                                    color = Color(0xFF181818), shape = RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)) {
                          Text(
                              text = post.author, // tu peux mettre "Student" / "TA" ici
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                    // You can put other info here (reply count, likes…)
                    // for now we leave it empty or add later
                  }
            }
      }
}

// Cached date formatters to avoid allocations on every call
private val dayFormat = java.text.SimpleDateFormat("d", java.util.Locale.ENGLISH)
private val yearFormat = java.text.SimpleDateFormat("yyyy", java.util.Locale.ENGLISH)
private val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH)

private fun formatDate(timestamp: Long): String {
  val date = java.util.Date(timestamp)
  val day = dayFormat.format(date)
  val month = monthFormat.format(date).lowercase()
  val year = yearFormat.format(date)
  // Format: "24 oct. 2025" (lowercase month with dot)
  return "$day $month. $year"
}
