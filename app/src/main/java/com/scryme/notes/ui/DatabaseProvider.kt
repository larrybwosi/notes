package com.scryme.notes.ui

import android.content.Context
import androidx.room.Room
import com.scryme.notes.data.local.NoteDatabase
import com.scryme.notes.data.repository.NoteRepositoryImpl
import com.scryme.notes.domain.repository.NoteRepository

object DatabaseProvider {
    private var database: NoteDatabase? = null
    private var repository: NoteRepository? = null

    fun getRepository(context: Context): NoteRepository {
        return repository ?: synchronized(this) {
            repository ?: buildRepository(context).also { repository = it }
        }
    }

    private fun buildRepository(context: Context): NoteRepository {
        val db = database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                "notes_database"
            )
            .fallbackToDestructiveMigration()
            .build()
            .also { database = it }
        }
        return NoteRepositoryImpl(db.noteDao)
    }
}
