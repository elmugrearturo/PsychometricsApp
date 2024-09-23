package com.arturocuriel.mipersonalidad.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dass_scores")
class DASSScores (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val depression: Int,
    val anxiety: Int,
    val stress: Int,
    val timestamp: Long
)