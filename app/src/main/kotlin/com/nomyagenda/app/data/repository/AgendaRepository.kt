package com.nomyagenda.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.nomyagenda.app.data.local.dao.AgendaEntryDao
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.remote.FirestoreDataSource
import kotlinx.coroutines.flow.Flow

class AgendaRepository(
    private val dao: AgendaEntryDao,
    private val remote: FirestoreDataSource? = null
) {

    fun getAll(): Flow<List<AgendaEntry>> = dao.getAll()

    fun search(query: String): Flow<List<AgendaEntry>> = dao.search(query)

    suspend fun getById(id: Int): AgendaEntry? = dao.getById(id)

    suspend fun upsert(entry: AgendaEntry): Long {
        val rowId = dao.upsert(entry)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (remote != null && userId != null) {
            try {
                val savedEntry = if (entry.id == 0) entry.copy(id = rowId.toInt()) else entry
                val fbId = remote.upsert(userId, savedEntry)
                if (fbId != savedEntry.firebaseId) {
                    dao.upsert(savedEntry.copy(firebaseId = fbId))
                }
            } catch (_: Exception) {
                // Offline or Firestore unavailable — Room is the source of truth
            }
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
                // Offline — local delete already done
            }
        }
    }

    suspend fun deleteAll() = dao.deleteAll()

    /**
     * Fetches all entries from Firestore and upserts them into Room.
     * Existing local entries with the same [AgendaEntry.firebaseId] are updated in place
     * (their local primary key is preserved).
     */
    suspend fun syncFromFirestore(userId: String) {
        remote ?: return
        try {
            val remoteEntries = remote.fetchAll(userId)
            for (remoteEntry in remoteEntries) {
                val existing = dao.getByFirebaseId(remoteEntry.firebaseId)
                if (existing != null) {
                    dao.upsert(remoteEntry.copy(id = existing.id))
                } else {
                    // id = 0 tells Room to auto-generate the primary key on insert
                    dao.upsert(remoteEntry.copy(id = 0))
                }
            }
        } catch (_: Exception) {
            // Network not available — continue with local data
        }
    }
}
