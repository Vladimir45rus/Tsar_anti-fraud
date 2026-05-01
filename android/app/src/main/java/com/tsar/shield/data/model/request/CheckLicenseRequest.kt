package com.tsar.shield.data.model.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Запрос на проверку лицензии на сервере
 */
@JsonClass(generateAdapter = true)
data class CheckLicenseRequest(
    @Json(name = "device_hash")
    val deviceHash: String,
    
    @Json(name = "phone_hash")
    val phoneHash: String,
    
    @Json(name = "timestamp")
    val timestamp: Long,
    
    @Json(name = "signature")
    val signature: String? = null,
    
    @Json(name = "app_version")
    val appVersion: String,
    
    @Json(name = "android_version")
    val androidVersion: String,
    
    @Json(name = "device_model")
    val deviceModel: String,
    
    @Json(name = "manufacturer")
    val manufacturer: String
)