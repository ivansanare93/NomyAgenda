package com.nomyagenda.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.nomyagenda.app.data.local.entity.AgendaEntry
import com.nomyagenda.app.data.local.entity.EntryType
import kotlinx.coroutines.tasks.await

class FirestoreDataSource {

    private val firestore = FirebaseFirestore.getInstance()

    private fun userEntries(userId: String) =
        firestore.collection("users").document(userId).collection("entries")

    /**
     * Writes [entry] to Firestore under the authenticated user's collection.
     * Returns the Firestore document ID (either the existing one or a newly generated one).
     */
    suspend fun upsert(userId: String, entry: AgendaEntry): String {
        val docRef = if (entry.firebaseId.isNotEmpty()) {
            userEntries(userId).document(entry.firebaseId)
        } else {
            userEntries(userId).document()
        }
        docRef.set(entry.toMap()).await()
        return docRef.id
    }

    /**
     * Deletes the document identified by [firebaseId] from the user's Firestore collection.
     */
    suspend fun delete(userId: String, firebaseId: String) {
        userEntries(userId).document(firebaseId).delete().await()
    }

    /**
     * Fetches all entries for [userId] from Firestore and returns them as a list of [AgendaEntry]
     * with `id = 0` (to be assigned by Room on local insert).
     */
    suspend fun fetchAll(userId: String): List<AgendaEntry> {
        val snapshot = userEntries(userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toAgendaEntry(doc.id)
        }
    }

    private fun AgendaEntry.toMap(): Map<String, Any?> = mapOf(
        // Note: `firebaseId` is intentionally excluded — it is the Firestore document ID,
        // not a field stored inside the document.
        "title" to title,
        "type" to type.name,
        "content" to content,
        "checklistJson" to checklistJson,
        "dueAt" to dueAt,
        "advanceNoticeMinutes" to advanceNoticeMinutes,
        "tags" to tags,
        "category" to category,
        "color" to color,
        "createdAt" to createdAt
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toAgendaEntry(docId: String): AgendaEntry? {
        return try {
            AgendaEntry(
                id = 0,
                title = getString("title") ?: return null,
                type = EntryType.valueOf(getString("type") ?: EntryType.NOTE.name),
                content = getString("content") ?: "",
                checklistJson = getString("checklistJson") ?: "[]",
                dueAt = getLong("dueAt"),
                advanceNoticeMinutes = getLong("advanceNoticeMinutes")?.toInt() ?: 0,
                tags = getString("tags") ?: "",
                category = getString("category") ?: "",
                color = getString("color") ?: "",
                createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
                firebaseId = docId
            )
        } catch (e: Exception) {
            null
        }
    }
}
