package com.tsarshield.data.database.converter

import androidx.room.TypeConverter
import com.tsarshield.data.model.LicenseStatus

/**
 * Конвертер для преобразования LicenseStatus <-> String в Room
 */
class LicenseStatusConverter {
    @TypeConverter
    fun fromString(value: String): LicenseStatus {
        return try {
            LicenseStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            LicenseStatus.UNKNOWN
        }
    }
    
    @TypeConverter
    fun toString(status: LicenseStatus): String {
        return status.name
    }
}