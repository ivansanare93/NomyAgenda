package com.nomyagenda.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateKey: String,             // "YYYY-MM-DD"
    val title: String = "",
    val content: String = "",
    val mood: String = "",           // emoji string or empty
    val photoPaths: String = "[]",   // JSON array of absolute file paths
    val color: String = "",
    val contentColor: String = "",
    val background: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
