package com.tsar.shield

import java.util.*

/**
 * Царь-Щит: Ядро системы обнаружения угроз.
 * Взаимодействует с MiMo v2.5 Pro для глубокого анализа логов.
 */
class SecurityCore {

    // Модель события безопасности
    data class SecurityEvent(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val type: String, // Например: "FAILED_LOGIN", "SUSPICIOUS_IP", "GEO_ANOMALY"
        val riskLevel: Int, // От 1 (низкий) до 5 (критичный)
        val details: String
    )

    private val eventLogs = mutableListOf<SecurityEvent>()

    /**
     * Фиксация подозрительного действия.
     * Если риск выше 4, активируется протокол блокировки.
     */
    fun logSecurityEvent(type: String, risk: Int, message: String) {
        val event = SecurityEvent(type = type, riskLevel = risk, details = message)
        eventLogs.add(event)
        
        println("Царь-Щит зафиксировал событие: [$type] Риск: $risk")

        if (risk >= 5) {
            activateLockdown()
        }
    }

    /**
     * Экстренная блокировка всех интерфейсов экосистемы.
     */
    private fun activateLockdown() {
        // Здесь будет вызов методов из Царь-ID для деавторизации
        println("⚠️ КРИТИЧЕСКАЯ УГРОЗА! Царь-Щит блокирует доступ к экосистеме.")
    }

    /**
     * Метод для экспорта данных в MiMo AI.
     * Позволяет ИИ анализировать последние 50 событий на предмет аномалий.
     */
    fun getDataForAIAnalysis(): List<SecurityEvent> {
        return eventLogs.takeLast(50)
    }
}
