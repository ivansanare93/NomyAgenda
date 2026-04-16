package com.nomyagenda.app.data.repository

import com.nomyagenda.app.data.local.dao.AgendaEventDao
import com.nomyagenda.app.data.local.entity.AgendaEvent
import kotlinx.coroutines.flow.Flow

class AgendaRepository(private val dao: AgendaEventDao) {

    val allEvents: Flow<List<AgendaEvent>> = dao.getAll()

    suspend fun insert(event: AgendaEvent) = dao.insert(event)

    suspend fun delete(event: AgendaEvent) = dao.delete(event)
}
