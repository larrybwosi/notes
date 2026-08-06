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

class NoteViewModel(
    private val repository: NoteRepository,
    private val context: android.content.Context? = null,
) : ViewModel() {
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

    // --- Settings Preferences ---
    private val prefs = context?.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs?.getBoolean("is_dark_mode", false) ?: false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _fontFamilyPreference = MutableStateFlow(prefs?.getString("font_family_pref", "Sans-Serif") ?: "Sans-Serif")
    val fontFamilyPreference: StateFlow<String> = _fontFamilyPreference.asStateFlow()

    private val _accentColorVal = MutableStateFlow(prefs?.getInt("accent_color_val", 0xFF1B63C2.toInt()) ?: 0xFF1B63C2.toInt())
    val accentColorVal: StateFlow<Int> = _accentColorVal.asStateFlow()

    private val _markdownEnabled = MutableStateFlow(prefs?.getBoolean("markdown_enabled", true) ?: true)
    val markdownEnabled: StateFlow<Boolean> = _markdownEnabled.asStateFlow()

    private val _userName = MutableStateFlow(prefs?.getString("user_name", "Abigail") ?: "Abigail")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _dailyReminderEnabled = MutableStateFlow(prefs?.getBoolean("daily_reminder_enabled", false) ?: false)
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled.asStateFlow()

    private val _dailyReminderTime = MutableStateFlow(prefs?.getString("daily_reminder_time", "09:00") ?: "09:00")
    val dailyReminderTime: StateFlow<String> = _dailyReminderTime.asStateFlow()

    // --- Enterprise Journaling Preference Fields ---
    private val _journalIncludeStandup = MutableStateFlow(prefs?.getBoolean("journal_include_standup", true) ?: true)
    val journalIncludeStandup: StateFlow<Boolean> = _journalIncludeStandup.asStateFlow()

    private val _journalIncludeProductivity = MutableStateFlow(prefs?.getBoolean("journal_include_productivity", true) ?: true)
    val journalIncludeProductivity: StateFlow<Boolean> = _journalIncludeProductivity.asStateFlow()

    private val _journalIncludeOkrs = MutableStateFlow(prefs?.getBoolean("journal_include_okrs", true) ?: true)
    val journalIncludeOkrs: StateFlow<Boolean> = _journalIncludeOkrs.asStateFlow()

    private val _journalIncludeHabitTracker = MutableStateFlow(prefs?.getBoolean("journal_include_habit_tracker", true) ?: true)
    val journalIncludeHabitTracker: StateFlow<Boolean> = _journalIncludeHabitTracker.asStateFlow()

    private val _journalIncludeTimeLogs = MutableStateFlow(prefs?.getBoolean("journal_include_time_logs", true) ?: true)
    val journalIncludeTimeLogs: StateFlow<Boolean> = _journalIncludeTimeLogs.asStateFlow()

    private val _journalIncludeTimeTracking = MutableStateFlow(prefs?.getBoolean("journal_include_time_tracking", true) ?: true)
    val journalIncludeTimeTracking: StateFlow<Boolean> = _journalIncludeTimeTracking.asStateFlow()

    private val _journalHabitsList = MutableStateFlow(prefs?.getString("journal_habits_list", "Meditated 🧘, Exercised 🏃, Drank 8 glasses of water 💧, Read a book 📖") ?: "Meditated 🧘, Exercised 🏃, Drank 8 glasses of water 💧, Read a book 📖")
    val journalHabitsList: StateFlow<String> = _journalHabitsList.asStateFlow()

    fun setJournalIncludeStandup(enabled: Boolean) {
        _journalIncludeStandup.value = enabled
        prefs?.edit()?.putBoolean("journal_include_standup", enabled)?.apply()
    }

    fun setJournalIncludeProductivity(enabled: Boolean) {
        _journalIncludeProductivity.value = enabled
        prefs?.edit()?.putBoolean("journal_include_productivity", enabled)?.apply()
    }

    fun setJournalIncludeOkrs(enabled: Boolean) {
        _journalIncludeOkrs.value = enabled
        prefs?.edit()?.putBoolean("journal_include_okrs", enabled)?.apply()
    }

    fun setJournalIncludeHabitTracker(enabled: Boolean) {
        _journalIncludeHabitTracker.value = enabled
        prefs?.edit()?.putBoolean("journal_include_habit_tracker", enabled)?.apply()
    }

    fun setJournalIncludeTimeLogs(enabled: Boolean) {
        _journalIncludeTimeLogs.value = enabled
        prefs?.edit()?.putBoolean("journal_include_time_logs", enabled)?.apply()
    }

    fun setJournalIncludeTimeTracking(enabled: Boolean) {
        _journalIncludeTimeTracking.value = enabled
        prefs?.edit()?.putBoolean("journal_include_time_tracking", enabled)?.apply()
    }

    fun setJournalHabitsList(habits: String) {
        _journalHabitsList.value = habits
        prefs?.edit()?.putString("journal_habits_list", habits)?.apply()
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs?.edit()?.putBoolean("is_dark_mode", enabled)?.apply()
    }

    fun setFontFamilyPreference(font: String) {
        _fontFamilyPreference.value = font
        prefs?.edit()?.putString("font_family_pref", font)?.apply()
    }

    fun setAccentColorVal(colorVal: Int) {
        _accentColorVal.value = colorVal
        prefs?.edit()?.putInt("accent_color_val", colorVal)?.apply()
    }

    fun setMarkdownEnabled(enabled: Boolean) {
        _markdownEnabled.value = enabled
        prefs?.edit()?.putBoolean("markdown_enabled", enabled)?.apply()
    }

    fun setUserName(name: String) {
        _userName.value = name
        prefs?.edit()?.putString("user_name", name)?.apply()
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        prefs?.edit()?.putBoolean("daily_reminder_enabled", enabled)?.apply()
        if (enabled) {
            scheduleDailyReminder()
        } else {
            cancelDailyReminder()
        }
    }

    fun setDailyReminderTime(time: String) {
        _dailyReminderTime.value = time
        prefs?.edit()?.putString("daily_reminder_time", time)?.apply()
        if (_dailyReminderEnabled.value) {
            scheduleDailyReminder()
        }
    }

    private fun scheduleDailyReminder() {
        val context = context ?: return
        com.scryme.notes.receiver.ReminderScheduler.scheduleDailyReminder(context, _dailyReminderTime.value)
    }

    private fun cancelDailyReminder() {
        val context = context ?: return
        com.scryme.notes.receiver.ReminderScheduler.cancelDailyReminder(context)
    }

    fun setNoteReminder(
        noteId: String,
        noteTitle: String,
        timestamp: Long,
    ) {
        val context = context ?: return
        com.scryme.notes.receiver.ReminderScheduler.scheduleNoteReminder(context, noteId, noteTitle, timestamp)
        prefs?.edit()?.apply {
            putLong("reminder_note_$noteId", timestamp)
            putString("reminder_title_note_$noteId", noteTitle)
        }?.apply()
    }

    fun cancelNoteReminder(noteId: String) {
        val context = context ?: return
        com.scryme.notes.receiver.ReminderScheduler.cancelNoteReminder(context, noteId)
        prefs?.edit()?.apply {
            remove("reminder_note_$noteId")
            remove("reminder_title_note_$noteId")
        }?.apply()
    }

    fun getNoteReminder(noteId: String): Long {
        return prefs?.getLong("reminder_note_$noteId", 0L) ?: 0L
    }

    fun getNoteReminders(noteId: String): List<com.scryme.notes.domain.model.NoteReminder> {
        val json = prefs?.getString("note_reminders_list_$noteId", null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.scryme.notes.domain.model.NoteReminder>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addNoteReminder(
        noteId: String,
        noteTitle: String,
        timestamp: Long,
    ) {
        val context = context ?: return
        val currentList = getNoteReminders(noteId).toMutableList()
        val reminderId = java.util.UUID.randomUUID().toString()
        val newReminder =
            com.scryme.notes.domain.model.NoteReminder(
                id = reminderId,
                noteId = noteId,
                noteTitle = noteTitle,
                timestamp = timestamp,
            )
        currentList.add(newReminder)

        // Save back
        val json = com.google.gson.Gson().toJson(currentList)
        prefs?.edit()?.putString("note_reminders_list_$noteId", json)?.apply()

        // Schedule it
        com.scryme.notes.receiver.ReminderScheduler.scheduleNoteReminder(context, noteId, noteTitle, timestamp, reminderId)
    }

    fun removeNoteReminder(
        noteId: String,
        reminderId: String,
    ) {
        val context = context ?: return
        val currentList = getNoteReminders(noteId).filter { it.id != reminderId }

        // Save back
        val json = com.google.gson.Gson().toJson(currentList)
        prefs?.edit()?.putString("note_reminders_list_$noteId", json)?.apply()

        // Cancel it
        com.scryme.notes.receiver.ReminderScheduler.cancelNoteReminder(context, reminderId)
    }

    fun cancelAllNoteReminders(noteId: String) {
        val context = context ?: return
        val currentList = getNoteReminders(noteId)
        for (reminder in currentList) {
            com.scryme.notes.receiver.ReminderScheduler.cancelNoteReminder(context, reminder.id)
        }
        prefs?.edit()?.remove("note_reminders_list_$noteId")?.apply()
    }

    fun createDailyJournalNote() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            val dateStr = dateFormat.format(java.util.Date())
            val title = "Journal - $dateStr"

            val blocks = mutableListOf<Block>()

            // 1. Quote Callout
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.CALLOUT,
                    text = "✨ \"The secret of your future is hidden in your daily routine.\" — Daily Reflection",
                ),
            )

            // 2. Daily Standup (What did I do yesterday, What am I doing today, Blockers)
            if (_journalIncludeStandup.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "📋 Daily Standup",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Yesterday: ",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Today: ",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Blockers: None",
                    ),
                )
            }

            // 3. OKRs / Core Goals
            if (_journalIncludeOkrs.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "🎯 Target Goals & OKRs",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Quarterly Goal: ",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Daily Priority: ",
                    ),
                )
            }

            // 4. Productivity / Focus Rating
            if (_journalIncludeProductivity.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "⚡ Focus & Productivity",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Productivity Level (1-10): ",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Main distraction: ",
                    ),
                )
            }

            // 5. Habits Tracker (Customized)
            if (_journalIncludeHabitTracker.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "✅ Daily Habit Tracker",
                    ),
                )
                val habits =
                    _journalHabitsList.value.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                for (habit in habits) {
                    blocks.add(
                        Block(
                            id = UUID.randomUUID().toString(),
                            type = BlockType.TODO_LIST_ITEM,
                            text = habit,
                            properties = mapOf("checked" to "false"),
                        ),
                    )
                }
            }

            // 6. Time Logs (Option C: Workday start, meetings etc.)
            if (_journalIncludeTimeLogs.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "🕒 Enterprise Time Logs",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Clock In: 09:00 AM",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Clock Out: 05:00 PM",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Key Meetings: ",
                    ),
                )
            }

            // 7. Time Tracking (Option A: Tasks and Duration)
            if (_journalIncludeTimeTracking.value) {
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.HEADER_2,
                        text = "💼 Task Time Tracking",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Task A - [Duration: 2.0 hrs]",
                    ),
                )
                blocks.add(
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.BULLETED_LIST_ITEM,
                        text = "Task B - [Duration: 1.5 hrs]",
                    ),
                )
            }

            // 8. General accomplishments, gratitude, and improvements
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.HEADER_2,
                    text = "🏆 What did I accomplish today?",
                ),
            )
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.BULLETED_LIST_ITEM,
                    text = "",
                ),
            )
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.HEADER_2,
                    text = "🙏 What am I grateful for?",
                ),
            )
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.BULLETED_LIST_ITEM,
                    text = "",
                ),
            )
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.HEADER_2,
                    text = "📈 How can I improve tomorrow?",
                ),
            )
            blocks.add(
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.BULLETED_LIST_ITEM,
                    text = "",
                ),
            )

            val newNote =
                Note(
                    id = newId,
                    title = title,
                    blocks = blocks,
                    parentId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )

            repository.saveNote(newNote)

            // Set the label as Journal
            prefs?.edit()?.putString("label_note_$newId", "Journal")?.apply()

            loadAllNotes()
            selectNote(newId)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            allNotes.value.forEach { note ->
                repository.deleteNote(note.id)
                cancelAllNoteReminders(note.id)
                cancelNoteReminder(note.id)
            }
            _activeNote.value = null
            _breadcrumbs.value = emptyList()
            _subNotes.value = emptyList()
            _focusedBlockId.value = null
            loadAllNotes()
        }
    }

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
                val initializedNote =
                    if (note.blocks.isEmpty()) {
                        val defaultBlock =
                            Block(
                                id = UUID.randomUUID().toString(),
                                type = BlockType.PARAGRAPH,
                                text = "",
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

    fun deselectActiveNote() {
        _activeNote.value = null
        _breadcrumbs.value = emptyList()
        _subNotes.value = emptyList()
        _focusedBlockId.value = null
    }

    private suspend fun loadMetadata(noteId: String) {
        _breadcrumbs.value = repository.getBreadcrumbs(noteId)
        _subNotes.value = repository.getSubNotes(noteId)
    }

    fun createRootNote(title: String = "Untitled Note") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val defaultBlock =
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.PARAGRAPH,
                    text = "",
                )
            val newNote =
                Note(
                    id = newId,
                    title = title,
                    blocks = listOf(defaultBlock),
                    parentId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            repository.saveNote(newNote)
            loadAllNotes()
            selectNote(newId)
        }
    }

    fun createChildNote(
        parentId: String,
        title: String = "Untitled Child Note",
    ) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val defaultBlock =
                Block(
                    id = UUID.randomUUID().toString(),
                    type = BlockType.PARAGRAPH,
                    text = "",
                )
            val parentNote = repository.getNote(parentId)
            val nextOrderIndex =
                if (parentNote != null) {
                    repository.getSubNotes(parentId).size
                } else {
                    0
                }

            val childNote =
                Note(
                    id = newId,
                    title = title,
                    blocks = listOf(defaultBlock),
                    parentId = parentId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    order = nextOrderIndex,
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
            cancelAllNoteReminders(noteId)
            cancelNoteReminder(noteId)
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
        val updated =
            current.copy(
                title = newTitle,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    // --- Block Manipulation ---

    fun setFocusedBlock(blockId: String?) {
        _focusedBlockId.value = blockId
    }

    fun updateBlockText(
        blockId: String,
        newText: String,
    ) {
        val current = _activeNote.value ?: return
        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId) {
                    // Adjust inline styles to fit length if text is shortened
                    val trimmedStyles =
                        block.inlineStyles.mapNotNull { span ->
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
        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun insertBlockAfter(
        currentBlockId: String,
        type: BlockType = BlockType.PARAGRAPH,
        initialText: String = "",
    ) {
        val current = _activeNote.value ?: return
        val index = current.blocks.indexOfFirst { it.id == currentBlockId }
        if (index == -1) return

        val newBlock =
            Block(
                id = UUID.randomUUID().toString(),
                type = type,
                text = initialText,
            )

        val updatedList = current.blocks.toMutableList()
        updatedList.add(index + 1, newBlock)

        val updated =
            current.copy(
                blocks = updatedList,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        _focusedBlockId.value = newBlock.id
        saveNoteDynamically(updated)
    }

    fun changeBlockType(
        blockId: String,
        newType: BlockType,
    ) {
        val current = _activeNote.value ?: return
        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId) {
                    // Preserve content and styles but modify the layout type
                    block.copy(type = newType)
                } else {
                    block
                }
            }
        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun toggleTodoBlockChecked(blockId: String) {
        val current = _activeNote.value ?: return
        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId && block.type == BlockType.TODO_LIST_ITEM) {
                    val isChecked = block.properties["checked"] == "true"
                    val newProps = block.properties.toMutableMap()
                    newProps["checked"] = (!isChecked).toString()
                    block.copy(properties = newProps)
                } else {
                    block
                }
            }
        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun updateCodeBlockLanguage(
        blockId: String,
        language: String,
    ) {
        val current = _activeNote.value ?: return
        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId && block.type == BlockType.CODE_BLOCK) {
                    val newProps = block.properties.toMutableMap()
                    newProps["language"] = language
                    block.copy(properties = newProps)
                } else {
                    block
                }
            }
        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun deleteBlock(blockId: String) {
        val current = _activeNote.value ?: return

        // If it's the last block, don't delete but reset its text to maintain at least one editable input
        if (current.blocks.size <= 1) {
            val resetBlock =
                current.blocks.first().copy(
                    type = BlockType.PARAGRAPH,
                    text = "",
                    inlineStyles = emptyList(),
                    properties = emptyMap(),
                )
            val updated =
                current.copy(
                    blocks = listOf(resetBlock),
                    updatedAt = System.currentTimeMillis(),
                )
            _activeNote.value = updated
            saveNoteDynamically(updated)
            return
        }

        val index = current.blocks.indexOfFirst { it.id == blockId }
        if (index == -1) return

        // Set focus to previous block or next block before removing
        val targetFocusId =
            if (index > 0) {
                current.blocks[index - 1].id
            } else {
                current.blocks[index + 1].id
            }

        val updatedList = current.blocks.toMutableList()
        updatedList.removeAt(index)

        val updated =
            current.copy(
                blocks = updatedList,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        _focusedBlockId.value = targetFocusId
        saveNoteDynamically(updated)
    }

    fun mergeBlockWithPrevious(blockId: String) {
        val current = _activeNote.value ?: return
        val index = current.blocks.indexOfFirst { it.id == blockId }
        if (index <= 0) {
            if (index == 0) {
                val block = current.blocks.firstOrNull()
                if (block != null && block.text.isEmpty()) {
                    deleteBlock(blockId)
                }
            }
            return
        }

        val prevBlock = current.blocks[index - 1]
        val currentBlock = current.blocks[index]

        val mergedText = prevBlock.text + currentBlock.text
        val prevLength = prevBlock.text.length
        val shiftedStyles =
            currentBlock.inlineStyles.map { span ->
                span.copy(
                    start = span.start + prevLength,
                    end = span.end + prevLength,
                )
            }
        val mergedStyles = prevBlock.inlineStyles + shiftedStyles

        val updatedPrevBlock =
            prevBlock.copy(
                text = mergedText,
                inlineStyles = mergedStyles,
            )

        val updatedList = current.blocks.toMutableList()
        updatedList[index - 1] = updatedPrevBlock
        updatedList.removeAt(index)

        val updated =
            current.copy(
                blocks = updatedList,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        _focusedBlockId.value = prevBlock.id
        saveNoteDynamically(updated)
    }

    private val backgroundStyles =
        setOf(
            StyleType.BACKGROUND_COLOR_YELLOW,
            StyleType.BACKGROUND_COLOR_LIGHT_GRAY,
            StyleType.BACKGROUND_COLOR_GREEN,
            StyleType.BACKGROUND_COLOR_BLUE,
            StyleType.BACKGROUND_COLOR_RED,
            StyleType.BACKGROUND_COLOR_PURPLE,
            StyleType.BACKGROUND_COLOR_ORANGE,
        )

    private fun clearStylesFromRange(
        block: Block,
        stylesToClear: Set<StyleType>,
        start: Int,
        end: Int,
    ): List<InlineStyleSpan> {
        var currentStyles = block.inlineStyles
        for (style in stylesToClear) {
            val nonOverlapping = currentStyles.filter { it.styleType != style }
            val overlappingOfThisStyle = currentStyles.filter { it.styleType == style }

            val splitSpans =
                overlappingOfThisStyle.flatMap { span ->
                    if (span.end <= start || span.start >= end) {
                        listOf(span)
                    } else {
                        val split = mutableListOf<InlineStyleSpan>()
                        if (span.start < start) {
                            split.add(InlineStyleSpan(style, span.start, start))
                        }
                        if (span.end > end) {
                            split.add(InlineStyleSpan(style, end, span.end))
                        }
                        split
                    }
                }
            currentStyles = nonOverlapping + splitSpans
        }
        return currentStyles
    }

    fun applyStyleToSelection(
        blockId: String,
        styleType: StyleType,
        start: Int,
        end: Int,
    ) {
        val current = _activeNote.value ?: return
        if (start >= end) return

        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId) {
                    val textLength = block.text.length
                    val sStart = start.coerceIn(0, textLength)
                    val sEnd = end.coerceIn(0, textLength)
                    if (sStart >= sEnd) return@map block

                    // If we are applying a background style, clear other background styles first
                    val blockWithCleanedBackgrounds =
                        if (styleType in backgroundStyles) {
                            val otherBackgrounds = backgroundStyles - styleType
                            val cleanedSpans = clearStylesFromRange(block, otherBackgrounds, sStart, sEnd)
                            block.copy(inlineStyles = cleanedSpans)
                        } else {
                            block
                        }

                    val sameTypeSpans = blockWithCleanedBackgrounds.inlineStyles.filter { it.styleType == styleType }.toMutableList()
                    val otherTypeSpans = blockWithCleanedBackgrounds.inlineStyles.filter { it.styleType != styleType }

                    // Check if [sStart, sEnd) is fully covered by sameTypeSpans
                    val isFullyCovered =
                        (sStart until sEnd).all { idx ->
                            sameTypeSpans.any { span -> idx >= span.start && idx < span.end }
                        }

                    val newSameTypeSpans =
                        if (isFullyCovered) {
                            // Remove styleType from [sStart, sEnd)
                            val result = mutableListOf<InlineStyleSpan>()
                            for (span in sameTypeSpans) {
                                if (span.end <= sStart || span.start >= sEnd) {
                                    // No overlap
                                    result.add(span)
                                } else {
                                    // Overlap
                                    if (span.start < sStart) {
                                        result.add(InlineStyleSpan(styleType, span.start, sStart))
                                    }
                                    if (span.end > sEnd) {
                                        result.add(InlineStyleSpan(styleType, sEnd, span.end))
                                    }
                                }
                            }
                            result
                        } else {
                            // Apply styleType to [sStart, sEnd) by adding and merging
                            val merged = (sameTypeSpans + InlineStyleSpan(styleType, sStart, sEnd)).sortedBy { it.start }
                            val result = mutableListOf<InlineStyleSpan>()
                            if (merged.isNotEmpty()) {
                                var currentSpan = merged[0]
                                for (i in 1 until merged.size) {
                                    val nextSpan = merged[i]
                                    if (currentSpan.end >= nextSpan.start) {
                                        // Overlap or adjacent, merge them
                                        currentSpan = InlineStyleSpan(styleType, currentSpan.start, maxOf(currentSpan.end, nextSpan.end))
                                    } else {
                                        result.add(currentSpan)
                                        currentSpan = nextSpan
                                    }
                                }
                                result.add(currentSpan)
                            }
                            result
                        }

                    blockWithCleanedBackgrounds.copy(inlineStyles = otherTypeSpans + newSameTypeSpans)
                } else {
                    block
                }
            }

        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun clearBackgroundStylesFromSelection(
        blockId: String,
        start: Int,
        end: Int,
    ) {
        val current = _activeNote.value ?: return
        if (start >= end) return

        val updatedBlocks =
            current.blocks.map { block ->
                if (block.id == blockId) {
                    val textLength = block.text.length
                    val sStart = start.coerceIn(0, textLength)
                    val sEnd = end.coerceIn(0, textLength)
                    if (sStart >= sEnd) return@map block

                    val cleanedSpans = clearStylesFromRange(block, backgroundStyles, sStart, sEnd)
                    block.copy(inlineStyles = cleanedSpans)
                } else {
                    block
                }
            }

        val updated =
            current.copy(
                blocks = updatedBlocks,
                updatedAt = System.currentTimeMillis(),
            )
        _activeNote.value = updated
        saveNoteDynamically(updated)
    }

    fun addTagToNote(
        noteId: String,
        tag: String,
    ) {
        viewModelScope.launch {
            val note = repository.getNote(noteId) ?: return@launch
            if (!note.tags.contains(tag)) {
                val updated =
                    note.copy(
                        tags = note.tags + tag,
                        updatedAt = System.currentTimeMillis(),
                    )
                repository.saveNote(updated)
                loadAllNotes()
                // Also update activeNote if currently selected
                if (_activeNote.value?.id == noteId) {
                    _activeNote.value = updated
                }
            }
        }
    }

    fun removeTagFromNote(
        noteId: String,
        tag: String,
    ) {
        viewModelScope.launch {
            val note = repository.getNote(noteId) ?: return@launch
            if (note.tags.contains(tag)) {
                val updated =
                    note.copy(
                        tags = note.tags - tag,
                        updatedAt = System.currentTimeMillis(),
                    )
                repository.saveNote(updated)
                loadAllNotes()
                // Also update activeNote if currently selected
                if (_activeNote.value?.id == noteId) {
                    _activeNote.value = updated
                }
            }
        }
    }

    fun createNoteFromPdf(
        title: String,
        pdfText: String,
    ) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val paragraphs =
                pdfText.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            val blocks =
                if (paragraphs.isEmpty()) {
                    listOf(Block(id = UUID.randomUUID().toString(), type = BlockType.PARAGRAPH, text = ""))
                } else {
                    paragraphs.map { paraText ->
                        Block(
                            id = UUID.randomUUID().toString(),
                            type = BlockType.PARAGRAPH,
                            text = paraText,
                        )
                    }
                }

            val newNote =
                Note(
                    id = newId,
                    title = title,
                    blocks = blocks,
                    parentId = null,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            repository.saveNote(newNote)
            loadAllNotes()
            selectNote(newId)
        }
    }

    fun appendPdfTextToActiveNote(pdfText: String) {
        val current = _activeNote.value ?: return
        viewModelScope.launch {
            val paragraphs =
                pdfText.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (paragraphs.isEmpty()) return@launch

            val newBlocks =
                paragraphs.map { paraText ->
                    Block(
                        id = UUID.randomUUID().toString(),
                        type = BlockType.PARAGRAPH,
                        text = paraText,
                    )
                }

            val updated =
                current.copy(
                    blocks = current.blocks + newBlocks,
                    updatedAt = System.currentTimeMillis(),
                )
            repository.saveNote(updated)
            loadAllNotes()
            _activeNote.value = updated
        }
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

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val context: android.content.Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
