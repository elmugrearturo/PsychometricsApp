package com.arturocuriel.mipersonalidad.room
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bfi_scores")
data class BFIScores(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val extraversion: Int,
    val agreeableness: Int,
    val openness: Int,
    val conscientiousness: Int,
    val neuroticism: Int,
    val timestamp: Long
)
