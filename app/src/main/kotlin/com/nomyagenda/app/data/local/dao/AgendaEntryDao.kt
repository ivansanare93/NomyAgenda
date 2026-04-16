package com.nomyagenda.app.data.local.dao

import androidx.room.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaEntryDao {

    @Query("SELECT * FROM agenda_entries ORDER BY COALESCE(dueAt, createdAt) ASC")
    fun getAll(): Flow<List<AgendaEntry>>

    @Query("""
        SELECT * FROM agenda_entries
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY COALESCE(dueAt, createdAt) ASC
    """)
    fun search(query: String): Flow<List<AgendaEntry>>

    @Query("SELECT * FROM agenda_entries WHERE id = :id")
    suspend fun getById(id: Int): AgendaEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: AgendaEntry)

    @Delete
    suspend fun delete(entry: AgendaEntry)
}
