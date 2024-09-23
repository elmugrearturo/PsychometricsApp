package com.arturocuriel.mipersonalidad.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DASSDao {
    @Insert
    suspend fun insertScore(score: DASSScores)

    @Query("SELECT * FROM dass_scores ORDER BY timestamp DESC")
    suspend fun getAllScores(): List<DASSScores>

    @Query("SELECT * FROM dass_scores ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastInsertedScore(): DASSScores?

    @Query("SELECT COUNT(*) FROM dass_scores")
    suspend fun getRowCount(): Int

    @Delete
    suspend fun delete(score: DASSScores)
}