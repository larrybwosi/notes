package com.scryme.notes

import com.scryme.notes.data.local.NoteDao
import com.scryme.notes.data.local.NoteEntity
import com.scryme.notes.data.repository.NoteRepositoryImpl
import com.scryme.notes.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * A highly reliable in-memory Mock DAO to run super-fast JVM local unit tests
 * without Android context/instrumentation.
 */
class MockNoteDao : NoteDao {
    private val database = mutableMapOf<String, NoteEntity>()

    override suspend fun insertNote(note: NoteEntity) {
        database[note.id] = note
    }

    override suspend fun updateNote(note: NoteEntity) {
        database[note.id] = note
    }

    override suspend fun deleteNote(note: NoteEntity) {
        database.remove(note.id)
    }

    override suspend fun getNoteById(id: String): NoteEntity? {
        return database[id]
    }

    override suspend fun getRootNotes(): List<NoteEntity> {
        return database.values
            .filter { it.parentId == null }
            .sortedWith(compareBy<NoteEntity> { it.orderIndex }.thenBy { it.createdAt })
    }

    override suspend fun getSubNotesOf(parentId: String): List<NoteEntity> {
        return database.values
            .filter { it.parentId == parentId }
            .sortedWith(compareBy<NoteEntity> { it.orderIndex }.thenBy { it.createdAt })
    }

    override suspend fun getAllNotes(): List<NoteEntity> {
        return database.values.toList()
    }

    override suspend fun deleteNoteById(id: String) {
        database.remove(id)
    }
}

class NoteRepositoryTest {

    private lateinit var mockDao: MockNoteDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        mockDao = MockNoteDao()
        repository = NoteRepositoryImpl(mockDao)
    }

    @Test
    fun testSaveAndGetNoteWithRichTextBlocks() = runBlocking {
        // Arrange
        val blocks = listOf(
            Block(
                id = "b1",
                type = BlockType.HEADER_1,
                text = "Welcome to Notion-like Notes!"
            ),
            Block(
                id = "b2",
                type = BlockType.PARAGRAPH,
                text = "This is a paragraph with bold and italic text.",
                inlineStyles = listOf(
                    InlineStyleSpan(StyleType.BOLD, 22, 26),
                    InlineStyleSpan(StyleType.ITALIC, 31, 37)
                )
            ),
            Block(
                id = "b3",
                type = BlockType.TODO_LIST_ITEM,
                text = "Finish this implementation",
                properties = mapOf("checked" to "true")
            )
        )

        val note = Note(
            id = "note_1",
            title = "My Rich Text Note",
            blocks = blocks,
            parentId = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Act
        repository.saveNote(note)
        val fetchedNote = repository.getNote("note_1")

        // Assert
        assertNotNull(fetchedNote)
        assertEquals("note_1", fetchedNote!!.id)
        assertEquals("My Rich Text Note", fetchedNote.title)
        assertEquals(3, fetchedNote.blocks.size)

        // Header block assertions
        assertEquals(BlockType.HEADER_1, fetchedNote.blocks[0].type)
        assertEquals("Welcome to Notion-like Notes!", fetchedNote.blocks[0].text)

        // Inline formatting assertions
        val secondBlock = fetchedNote.blocks[1]
        assertEquals(BlockType.PARAGRAPH, secondBlock.type)
        assertEquals(2, secondBlock.inlineStyles.size)
        assertEquals(StyleType.BOLD, secondBlock.inlineStyles[0].styleType)
        assertEquals(22, secondBlock.inlineStyles[0].start)
        assertEquals(26, secondBlock.inlineStyles[0].end)

        // Properties (e.g. checked TODO) assertion
        val todoBlock = fetchedNote.blocks[2]
        assertEquals(BlockType.TODO_LIST_ITEM, todoBlock.type)
        assertEquals("true", todoBlock.properties["checked"])
    }

    @Test
    fun testRootAndSubNotesRetrieval() = runBlocking {
        // Arrange: Create hierarchical setup
        val rootNote1 = Note("root_1", "Root 1", emptyList(), null, 1000L, 1000L, order = 1)
        val rootNote2 = Note("root_2", "Root 2", emptyList(), null, 900L, 900L, order = 0) // Should appear first due to orderIndex
        val subNote1 = Note("sub_1", "Sub 1", emptyList(), "root_1", 1000L, 1000L, order = 0)
        val subNote2 = Note("sub_2", "Sub 2", emptyList(), "root_1", 1100L, 1100L, order = 1)

        repository.saveNote(rootNote1)
        repository.saveNote(rootNote2)
        repository.saveNote(subNote1)
        repository.saveNote(subNote2)

        // Act & Assert (Root Notes ordered by orderIndex)
        val roots = repository.getRootNotes()
        assertEquals(2, roots.size)
        assertEquals("root_2", roots[0].id)
        assertEquals("root_1", roots[1].id)

        // Act & Assert (Sub Notes of root_1)
        val subs = repository.getSubNotes("root_1")
        assertEquals(2, subs.size)
        assertEquals("sub_1", subs[0].id)
        assertEquals("sub_2", subs[1].id)
    }

    @Test
    fun testRecursiveDeleteNote() = runBlocking {
        // Arrange hierarchy: root -> sub -> grandchild
        val root = Note("root", "Root", emptyList(), null, 1000, 1000)
        val sub = Note("sub", "Sub", emptyList(), "root", 1000, 1000)
        val grandchild = Note("grandchild", "Grandchild", emptyList(), "sub", 1000, 1000)

        repository.saveNote(root)
        repository.saveNote(sub)
        repository.saveNote(grandchild)

        // Verify elements are there
        assertNotNull(repository.getNote("root"))
        assertNotNull(repository.getNote("sub"))
        assertNotNull(repository.getNote("grandchild"))

        // Act: Delete root
        repository.deleteNote("root")

        // Assert: Root, sub, and grandchild are all deleted
        assertNull(repository.getNote("root"))
        assertNull(repository.getNote("sub"))
        assertNull(repository.getNote("grandchild"))
    }

    @Test
    fun testMoveNoteWithNoCycle() = runBlocking {
        // Arrange
        val root1 = Note("root_1", "Root 1", emptyList(), null, 1000, 1000)
        val root2 = Note("root_2", "Root 2", emptyList(), null, 1000, 1000)
        val sub = Note("sub", "Sub", emptyList(), "root_1", 1000, 1000)

        repository.saveNote(root1)
        repository.saveNote(root2)
        repository.saveNote(sub)

        // Act: Move sub to root2
        val success = repository.moveNote("sub", "root_2")

        // Assert
        assertTrue(success)
        val updatedSub = repository.getNote("sub")
        assertNotNull(updatedSub)
        assertEquals("root_2", updatedSub!!.parentId)
    }

    @Test
    fun testMoveNoteFailsForCycles() = runBlocking {
        // Arrange
        val root = Note("root", "Root", emptyList(), null, 1000, 1000)
        val child = Note("child", "Child", emptyList(), "root", 1000, 1000)
        val grandchild = Note("grandchild", "Grandchild", emptyList(), "child", 1000, 1000)

        repository.saveNote(root)
        repository.saveNote(child)
        repository.saveNote(grandchild)

        // Act & Assert 1: Move root inside grandchild (direct cycle)
        val success1 = repository.moveNote("root", "grandchild")
        assertFalse(success1)
        assertEquals(null, repository.getNote("root")!!.parentId) // Remains root

        // Act & Assert 2: Move root to child
        val success2 = repository.moveNote("root", "child")
        assertFalse(success2)

        // Act & Assert 3: Move root to root itself
        val success3 = repository.moveNote("root", "root")
        assertFalse(success3)
    }

    @Test
    fun testBreadcrumbsGeneration() = runBlocking {
        // Arrange
        val root = Note("root", "Root Note", emptyList(), null, 1000, 1000)
        val child = Note("child", "Child Note", emptyList(), "root", 1000, 1000)
        val grandchild = Note("grandchild", "Grandchild Note", emptyList(), "child", 1000, 1000)

        repository.saveNote(root)
        repository.saveNote(child)
        repository.saveNote(grandchild)

        // Act
        val breadcrumbs = repository.getBreadcrumbs("grandchild")

        // Assert
        assertEquals(3, breadcrumbs.size)
        assertEquals("root", breadcrumbs[0].id)
        assertEquals("Root Note", breadcrumbs[0].title)
        assertEquals("child", breadcrumbs[1].id)
        assertEquals("Child Note", breadcrumbs[1].title)
        assertEquals("grandchild", breadcrumbs[2].id)
        assertEquals("Grandchild Note", breadcrumbs[2].title)
    }
}
