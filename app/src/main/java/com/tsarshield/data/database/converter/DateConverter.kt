package com.tsarshield.data.database.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Конвертер для преобразования Date <-> Long (timestamp) в Room
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}