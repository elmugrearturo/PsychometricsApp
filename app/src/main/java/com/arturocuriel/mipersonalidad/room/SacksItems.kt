package com.arturocuriel.mipersonalidad.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sacks_items")
data class SacksItems (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemNumber : Int,
    val response : String,
    val timestamp: Long
)