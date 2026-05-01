package com.tsar.shield.core.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Менеджер записи аудио с микрофона.
 * Работает в фоне, отдавая сырые PCM данные для STT движка.
 */
class AudioRecorderManager {

    companion object {
        // Частота дискретизации 16000 Гц - стандарт для большинства STT (Vosk, Silero)
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    
    // Поток для передачи байтов аудио наружу
    private val _audioDataFlow = MutableStateFlow<ByteArray?>(null)
    val audioDataFlow: StateFlow<ByteArray?> = _audioDataFlow.asStateFlow()

    // Поток статуса (Started, Stopped, Error)
    private val _statusFlow = MutableStateFlow<RecordingStatus>(RecordingStatus.IDLE)
    val statusFlow: StateFlow<RecordingStatus> = _statusFlow.asStateFlow()

    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    enum class RecordingStatus {
        IDLE, RECORDING, ERROR
    }

    /**
     * Запуск записи. Должен вызываться из фоновой корутины.
     */
    fun startRecording() {
        if (isRecording.get()) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2 // Увеличиваем буфер для стабильности
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _statusFlow.value = RecordingStatus.ERROR
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            _statusFlow.value = RecordingStatus.RECORDING

            // Запускаем цикл чтения в отдельном потоке
            Thread {
                readAudioLoop()
            }.start()

        } catch (e: SecurityException) {
            _statusFlow.value = RecordingStatus.ERROR
            e.printStackTrace()
        } catch (e: Exception) {
            _statusFlow.value = RecordingStatus.ERROR
            e.printStackTrace()
        }
    }

    /**
     * Цикл чтения данных из микрофона
     */
    private fun readAudioLoop() {
        val buffer = ByteArray(bufferSize)
        
        while (isRecording.get()) {
            val readResult = audioRecord?.read(buffer, 0, buffer.size)
            
            if (readResult != null && readResult > 0) {
                // Копируем только прочитанные байты
                val data = buffer.copyOf(readResult)
                _audioDataFlow.value = data
            } else if (readResult == AudioRecord.ERROR_INVALID_OPERATION || 
                       readResult == AudioRecord.ERROR_BAD_VALUE) {
                break
            }
        }
    }

    /**
     * Остановка записи
     */
    fun stopRecording() {
        if (!isRecording.get()) return

        isRecording.set(false)
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            _statusFlow.value = RecordingStatus.IDLE
            _audioDataFlow.value = null
        }
    }

    /**
     * Проверка доступности микрофона
     */
    fun isMicAvailable(): Boolean {
        return AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) 
            != AudioRecord.ERROR_BAD_VALUE
    }
}
