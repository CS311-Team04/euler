package com.android.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.EulerRed
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.resolveDefaults

/**
 * Styling configuration for Markdown rendering.
 * Provides consistent styling across the app for different markdown elements.
 */
object MarkdownStyles {
    // Code block styling
    val codeBlockBackground = Color(0xFF1E1E1E)
    val codeBlockBorder = Color(0xFF3C3C3C)
    val codeTextColor = Color(0xFFE6E6E6)
    val inlineCodeBackground = Color(0xFF2D2D2D)
    
    // Link styling
    val linkColor = Color(0xFF6B9FFF)
    
    // List styling
    val bulletColor = EulerRed
    
    // Heading colors
    val headingColor = Color(0xFFFFFFFF)
    
    // Blockquote styling
    val blockquoteBorderColor = EulerRed
    val blockquoteBackground = Color(0xFF1A1A1A)
    
    // Table styling
    val tableBorderColor = Color(0xFF3C3C3C)
    val tableHeaderBackground = Color(0xFF2A2A2A)
}

/**
 * A composable that renders Markdown text with EULER's styling.
 * Uses the compose-richtext library for parsing and rendering markdown.
 *
 * Supports:
 * - **Bold** and *italic* text
 * - `inline code` and code blocks
 * - [Links](url)
 * - Lists (ordered and unordered)
 * - Headers (##, ###, etc.)
 * - Blockquotes
 * - Tables
 * - Horizontal rules
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the component
 * @param textColor The base text color (defaults to theme's onBackground)
 * @param style The text style to use as base
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    // Use default rich text style - the library handles styling internally
    val richTextStyle = remember {
        RichTextStyle().resolveDefaults()
    }
    
    RichText(
        modifier = modifier,
        style = richTextStyle
    ) {
        Markdown(content = markdown)
    }
}

/**
 * Simplified markdown text renderer for basic formatting.
 * Use this for simpler cases where full markdown parsing isn't needed.
 * Handles: bold, italic, inline code, links
 */
@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    lineHeight: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    val annotatedString: AnnotatedString = remember(text, textColor) {
        parseSimpleMarkdown(text, textColor)
    }
    
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(lineHeight = lineHeight, color = textColor),
        onTextLayout = {}
    )
}

/**
 * Parses simple markdown formatting into an AnnotatedString.
 * Handles: **bold**, *italic*, `code`, [links](url)
 */
private fun parseSimpleMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        
        while (i < len) {
            when {
                // Bold: **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Bold: __text__
                text.startsWith("__", i) -> {
                    val end = text.indexOf("__", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: *text* (but not **)
                text.startsWith("*", i) && !text.startsWith("**", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && !text.startsWith("**", end)) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic: _text_ (but not __)
                text.startsWith("_", i) && !text.startsWith("__", i) -> {
                    val end = text.indexOf("_", i + 1)
                    if (end != -1 && !text.startsWith("__", end)) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code: `code`
                text.startsWith("`", i) && !text.startsWith("```", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = MarkdownStyles.inlineCodeBackground,
                            color = MarkdownStyles.codeTextColor
                        )) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Link: [text](url)
                text.startsWith("[", i) -> {
                    val closeText = text.indexOf("]", i)
                    val openUrl = text.indexOf("(", closeText)
                    val closeUrl = text.indexOf(")", openUrl)
                    if (closeText != -1 && openUrl == closeText + 1 && closeUrl != -1) {
                        val linkText = text.substring(i + 1, closeText)
                        val url = text.substring(openUrl + 1, closeUrl)
                        pushStringAnnotation("URL", url)
                        withStyle(SpanStyle(
                            color = MarkdownStyles.linkColor,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append(linkText)
                        }
                        pop()
                        i = closeUrl + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

/**
 * A styled code block component for displaying code snippets.
 * Used for fenced code blocks (```code```)
 */
@Composable
fun CodeBlock(
    code: String,
    language: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MarkdownStyles.codeBlockBackground,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (language != null && language.isNotBlank()) {
                Text(
                    text = language.uppercase(),
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    color = MarkdownStyles.codeTextColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * A styled blockquote component.
 */
@Composable
fun BlockQuote(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MarkdownStyles.blockquoteBorderColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MarkdownStyles.blockquoteBackground)
                .padding(12.dp)
        ) {
            content()
        }
    }
}

/**
 * Styled bullet point for unordered lists.
 */
@Composable
fun BulletPoint(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bulletColor: Color = MarkdownStyles.bulletColor
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(bulletColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * Styled numbered list item.
 */
@Composable
fun NumberedListItem(
    number: Int,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    numberColor: Color = MarkdownStyles.bulletColor
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            color = numberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

/**
 * A horizontal divider styled for markdown rendering.
 */
@Composable
fun MarkdownDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF3C3C3C)
) {
    HorizontalDivider(
        modifier = modifier.padding(vertical = 16.dp),
        thickness = 1.dp,
        color = color
    )
}
