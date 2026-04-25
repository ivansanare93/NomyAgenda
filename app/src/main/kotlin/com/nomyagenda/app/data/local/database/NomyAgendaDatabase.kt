package com.nomyagenda.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.dao.DiaryEntryDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.Converters
import com.nomyagenda.app.data.local.entity.DiaryEntry

@Database(entities = [AgendaEntry::class, DiaryEntry::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NomyAgendaDatabase : RoomDatabase() {

    abstract fun agendaEntryDao(): AgendaEntryDao

    abstract fun diaryEntryDao(): DiaryEntryDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `diary_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateKey` TEXT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL DEFAULT '',
                        `mood` TEXT NOT NULL DEFAULT '',
                        `photoPaths` TEXT NOT NULL DEFAULT '[]',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
