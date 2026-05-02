package com.nomyagenda.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.dao.PendingDeleteDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.PendingDelete
import com.nomyagenda.app.data.remote.FirestoreDataSource
import kotlinx.coroutines.flow.Flow

class AgendaRepository(
    private val dao: AgendaEntryDao,
    private val remote: FirestoreDataSource? = null,
    private val pendingDeleteDao: PendingDeleteDao? = null
) {

    fun getAll(): Flow<List<AgendaEntry>> = dao.getAll()

    fun search(query: String): Flow<List<AgendaEntry>> = dao.search(query)

    suspend fun getById(id: Int): AgendaEntry? = dao.getById(id)

    suspend fun upsert(entry: AgendaEntry): Long {
        // Si es edición, preservar los campos que no gestiona el editor (createdAt, firebaseId, category)
        val toSave = if (entry.id != 0) {
            val existing = dao.getById(entry.id)
            if (existing != null) entry.copy(
                createdAt = existing.createdAt,
                firebaseId = existing.firebaseId,
                category = existing.category
            ) else entry
        } else {
            entry
        }

        val rowId = dao.upsert(toSave)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (remote != null && userId != null) {
            try {
                val savedEntry = if (toSave.id == 0) toSave.copy(id = rowId.toInt()) else toSave
                val fbId = remote.upsert(userId, savedEntry)
                if (fbId != savedEntry.firebaseId) {
                    dao.upsert(savedEntry.copy(firebaseId = fbId))
                }
            } catch (_: Exception) { }
        }
        return rowId
    }

    suspend fun getFutureReminders(after: Long): List<AgendaEntry> = dao.getFutureReminders(after)

    suspend fun delete(entry: AgendaEntry) {
        dao.delete(entry)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (remote != null && userId != null && entry.firebaseId.isNotEmpty()) {
            try {
                remote.delete(userId, entry.firebaseId)
            } catch (_: Exception) {
                // Offline — queue the Firestore delete to be retried on next sync
                pendingDeleteDao?.insert(PendingDelete(entry.firebaseId))
            }
        }
    }

    suspend fun deleteAll() = dao.deleteAll()

    /**
     * Fetches all entries from Firestore and upserts them into Room.
     * Existing local entries with the same [AgendaEntry.firebaseId] are updated in place
     * (their local primary key is preserved).
     *
     * Before fetching, any Firestore deletes that failed while offline are retried so that
     * previously-deleted entries are not re-imported.
     */
    suspend fun syncFromFirestore(userId: String) {
        remote ?: return
        try {
            // Flush pending remote deletes first so they are not re-imported below.
            pendingDeleteDao?.getAll()?.forEach { pending ->
                try {
                    remote.delete(userId, pending.firebaseId)
                    pendingDeleteDao?.delete(pending.firebaseId)
                } catch (_: Exception) {
                    // Still offline — leave it in the queue and try again next time
                }
            }

            val remoteEntries = remote.fetchAll(userId)
            for (remoteEntry in remoteEntries) {
                val existing = dao.getByFirebaseId(remoteEntry.firebaseId)
                if (existing != null) {
                    dao.upsert(remoteEntry.copy(id = existing.id, createdAt = existing.createdAt)) // ← añadir createdAt
                } else {
                    dao.upsert(remoteEntry.copy(id = 0))
                }
            }
        } catch (_: Exception) {
            // Network not available — continue with local data
        }
    }
}
