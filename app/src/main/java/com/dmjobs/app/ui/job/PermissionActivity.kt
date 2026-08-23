package com.dmjobs.app.ui.job

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dmjobs.app.databinding.ActivityPermissionBinding
import com.dmjobs.app.service.WhatsAppAccessibilityService

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateUI()

        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.btnOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }

        binding.btnBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        }

        binding.btnContinue.setOnClickListener {
            if (allPermissionsGranted()) {
                startActivity(Intent(this, MessagingProgressActivity::class.java))
                finish()
            } else {
                updateUI()
                binding.tvError.text = "Please grant all permissions to continue"
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val accessOk = isAccessibilityEnabled()
        val overlayOk = Settings.canDrawOverlays(this)
        val batteryOk = isBatteryOptimizationDisabled()

        binding.ivAccessibilityCheck.setImageResource(
            if (accessOk) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background
        )
        binding.ivOverlayCheck.setImageResource(
            if (overlayOk) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background
        )
        binding.ivBatteryCheck.setImageResource(
            if (batteryOk) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background
        )

        binding.btnContinue.isEnabled = accessOk && overlayOk && batteryOk
    }

    private fun allPermissionsGranted() =
        isAccessibilityEnabled() && Settings.canDrawOverlays(this) && isBatteryOptimizationDisabled()

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(
            "${packageName}/${WhatsAppAccessibilityService::class.java.name}"
        )
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
}

