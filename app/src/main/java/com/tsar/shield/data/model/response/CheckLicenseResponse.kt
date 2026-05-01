package com.tsar.shield.data.model.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tsar.shield.data.model.LicenseStatus
import java.util.Date

/**
 * Ответ сервера на запрос проверки лицензии
 */
@JsonClass(generateAdapter = true)
data class CheckLicenseResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "license_id")
    val licenseId: String?,
    
    @Json(name = "status")
    val status: LicenseStatus,
    
    @Json(name = "plan_type")
    val planType: String?,
    
    @Json(name = "activated_at")
    val activatedAt: Long?,
    
    @Json(name = "expires_at")
    val expiresAt: Long?,
    
    @Json(name = "grace_period_end")
    val gracePeriodEnd: Long?,
    
    @Json(name = "transfer_count")
    val transferCount: Int = 0,
    
    @Json(name = "emergency_transfer_count")
    val emergencyTransferCount: Int = 0,
    
    @Json(name = "is_suspicious")
    val isSuspicious: Boolean = false,
    
    @Json(name = "jwt_token")
    val jwtToken: String?,
    
    @Json(name = "token_expiry")
    val tokenExpiry: Long?,
    
    @Json(name = "metadata")
    val metadata: String?,
    
    @Json(name = "message")
    val message: String?,
    
    @Json(name = "next_sync_recommended")
    val nextSyncRecommended: Long?,
    
    @Json(name = "server_time")
    val serverTime: Long
) {
    /**
     * Проверяет, действителен ли JWT токен
     */
    fun isTokenValid(): Boolean {
        if (jwtToken.isNullOrEmpty() || tokenExpiry == null) {
            return false
        }
        
        val now = Date().time
        return tokenExpiry > now
    }
    
    /**
     * Получает дату истечения срока действия лицензии
     */
    fun getExpiresAtDate(): Date? {
        return expiresAt?.let { Date(it) }
    }
    
    /**
     * Получает дату окончания grace period
     */
    fun getGracePeriodEndDate(): Date? {
        return gracePeriodEnd?.let { Date(it) }
    }
}