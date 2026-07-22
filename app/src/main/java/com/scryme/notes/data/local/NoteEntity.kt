package com.scryme.notes.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.scryme.notes.domain.model.Block
import com.scryme.notes.domain.model.Note

/**
 * NoteEntity is the database representation of a Note.
 * It references its parentId to maintain hierarchical relationships,
 * and contains the block content as serialized rich text.
 * To enable clean cascading deletes when a parent note is deleted,
 * we use self-referential ForeignKey with onDelete = ForeignKey.CASCADE.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parentId")
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val blocks: List<Block>,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val orderIndex: Int
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            blocks = blocks,
            parentId = parentId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            order = orderIndex
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                title = note.title,
                blocks = note.blocks,
                parentId = note.parentId,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                orderIndex = note.order
            )
        }
    }
}
