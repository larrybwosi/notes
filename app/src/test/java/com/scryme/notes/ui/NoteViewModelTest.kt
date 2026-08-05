package com.scryme.notes.ui

import com.scryme.notes.MockNoteDao
import com.scryme.notes.data.repository.NoteRepositoryImpl
import com.scryme.notes.domain.model.BlockType
import com.scryme.notes.domain.model.StyleType
import com.scryme.notes.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockDao: MockNoteDao
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var viewModel: NoteViewModel

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = MockNoteDao()
        repository = NoteRepositoryImpl(mockDao)
        fakePrefs = FakeSharedPreferences()
        val mockContext =
            object : android.content.ContextWrapper(null) {
                override fun getSharedPreferences(
                    name: String,
                    mode: Int,
                ): android.content.SharedPreferences {
                    return fakePrefs
                }

                override fun getSystemService(name: String): Any? {
                    return null
                }

                override fun getPackageName(): String {
                    return "com.scryme.notes"
                }
            }
        viewModel = NoteViewModel(repository, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCreateRootNote_InitializesWithDefaultParagraph() =
        runTest {
            // Act
            viewModel.createRootNote("My Test Page")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val allNotes = viewModel.allNotes.value
            assertEquals(1, allNotes.size)
            assertEquals("My Test Page", allNotes[0].title)
            assertNull(allNotes[0].parentId)

            // Selected note is set as active with at least one paragraph block
            val active = viewModel.activeNote.value
            assertNotNull(active)
            assertEquals("My Test Page", active!!.title)
            assertEquals(1, active.blocks.size)
            assertEquals(BlockType.PARAGRAPH, active.blocks[0].type)
        }

    @Test
    fun testCreateChildNote_CorrectlySetsParentIdAndExpandsParent() =
        runTest {
            // Arrange: Create a parent page first
            viewModel.createRootNote("Parent")
            testDispatcher.scheduler.advanceUntilIdle()
            val parentId = viewModel.activeNote.value!!.id

            // Act: Create child page
            viewModel.createChildNote(parentId, "Child Page")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val allNotes = viewModel.allNotes.value
            assertEquals(2, allNotes.size)

            val child = allNotes.find { it.title == "Child Page" }
            assertNotNull(child)
            assertEquals(parentId, child!!.parentId)

            // Expanded note IDs must contain the parent
            assertTrue(viewModel.expandedNoteIds.value.contains(parentId))
        }

    @Test
    fun testUpdateActiveNoteTitle_ReflectsInStateAndDao() =
        runTest {
            // Arrange
            viewModel.createRootNote("Initial Title")
            testDispatcher.scheduler.advanceUntilIdle()

            // Act
            viewModel.updateActiveNoteTitle("Updated Title")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            assertEquals("Updated Title", viewModel.activeNote.value?.title)

            val dbNote = repository.getNote(viewModel.activeNote.value!!.id)
            assertNotNull(dbNote)
            assertEquals("Updated Title", dbNote!!.title)
        }

    @Test
    fun testBlockInsertion_AppearsDirectlyAfterActiveBlock() =
        runTest {
            // Arrange
            viewModel.createRootNote("Root")
            testDispatcher.scheduler.advanceUntilIdle()
            val note = viewModel.activeNote.value!!
            val originalBlockId = note.blocks[0].id

            // Act: Insert header block
            viewModel.insertBlockAfter(originalBlockId, BlockType.HEADER_1)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val active = viewModel.activeNote.value
            assertNotNull(active)
            assertEquals(2, active!!.blocks.size)
            assertEquals(originalBlockId, active.blocks[0].id)
            assertEquals(BlockType.HEADER_1, active.blocks[1].type)
            assertEquals(viewModel.focusedBlockId.value, active.blocks[1].id)
        }

    @Test
    fun testBlockInsertion_WithInitialText() =
        runTest {
            // Arrange
            viewModel.createRootNote("Root")
            testDispatcher.scheduler.advanceUntilIdle()
            val note = viewModel.activeNote.value!!
            val originalBlockId = note.blocks[0].id

            // Act: Insert paragraph block with some initial text
            viewModel.insertBlockAfter(originalBlockId, BlockType.PARAGRAPH, "Hello new block")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val active = viewModel.activeNote.value
            assertNotNull(active)
            assertEquals(2, active!!.blocks.size)
            assertEquals("Hello new block", active.blocks[1].text)
            assertEquals(BlockType.PARAGRAPH, active.blocks[1].type)
        }

    @Test
    fun testBlockDeletion_DoesNotRemoveLastBlockButResetsText() =
        runTest {
            // Arrange
            viewModel.createRootNote("Page")
            testDispatcher.scheduler.advanceUntilIdle()
            val note = viewModel.activeNote.value!!
            val blockId = note.blocks[0].id
            viewModel.updateBlockText(blockId, "Some text")
            testDispatcher.scheduler.advanceUntilIdle()

            // Act: Delete the only block
            viewModel.deleteBlock(blockId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: Size is still 1, but text is empty
            val active = viewModel.activeNote.value
            assertNotNull(active)
            assertEquals(1, active!!.blocks.size)
            assertEquals("", active.blocks[0].text)
        }

    @Test
    fun testApplyStyleToSelection_CreatesInlineSpanStyle() =
        runTest {
            // Arrange
            viewModel.createRootNote("Page")
            testDispatcher.scheduler.advanceUntilIdle()
            val note = viewModel.activeNote.value!!
            val blockId = note.blocks[0].id
            viewModel.updateBlockText(blockId, "Notion-like inline styles")
            testDispatcher.scheduler.advanceUntilIdle()

            // Act: Apply Bold to word "inline" (indices 12 to 18)
            viewModel.applyStyleToSelection(blockId, StyleType.BOLD, 12, 18)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val block = viewModel.activeNote.value!!.blocks[0]
            assertEquals(1, block.inlineStyles.size)
            assertEquals(StyleType.BOLD, block.inlineStyles[0].styleType)
            assertEquals(12, block.inlineStyles[0].start)
            assertEquals(18, block.inlineStyles[0].end)
        }

    @Test
    fun testSetUserName_CorrectlyUpdatesState() =
        runTest {
            // Act
            viewModel.setUserName("Harrison")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            assertEquals("Harrison", viewModel.userName.value)
        }

    @Test
    fun testSetDailyReminder_CorrectlyUpdatesState() =
        runTest {
            // Act
            viewModel.setDailyReminderEnabled(true)
            viewModel.setDailyReminderTime("08:30")
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            assertTrue(viewModel.dailyReminderEnabled.value)
            assertEquals("08:30", viewModel.dailyReminderTime.value)
        }

    @Test
    fun testCreateDailyJournalNote_InitializesWithJournalLayoutAndLabel() =
        runTest {
            // Act: Toggle some preferences
            viewModel.setJournalIncludeStandup(true)
            viewModel.setJournalIncludeOkrs(true)
            viewModel.setJournalIncludeProductivity(true)
            viewModel.setJournalIncludeTimeLogs(true)
            viewModel.setJournalIncludeTimeTracking(true)
            viewModel.setJournalIncludeHabitTracker(true)
            viewModel.setJournalHabitsList("Drink water 💧, Eat healthy 🥗")

            viewModel.createDailyJournalNote()
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val active = viewModel.activeNote.value
            assertNotNull(active)
            assertTrue(active!!.title.startsWith("Journal - "))

            // Check that custom habits are populated correctly
            val customHabits = active.blocks.filter { it.type == BlockType.TODO_LIST_ITEM }
            assertEquals(2, customHabits.size)
            assertEquals("Drink water 💧", customHabits[0].text)
            assertEquals("Eat healthy 🥗", customHabits[1].text)

            // Check standup, okr, productivity, timelogs, timetracking headers exist
            val headers = active.blocks.filter { it.type == BlockType.HEADER_2 }.map { it.text }
            assertTrue(headers.contains("📋 Daily Standup"))
            assertTrue(headers.contains("🎯 Target Goals & OKRs"))
            assertTrue(headers.contains("⚡ Focus & Productivity"))
            assertTrue(headers.contains("🕒 Enterprise Time Logs"))
            assertTrue(headers.contains("💼 Task Time Tracking"))

            // Verify label saved in prefs
            val savedLabel = fakePrefs.getString("label_note_${active.id}", null)
            assertEquals("Journal", savedLabel)
        }

    @Test
    fun testCreateDailyJournalNote_DynamicFilterSections() =
        runTest {
            // Act: Disable most sections
            viewModel.setJournalIncludeStandup(false)
            viewModel.setJournalIncludeOkrs(false)
            viewModel.setJournalIncludeProductivity(false)
            viewModel.setJournalIncludeTimeLogs(false)
            viewModel.setJournalIncludeTimeTracking(false)
            viewModel.setJournalIncludeHabitTracker(false)

            viewModel.createDailyJournalNote()
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val active = viewModel.activeNote.value
            assertNotNull(active)

            val headers = active!!.blocks.filter { it.type == BlockType.HEADER_2 }.map { it.text }
            assertTrue(!headers.contains("📋 Daily Standup"))
            assertTrue(!headers.contains("🎯 Target Goals & OKRs"))
            assertTrue(!headers.contains("⚡ Focus & Productivity"))
            assertTrue(!headers.contains("🕒 Enterprise Time Logs"))
            assertTrue(!headers.contains("💼 Task Time Tracking"))
            assertTrue(!headers.contains("✅ Daily Habit Tracker"))
        }

    @Test
    fun testMultipleReminders_CanAddGetAndRemoveReminders() =
        runTest {
            val noteId = "test_note_id"
            val noteTitle = "Test Reminder Note"
            val timestamp1 = System.currentTimeMillis() + 10_000L
            val timestamp2 = System.currentTimeMillis() + 60_000L

            // Act: Add two distinct reminders
            viewModel.addNoteReminder(noteId, noteTitle, timestamp1)
            viewModel.addNoteReminder(noteId, noteTitle, timestamp2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert both exist
            val reminders = viewModel.getNoteReminders(noteId)
            assertEquals(2, reminders.size)
            assertEquals(noteId, reminders[0].noteId)
            assertEquals(noteTitle, reminders[0].noteTitle)
            assertEquals(timestamp1, reminders[0].timestamp)
            assertEquals(timestamp2, reminders[1].timestamp)

            // Act: Remove the first one
            val firstId = reminders[0].id
            viewModel.removeNoteReminder(noteId, firstId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert only the second one remains
            val remainingReminders = viewModel.getNoteReminders(noteId)
            assertEquals(1, remainingReminders.size)
            assertEquals(timestamp2, remainingReminders[0].timestamp)
        }

    @Test
    fun testNoteDeletion_ClearsAllAssociatedReminders() =
        runTest {
            // Arrange
            viewModel.createRootNote("Page with Reminders")
            testDispatcher.scheduler.advanceUntilIdle()
            val noteId = viewModel.activeNote.value!!.id

            viewModel.addNoteReminder(noteId, "Page with Reminders", System.currentTimeMillis() + 10_000L)
            viewModel.addNoteReminder(noteId, "Page with Reminders", System.currentTimeMillis() + 20_000L)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(2, viewModel.getNoteReminders(noteId).size)

            // Act: Delete Note
            viewModel.deleteNote(noteId)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert: Reminders list should be empty
            assertEquals(0, viewModel.getNoteReminders(noteId).size)
        }
}

class FakeSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map

    override fun getString(
        key: String,
        defValue: String?,
    ): String? =
        (map[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(
        key: String,
        defValues: Set<String>?,
    ): Set<String>? =
        (map[key] as? Set<String>) ?: defValues

    override fun getInt(
        key: String,
        defValue: Int,
    ): Int =
        (map[key] as? Int) ?: defValue

    override fun getLong(
        key: String,
        defValue: Long,
    ): Long =
        (map[key] as? Long) ?: defValue

    override fun getFloat(
        key: String,
        defValue: Float,
    ): Float =
        (map[key] as? Float) ?: defValue

    override fun getBoolean(
        key: String,
        defValue: Boolean,
    ): Boolean =
        (map[key] as? Boolean) ?: defValue

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun edit(): android.content.SharedPreferences.Editor = FakeEditor(map)

    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val map: MutableMap<String, Any?>) : android.content.SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()

        override fun putString(
            key: String,
            value: String?,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = value
            removals.remove(key)
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>?,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = values
            removals.remove(key)
            return this
        }

        override fun putInt(
            key: String,
            value: Int,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = value
            removals.remove(key)
            return this
        }

        override fun putLong(
            key: String,
            value: Long,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = value
            removals.remove(key)
            return this
        }

        override fun putFloat(
            key: String,
            value: Float,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = value
            removals.remove(key)
            return this
        }

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): android.content.SharedPreferences.Editor {
            tempMap[key] = value
            removals.remove(key)
            return this
        }

        override fun remove(key: String): android.content.SharedPreferences.Editor {
            removals.add(key)
            tempMap.remove(key)
            return this
        }

        override fun clear(): android.content.SharedPreferences.Editor {
            tempMap.clear()
            removals.addAll(map.keys)
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            for (key in removals) {
                map.remove(key)
            }
            map.putAll(tempMap)
        }
    }
}
