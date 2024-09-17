package com.arturocuriel.mipersonalidad.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid : String,
    val age : Int,
    val gender : String,
    val nationality : String,
    val timestamp: Long
)