package com.arturocuriel.mipersonalidad.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsersDao {
    @Insert
    suspend fun insertUser(user: Users)

    @Query("SELECT * FROM users ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastInsertedUser(): Users?

    @Query("DELETE FROM users")
    suspend fun emptyUserData()

    @Delete
    suspend fun delete(user: Users)
}