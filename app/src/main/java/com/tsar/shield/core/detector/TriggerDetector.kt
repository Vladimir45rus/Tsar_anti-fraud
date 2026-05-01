package com.tsar.shield.core.detector

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

/**
 * Детектор триггерных фраз мошенников.
 * Анализирует распознанный текст на наличие опасных паттернов.
 */
class TriggerDetector {

    companion object {
        private const val TAG = "TriggerDetector"

        // Базовый список триггерных слов и фраз (можно расширять)
        val DEFAULT_TRIGGERS = listOf(
            // Банковская тематика
            "служба безопасности банка",
            "блокировка карты",
            "подозрительная операция",
            "подтверждение перевода",
            "безопасный счет",
            "компрометация данных",
            
            // Запросы конфиденциальных данных
            "код из смс",
            "cvv код",
            "номер карты",
            "пин код",
            "пароль от банка",
            "секретное слово",
            
            // Давление и срочность
            "срочно переведите",
            "время истекает",
            "немедленно оплатите",
            "иначе потеряете деньги",
            "ваш счет взломан",
            
            // Технические термины для запугивания
            "двухфакторная аутентификация",
            "верификация личности",
            "обновление реквизитов",
            "защитный протокол"
        )
    }

    private val triggers = mutableSetOf<String>()
    
    private val _detectedTriggers = MutableStateFlow<List<DetectedTrigger>>(emptyList())
    val detectedTriggers: StateFlow<List<DetectedTrigger>> = _detectedTriggers.asStateFlow()

    private val _threatLevel = MutableStateFlow<ThreatLevel>(ThreatLevel.NONE)
    val threatLevel: StateFlow<ThreatLevel> = _threatLevel.asStateFlow()

    init {
        // Добавляем триггеры по умолчанию
        triggers.addAll(DEFAULT_TRIGGERS.map { it.lowercase() })
    }

    enum class ThreatLevel {
        NONE,       // Угроз нет
        LOW,        // 1 совпадение
        MEDIUM,     // 2-3 совпадения
        HIGH        // 4+ совпадений или критические фразы
    }

    data class DetectedTrigger(
        val phrase: String,
        val matchedText: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Добавить пользовательский триггер
     */
    fun addTrigger(phrase: String) {
        triggers.add(phrase.lowercase())
        Log.d(TAG, "Добавлен триггер: $phrase")
    }

    /**
     * Удалить триггер
     */
    fun removeTrigger(phrase: String): Boolean {
        return triggers.remove(phrase.lowercase())
    }

    /**
     * Очистить все пользовательские триггеры (оставить только дефолтные)
     */
    fun resetToDefaults() {
        triggers.clear()
        triggers.addAll(DEFAULT_TRIGGERS.map { it.lowercase() })
    }

    /**
     * Анализ текста на наличие триггеров
     * Возвращает true, если обнаружена угроза
     */
    fun analyzeText(text: String): Boolean {
        if (text.isBlank()) return false

        val lowerText = text.lowercase()
        val detected = mutableListOf<DetectedTrigger>()

        for (trigger in triggers) {
            if (lowerText.contains(trigger)) {
                detected.add(DetectedTrigger(trigger, text))
            }
        }

        if (detected.isNotEmpty()) {
            _detectedTriggers.value = detected
            updateThreatLevel(detected.size)
            Log.w(TAG, "ОБНАРУЖЕНЫ ТРИГГЕРЫ! (${detected.size} шт.): ${detected.joinToString { it.phrase }}")
            return true
        }

        return false
    }

    /**
     * Обновление уровня угрозы
     */
    private fun updateThreatLevel(count: Int) {
        _threatLevel.value = when {
            count >= 4 -> ThreatLevel.HIGH
            count >= 2 -> ThreatLevel.MEDIUM
            count >= 1 -> ThreatLevel.LOW
            else -> ThreatLevel.NONE
        }
    }

    /**
     * Сброс состояния детектора
     */
    fun reset() {
        _detectedTriggers.value = emptyList()
        _threatLevel.value = ThreatLevel.NONE
    }

    /**
     * Получить текущий список всех триггеров
     */
    fun getAllTriggers(): List<String> {
        return triggers.toList()
    }
}
