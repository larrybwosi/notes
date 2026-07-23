package com.scryme.notes.data.repository

import com.scryme.notes.data.local.NoteDao
import com.scryme.notes.data.local.NoteEntity
import com.scryme.notes.domain.model.Note
import com.scryme.notes.domain.repository.NoteRepository

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
) : NoteRepository {
    override suspend fun saveNote(note: Note) {
        noteDao.insertNote(NoteEntity.fromDomain(note))
    }

    override suspend fun getNote(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun deleteNote(id: String) {
        // Room foreign key CASCADE will delete child notes automatically in SQLite,
        // but if we are executing local unit tests without active SQLite cascade enforcement,
        // we can also delete children recursively to ensure reliability in any test environment.
        deleteNoteRecursively(id)
    }

    private suspend fun deleteNoteRecursively(id: String) {
        val children = noteDao.getSubNotesOf(id)
        for (child in children) {
            deleteNoteRecursively(child.id)
        }
        noteDao.deleteNoteById(id)
    }

    override suspend fun getRootNotes(): List<Note> {
        return noteDao.getRootNotes().map { it.toDomain() }
    }

    override suspend fun getSubNotes(parentId: String): List<Note> {
        return noteDao.getSubNotesOf(parentId).map { it.toDomain() }
    }

    override suspend fun getAllNotes(): List<Note> {
        return noteDao.getAllNotes().map { it.toDomain() }
    }

    override suspend fun moveNote(
        noteId: String,
        newParentId: String?,
    ): Boolean {
        // Ensure note exists
        val noteEntity = noteDao.getNoteById(noteId) ?: return false

        // Prevent moving a note to itself
        if (noteId == newParentId) return false

        // Prevent cycles: Check if newParentId is a descendant of noteId
        if (newParentId != null) {
            if (isDescendant(descendantId = newParentId, ancestorId = noteId)) {
                return false
            }
        }

        val updatedEntity =
            noteEntity.copy(
                parentId = newParentId,
                updatedAt = System.currentTimeMillis(),
            )
        noteDao.updateNote(updatedEntity)
        return true
    }

    override suspend fun getBreadcrumbs(noteId: String): List<Note> {
        val path = mutableListOf<Note>()
        var currentId: String? = noteId
        val visited = mutableSetOf<String>() // Safety against potential cycles

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId)
            val note = noteDao.getNoteById(currentId)?.toDomain()
            if (note != null) {
                path.add(0, note) // Insert at the beginning to reverse the order (root first)
                currentId = note.parentId
            } else {
                break
            }
        }
        return path
    }

    /**
     * Helper check to detect cycles: determines if descendantId is a child/descendant of ancestorId.
     */
    private suspend fun isDescendant(
        descendantId: String,
        ancestorId: String,
    ): Boolean {
        var currentId: String? = descendantId
        val visited = mutableSetOf<String>()

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId)
            if (currentId == ancestorId) {
                return true
            }
            val currentNote = noteDao.getNoteById(currentId)
            currentId = currentNote?.parentId
        }
        return false
    }
}
