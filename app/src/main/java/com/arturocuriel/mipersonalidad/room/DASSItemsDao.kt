package com.arturocuriel.mipersonalidad.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DASSItemsDao {
    @Insert
    suspend fun insertItemResponse(item: DASSItems)

    @Query("SELECT * FROM dass_items ORDER BY itemNumber")
    suspend fun getItemResponses(): List<DASSItems>

    @Query("DELETE FROM dass_items")
    suspend fun emptyItemResponses()

    @Delete
    suspend fun delete(item: DASSItems)
}