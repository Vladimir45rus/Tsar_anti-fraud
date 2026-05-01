package com.tsar.shield.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Ответ сервера на запрос синхронизации триггерных фраз
 */
@JsonClass(generateAdapter = true)
data class SyncTriggersResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "triggers")
    val triggers: List<TriggerPhrase>,
    
    @Json(name = "version")
    val version: String,
    
    @Json(name = "last_updated")
    val lastUpdated: Long,
    
    @Json(name = "next_sync_recommended")
    val nextSyncRecommended: Long?,
    
    @Json(name = "message")
    val message: String?
)

/**
 * Модель триггерной фразы
 */
@JsonClass(generateAdapter = true)
data class TriggerPhrase(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "phrase")
    val phrase: String,
    
    @Json(name = "language")
    val language: String,
    
    @Json(name = "category")
    val category: String,
    
    @Json(name = "threat_level")
    val threatLevel: Int,
    
    @Json(name = "weight")
    val weight: Double,
    
    @Json(name = "enabled")
    val enabled: Boolean,
    
    @Json(name = "created_at")
    val createdAt: Long,
    
    @Json(name = "updated_at")
    val updatedAt: Long
)