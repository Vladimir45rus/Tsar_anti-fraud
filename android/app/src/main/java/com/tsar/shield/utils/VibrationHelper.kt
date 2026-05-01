package com.tsar.shield.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Вспомогательный класс для управления вибрацией устройства
 * Реализует паттерны вибрации для различных типов предупреждений
 */
class VibrationHelper(private val context: Context) {
    
    companion object {
        // Паттерны вибрации (в миллисекундах)
        private val PATTERN_WARNING = longArrayOf(0, 500, 200, 500) // пауза, длинный, короткий, длинный
        private val PATTERN_DANGER = longArrayOf(0, 800, 300, 800, 300, 800) // три длинных с короткими паузами
        private val PATTERN_INFO = longArrayOf(0, 200) // короткий сигнал
        
        // Амплитуды (от 0 до 255)
        private val AMPLITUDE_WARNING = intArrayOf(0, 255, 128, 255)
        private val AMPLITUDE_DANGER = intArrayOf(0, 255, 200, 255, 200, 255)
        private val AMPLITUDE_INFO = intArrayOf(0, 128)
        
        private const val VIBRATION_PERMISSION = "android.permission.VIBRATE"
    }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Проверяет, доступна ли вибрация на устройстве
     */
    fun isVibrationAvailable(): Boolean {
        return vibrator?.hasVibrator() ?: false
    }
    
    /**
     * Воспроизводит вибрацию для предупреждения о социальной инженерии
     * @param threatLevel уровень угрозы (1-3, где 3 - максимальный)
     */
    fun vibrateForThreat(threatLevel: Int) {
        when (threatLevel) {
            1 -> vibrateInfo()
            2 -> vibrateWarning()
            3 -> vibrateDanger()
            else -> vibrateWarning()
        }
    }
    
    /**
     * Вибрация для высокого уровня угрозы (опасность)
     */
    fun vibrateDanger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(PATTERN_DANGER, AMPLITUDE_DANGER, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(PATTERN_DANGER, -1)
        }
    }
    
    /**
     * Вибрация для среднего уровня угрозы (предупреждение)
     */
    fun vibrateWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(PATTERN_WARNING, AMPLITUDE_WARNING, -1)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(PATTERN_WARNING, -1)
        }
    }
    
    /**
     * Вибрация для информационного уведомления
     */
    fun vibrateInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(200, 128)
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(200)
        }
    }
    
    /**
     * Останавливает текущую вибрацию
     */
    fun cancelVibration() {
        vibrator?.cancel()
    }
    
    /**
     * Проверяет, вибрирует ли устройство в данный момент (только API 26+)
     */
    fun isVibrating(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasAmplitudeControl() ?: false
        } else {
            false
        }
    }
}