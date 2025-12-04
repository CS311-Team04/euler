package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.resolveDefaults
import com.halilibo.richtext.ui.string.RichTextStringStyle

/**
 * A composable that renders Markdown text with proper styling for the EULER chat interface.
 *
 * Supports common Markdown syntax:
 * - **Bold** and *italic* text
 * - Headers (##, ###)
 * - Bullet lists (- item)
 * - Numbered lists (1. item)
 * - Horizontal rules (---)
 * - Code blocks (```)
 * - Inline code (`code`)
 * - Links [text](url)
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the root element
 * @param color Text color for the content
 * @param style Base text style to use
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    val richTextStyle = RichTextStyle(
        stringStyle = RichTextStringStyle(
            linkStyle = SpanStyle(
                color = primaryColor
            ),
            codeStyle = SpanStyle(
                color = tertiaryColor,
                background = surfaceVariant
            ),
            boldStyle = SpanStyle(
                fontWeight = FontWeight.Bold
            ),
            italicStyle = SpanStyle(
                fontStyle = FontStyle.Italic
            )
        ),
        paragraphSpacing = 8.sp
    ).resolveDefaults()

    RichText(
        modifier = modifier,
        style = richTextStyle
    ) {
        Markdown(content = markdown)
    }
}

/**
 * Sanitizes text for Markdown rendering by escaping special characters
 * that might interfere with Markdown parsing.
 *
 * Use this for user-generated content that should be displayed literally.
 */
fun escapeMarkdown(text: String): String {
    return text
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("*", "\\*")
        .replace("_", "\\_")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("#", "\\#")
        .replace("+", "\\+")
        .replace("-", "\\-")
        .replace(".", "\\.")
        .replace("!", "\\!")
}
