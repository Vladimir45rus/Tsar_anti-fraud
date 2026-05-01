package com.tsar.shield.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tsar.shield.data.database.converter.DateConverter
import com.tsar.shield.data.database.converter.LicenseStatusConverter
import com.tsar.shield.data.database.dao.LicenseDao
import com.tsar.shield.data.database.dao.TriggerDao
import com.tsar.shield.data.database.entity.LicenseEntity
import com.tsar.shield.data.database.entity.TriggerEntity
import android.util.Log

/**
 * Главная база данных приложения Царь-Щит
 * Использует Room Persistence Library для локального хранения данных
 * 
 * Версия 1: начальная версия с таблицей лицензий
 * Версия 2+: будут добавлены таблицы для логов, триггеров и статистики
 */
@Database(
    entities = [LicenseEntity::class, TriggerEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(
    DateConverter::class,
    LicenseStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun licenseDao(): LicenseDao
    
    abstract fun triggerDao(): TriggerDao
    
    companion object {
        private const val TAG = "AppDatabase"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "tsarshield.db"
        
        /**
         * Получить экземпляр базы данных (синглтон)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Creating database instance: $DATABASE_NAME")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Получить экземпляр базы данных в памяти (для тестирования)
         */
        fun getInMemoryInstance(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
    
    /**
     * Callback для инициализации базы данных
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabase", "Database created successfully")
        }
        
        override fun onOpen(db: android.database.sqlite.SQLiteDatabase) {
            super.onOpen(db)
            Log.d("AppDatabase", "Database opened successfully")
            db.execSQL("PRAGMA foreign_keys = ON;")
        }
    }
}