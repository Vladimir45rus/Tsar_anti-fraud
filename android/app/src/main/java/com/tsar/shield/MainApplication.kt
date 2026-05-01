package com.tsar.shield

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactHost
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import expo.modules.ApplicationLifecycleDispatcher
import expo.modules.ReactNativeHostWrapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.tsar.shield.data.database.AppDatabase

@HiltAndroidApp
class MainApplication : Application(), ReactApplication, Configuration.Provider {

  companion object {
    private const val TAG = "MainApplication"
  }

  @Inject
  lateinit var workerFactory: HiltWorkerFactory

  override val reactNativeHost: ReactNativeHost = ReactNativeHostWrapper(
        this,
        object : DefaultReactNativeHost(this) {
          override fun getPackages(): List<ReactPackage> {
            return PackageList(this).packages
          }

          override fun getJSMainModuleName(): String = ".expo/.virtual-metro-entry"

          override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

          override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
          override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }
  )

  override val reactHost: ReactHost
    get() = ReactNativeHostWrapper.createReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "Application onCreate")
    
    // Инициализация SoLoader для React Native
    SoLoader.init(this, false)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      load()
    }
    
    // Инициализация Expo
    ApplicationLifecycleDispatcher.onApplicationCreate(this)
    
    // Инициализация базы данных
    AppDatabase.getInstance(this)
    
    // Логирование информации о приложении
    logAppInfo()
    
    Log.d(TAG, "App components initialized")
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig)
  }

  override fun getWorkManagerConfiguration(): Configuration {
    return Configuration.Builder()
      .setWorkerFactory(workerFactory)
      .setMinimumLoggingLevel(Log.INFO)
      .build()
  }

  private fun logAppInfo() {
    Log.d(TAG, "App Info:")
    Log.d(TAG, "  Package: ${packageName}")
    try {
      Log.d(TAG, "  Version: ${packageManager.getPackageInfo(packageName, 0).versionName}")
    } catch (e: Exception) {
      Log.e(TAG, "Error getting version info", e)
    }
    Log.d(TAG, "  SDK: ${android.os.Build.VERSION.SDK_INT}")
    Log.d(TAG, "  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
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
