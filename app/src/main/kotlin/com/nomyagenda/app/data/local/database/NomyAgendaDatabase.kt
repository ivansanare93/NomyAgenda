package com.nomyagenda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.Converters

@Database(entities = [AgendaEntry::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NomyAgendaDatabase : RoomDatabase() {

    abstract fun agendaEntryDao(): AgendaEntryDao

    companion object {
        @Volatile
        private var INSTANCE: NomyAgendaDatabase? = null

        fun getDatabase(context: Context): NomyAgendaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NomyAgendaDatabase::class.java,
                    "nomy_agenda_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
