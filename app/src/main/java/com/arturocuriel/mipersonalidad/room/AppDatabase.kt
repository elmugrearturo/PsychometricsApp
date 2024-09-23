package com.arturocuriel.mipersonalidad.room
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [
    BFIScores::class,
    BFItems::class,
    DASSScores::class,
    DASSItems::class,
    Users::class,
    SacksItems::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bfiDao() : BFIDao
    abstract fun bfiItemsDao() : BFIItemsDao
    abstract fun dassDao() : DASSDao
    abstract fun dassItemsDao() : DASSItemsDao
    abstract fun usersDao() : UsersDao
    abstract fun sacksDao() : SacksDao
}