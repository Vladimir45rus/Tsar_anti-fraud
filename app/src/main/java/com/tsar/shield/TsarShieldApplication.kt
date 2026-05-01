package com.tsar.shield

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tsar.shield.data.database.AppDatabase
import com.tsar.shield.di.AppModule
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TsarShieldApplication : Application(), Configuration.Provider {
    
    companion object {
        private const val TAG = "TsarShieldApplication"
    }
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        
        // Инициализация базы данных
        AppDatabase.getInstance(this)
        
        // Инициализация других компонентов
        initializeAppComponents()
        
        // Логирование информации о приложении
        logAppInfo()
    }
    
    private fun initializeAppComponents() {
        // Здесь будет инициализация других компонентов приложения
        // Например: Crashlytics, Analytics, и т.д.
        
        Log.d(TAG, "App components initialized")
    }
    
    private fun logAppInfo() {
        Log.d(TAG, "App Info:")
        Log.d(TAG, "  Package: ${packageName}")
        Log.d(TAG, "  Version: ${packageManager.getPackageInfo(packageName, 0).versionName}")
        Log.d(TAG, "  SDK: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
    
    override fun onTerminate() {
        Log.d(TAG, "Application onTerminate")
        super.onTerminate()
    }
    
    override fun onLowMemory() {
        Log.w(TAG, "Application onLowMemory")
        super.onLowMemory()
    }
    
    override fun onTrimMemory(level: Int) {
        Log.d(TAG, "Application onTrimMemory level: $level")
        super.onTrimMemory(level)
    }
}