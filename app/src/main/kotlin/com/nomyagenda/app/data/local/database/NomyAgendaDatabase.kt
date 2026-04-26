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
import com.nomyagenda.app.data.local.dao.PendingDeleteDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.Converters
import com.nomyagenda.app.data.local.entity.DiaryEntry
import com.nomyagenda.app.data.local.entity.PendingDelete

@Database(entities = [AgendaEntry::class, DiaryEntry::class, PendingDelete::class], version = 10, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NomyAgendaDatabase : RoomDatabase() {

    abstract fun agendaEntryDao(): AgendaEntryDao

    abstract fun diaryEntryDao(): DiaryEntryDao

    abstract fun pendingDeleteDao(): PendingDeleteDao

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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE diary_entries ADD COLUMN color TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE diary_entries ADD COLUMN contentColor TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE agenda_entries ADD COLUMN contentColor TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_deletes` (`firebaseId` TEXT NOT NULL, PRIMARY KEY(`firebaseId`))"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE diary_entries ADD COLUMN background TEXT NOT NULL DEFAULT ''"
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
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
