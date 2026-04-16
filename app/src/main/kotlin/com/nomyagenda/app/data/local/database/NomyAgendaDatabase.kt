package com.nomyagenda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nomyagenda.app.data.local.dao.AgendaEventDao
import com.nomyagenda.app.data.local.entity.AgendaEvent

@Database(entities = [AgendaEvent::class], version = 1, exportSchema = false)
abstract class NomyAgendaDatabase : RoomDatabase() {

    abstract fun agendaEventDao(): AgendaEventDao

    companion object {
        @Volatile
        private var INSTANCE: NomyAgendaDatabase? = null

        fun getDatabase(context: Context): NomyAgendaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NomyAgendaDatabase::class.java,
                    "nomy_agenda_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
