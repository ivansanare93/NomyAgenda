package com.nomyagenda.app.data.local.dao

import androidx.room.*
import com.nomyagenda.app.data.local.entity.AgendaEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaEntryDao {

    @Query("""
    SELECT * FROM agenda_entries
    ORDER BY 
        CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END,
        dueAt DESC,
        createdAt DESC
    """)
        fun getAll(): Flow<List<AgendaEntry>>
    
    @Query("""
    SELECT * FROM agenda_entries
    WHERE title LIKE '%' || :query || '%'
       OR content LIKE '%' || :query || '%'
       OR tags LIKE '%' || :query || '%'
       OR category LIKE '%' || :query || '%'
    ORDER BY 
        CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END,
        dueAt DESC,
        createdAt DESC
    """)
    fun search(query: String): Flow<List<AgendaEntry>>

    @Query("SELECT * FROM agenda_entries WHERE id = :id")
    suspend fun getById(id: Int): AgendaEntry?

    @Query("SELECT * FROM agenda_entries WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getByFirebaseId(firebaseId: String): AgendaEntry?

    @Query("SELECT * FROM agenda_entries WHERE type = 'REMINDER' AND dueAt > :after")
    suspend fun getFutureReminders(after: Long): List<AgendaEntry>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: AgendaEntry): Long

    @Update
    suspend fun update(entry: AgendaEntry)

    // Devuelve el id final de la fila (nuevo o existente)
    suspend fun upsert(entry: AgendaEntry): Long {
        val result = insert(entry)
        return if (result == -1L) {
            update(entry)
            entry.id.toLong()
        } else {
            result
        }
    }

    @Delete
    suspend fun delete(entry: AgendaEntry)

    @Query("DELETE FROM agenda_entries")
    suspend fun deleteAll()
}
