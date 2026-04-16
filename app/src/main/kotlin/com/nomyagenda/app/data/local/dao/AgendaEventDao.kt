package com.nomyagenda.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.nomyagenda.app.data.local.entity.AgendaEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaEventDao {

    @Query("SELECT * FROM agenda_events ORDER BY dateTimeMillis ASC")
    fun getAll(): Flow<List<AgendaEvent>>

    @Insert
    suspend fun insert(event: AgendaEvent)

    @Delete
    suspend fun delete(event: AgendaEvent)
}
