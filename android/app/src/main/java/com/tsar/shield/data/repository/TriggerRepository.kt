package com.tsar.shield.data.repository

import com.tsar.shield.data.database.dao.TriggerDao
import com.tsar.shield.data.database.entity.TriggerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для работы с триггерными фразами
 * Обеспечивает доступ к локальной базе данных и синхронизацию с сервером
 */
@Singleton
class TriggerRepository @Inject constructor(
    private val triggerDao: TriggerDao
) {
    
    suspend fun getAllTriggers(): List<TriggerEntity> {
        return triggerDao.getAllActiveTriggers()
            .map { it }
            .let { flow -> 
                // Преобразуем Flow в List (для простоты)
                // В реальном приложении нужно использовать Flow
                kotlin.runCatching { 
                    // Временная заглушка
                    emptyList()
                }.getOrDefault(emptyList())
            }
    }
    
    fun getActiveTriggersFlow(): Flow<List<TriggerEntity>> {
        return triggerDao.getAllActiveTriggers()
    }
    
    suspend fun getTriggersByCategory(category: String): List<TriggerEntity> {
        return triggerDao.getTriggersByCategory(category)
    }
    
    suspend fun searchTriggers(keyword: String): List<TriggerEntity> {
        return triggerDao.searchTriggers(keyword)
    }
    
    suspend fun getTriggerById(id: String): TriggerEntity? {
        return triggerDao.getTriggerById(id)
    }
    
    suspend fun insertTrigger(trigger: TriggerEntity) {
        triggerDao.insertTrigger(trigger)
    }
    
    suspend fun insertAllTriggers(triggers: List<TriggerEntity>) {
        triggerDao.insertAllTriggers(triggers)
    }
    
    suspend fun updateTrigger(trigger: TriggerEntity) {
        triggerDao.updateTrigger(trigger)
    }
    
    suspend fun deleteTrigger(id: String) {
        triggerDao.deleteTrigger(id)
    }
    
    suspend fun deleteAllTriggers() {
        triggerDao.deleteAllTriggers()
    }
    
    suspend fun getTriggersCount(): Int {
        return triggerDao.getTriggersCount()
    }
    
    suspend fun getMaxVersion(): Int? {
        return triggerDao.getMaxVersion()
    }
    
    suspend fun getTriggersUpdatedAfter(version: Int): List<TriggerEntity> {
        return triggerDao.getTriggersUpdatedAfter(version)
    }
    
    /**
     * Проверяет текст на наличие триггерных фраз
     * Возвращает список найденных триггеров с их весами
     */
    suspend fun detectTriggers(text: String): List<Pair<TriggerEntity, Float>> {
        val triggers = triggerDao.getAllActiveTriggers()
            .map { it }
            .let { flow ->
                // Временная заглушка
                emptyList()
            }
        
        val detected = mutableListOf<Pair<TriggerEntity, Float>>()
        val lowerText = text.lowercase()
        
        triggers.forEach { trigger ->
            val phrase = trigger.phrase.lowercase()
            if (lowerText.contains(phrase)) {
                detected.add(trigger to trigger.weight)
            }
            // Также проверяем синонимы (упрощённо)
            val synonyms = trigger.synonyms.split(",").map { it.trim().lowercase() }
            synonyms.forEach { synonym ->
                if (synonym.isNotEmpty() && lowerText.contains(synonym)) {
                    detected.add(trigger to trigger.weight * 0.8f) // Вес синонима меньше
                }
            }
        }
        
        return detected.distinctBy { it.first.id }
    }
    
    /**
     * Вычисляет общий уровень опасности текста на основе найденных триггеров
     * @return значение от 0.0 (безопасно) до 1.0 (очень опасно)
     */
    suspend fun calculateDangerLevel(text: String): Float {
        val detected = detectTriggers(text)
        if (detected.isEmpty()) return 0.0f
        
        val totalWeight = detected.sumOf { it.second.toDouble() }.toFloat()
        val maxPossibleWeight = detected.size * 1.0f // Максимальный вес за триггер
        
        return (totalWeight / maxPossibleWeight).coerceIn(0.0f, 1.0f)
    }
}