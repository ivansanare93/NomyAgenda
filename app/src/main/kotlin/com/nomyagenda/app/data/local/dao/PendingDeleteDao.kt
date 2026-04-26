package com.nomyagenda.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomyagenda.app.data.local.entity.PendingDelete

@Dao
interface PendingDeleteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pendingDelete: PendingDelete)

    @Query("SELECT * FROM pending_deletes")
    suspend fun getAll(): List<PendingDelete>

    @Query("DELETE FROM pending_deletes WHERE firebaseId = :firebaseId")
    suspend fun delete(firebaseId: String)
}
