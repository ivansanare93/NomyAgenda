package com.nomyagenda.app.data.local.dao

import androidx.room.*
import com.nomyagenda.app.data.local.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Query("SELECT * FROM diary_entries ORDER BY dateKey DESC, createdAt DESC")
    fun getAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey ORDER BY createdAt ASC")
    fun getByDate(dateKey: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Int): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntry): Long

    @Delete
    suspend fun delete(entry: DiaryEntry)
}
