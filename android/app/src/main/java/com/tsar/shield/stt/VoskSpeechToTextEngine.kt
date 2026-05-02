package com.tsar.shield.stt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.vosk.LibVosk
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.IOException

/**
 * Реализация STT движка на базе Vosk для оффлайн-распознавания русской речи.
 * Использует легковесную модель (~40MB) для быстрой работы на мобильных устройствах.
 */
class VoskSpeechToTextEngine(
    private val context: Context
) : SpeechToTextEngine {

    companion object {
        private const val TAG = "VoskSTTEngine"
        private const val MODEL_PATH = "vosk-model-small-ru-0.22"
        private const val SAMPLE_RATE = 16000f
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val isInitializedFlow = MutableStateFlow(false)
    private val partialResultsFlow = MutableStateFlow("")
    private val finalResultsFlow = MutableStateFlow("")

    override val isInitialized: StateFlow<Boolean> = isInitializedFlow
    override val partialResults: StateFlow<String> = partialResultsFlow
    override val finalResults: StateFlow<String> = finalResultsFlow

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                // Инициализация библиотеки Vosk
                LibVosk.setLogLevel(0) // Отключаем лишние логи

                // Проверка наличия модели в assets и её копирование во внутреннее хранилище
                val modelPath = loadModelFromAssets()

                // Загрузка модели
                model = Model(modelPath)
                recognizer = Recognizer(model, SAMPLE_RATE)

                isInitializedFlow.value = true
                Log.i(TAG, "Vosk STT engine initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Vosk STT engine", e)
                isInitializedFlow.value = false
            }
        }
    }

    /**
     * Загружает модель из assets во внутреннее хранилище приложения.
     * Модель хранится в виде ZIP-архива и распаковывается при первом запуске.
     */
    private suspend fun loadModelFromAssets(): String = withContext(Dispatchers.IO) {
        val syncPath = context.filesDir.absolutePath + "/vosk-model"

        try {
            // Проверяем, существует ли уже распакованная модель
            val modelDir = java.io.File(syncPath)
            if (!modelDir.exists()) {
                Log.i(TAG, "Extracting model from assets...")
                StorageService.unpack(
                    context,
                    MODEL_PATH,
                    "vosk-model",
                    { progress ->
                        Log.d(TAG, "Model extraction progress: $progress%")
                    },
                    { error ->
                        Log.e(TAG, "Model extraction error: $error")
                    }
                )
            } else {
                Log.d(TAG, "Model already exists at $syncPath")
            }

            syncPath
        } catch (e: IOException) {
            Log.e(TAG, "Error loading model from assets", e)
            throw e
        }
    }

    override suspend fun processAudio(pcmData: ShortArray) {
        withContext(Dispatchers.Default) {
            if (!isInitializedFlow.value || recognizer == null) {
                Log.w(TAG, "STT engine not initialized, skipping audio processing")
                return@withContext
            }

            try {
                // Обработка аудио данных
                if (recognizer!!.acceptWaveForm(pcmData, pcmData.size * 2)) {
                    // Получен финальный результат распознавания
                    val result = recognizer!!.result
                    finalResultsFlow.value = parseResult(result)
                    Log.d(TAG, "Final recognition result: ${finalResultsFlow.value}")
                } else {
                    // Получен частичный результат (в процессе говорения)
                    val partial = recognizer!!.partialResult
                    partialResultsFlow.value = parseResult(partial)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio data", e)
            }
        }
    }

    override suspend fun reset() {
        withContext(Dispatchers.Default) {
            recognizer?.reset()
            partialResultsFlow.value = ""
            Log.d(TAG, "STT recognizer reset")
        }
    }

    override fun release() {
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        isInitializedFlow.value = false
        partialResultsFlow.value = ""
        finalResultsFlow.value = ""
        Log.i(TAG, "STT engine released")
    }

    /**
     * Парсит JSON-ответ от Vosk и извлекает текст.
     * Формат ответа: {"text": "распознанный текст"}
     */
    private fun parseResult(jsonResult: String): String {
        return try {
            val startIndex = jsonResult.indexOf("\"text\"")
            if (startIndex == -1) return ""

            val colonIndex = jsonResult.indexOf(":", startIndex)
            val quoteStart = jsonResult.indexOf("\"", colonIndex) + 1
            val quoteEnd = jsonResult.indexOf("\"", quoteStart)

            if (quoteStart > 0 && quoteEnd > quoteStart) {
                jsonResult.substring(quoteStart, quoteEnd)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON result: $jsonResult", e)
            ""
        }
    }
}
