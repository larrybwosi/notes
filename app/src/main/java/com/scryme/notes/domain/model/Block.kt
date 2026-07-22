package com.scryme.notes.domain.model

/**
 * Represents the type of a Block in the Notion-like editor.
 */
enum class BlockType {
    PARAGRAPH,
    HEADER_1,
    HEADER_2,
    HEADER_3,
    BULLETED_LIST_ITEM,
    NUMBERED_LIST_ITEM,
    TODO_LIST_ITEM,
    CODE_BLOCK,
    QUOTE,
    CALLOUT,
}

/**
 * Represents inline text styles, such as bold, italic, underline, or color formatting.
 */
enum class StyleType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE,
    TEXT_COLOR_RED,
    TEXT_COLOR_BLUE,
    TEXT_COLOR_GREEN,
    BACKGROUND_COLOR_YELLOW,
    BACKGROUND_COLOR_LIGHT_GRAY,
}

/**
 * Defines a specific span of style applied inline to the block's text content.
 * e.g. applying bold from index 5 to 10.
 */
data class InlineStyleSpan(
    val styleType: StyleType,
    val start: Int,
    val end: Int,
)

/**
 * A block is the primary structural unit of a page/note.
 * Contains text content, styled spans, block metadata (like checked state for todo list items, code language, etc.),
 * and block types.
 */
data class Block(
    val id: String,
    val type: BlockType,
    val text: String,
    val inlineStyles: List<InlineStyleSpan> = emptyList(),
    // For metadata like "checked" for TODO, "language" for code, etc.
    val properties: Map<String, String> = emptyMap(),
)
