package com.tsarshield.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tsarshield.R
import com.tsarshield.utils.VibrationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Служба для отображения оверлейного предупреждения о социальной инженерии
 * Отображается поверх всех окон и сопровождается вибрацией
 */
@AndroidEntryPoint
class OverlayService : Service() {
    
    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1004
        
        // Константы для intent extras
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_WARNING_TYPE = "warning_type"
        const val EXTRA_THREATS = "threats"
        const val EXTRA_DETECTED_TEXT = "detected_text"
        const val EXTRA_THREAT_LEVEL = "threat_level"
        
        // Уровни угроз
        const val THREAT_LEVEL_HIGH = 3
        const val THREAT_LEVEL_MEDIUM = 2
        const val THREAT_LEVEL_LOW = 1
        const val THREAT_LEVEL_INFO = 0
        
        // Типы предупреждений
        const val WARNING_SOCIAL_ENGINEERING = "social_engineering"
        const val WARNING_SUSPICIOUS_CALL = "suspicious_call"
        const val WARNING_PHISHING = "phishing"
    }
    
    @Inject
    lateinit var vibrator: Vibrator
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: LayoutParams
    private var isOverlayShowing = false
    private var vibrationPattern: LongArray? = null
    private lateinit var vibrationHelper: VibrationHelper
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService создан")
        vibrationHelper = VibrationHelper(this)
        createNotificationChannel()
        startForegroundService()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "OverlayService запущен")
        
        if (intent == null) {
            Log.w(TAG, "OverlayService запущен без intent, остановка")
            stopSelf()
            return START_NOT_STICKY
        }
        
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Неизвестный номер"
        val warningType = intent.getStringExtra(EXTRA_WARNING_TYPE) ?: WARNING_SOCIAL_ENGINEERING
        val threats = intent.getStringArrayExtra(EXTRA_THREATS)?.toList() ?: emptyList()
        val detectedText = intent.getStringExtra(EXTRA_DETECTED_TEXT) ?: ""
        val threatLevel = intent.getIntExtra(EXTRA_THREAT_LEVEL, THREAT_LEVEL_MEDIUM)
        
        Log.d(TAG, "Показать оверлей: номер=$phoneNumber, тип=$warningType, угрозы=$threats")
        
        // Показать оверлей
        showOverlay(phoneNumber, warningType, threats, detectedText, threatLevel)
        
        // Запустить вибрацию
        startVibration(threatLevel)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "OverlayService уничтожается")
        dismissOverlay()
        stopVibration()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Предупреждения о безопасности",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображение предупреждений о социальной инженерии"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Царь-Щит: Активный мониторинг")
            .setContentText("Отображение предупреждений о безопасности")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun showOverlay(
        phoneNumber: String,
        warningType: String,
        threats: List<String>,
        detectedText: String,
        threatLevel: Int
    ) {
        if (isOverlayShowing) {
            Log.d(TAG, "Оверлей уже отображается, обновление")
            dismissOverlay()
        }
        
        // Создание layout параметров
        params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL or
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        } else {
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_PHONE,
                LayoutParams.FLAG_NOT_FOCUSABLE or
                LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
        }
        
        params.gravity = Gravity.TOP
        params.x = 0
        params.y = 100 // Отступ от верха
        
        // Загрузка layout
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_warning, null)
        
        // Настройка содержимого
        val tvTitle = overlayView.findViewById<TextView>(R.id.tv_title)
        val tvPhoneNumber = overlayView.findViewById<TextView>(R.id.tv_phone_number)
        val tvWarning = overlayView.findViewById<TextView>(R.id.tv_warning)
        val tvThreats = overlayView.findViewById<TextView>(R.id.tv_threats)
        val btnDismiss = overlayView.findViewById<Button>(R.id.btn_dismiss)
        val btnBlock = overlayView.findViewById<Button>(R.id.btn_block)
        val btnMoreInfo = overlayView.findViewById<Button>(R.id.btn_more_info)
        
        // Установка текста
        tvTitle.text = getWarningTitle(warningType)
        tvPhoneNumber.text = "Номер: $phoneNumber"
        tvWarning.text = getWarningText(threatLevel)
        
        val threatsText = if (threats.isNotEmpty()) {
            "Обнаружено: ${threats.joinToString(", ")}"
        } else {
            "Обнаружены подозрительные фразы"
        }
        tvThreats.text = threatsText
        
        // Настройка цвета в зависимости от уровня угрозы
        val backgroundColor = getThreatColor(threatLevel)
        overlayView.setBackgroundColor(backgroundColor)
        
        // Обработчики кнопок
        btnDismiss.setOnClickListener {
            Log.d(TAG, "Пользователь закрыл предупреждение")
            dismissOverlay()
        }
        
        btnBlock.setOnClickListener {
            Log.d(TAG, "Пользователь заблокировал номер: $phoneNumber")
            blockCall(phoneNumber)
            dismissOverlay()
        }
        
        btnMoreInfo.setOnClickListener {
            Log.d(TAG, "Пользователь запросил подробности")
            showMoreInfo(phoneNumber, threats, detectedText)
        }
        
        // Добавление оверлея в WindowManager
        windowManager.addView(overlayView, params)
        isOverlayShowing = true
        
        // Автоматическое скрытие через 30 секунд
        overlayView.postDelayed({
            if (isOverlayShowing) {
                Log.d(TAG, "Автоматическое скрытие оверлея")
                dismissOverlay()
            }
        }, 30000)
    }
    
    private fun getWarningTitle(warningType: String): String {
        return when (warningType) {
            WARNING_SOCIAL_ENGINEERING -> "⚠️ СОЦИАЛЬНАЯ ИНЖЕНЕРИЯ"
            WARNING_SUSPICIOUS_CALL -> "⚠️ ПОДОЗРИТЕЛЬНЫЙ ВЫЗОВ"
            WARNING_PHISHING -> "⚠️ ФИШИНГ"
            else -> "⚠️ ВНИМАНИЕ"
        }
    }
    
    private fun getWarningText(threatLevel: Int): String {
        return when (threatLevel) {
            THREAT_LEVEL_HIGH -> "ВЫСОКАЯ ОПАСНОСТЬ! Возможна попытка мошенничества"
            THREAT_LEVEL_MEDIUM -> "СРЕДНЯЯ ОПАСНОСТЬ! Подозрительный разговор"
            THREAT_LEVEL_LOW -> "НИЗКАЯ ОПАСНОСТЬ! Возможный обман"
            else -> "ВНИМАНИЕ! Анализ разговора"
        }
    }
    
    private fun getThreatColor(threatLevel: Int): Int {
        return when (threatLevel) {
            THREAT_LEVEL_HIGH -> Color.parseColor("#FF5252") // Красный
            THREAT_LEVEL_MEDIUM -> Color.parseColor("#FF9800") // Оранжевый
            THREAT_LEVEL_LOW -> Color.parseColor("#FFEB3B") // Желтый
            else -> Color.parseColor("#2196F3") // Синий
        }
    }
    
    private fun dismissOverlay() {
        if (isOverlayShowing && ::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
                isOverlayShowing = false
                Log.d(TAG, "Оверлей скрыт")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при скрытии оверлея", e)
            }
        }
        stopVibration()
        stopSelf()
    }
    
    private fun startVibration(threatLevel: Int) {
        if (!vibrationHelper.isVibrationAvailable()) {
            Log.w(TAG, "Устройство не поддерживает вибрацию")
            return
        }
        
        vibrationHelper.vibrateForThreat(threatLevel)
        Log.d(TAG, "Вибрация запущена, уровень угрозы: $threatLevel")
    }
    
    private fun stopVibration() {
        vibrationHelper.cancelVibration()
        Log.d(TAG, "Вибрация остановлена")
    }
    
    private fun blockCall(phoneNumber: String) {
        Log.d(TAG, "Блокировка вызова от номера: $phoneNumber")
        
        // TODO: Реализовать логику блокировки вызова
        // Можно добавить номер в черный список или завершить текущий вызов
        
        // Показать уведомление о блокировке
        showBlockNotification(phoneNumber)
    }
    
    private fun showMoreInfo(phoneNumber: String, threats: List<String>, detectedText: String) {
        // TODO: Реализовать отображение подробной информации
        // Можно открыть Activity с деталями или показать диалог
        Log.d(TAG, "Показать подробности: номер=$phoneNumber, угрозы=$threats, текст=$detectedText")
    }
    
    private fun showBlockNotification(phoneNumber: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Вызов заблокирован")
            .setContentText("Номер $phoneNumber добавлен в черный список")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}