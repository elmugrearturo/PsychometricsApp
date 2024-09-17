package com.arturocuriel.mipersonalidad.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BFIItemsDao {
    @Insert
    suspend fun insertItemResponse(item: BFItems)

    @Query("SELECT * FROM big_five_items ORDER BY itemNumber")
    suspend fun getItemResponses(): List<BFItems>

    @Query("DELETE FROM big_five_items")
    suspend fun emptyItemResponses()

    @Delete
    suspend fun delete(item: BFItems)
}