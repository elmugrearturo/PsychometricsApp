package com.example.mipersonalidad.room
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BFIDao {
    @Insert
    suspend fun insertScore(score: BFIScores)

    @Query("SELECT * FROM bfi_scores ORDER BY timestamp DESC")
    suspend fun getAllScores(): List<BFIScores>

    @Delete
    fun delete(score: BFIScores)
}