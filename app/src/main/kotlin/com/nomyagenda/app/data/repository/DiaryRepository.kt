package com.nomyagenda.app.data.repository

import com.nomyagenda.app.data.local.dao.DiaryEntryDao
import com.nomyagenda.app.data.local.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val dao: DiaryEntryDao) {

    fun getAll(): Flow<List<DiaryEntry>> = dao.getAll()

    fun getByDate(dateKey: String): Flow<List<DiaryEntry>> = dao.getByDate(dateKey)

    suspend fun getById(id: Int): DiaryEntry? = dao.getById(id)

    suspend fun upsert(entry: DiaryEntry): Long = dao.upsert(entry)

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)
}
