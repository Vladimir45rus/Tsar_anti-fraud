package com.tsarshield.data.database.dao

import androidx.room.*
import com.tsarshield.data.database.entity.LicenseEntity
import com.tsarshield.data.model.LicenseStatus
import java.util.Date

/**
 * Data Access Object (DAO) для работы с лицензиями в базе данных Room
 */
@Dao
interface LicenseDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: LicenseEntity)
    
    @Update
    suspend fun updateLicense(license: LicenseEntity)
    
    @Delete
    suspend fun deleteLicense(license: LicenseEntity)
    
    @Query("SELECT * FROM licenses WHERE licenseId = :licenseId")
    suspend fun getLicenseById(licenseId: String): LicenseEntity?
    
    @Query("SELECT * FROM licenses WHERE deviceHash = :deviceHash")
    suspend fun getLicenseByDeviceHash(deviceHash: String): LicenseEntity?
    
    @Query("SELECT * FROM licenses WHERE phoneHash = :phoneHash")
    suspend fun getLicensesByPhoneHash(phoneHash: String): List<LicenseEntity>
    
    @Query("SELECT * FROM licenses WHERE status = :status")
    suspend fun getLicensesByStatus(status: LicenseStatus): List<LicenseEntity>
    
    @Query("SELECT COUNT(*) FROM licenses WHERE status = 'PAID'")
    suspend fun getPaidLicenseCount(): Int
    
    @Query("""
        SELECT * FROM licenses 
        WHERE lastSync < :threshold 
        AND isSuspicious = 0
        ORDER BY lastSync ASC
        LIMIT :limit
    """)
    suspend fun getLicensesNeedingSync(
        threshold: Date,
        limit: Int = 10
    ): List<LicenseEntity>
    
    @Query("""
        SELECT * FROM licenses 
        WHERE gracePeriodEnd IS NOT NULL 
        AND gracePeriodEnd < :now
        AND status = 'EXPIRED'
    """)
    suspend fun getLicensesWithExpiredGracePeriod(now: Date): List<LicenseEntity>
    
    @Query("SELECT COUNT(*) FROM licenses")
    suspend fun getTotalLicenseCount(): Int
    
    @Query("DELETE FROM licenses WHERE licenseId = :licenseId")
    suspend fun deleteLicenseById(licenseId: String)
    
    @Query("DELETE FROM licenses")
    suspend fun deleteAllLicenses()
    
    @Query("""
        UPDATE licenses 
        SET lastSync = :syncTime 
        WHERE licenseId = :licenseId
    """)
    suspend fun updateLastSync(licenseId: String, syncTime: Date)
    
    @Query("""
        UPDATE licenses 
        SET status = :newStatus 
        WHERE licenseId = :licenseId
    """)
    suspend fun updateLicenseStatus(licenseId: String, newStatus: LicenseStatus)
    
    @Query("""
        SELECT * FROM licenses 
        WHERE expiresAt IS NOT NULL 
        AND expiresAt < :now 
        AND status IN ('PAID', 'TRIAL')
    """)
    suspend fun getExpiredLicenses(now: Date): List<LicenseEntity>
}