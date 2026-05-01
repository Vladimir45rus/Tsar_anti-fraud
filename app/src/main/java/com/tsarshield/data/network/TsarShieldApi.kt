package com.tsarshield.data.network

import com.tsarshield.data.model.request.CheckLicenseRequest
import com.tsarshield.data.model.response.CheckLicenseResponse
import com.tsarshield.data.model.response.SyncTriggersResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Интерфейс API для взаимодействия с сервером Царь-Щит
 */
interface TsarShieldApi {
    
    /**
     * Проверка лицензии на сервере
     * @param request данные запроса
     * @return ответ с информацией о лицензии
     */
    @POST("api/v1/license/check")
    suspend fun checkLicense(
        @Body request: CheckLicenseRequest
    ): Response<CheckLicenseResponse>
    
    /**
     * Синхронизация триггерных фраз с сервером
     * @param deviceHash хэш устройства
     * @param lastSyncTimestamp время последней синхронизации
     * @param authorization JWT токен
     * @return список обновленных триггеров
     */
    @GET("api/v1/triggers/sync")
    suspend fun syncTriggers(
        @Query("device_hash") deviceHash: String,
        @Query("last_sync") lastSyncTimestamp: Long,
        @Header("Authorization") authorization: String? = null
    ): Response<SyncTriggersResponse>
    
    /**
     * Отправка алерта о обнаруженной угрозе
     * @param deviceHash хэш устройства
     * @param phoneNumber номер телефона (захешированный)
     * @param threatLevel уровень угрозы
     * @param detectedText обнаруженный текст
     * @param triggerPhrases список триггерных фраз
     * @param authorization JWT токен
     * @return статус отправки
     */
    @POST("api/v1/alert/send")
    suspend fun sendAlert(
        @Query("device_hash") deviceHash: String,
        @Query("phone_hash") phoneNumber: String,
        @Query("threat_level") threatLevel: Int,
        @Query("detected_text") detectedText: String,
        @Query("trigger_phrases") triggerPhrases: List<String>,
        @Header("Authorization") authorization: String? = null
    ): Response<Unit>
    
    /**
     * Проверка состояния сервера
     * @return статус сервера
     */
    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Unit>
    
    /**
     * Получение информации о сервере
     * @return метаданные сервера
     */
    @GET("api/v1/info")
    suspend fun getServerInfo(): Response<ServerInfoResponse>
}

/**
 * Ответ с информацией о сервере
 */
data class ServerInfoResponse(
    val serverVersion: String,
    val apiVersion: String,
    val supportedFeatures: List<String>,
    val maintenanceMode: Boolean,
    val message: String?
)