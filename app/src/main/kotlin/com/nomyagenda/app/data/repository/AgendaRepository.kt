package com.nomyagenda.app.data.repository

import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import kotlinx.coroutines.flow.Flow

class AgendaRepository(private val dao: AgendaEntryDao) {

    fun getAll(): Flow<List<AgendaEntry>> = dao.getAll()

    fun search(query: String): Flow<List<AgendaEntry>> = dao.search(query)

    suspend fun getById(id: Int): AgendaEntry? = dao.getById(id)

    suspend fun upsert(entry: AgendaEntry) = dao.upsert(entry)

    suspend fun delete(entry: AgendaEntry) = dao.delete(entry)
}
