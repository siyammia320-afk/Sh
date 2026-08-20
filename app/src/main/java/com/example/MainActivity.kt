package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.data.TrafficManager
import com.example.service.FloatingOverlayService
import com.example.service.TrafficKillVpnService
import com.example.ui.TrafficSwitchScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    private var hasOverlayPermission by mutableStateOf(false)
    private var hasVpnPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)

    // VPN Preparation launcher
    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            hasVpnPermission = true
            Toast.makeText(this, "Traffic Cutoff permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            hasVpnPermission = false
            Toast.makeText(this, "VPN permission is required to cutoff traffic", Toast.LENGTH_LONG).show()
        }
    }

    // Overlay Permission launcher
    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissions()
        if (hasOverlayPermission) {
            Toast.makeText(this, "Floating Overlay permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Notification Permission launcher
    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        enableEdgeToEdge()

        TrafficManager.startMonitoring(this)

        setContent {
            MyApplicationTheme {
                TrafficSwitchScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    hasVpnPermission = hasVpnPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestVpn = { requestVpnPermission() },
                    onRequestNotification = { requestNotificationPermission() },
                    onStartService = { handleStart() },
                    onStopService = { handleStop() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        // Check overlay permission
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        // Check VPN preparation status
        val vpnIntent = VpnService.prepare(this)
        hasVpnPermission = (vpnIntent == null)

        // Check Notification permission
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            hasVpnPermission = true
            Toast.makeText(this, "Traffic Cutoff permission already granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleStart() {
        checkPermissions()

        if (!hasOverlayPermission) {
            Toast.makeText(this, "Please grant Overlay permission first", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }

        if (!hasVpnPermission) {
            Toast.makeText(this, "Please grant Traffic Cutoff (VPN) permission", Toast.LENGTH_SHORT).show()
            requestVpnPermission()
            return
        }

        if (!hasNotificationPermission) {
            requestNotificationPermission()
        }

        // Start the Floating Overlay Service
        val intent = Intent(this, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "Floating button active! Tap it to cutoff traffic to 00", Toast.LENGTH_LONG).show()
    }

    private fun handleStop() {
        // Stop Floating Overlay Service
        val overlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
            action = FloatingOverlayService.ACTION_STOP
        }
        startService(overlayIntent)

        // Restore network and stop VPN
        TrafficManager.restoreTraffic(this)

        Toast.makeText(this, "Floating switch stopped. Network restored.", Toast.LENGTH_SHORT).show()
    }
}
