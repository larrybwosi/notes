package com.scryme.notes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.scryme.notes.domain.model.InlineStyleSpan
import com.scryme.notes.domain.model.StyleType

object RichTextTransformer {
    fun toAnnotatedString(
        text: String,
        inlineStyles: List<InlineStyleSpan>,
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)

        for (span in inlineStyles) {
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(0, text.length)
            if (start >= end) continue

            val style = getSpanStyle(span.styleType)
            if (style != null) {
                builder.addStyle(style, start, end)
            }
        }

        return builder.toAnnotatedString()
    }

    private fun getSpanStyle(styleType: StyleType): SpanStyle? {
        return when (styleType) {
            StyleType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            StyleType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            StyleType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            StyleType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            StyleType.CODE ->
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0xFFF0F0F0),
                    color = Color(0xFFE91E63),
                )
            StyleType.TEXT_COLOR_RED -> SpanStyle(color = Color(0xFFD11A2A))
            StyleType.TEXT_COLOR_BLUE -> SpanStyle(color = Color(0xFF1B63C2))
            StyleType.TEXT_COLOR_GREEN -> SpanStyle(color = Color(0xFF2E7D32))
            StyleType.BACKGROUND_COLOR_YELLOW -> SpanStyle(background = Color(0xFFFFF9C4))
            StyleType.BACKGROUND_COLOR_LIGHT_GRAY -> SpanStyle(background = Color(0xFFEEEEEE))
        }
    }
}
