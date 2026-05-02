package com.tsarshield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tsarshield.R
import com.tsarshield.data.repository.LicenseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Служба распознавания речи (STT) в реальном времени
 * Записывает аудио с микрофона, обрабатывает через оффлайн движок (Vosk/Silero)
 * и анализирует текст на наличие триггерных фраз социальной инженерии
 */
@AndroidEntryPoint
class STTService : Service() {
    
    companion object {
        private const val TAG = "STTService"
        private const val NOTIFICATION_CHANNEL_ID = "stt_service_channel"
        private const val NOTIFICATION_ID = 1003
        
        // Параметры аудио записи
        private const val SAMPLE_RATE = 16000 // 16 kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 1024 * 4 // 4 KB буфер
        
        // Триггерные фразы для обнаружения социальной инженерии
        private val TRIGGER_PHRASES = listOf(
            "банк", "карта", "кредит", "перевод", "деньги",
            "пароль", "код", "безопасность", "счет", "оплата",
            "мошенник", "обман", "срочно", "угроза", "штраф",
            "полиция", "суд", "арест", "взлом", "взломали"
        )
    }
    
    @Inject
    lateinit var licenseRepository: LicenseRepository
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    // STT движок (заглушка, будет заменен на нативную реализацию)
    private var sttEngine: STTEngine? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "STTService создан")
        createNotificationChannel()
        startForegroundService()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "STTService запущен с intent: ${intent?.extras}")
        
        val phoneNumber = intent?.getStringExtra("phone_number")
        val source = intent?.getStringExtra("source") ?: "unknown"
        
        Log.d(TAG, "Анализ вызова: номер=$phoneNumber, источник=$source")
        
        // Проверка лицензии
        if (!licenseRepository.isLicenseValid()) {
            Log.w(TAG, "Лицензия недействительна, остановка службы")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Инициализация STT движка
        initializeSTTEngine()
        
        // Запуск записи аудио
        startAudioRecording()
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "STTService уничтожается")
        stopAudioRecording()
        cleanupSTTEngine()
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Анализ аудио вызовов",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Анализ телефонных разговоров на предмет социальной инженерии"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Царь-Щит: Анализ аудио")
            .setContentText("Анализ телефонных разговоров в реальном времени")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun initializeSTTEngine() {
        // TODO: Заменить на реальную инициализацию Vosk/Silero движка
        sttEngine = STTEngine()
        val initialized = sttEngine?.initialize(applicationContext) ?: false
        
        if (initialized) {
            Log.d(TAG, "STT движок инициализирован успешно")
        } else {
            Log.e(TAG, "Ошибка инициализации STT движка")
            // В режиме разработки используем заглушку
            Log.w(TAG, "Используется заглушка STT движка")
        }
    }
    
    private fun cleanupSTTEngine() {
        sttEngine?.cleanup()
        sttEngine = null
    }
    
    private fun startAudioRecording() {
        if (isRecording) {
            Log.w(TAG, "Запись уже запущена")
            return
        }
        
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Неподдерживаемые параметры аудио")
            return
        }
        
        val bufferSize = maxOf(BUFFER_SIZE, minBufferSize)
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            isRecording = true
            
            Log.d(TAG, "Запись аудио начата, размер буфера: $bufferSize")
            
            // Запуск корутины для обработки аудио
            recordingJob = serviceScope.launch {
                processAudioStream()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска записи аудио", e)
            stopAudioRecording()
        }
    }
    
    private fun stopAudioRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка остановки записи аудио", e)
        }
        
        audioRecord = null
        Log.d(TAG, "Запись аудио остановлена")
    }
    
    private suspend fun processAudioStream() {
        val buffer = ShortArray(BUFFER_SIZE / 2) // 16-bit = 2 байта на сэмпл
        
        while (isRecording && audioRecord != null) {
            try {
                val bytesRead = audioRecord!!.read(buffer, 0, buffer.size)
                
                if (bytesRead > 0) {
                    // Обработка аудио буфера
                    processAudioBuffer(buffer.sliceArray(0 until bytesRead))
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Ошибка чтения аудио: неверная операция")
                    break
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Ошибка чтения аудио: неверное значение")
                    break
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка обработки аудио потока", e)
                break
            }
        }
    }
    
    private fun processAudioBuffer(audioData: ShortArray) {
        // Распознавание речи через STT движок
        val recognizedText = sttEngine?.processAudioBuffer(audioData) ?: ""
        
        if (recognizedText.isNotEmpty()) {
            Log.d(TAG, "Распознанный текст: $recognizedText")
            
            // Анализ на триггерные фразы
            val detectedThreats = analyzeTextForThreats(recognizedText)
            
            if (detectedThreats.isNotEmpty()) {
                Log.w(TAG, "Обнаружены угрозы: $detectedThreats")
                triggerThreatResponse(detectedThreats, recognizedText)
            }
        }
    }
    
    private fun analyzeTextForThreats(text: String): List<String> {
        val lowerText = text.lowercase()
        return TRIGGER_PHRASES.filter { phrase ->
            lowerText.contains(phrase.lowercase())
        }
    }
    
    private fun triggerThreatResponse(detectedThreats: List<String>, fullText: String) {
        // Показать оверлей с предупреждением
        val intent = Intent(this, OverlayService::class.java)
        intent.putExtra("warning_type", "social_engineering")
        intent.putExtra("threats", detectedThreats.toTypedArray())
        intent.putExtra("detected_text", fullText)
        startService(intent)
        
        // Запустить вибрацию
        triggerVibration()
        
        // Записать событие в лог
        logThreatEvent(detectedThreats, fullText)
    }
    
    private fun triggerVibration() {
        // TODO: Реализовать вибрацию через VibrationManager
        Log.d(TAG, "Вибрация активирована для предупреждения")
    }
    
    private fun logThreatEvent(detectedThreats: List<String>, text: String) {
        // TODO: Записать событие в локальную БД
        Log.i(TAG, "Событие угрозы: угрозы=$detectedThreats, текст=$text")
    }
}

/**
 * Заглушка STT движка для разработки
 * В реальной реализации будет заменена на нативную интеграцию с Vosk/Silero
 */
class STTEngine {
    
    private var isInitialized = false
    
    fun initialize(context: Context): Boolean {
        Log.d("STTEngine", "Инициализация STT движка (заглушка)")
        // TODO: Загрузить модель Vosk/Silero из assets
        isInitialized = true
        return true
    }
    
    fun processAudioBuffer(audioData: ShortArray): String {
        if (!isInitialized) {
            return ""
        }
        
        // Заглушка: возвращаем тестовый текст для демонстрации
        // В реальной реализации здесь будет вызов нативного кода
        return if (audioData.size > 1000) {
            // Имитация распознавания с вероятностью 10%
            if (Math.random() < 0.1) {
                "переведите деньги на карту банка"
            } else {
                ""
            }
        } else {
            ""
        }
    }
    
    fun cleanup() {
        Log.d("STTEngine", "Очистка STT движка")
        isInitialized = false
    }
}