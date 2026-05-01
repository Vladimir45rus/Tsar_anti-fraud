package com.tsar.shield.core.stt

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс для движка распознавания речи (STT).
 * Позволяет легко переключаться между Vosk, Silero или другими решениями.
 */
interface SpeechToTextEngine {
    
    /**
     * Инициализация движка (загрузка модели и т.д.)
     * Должен быть вызван перед началом распознавания
     */
    suspend fun initialize(): Boolean
    
    /**
     * Обработка аудио чанка (PCM данные 16kHz, 16-bit, mono)
     * Возвращает распознанный текст, если есть результат
     */
    suspend fun processAudioChunk(audioData: ByteArray): String?
    
    /**
     * Поток с промежуточными результатами распознавания
     */
    val partialResults: StateFlow<String?>
    
    /**
     * Поток с финальными результатами распознавания
     */
    val finalResults: StateFlow<String?>
    
    /**
     * Сброс состояния (для начала нового сеанса распознавания)
     */
    fun reset()
    
    /**
     * Освобождение ресурсов
     */
    fun shutdown()
    
    /**
     * Статус движка
     */
    val isReady: Boolean
}
