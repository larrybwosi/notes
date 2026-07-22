package com.scryme.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scryme.notes.domain.model.Block
import com.scryme.notes.domain.model.BlockType
import com.scryme.notes.domain.model.InlineStyleSpan
import com.scryme.notes.domain.model.Note
import com.scryme.notes.domain.model.StyleType
import com.scryme.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())
    val allNotes: StateFlow<List<Note>> = _allNotes.asStateFlow()

    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<Note>>(emptyList())
    val breadcrumbs: StateFlow<List<Note>> = _breadcrumbs.asStateFlow()

    private val _subNotes = MutableStateFlow<List<Note>>(emptyList())
    val subNotes: StateFlow<List<Note>> = _subNotes.asStateFlow()

    // Tracking sidebar collapse/expanded states by Note ID
    private val _expandedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedNoteIds: StateFlow<Set<String>> = _expandedNoteIds.asStateFlow()

    // Track focused block ID for the editor
    private val _focusedBlockId = MutableStateFlow<String?>(null)
    val focusedBlockId: StateFlow<String?> = _focusedBlockId.asStateFlow()

    // Search query for notes list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadAllNotes()
    }

    fun loadAllNotes() {
        viewModelScope.launch {
            val notes = repository.getRootNotes() + repository.getAllNotes()
            // Distinct them to avoid duplicates if repository returns root notes separately
            _allNotes.value = notes.distinctBy { it.id }

            // Reload active note if present
            _activeNote.value?.let { current ->
                val updated = repository.getNote(current.id)
                if (updated != null) {
                    _activeNote.value = updated
                    loadMetadata(updated.id)
                } else {
                    _activeNote.value = null
                    _breadcrumbs.value = emptyList()
                    _subNotes.value = emptyList()
                }
            }
        }
    }

    private suspend fun getAllNotes(): List<Note> {
        // Fallback or helper to query all notes from database safely
        return repository.getRootNotes().flatMap { root ->
            listOf(root) + getSubNotesRecursively(root.id)
        }.distinctBy { it.id }
    }

    private suspend fun getSubNotesRecursively(parentId: String): List<Note> {
        val subs = repository.getSubNotes(parentId)
        return subs + subs.flatMap { getSubNotesRecursively(it.id) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleNoteExpanded(noteId: String) {
        _expandedNoteIds.update { set ->
            if (set.contains(noteId)) set - noteId else set + noteId
        }
    }

    fun selectNote(noteId: String) {
        viewModelScope.launch {
            val note = repository.getNote(noteId)
            if (note != null) {
                // If note has no blocks, create a default paragraph block so user can click to type immediately
                val initializedNote = if (note.blocks.isEmpty()) {
                    val defaultBlock = Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.PARAGRAPH,
                        text = ""
                    )
                    note.copy(blocks = listOf(defaultBlock))
                } else {
                    note
                }

                _activeNote.value = initializedNote
                _focusedBlockId.value = initializedNote.blocks.firstOrNull()?.id
                loadMetadata(noteId)
                repository.saveNote(initializedNote)
            }
        }
    }

    private suspend fun loadMetadata(noteId: String) {
        _breadcrumbs.value = repository.getBreadcrumbs(noteId)
        _subNotes.value = repository.getSubNotes(noteId)
    }

    fun createRootNote(title: String = "Untitled Note") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val defaultBlock = Block(
                id = UUID.randomUUID().toString(),
                type = BlockType.PARAGRAPH,
                text = ""
            )
            val newNote = Note(
                id = newId,
                title = title,
                blocks = listOf(defaultBlock),
                parentId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(newNote)
            loadAllNotes()
            selectNote(newId)
        }
    }

    fun createChildNote(parentId: String, title: String = "Untitled Child Note") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val defaultBlock = Block(
                id = UUID.randomUUID().toString(),
                type = BlockType.PARAGRAPH,
                text = ""
            )
            val parentNote = repository.getNote(parentId)
            val nextOrderIndex = if (parentNote != null) {
                repository.getSubNotes(parentId).size
            } else 0

            val childNote = Note(
                id = newId,
                title = title,
                blocks = listOf(defaultBlock),
                parentId = parentId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                order = nextOrderIndex
            )
            repository.saveNote(childNote)

            // Expand the parent so user sees the newly added child node
            _expandedNoteIds.update { it + parentId }

            loadAllNotes()

            // Select the parent page to keep focus or navigate to the child note directly?
            // Usually, Notion navigates straight to the newly created child note page. Let's do that!
            selectNote(newId)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = null
                _breadcrumbs.value = emptyList()
                _subNotes.value = emptyList()
            }
            loadAllNotes()
        }
    }

    fun updateActiveNoteTitle(newTitle: String) {
        val current = _activeNote.value ?: return
        val updated = current.copy(
            title = newTitle,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    // --- Block Manipulation ---

    fun setFocusedBlock(blockId: String?) {
        _focusedBlockId.value = blockId
    }

    fun updateBlockText(blockId: String, newText: String) {
        val current = _activeNote.value ?: return
        val updatedBlocks = current.blocks.map { block ->
            if (block.id == blockId) {
                // Adjust inline styles to fit length if text is shortened
                val trimmedStyles = block.inlineStyles.mapNotNull { span ->
                    val start = span.start.coerceAtMost(newText.length)
                    val end = span.end.coerceAtMost(newText.length)
                    if (start < end) {
                        span.copy(start = start, end = end)
                    } else {
                        null
                    }
                }
                block.copy(text = newText, inlineStyles = trimmedStyles)
            } else {
                block
            }
        }
        val updated = current.copy(
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun insertBlockAfter(currentBlockId: String, type: BlockType = BlockType.PARAGRAPH) {
        val current = _activeNote.value ?: return
        val index = current.blocks.indexOfFirst { it.id == currentBlockId }
        if (index == -1) return

        val newBlock = Block(
            id = UUID.randomUUID().toString(),
            type = type,
            text = ""
        )

        val updatedList = current.blocks.toMutableList()
        updatedList.add(index + 1, newBlock)

        val updated = current.copy(
            blocks = updatedList,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        _focusedBlockId.value = newBlock.id
        saveNoteDynamically(updated)
    }

    fun changeBlockType(blockId: String, newType: BlockType) {
        val current = _activeNote.value ?: return
        val updatedBlocks = current.blocks.map { block ->
            if (block.id == blockId) {
                // Preserve content and styles but modify the layout type
                block.copy(type = newType)
            } else {
                block
            }
        }
        val updated = current.copy(
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun toggleTodoBlockChecked(blockId: String) {
        val current = _activeNote.value ?: return
        val updatedBlocks = current.blocks.map { block ->
            if (block.id == blockId && block.type == BlockType.TODO_LIST_ITEM) {
                val isChecked = block.properties["checked"] == "true"
                val newProps = block.properties.toMutableMap()
                newProps["checked"] = (!isChecked).toString()
                block.copy(properties = newProps)
            } else {
                block
            }
        }
        val updated = current.copy(
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun updateCodeBlockLanguage(blockId: String, language: String) {
        val current = _activeNote.value ?: return
        val updatedBlocks = current.blocks.map { block ->
            if (block.id == blockId && block.type == BlockType.CODE_BLOCK) {
                val newProps = block.properties.toMutableMap()
                newProps["language"] = language
                block.copy(properties = newProps)
            } else {
                block
            }
        }
        val updated = current.copy(
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun deleteBlock(blockId: String) {
        val current = _activeNote.value ?: return

        // If it's the last block, don't delete but reset its text to maintain at least one editable input
        if (current.blocks.size <= 1) {
            val resetBlock = current.blocks.first().copy(
                type = BlockType.PARAGRAPH,
                text = "",
                inlineStyles = emptyList(),
                properties = emptyMap()
            )
            val updated = current.copy(
                blocks = listOf(resetBlock),
                updatedAt = System.currentTimeMillis()
            )
            _activeNote.value = updated
            saveNoteDynamically(updated)
            return
        }

        val index = current.blocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        // Set focus to previous block or next block before removing
        val targetFocusId = if (index > 0) {
            current.blocks[index - 1].id
        } else {
            current.blocks[index + 1].id
        }

        val updatedList = current.blocks.toMutableList()
        updatedList.removeAt(index)

        val updated = current.copy(
            blocks = updatedList,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        _focusedBlockId.value = targetFocusId
        saveNoteDynamically(updated)
    }

    fun applyStyleToSelection(blockId: String, styleType: StyleType, start: Int, end: Int) {
        val current = _activeNote.value ?: return
        if (start >= end) return

        val updatedBlocks = current.blocks.map { block ->
            if (block.id == blockId) {
                val styles = block.inlineStyles.toMutableList()

                // If exact style exists for this selection range, toggle it off
                val existingIndex = styles.indexOfFirst {
                    it.styleType == styleType && it.start == start && it.end == end
                }
                if (existingIndex != -1) {
                    styles.removeAt(existingIndex)
                } else {
                    // Otherwise apply style. We can clean up overlapping styles for simplicity
                    styles.add(InlineStyleSpan(styleType, start, end))
                }

                block.copy(inlineStyles = styles)
            } else {
                block
            }
        }

        val updated = current.copy(
            blocks = updatedBlocks,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    private fun saveNoteDynamically(note: Note) {
        viewModelScope.launch {
            repository.saveNote(note)
            // Silently sync sidebar note lists without losing selected/expanded focus state
            val all = repository.getRootNotes() + repository.getAllNotes()
            _allNotes.value = all.distinctBy { it.id }
            _breadcrumbs.value = repository.getBreadcrumbs(note.id)
            _subNotes.value = repository.getSubNotes(note.id)
        }
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
