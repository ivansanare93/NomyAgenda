package com.nomyagenda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.Converters

@Database(entities = [AgendaEntry::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NomyAgendaDatabase : RoomDatabase() {

    abstract fun agendaEntryDao(): AgendaEntryDao

    companion object {
        @Volatile
        private var INSTANCE: NomyAgendaDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE agenda_entries ADD COLUMN firebaseId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getDatabase(context: Context): NomyAgendaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NomyAgendaDatabase::class.java,
                    "nomy_agenda_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
