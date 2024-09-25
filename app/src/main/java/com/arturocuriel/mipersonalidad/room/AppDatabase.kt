package com.arturocuriel.mipersonalidad.room
import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is null, we create a new database instance
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-database"
                ).build()
                INSTANCE = instance
                // Return the newly created instance
                instance
            }
        }
    }
}