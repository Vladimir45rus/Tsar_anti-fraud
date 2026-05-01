package com.tsar.shield.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Сущность триггерной фразы для обнаружения социальной инженерии
 * Хранится в локальной базе данных и синхронизируется с сервером
 */
@Entity(tableName = "triggers")
data class TriggerEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "trigger_id")
    val id: String, // Уникальный идентификатор триггера (например, "bank_transfer")
    
    @ColumnInfo(name = "phrase")
    val phrase: String, // Сама фраза для поиска ("банк", "перевод")
    
    @ColumnInfo(name = "category")
    val category: String, // Категория: "финансы", "угрозы", "личные данные"
    
    @ColumnInfo(name = "weight")
    val weight: Float, // Вес триггера (0.0-1.0) для определения опасности
    
    @ColumnInfo(name = "synonyms")
    val synonyms: String, // JSON массив синонимов ["карта", "счет", "деньги"]
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean, // Активен ли триггер
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    
    @ColumnInfo(name = "version")
    val version: Int // Версия триггера для синхронизации
)