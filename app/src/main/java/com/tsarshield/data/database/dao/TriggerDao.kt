package com.tsarshield.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tsarshield.data.database.entity.TriggerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с триггерными фразами
 */
@Dao
interface TriggerDao {
    
    @Query("SELECT * FROM triggers WHERE is_active = 1 ORDER BY weight DESC")
    fun getAllActiveTriggers(): Flow<List<TriggerEntity>>
    
    @Query("SELECT * FROM triggers WHERE category = :category AND is_active = 1")
    suspend fun getTriggersByCategory(category: String): List<TriggerEntity>
    
    @Query("SELECT * FROM triggers WHERE phrase LIKE '%' || :keyword || '%' OR synonyms LIKE '%' || :keyword || '%'")
    suspend fun searchTriggers(keyword: String): List<TriggerEntity>
    
    @Query("SELECT * FROM triggers WHERE trigger_id = :id")
    suspend fun getTriggerById(id: String): TriggerEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: TriggerEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTriggers(triggers: List<TriggerEntity>)
    
    @Update
    suspend fun updateTrigger(trigger: TriggerEntity)
    
    @Query("DELETE FROM triggers WHERE trigger_id = :id")
    suspend fun deleteTrigger(id: String)
    
    @Query("DELETE FROM triggers")
    suspend fun deleteAllTriggers()
    
    @Query("SELECT COUNT(*) FROM triggers")
    suspend fun getTriggersCount(): Int
    
    @Query("SELECT MAX(version) FROM triggers")
    suspend fun getMaxVersion(): Int?
    
    @Query("SELECT * FROM triggers WHERE version > :version")
    suspend fun getTriggersUpdatedAfter(version: Int): List<TriggerEntity>
}