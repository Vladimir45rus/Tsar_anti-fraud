package com.tsar.shield.core.stt

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Заглушка STT движка для тестирования.
 * Имитирует распознавание речи без реальной обработки аудио.
 * 
 * TODO: Заменить на реальную реализацию с Vosk/Silero
 */
class StubSpeechToTextEngine(
    private val context: Context
) : SpeechToTextEngine {

    companion object {
        private const val TAG = "StubSTT"
        
        // Тестовые фразы для имитации мошеннического разговора
        private val TEST_PHRASES = listOf(
            "Здравствуйте, это служба безопасности банка",
            "На вашем счете подозрительная активность",
            "Назовите код из СМС для подтверждения",
            "Вам нужно перевести деньги на безопасный счет",
            "Скажите номер вашей карты",
            "Мы фиксируем попытку кражи средств"
        )
    }

    private val _partialResults = MutableStateFlow<String?>(null)
    override val partialResults: StateFlow<String?> = _partialResults.asStateFlow()

    private val _finalResults = MutableStateFlow<String?>(null)
    override val finalResults: StateFlow<String?> = _finalResults.asStateFlow()

    private var _isReady = false
    override val isReady: Boolean get() = _isReady

    private var phraseIndex = 0

    override suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Имитация загрузки модели
                Log.d(TAG, "Инициализация заглушки STT...")
                kotlinx.coroutines.delay(500)
                _isReady = true
                Log.d(TAG, "STT заглушка готова")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка инициализации STT заглушки", e)
                false
            }
        }
    }

    override suspend fun processAudioChunk(audioData: ByteArray): String? {
        if (!_isReady) return null
        
        // Имитируем распознавание каждые N чанков
        // В реальности здесь был бы вызов Vosk/Silero
        if (audioData.size > 1000) { // Простая эвристика "активности"
            phraseIndex = (phraseIndex + 1) % TEST_PHRASES.size
            val phrase = TEST_PHRASES[phraseIndex]
            
            // Сначала частичный результат
            _partialResults.value = phrase.take(phrase.length / 2)
            
            // Затем финальный
            kotlinx.coroutines.delay(300)
            _finalResults.value = phrase
            _partialResults.value = null
            
            Log.d(TAG, "Распознано (заглушка): $phrase")
            return phrase
        }
        
        return null
    }

    override fun reset() {
        Log.d(TAG, "Сброс STT заглушки")
        phraseIndex = 0
        _partialResults.value = null
        _finalResults.value = null
    }

    override fun shutdown() {
        Log.d(TAG, "Остановка STT заглушки")
        _isReady = false
        _partialResults.value = null
        _finalResults.value = null
    }
}
