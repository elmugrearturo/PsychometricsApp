package com.example.mipersonalidad.room
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BFIScores::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bfiDao() : BFIDao
}