package com.arturocuriel.mipersonalidad.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "big_five_items")
data class BFItems (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemNumber : Int,
    val response : Int,
    val timestamp: Long
)