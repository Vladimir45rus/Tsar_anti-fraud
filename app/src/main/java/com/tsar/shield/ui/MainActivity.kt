package com.tsar.shield.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tsar.shield.R
import com.tsar.shield.databinding.ActivityMainBinding
import com.tsar.shield.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        
        // Коды запросов разрешений
        private const val REQUEST_CODE_OVERLAY = 1001
        private const val REQUEST_CODE_CALL_SCREENING = 1002
    }
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    // Регистрация для запроса разрешений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val grantedPermissions = permissions.filter { it.value }.keys
        val deniedPermissions = permissions.filter { !it.value }.keys
        
        Log.d(TAG, "Granted permissions: $grantedPermissions")
        Log.d(TAG, "Denied permissions: $deniedPermissions")
        
        if (deniedPermissions.isNotEmpty()) {
            showPermissionRationale(deniedPermissions)
        } else {
            onAllPermissionsGranted()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d(TAG, "MainActivity created")
        
        setupUI()
        setupObservers()
        checkAndRequestPermissions()
    }
    
    private fun setupUI() {
        binding.toolbar.title = getString(R.string.app_name)
        setSupportActionBar(binding.toolbar)
        
        binding.btnStart.setOnClickListener {
            startProtection()
        }
        
        binding.btnSettings.setOnClickListener {
            openSettings()
        }
        
        binding.btnPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }
        
        binding.btnLicense.setOnClickListener {
            openLicenseScreen()
        }
        
        binding.btnTestCall.setOnClickListener {
            testCallDetection()
        }
    }
    
    private fun setupObservers() {
        viewModel.appStatus.observe(this) { status ->
            binding.tvStatus.text = status
        }
        
        viewModel.permissionStatus.observe(this) { permissions ->
            updatePermissionStatus(permissions)
        }
        
        viewModel.licenseStatus.observe(this) { license ->
            binding.tvLicenseStatus.text = license
        }
    }
    
    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $missingPermissions")
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All permissions granted")
            onAllPermissionsGranted()
        }
        
        // Проверка разрешения на оверлей (особый случай)
        checkOverlayPermission()
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
        
        return permissions
    }
    
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialog()
            }
        }
    }
    
    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.overlay_permission_title))
            .setMessage(getString(R.string.overlay_permission_message))
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton(getString(R.string.later), null)
            .show()
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
        }
    }
    
    private fun showPermissionRationale(deniedPermissions: Set<String>) {
        val message = buildString {
            append(getString(R.string.permission_rationale))
            append("\n\n")
            deniedPermissions.forEach { permission ->
                append("• ${getPermissionDescription(permission)}\n")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permissions_required))
            .setMessage(message)
            .setPositiveButton(getString(R.string.retry)) { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> getString(R.string.permission_microphone)
            Manifest.permission.READ_PHONE_STATE -> getString(R.string.permission_phone_state)
            Manifest.permission.READ_CONTACTS -> getString(R.string.permission_contacts)
            Manifest.permission.POST_NOTIFICATIONS -> getString(R.string.permission_notifications)
            Manifest.permission.READ_MEDIA_AUDIO -> getString(R.string.permission_media_audio)
            else -> permission
        }
    }
    
    private fun onAllPermissionsGranted() {
        Log.d(TAG, "All permissions granted, initializing services")
        
        // Инициализация служб
        viewModel.initializeServices()
        
        // Проверка лицензии
        viewModel.checkLicenseStatus()
        
        // Обновление UI
        binding.btnStart.isEnabled = true
        binding.tvStatus.text = getString(R.string.status_ready)
        
        Toast.makeText(this, R.string.all_permissions_granted, Toast.LENGTH_SHORT).show()
    }
    
    private fun updatePermissionStatus(permissions: Map<String, Boolean>) {
        val statusText = buildString {
            permissions.forEach { (permission, granted) ->
                val status = if (granted) "✅" else "❌"
                val description = getPermissionDescription(permission)
                append("$status $description\n")
            }
        }
        
        binding.tvPermissionStatus.text = statusText
    }
    
    private fun startProtection() {
        Log.d(TAG, "Starting protection")
        
        viewModel.startProtection().observe(this) { success ->
            if (success) {
                binding.tvStatus.text = getString(R.string.status_protection_active)
                Toast.makeText(this, R.string.protection_started, Toast.LENGTH_SHORT).show()
            } else {
                binding.tvStatus.text = getString(R.string.status_protection_failed)
                Toast.makeText(this, R.string.protection_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
    
    private fun openLicenseScreen() {
        val intent = Intent(this, LicenseActivity::class.java)
        startActivity(intent)
    }
    
    private fun testCallDetection() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.test_call_title))
            .setMessage(getString(R.string.test_call_message))
            .setPositiveButton(getString(R.string.start_test)) { _, _ ->
                viewModel.startTestCall()
                Toast.makeText(this, R.string.test_call_started, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_OVERLAY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, R.string.overlay_permission_granted, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            REQUEST_CODE_CALL_SCREENING -> {
                // Обработка результата запроса CallScreening
                Toast.makeText(this, R.string.call_screening_updated, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")
        
        // Обновление статуса при возвращении на экран
        viewModel.updateStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}