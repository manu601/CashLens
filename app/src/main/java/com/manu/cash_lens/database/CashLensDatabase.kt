package com.manu.cash_lens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CashLensDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: CashLensDatabase? = null

        fun getDatabase(context: Context): CashLensDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashLensDatabase::class.java,
                    "cashlens_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}