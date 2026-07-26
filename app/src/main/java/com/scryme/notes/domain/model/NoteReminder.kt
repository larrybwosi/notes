package com.scryme.notes.domain.model

data class NoteReminder(
    val id: String,
    val noteId: String,
    val noteTitle: String,
    val timestamp: Long,
)
