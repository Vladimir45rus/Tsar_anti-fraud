package com.tsar.shield.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tsar.shield.R
import com.tsar.shield.data.repository.LicenseRepository
import com.tsar.shield.service.CallScreeningService
import com.tsar.shield.service.STTService
import com.tsar.shield.utils.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val licenseRepository: LicenseRepository,
    private val permissionChecker: PermissionChecker
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    private val _appStatus = MutableLiveData<String>()
    val appStatus: LiveData<String> = _appStatus
    
    private val _permissionStatus = MutableLiveData<Map<String, Boolean>>()
    val permissionStatus: LiveData<Map<String, Boolean>> = _permissionStatus
    
    private val _licenseStatus = MutableLiveData<String>()
    val licenseStatus: LiveData<String> = _licenseStatus
    
    private val _protectionStarted = MutableLiveData<Boolean>()
    val protectionStarted: LiveData<Boolean> = _protectionStarted
    
    init {
        Log.d(TAG, "MainViewModel initialized")
        updateStatus()
    }
    
    fun updateStatus() {
        viewModelScope.launch {
            updatePermissionStatus()
            updateLicenseStatus()
            updateServiceStatus()
        }
    }
    
    private suspend fun updatePermissionStatus() {
        withContext(Dispatchers.IO) {
            val permissions = permissionChecker.checkAllPermissions()
            _permissionStatus.postValue(permissions)
            
            val grantedCount = permissions.count { it.value }
            val totalCount = permissions.size
            
            val status = getApplication<Application>().getString(
                R.string.permission_status_format,
                grantedCount,
                totalCount
            )
            _appStatus.postValue(status)
        }
    }
    
    private suspend fun updateLicenseStatus() {
        withContext(Dispatchers.IO) {
            try {
                val status = licenseRepository.getLicenseStatus()
                _licenseStatus.postValue(status.name)
                
                val statusText = getApplication<Application>().getString(
                    R.string.license_status_format,
                    status.name
                )
                // Обновляем общий статус с информацией о лицензии
                _appStatus.postValue(statusText)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get license status", e)
                _licenseStatus.postValue("ERROR")
            }
        }
    }
    
    private fun updateServiceStatus() {
        val context = getApplication<Application>()
        
        val isCallScreeningRunning = isServiceRunning(CallScreeningService::class.java)
        val isSTTRunning = isServiceRunning(STTService::class.java)
        
        val status = if (isCallScreeningRunning && isSTTRunning) {
            context.getString(R.string.status_services_running)
        } else if (isCallScreeningRunning) {
            context.getString(R.string.status_call_screening_running)
        } else if (isSTTRunning) {
            context.getString(R.string.status_stt_running)
        } else {
            context.getString(R.string.status_services_stopped)
        }
        
        _appStatus.postValue(status)
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val context = getApplication<Application>()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
        
        return runningServices.any { it.service.className == serviceClass.name }
    }
    
    fun initializeServices() {
        viewModelScope.launch {
            try {
                // Инициализация STT службы
                initializeSTTService()
                
                // Инициализация CallScreening службы
                initializeCallScreeningService()
                
                Log.d(TAG, "Services initialized successfully")
                _appStatus.postValue(getApplication<Application>().getString(R.string.status_initialized))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize services", e)
                _appStatus.postValue(getApplication<Application>().getString(R.string.status_initialization_failed))
            }
        }
    }
    
    private suspend fun initializeSTTService() {
        withContext(Dispatchers.IO) {
            // Здесь будет инициализация STT движка
            // Пока просто логируем
            Log.d(TAG, "STT service initialization started")
            
            // Симуляция инициализации
            Thread.sleep(500)
            
            Log.d(TAG, "STT service initialized")
        }
    }
    
    private suspend fun initializeCallScreeningService() {
        withContext(Dispatchers.IO) {
            // Здесь будет инициализация CallScreening службы
            Log.d(TAG, "CallScreening service initialization started")
            
            // Проверка доступности CallScreening API
            val packageManager = getApplication<Application>().packageManager
            val hasCallScreening = packageManager.hasSystemFeature(PackageManager.FEATURE_TELECOM)
            
            if (hasCallScreening) {
                Log.d(TAG, "CallScreening API available")
            } else {
                Log.w(TAG, "CallScreening API not available, will use Accessibility")
            }
            
            // Симуляция инициализации
            Thread.sleep(300)
            
            Log.d(TAG, "CallScreening service initialized")
        }
    }
    
    fun startProtection(): LiveData<Boolean> {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting protection")
                
                // Запуск служб защиты
                startCallScreeningProtection()
                startSTTProtection()
                
                // Обновление статуса
                _protectionStarted.postValue(true)
                _appStatus.postValue(getApplication<Application>().getString(R.string.status_protection_active))
                
                Log.d(TAG, "Protection started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start protection", e)
                _protectionStarted.postValue(false)
                _appStatus.postValue(getApplication<Application>().getString(R.string.status_protection_failed))
            }
        }
        
        return _protectionStarted
    }
    
    private suspend fun startCallScreeningProtection() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting CallScreening protection")
            
            // Здесь будет реальный запуск CallScreening службы
            // Пока просто логируем
            Thread.sleep(200)
            
            Log.d(TAG, "CallScreening protection started")
        }
    }
    
    private suspend fun startSTTProtection() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting STT protection")
            
            // Здесь будет реальный запуск STT службы
            // Пока просто логируем
            Thread.sleep(200)
            
            Log.d(TAG, "STT protection started")
        }
    }
    
    fun checkLicenseStatus() {
        viewModelScope.launch {
            updateLicenseStatus()
        }
    }
    
    fun startTestCall() {
        viewModelScope.launch {
            Log.d(TAG, "Starting test call simulation")
            
            // Здесь будет симуляция тестового вызова
            // Пока просто логируем
            _appStatus.postValue(getApplication<Application>().getString(R.string.status_test_call))
            
            // Симуляция обработки вызова
            Thread.sleep(1000)
            
            // Симуляция обнаружения угрозы
            simulateThreatDetection()
            
            Log.d(TAG, "Test call simulation completed")
        }
    }
    
    private fun simulateThreatDetection() {
        Log.d(TAG, "Simulating threat detection")
        
        // Здесь будет логика симуляции обнаружения угрозы
        // Пока просто обновляем статус
        _appStatus.postValue(getApplication<Application>().getString(R.string.status_threat_detected))
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared")
    }
}