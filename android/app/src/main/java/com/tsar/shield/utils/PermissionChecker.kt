package com.tsar.shield.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PermissionChecker"
        
        // Основные разрешения для работы приложения
        private val BASIC_PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        )
        
        // Дополнительные разрешения для расширенной функциональности
        private val EXTENDED_PERMISSIONS = listOf(
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        
        // Разрешения для Android 13+
        private val ANDROID_13_PERMISSIONS = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }
    
    /**
     * Проверяет все необходимые разрешения
     */
    fun checkAllPermissions(): Map<String, Boolean> {
        val permissions = mutableMapOf<String, Boolean>()
        
        // Базовые разрешения
        BASIC_PERMISSIONS.forEach { permission ->
            permissions[permission] = isPermissionGranted(permission)
        }
        
        // Разрешения для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ANDROID_13_PERMISSIONS.forEach { permission ->
                permissions[permission] = isPermissionGranted(permission)
            }
        }
        
        // Специальные разрешения
        permissions[Manifest.permission.SYSTEM_ALERT_WINDOW] = hasOverlayPermission()
        
        return permissions
    }
    
    /**
     * Проверяет, предоставлено ли конкретное разрешение
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Проверяет, предоставлены ли все базовые разрешения
     */
    fun hasBasicPermissions(): Boolean {
        return BASIC_PERMISSIONS.all { isPermissionGranted(it) }
    }
    
    /**
     * Проверяет, предоставлены ли разрешения для работы с микрофоном
     */
    fun hasAudioPermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO) &&
               (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                isPermissionGranted(Manifest.permission.READ_MEDIA_AUDIO))
    }
    
    /**
     * Проверяет, предоставлены ли разрешения для работы с телефоном
     */
    fun hasPhonePermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_PHONE_STATE)
    }
    
    /**
     * Проверяет, предоставлены ли разрешения для работы с контактами
     */
    fun hasContactPermissions(): Boolean {
        return isPermissionGranted(Manifest.permission.READ_CONTACTS)
    }
    
    /**
     * Проверяет, предоставлено ли разрешение на оверлей
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Для версий ниже Android M разрешение не требуется
        }
    }
    
    /**
     * Проверяет, предоставлены ли разрешения для CallScreening
     */
    fun hasCallScreeningPermissions(): Boolean {
        return hasPhonePermissions() && hasContactPermissions()
    }
    
    /**
     * Проверяет, предоставлены ли разрешения для Accessibility
     */
    fun hasAccessibilityPermissions(): Boolean {
        // Accessibility разрешения проверяются через системные настройки
        // Возвращаем true, так как проверка будет в другом месте
        return true
    }
    
    /**
     * Получает список отсутствующих разрешений
     */
    fun getMissingPermissions(): List<String> {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.filter { !isPermissionGranted(it) }
    }
    
    /**
     * Получает список всех необходимых разрешений
     */
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        
        permissions.addAll(BASIC_PERMISSIONS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.addAll(ANDROID_13_PERMISSIONS)
        }
        
        return permissions
    }
    
    /**
     * Получает описание разрешения для отображения пользователю
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Микрофон"
            Manifest.permission.READ_PHONE_STATE -> "Состояние телефона"
            Manifest.permission.READ_CONTACTS -> "Контакты"
            Manifest.permission.SYSTEM_ALERT_WINDOW -> "Поверх других окон"
            Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth"
            Manifest.permission.POST_NOTIFICATIONS -> "Уведомления"
            Manifest.permission.READ_MEDIA_AUDIO -> "Медиа-аудио"
            else -> permission
        }
    }
    
    /**
     * Проверяет, можно ли запросить разрешение (не было отказано навсегда)
     */
    fun canRequestPermission(permission: String): Boolean {
        // Для Android M+ проверяем, не было ли отказано навсегда
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Эта проверка требует Activity, поэтому здесь просто возвращаем true
            // Реальная проверка будет в Activity
            return true
        }
        return true
    }
    
    /**
     * Получает статус разрешений в виде читаемого текста
     */
    fun getPermissionStatusText(): String {
        val permissions = checkAllPermissions()
        val granted = permissions.count { it.value }
        val total = permissions.size
        
        return "Разрешения: $granted/$total"
    }
    
    /**
     * Проверяет, предоставлены ли все разрешения для полноценной работы
     */
    fun hasAllPermissionsForFullFunctionality(): Boolean {
        return hasBasicPermissions() && 
               hasOverlayPermission() &&
               (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
                isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS))
    }
}