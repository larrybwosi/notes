package com.scryme.notes.domain.repository

import com.scryme.notes.domain.model.Note

/**
 * Interface defining all repository operations for managing hierarchical,
 * rich text notes.
 */
interface NoteRepository {

    /**
     * Inserts or updates a note.
     */
    suspend fun saveNote(note: Note)

    /**
     * Retrieves a single note by its ID.
     */
    suspend fun getNote(id: String): Note?

    /**
     * Deletes a note and recursively deletes all its child notes.
     */
    suspend fun deleteNote(id: String)

    /**
     * Retrieves all root-level notes (i.e. those with parentId == null).
     */
    suspend fun getRootNotes(): List<Note>

    /**
     * Retrieves all immediate child notes for a given parent note ID.
     */
    suspend fun getSubNotes(parentId: String): List<Note>

    /**
     * Moves a note to a different parent (or makes it a root note by passing null).
     * This checks for cycles to prevent a note from being moved inside itself or one of its descendants.
     * Returns true if the move was successful, false otherwise.
     */
    suspend fun moveNote(noteId: String, newParentId: String?): Boolean

    /**
     * Computes the breadcrumb trail (list of Notes from root down to this note).
     * Helps with navigation similar to Notion's hierarchy trails.
     */
    suspend fun getBreadcrumbs(noteId: String): List<Note>
}
