package com.arturocuriel.mipersonalidad.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SacksDao {
    @Insert
    suspend fun insertItemResponse(item: SacksItems)

    @Query("SELECT * FROM sacks_items ORDER BY itemNumber")
    suspend fun getItemResponses(): List<SacksItems>

    @Query("DELETE FROM sacks_items")
    suspend fun emptyItemResponses()

    @Delete
    suspend fun delete(item: SacksItems)
}