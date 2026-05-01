package com.tsarshield.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tsarshield.data.database.converter.DateConverter
import com.tsarshield.data.database.converter.LicenseStatusConverter
import com.tsarshield.data.model.LicenseStatus
import java.util.Date

/**
 * Сущность лицензии для хранения в локальной базе данных Room
 * Хранит информацию о лицензии пользователя, включая статус, сроки действия и метаданные
 */
@Entity(tableName = "licenses")
@TypeConverters(DateConverter::class, LicenseStatusConverter::class)
data class LicenseEntity(
    @PrimaryKey
    val licenseId: String,
    
    val deviceHash: String,
    val phoneHash: String,
    
    val status: LicenseStatus,
    val planType: String,
    
    val activatedAt: Date,
    val expiresAt: Date?,
    val lastSync: Date,
    
    val transferCount: Int = 0,
    val emergencyTransferCount: Int = 0,
    
    val gracePeriodEnd: Date?,
    val isSuspicious: Boolean = false,
    
    val jwtToken: String?,
    val tokenExpiry: Date?,
    
    val metadata: String? // JSON с дополнительными данными
) {
    /**
     * Проверяет, действительна ли лицензия в данный момент
     * Учитывает срок действия, grace period и статус
     */
    fun isValid(): Boolean {
        val now = Date()
        
        // Проверка статуса
        if (status != LicenseStatus.PAID && status != LicenseStatus.TRIAL) {
            return false
        }
        
        // Проверка срока действия
        expiresAt?.let {
            if (now.after(it)) {
                // Проверка grace period
                gracePeriodEnd?.let { graceEnd ->
                    if (now.before(graceEnd)) {
                        return true
                    }
                }
                return false
            }
        }
        
        return true
    }
    
    /**
     * Проверяет, находится ли лицензия в grace period
     */
    fun isInGracePeriod(): Boolean {
        val now = Date()
        return gracePeriodEnd?.let { now.before(it) } ?: false
    }
    
    /**
     * Проверяет, требуется ли синхронизация с сервером
     * @param maxAgeHours максимальный возраст данных в часах
     */
    fun needsSync(maxAgeHours: Int = 24): Boolean {
        val now = Date()
        val maxAgeMs = maxAgeHours * 60 * 60 * 1000L
        return now.time - lastSync.time > maxAgeMs
    }
}