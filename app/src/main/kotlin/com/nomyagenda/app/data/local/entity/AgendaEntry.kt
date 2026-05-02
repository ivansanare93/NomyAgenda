package com.nomyagenda.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agenda_entries")
data class AgendaEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: EntryType = EntryType.NOTE,
    val content: String = "",
    val checklistJson: String = "[]",
    val dueAt: Long? = null,
    val advanceNoticeMinutes: Int = 0,
    val tags: String = "",
    val category: String = "",
    val color: String = "",
    val contentColor: String = "",
    val fontFamily: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val firebaseId: String = ""
)
