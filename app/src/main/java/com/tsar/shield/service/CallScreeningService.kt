package com.tsarshield.service

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import com.tsarshield.data.repository.LicenseRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Служба перехвата входящих вызовов (Android 10+)
 * Анализирует метаданные вызова и запускает STT анализ при необходимости
 */
@AndroidEntryPoint
class CallScreeningService : CallScreeningService() {
    
    companion object {
        private const val TAG = "CallScreeningService"
    }
    
    @Inject
    lateinit var licenseRepository: LicenseRepository
    
    override fun onScreenCall(callDetails: Call.Details) {
        Log.d(TAG, "Входящий вызов обнаружен: ${callDetails.handle}")
        
        val phoneNumber = callDetails.handle?.schemeSpecificPart
        val isContact = checkIfContact(phoneNumber)
        
        if (!isContact) {
            // Запуск STT движка для анализа аудио
            startSTTAnalysis(callDetails)
            
            // Показать оверлей с предупреждением
            showWarningOverlay(phoneNumber)
            
            // Заблокировать вызов если обнаружена угроза
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            
            respondToCall(callDetails, response)
            
            // Логирование события
            logCallEvent(phoneNumber, "blocked", "non_contact")
        } else {
            // Разрешить вызов от контакта
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()
            
            respondToCall(callDetails, response)
            
            // Логирование события
            logCallEvent(phoneNumber, "allowed", "contact")
        }
    }
    
    private fun checkIfContact(phoneNumber: String?): Boolean {
        // TODO: Реализовать проверку в контактах устройства
        // Временная реализация - всегда возвращать false для тестирования
        return false
    }
    
    private fun startSTTAnalysis(callDetails: Call.Details) {
        // Запуск фоновой службы STT для анализа аудио
        val intent = Intent(this, STTService::class.java)
        intent.putExtra("call_details", callDetails)
        ContextCompat.startForegroundService(this, intent)
        
        Log.d(TAG, "STT анализ запущен для вызова")
    }
    
    private fun showWarningOverlay(phoneNumber: String?) {
        // Показать оверлей с предупреждением
        val intent = Intent(this, OverlayService::class.java)
        intent.putExtra("phone_number", phoneNumber)
        intent.putExtra("warning_type", "social_engineering")
        ContextCompat.startForegroundService(this, intent)
        
        Log.d(TAG, "Оверлей предупреждения показан для номера: $phoneNumber")
    }
    
    private fun logCallEvent(phoneNumber: String?, action: String, reason: String) {
        // TODO: Реализовать логирование в локальную БД
        Log.i(TAG, "Событие вызова: номер=$phoneNumber, действие=$action, причина=$reason")
    }
}