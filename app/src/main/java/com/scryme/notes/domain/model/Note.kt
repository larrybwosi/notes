package com.scryme.notes.domain.model

/**
 * Represents a rich text note page, containing a sequential list of Blocks
 * and defining parent-child hierarchy similar to Notion.
 */
data class Note(
    val id: String,
    val title: String,
    val blocks: List<Block>,
    // Null if it is a root note, or points to the parent Note ID
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    // For ordering sibling notes
    val order: Int = 0,
)
