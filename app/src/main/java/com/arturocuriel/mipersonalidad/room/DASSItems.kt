package com.arturocuriel.mipersonalidad.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dass_items")
data class DASSItems (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemNumber : Int,
    val response : Int,
    val timestamp: Long
)