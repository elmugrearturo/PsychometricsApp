package com.arturocuriel.mipersonalidad.room
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    BFIScores::class,
    BFItems::class,
    Users::class,
    SacksItems::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bfiDao() : BFIDao
    abstract fun bfiItemsDao() : BFIItemsDao
    abstract fun usersDao() : UsersDao
    abstract fun sacksDao() : SacksDao
}