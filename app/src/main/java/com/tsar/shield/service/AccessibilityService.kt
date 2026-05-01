package com.tsar.shield.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import androidx.core.content.ContextCompat
import com.tsar.shield.data.repository.LicenseRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Служба Accessibility для анализа экрана и обнаружения входящих вызовов
 * Используется как fallback для Android 8-9 и для анализа содержимого экрана
 */
@AndroidEntryPoint
class AccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AccessibilityService"
        
        // Пакеты приложений для анализа
        private const val CALL_SCREEN_PACKAGE_DIALER = "com.android.dialer"
        private const val CALL_SCREEN_PACKAGE_PHONE = "com.android.phone"
        private const val CALL_SCREEN_PACKAGE_CONTACTS = "com.android.contacts"
        
        // Тексты для обнаружения входящих вызовов
        private val INCOMING_CALL_TEXTS = listOf(
            "Входящий вызов",
            "Incoming call",
            "Вызов",
            "Call"
        )
        
        // Идентификаторы элементов для извлечения номера
        private val PHONE_NUMBER_VIEW_IDS = listOf(
            "com.android.dialer:id/contactgrid_contact_name",
            "com.android.dialer:id/phone_number",
            "com.android.incallui:id/contactgrid_contact_name",
            "com.android.incallui:id/phone_number"
        )
    }
    
    @Inject
    lateinit var licenseRepository: LicenseRepository
    
    private var isMonitoring = false
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")
        
        // Настройка информации о службе
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        
        this.serviceInfo = info
        isMonitoring = true
        
        Log.d(TAG, "AccessibilityService configured and monitoring started")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isMonitoring) return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                val className = event.className?.toString()
                
                Log.d(TAG, "Window changed: $packageName/$className")
                
                if (isCallScreenPackage(packageName)) {
                    analyzeCallScreen()
                }
            }
            
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Анализ содержимого окна
                analyzeWindowContent()
            }
            
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Обработка кликов (например, на кнопках принятия/отклонения вызова)
                handleViewClick(event)
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
        isMonitoring = false
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "AccessibilityService unbound")
        isMonitoring = false
        return super.onUnbind(intent)
    }
    
    private fun isCallScreenPackage(packageName: String?): Boolean {
        return packageName in listOf(
            CALL_SCREEN_PACKAGE_DIALER,
            CALL_SCREEN_PACKAGE_PHONE,
            CALL_SCREEN_PACKAGE_CONTACTS
        )
    }
    
    private fun analyzeCallScreen() {
        val rootNode = rootInActiveWindow ?: return
        
        // Поиск текста "Входящий вызов"
        val hasIncomingCall = INCOMING_CALL_TEXTS.any { text ->
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            nodes.isNotEmpty()
        }
        
        if (hasIncomingCall) {
            Log.d(TAG, "Входящий вызов обнаружен через Accessibility")
            
            // Поиск номера телефона
            val phoneNumber = extractPhoneNumber(rootNode)
            
            // Запуск анализа
            startCallAnalysis(phoneNumber)
        }
    }
    
    private fun analyzeWindowContent() {
        // Анализ изменений содержимого окна
        // Можно использовать для обнаружения новых элементов интерфейса
        val rootNode = rootInActiveWindow ?: return
        
        // Поиск подозрительных текстов (например, "банк", "перевод", "код")
        val suspiciousPatterns = listOf("банк", "перевод", "код", "пароль", "карта")
        suspiciousPatterns.forEach { pattern ->
            val nodes = rootNode.findAccessibilityNodeInfosByText(pattern)
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Обнаружен подозрительный текст: $pattern")
                // Можно показать предупреждение пользователю
            }
        }
    }
    
    private fun handleViewClick(event: AccessibilityEvent) {
        val className = event.className?.toString()
        val text = event.text?.firstOrNull()?.toString()
        
        Log.d(TAG, "View clicked: $className, text: $text")
        
        // Проверка нажатия на кнопки принятия/отклонения вызова
        if (className?.contains("Button") == true) {
            when (text) {
                "Принять", "Accept" -> {
                    Log.d(TAG, "Пользователь принял вызов")
                    // Запуск анализа после принятия вызова
                    startCallAnalysisAfterAnswer()
                }
                "Отклонить", "Decline" -> {
                    Log.d(TAG, "Пользователь отклонил вызов")
                }
            }
        }
    }
    
    private fun extractPhoneNumber(rootNode: AccessibilityNodeInfo): String? {
        // Поиск номера телефона по известным идентификаторам view
        PHONE_NUMBER_VIEW_IDS.forEach { viewId ->
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (nodes.isNotEmpty()) {
                val number = nodes[0].text?.toString()
                if (!number.isNullOrEmpty()) {
                    Log.d(TAG, "Номер телефона найден: $number")
                    return number
                }
            }
        }
        
        // Альтернативный метод: поиск по паттерну номера телефона
        val allText = extractAllText(rootNode)
        val phonePattern = Regex("""[\d\s\-\+\(\)]{7,}""")
        val match = phonePattern.find(allText)
        
        return match?.value
    }
    
    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        
        val builder = StringBuilder()
        builder.append(text).append(" ").append(contentDescription).append(" ")
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                builder.append(extractAllText(child))
            }
        }
        
        return builder.toString()
    }
    
    private fun startCallAnalysis(phoneNumber: String?) {
        Log.d(TAG, "Запуск анализа вызова для номера: $phoneNumber")
        
        // Проверка лицензии
        val licenseValid = licenseRepository.isLicenseValid()
        if (!licenseValid) {
            Log.w(TAG, "Лицензия недействительна, анализ пропущен")
            return
        }
        
        // Запуск STT анализа
        val intent = Intent(this, STTService::class.java)
        intent.putExtra("phone_number", phoneNumber)
        intent.putExtra("source", "accessibility")
        ContextCompat.startForegroundService(this, intent)
        
        // Показать оверлей с предупреждением
        showWarningOverlay(phoneNumber)
    }
    
    private fun startCallAnalysisAfterAnswer() {
        // Запуск анализа после принятия вызова
        val intent = Intent(this, STTService::class.java)
        intent.putExtra("call_answered", true)
        ContextCompat.startForegroundService(this, intent)
    }
    
    private fun showWarningOverlay(phoneNumber: String?) {
        val intent = Intent(this, OverlayService::class.java)
        intent.putExtra("phone_number", phoneNumber)
        intent.putExtra("warning_type", "social_engineering")
        intent.putExtra("source", "accessibility")
        ContextCompat.startForegroundService(this, intent)
        
        Log.d(TAG, "Оверлей предупреждения показан для номера: $phoneNumber")
    }
}