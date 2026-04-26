package com.nomyagenda.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks Firestore document IDs that must be deleted from the remote collection the next time
 * the app is online. Entries are queued here when [AgendaRepository.delete] is called while
 * offline (or when the Firestore delete fails for any other transient reason).
 */
@Entity(tableName = "pending_deletes")
data class PendingDelete(
    @PrimaryKey val firebaseId: String
)
