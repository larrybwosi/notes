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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockDao = MockNoteDao()
        repository = NoteRepositoryImpl(mockDao)
        viewModel = NoteViewModel(repository)
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
}
