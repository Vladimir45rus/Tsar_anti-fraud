package com.tsar.shield.data.repository

import android.content.Context
import android.util.Log
import com.tsar.shield.data.model.LicenseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "LicenseRepository"
        private const val PREFS_NAME = "license_prefs"
        private const val KEY_LICENSE_STATUS = "license_status"
        private const val KEY_DEVICE_HASH = "device_hash"
        private const val KEY_LAST_SYNC = "last_sync"
    }
    
    /**
     * Получает текущий статус лицензии
     */
    suspend fun getLicenseStatus(): LicenseStatus {
        return withContext(Dispatchers.IO) {
            try {
                // В реальной реализации здесь будет проверка локального кэша и сервера
                // Пока возвращаем демо-статус
                getCachedLicenseStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get license status", e)
                LicenseStatus.UNKNOWN
            }
        }
    }
    
    /**
     * Получает статус лицензии из кэша
     */
    private fun getCachedLicenseStatus(): LicenseStatus {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val statusName = prefs.getString(KEY_LICENSE_STATUS, LicenseStatus.FREE.name)
        
        return try {
            LicenseStatus.valueOf(statusName ?: LicenseStatus.FREE.name)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid license status in cache: $statusName")
            LicenseStatus.FREE
        }
    }
    
    /**
     * Сохраняет статус лицензии в кэш
     */
    suspend fun cacheLicenseStatus(status: LicenseStatus) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_LICENSE_STATUS, status.name)
                .apply()
            
            Log.d(TAG, "License status cached: $status")
        }
    }
    
    /**
     * Генерирует хэш устройства
     */
    suspend fun generateDeviceHash(): String {
        return withContext(Dispatchers.IO) {
            // В реальной реализации здесь будет сложная логика генерации хэша
            // Пока возвращаем простой демо-хэш
            "demo_device_hash_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Получает сохраненный хэш устройства
     */
    suspend fun getDeviceHash(): String {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var deviceHash = prefs.getString(KEY_DEVICE_HASH, null)
            
            if (deviceHash == null) {
                deviceHash = generateDeviceHash()
                prefs.edit()
                    .putString(KEY_DEVICE_HASH, deviceHash)
                    .apply()
                
                Log.d(TAG, "New device hash generated: $deviceHash")
            }
            
            deviceHash
        }
    }
    
    /**
     * Проверяет, активна ли лицензия
     */
    suspend fun isLicenseActive(): Boolean {
        return withContext(Dispatchers.IO) {
            val status = getLicenseStatus()
            status == LicenseStatus.PAID || status == LicenseStatus.TRIAL
        }
    }
    
    /**
     * Проверяет, истекла ли лицензия
     */
    suspend fun isLicenseExpired(): Boolean {
        return withContext(Dispatchers.IO) {
            val status = getLicenseStatus()
            status == LicenseStatus.EXPIRED
        }
    }
    
    /**
     * Проверяет, доступен ли пробный период
     */
    suspend fun isTrialAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            // В реальной реализации здесь будет проверка даты активации
            // Пока возвращаем true для демо
            true
        }
    }
    
    /**
     * Активирует пробный период
     */
    suspend fun activateTrial(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isTrialAvailable()) {
                    cacheLicenseStatus(LicenseStatus.TRIAL)
                    Log.d(TAG, "Trial activated")
                    true
                } else {
                    Log.w(TAG, "Trial not available")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate trial", e)
                false
            }
        }
    }
    
    /**
     * Активирует платную лицензию
     */
    suspend fun activatePaidLicense(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                cacheLicenseStatus(LicenseStatus.PAID)
                Log.d(TAG, "Paid license activated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to activate paid license", e)
                false
            }
        }
    }
    
    /**
     * Синхронизирует статус лицензии с сервером
     */
    suspend fun syncWithServer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting license sync with server")
                
                // В реальной реализации здесь будет запрос к серверу
                // Пока симулируем успешную синхронизацию
                Thread.sleep(500) // Симуляция сетевого запроса
                
                // Обновляем время последней синхронизации
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .apply()
                
                Log.d(TAG, "License sync completed successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "License sync failed", e)
                false
            }
        }
    }
    
    /**
     * Получает время последней синхронизации
     */
    suspend fun getLastSyncTime(): Long {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getLong(KEY_LAST_SYNC, 0)
        }
    }
    
    /**
     * Проверяет, требуется ли синхронизация
     */
    suspend fun isSyncNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            val lastSync = getLastSyncTime()
            val currentTime = System.currentTimeMillis()
            val syncInterval = 24 * 60 * 60 * 1000L // 24 часа
            
            currentTime - lastSync > syncInterval
        }
    }
    
    /**
     * Сбрасывает лицензию (для тестирования)
     */
    suspend fun resetLicense() {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_LICENSE_STATUS)
                .remove(KEY_DEVICE_HASH)
                .remove(KEY_LAST_SYNC)
                .apply()
            
            Log.d(TAG, "License reset")
        }
    }
}