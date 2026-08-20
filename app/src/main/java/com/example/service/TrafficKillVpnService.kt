package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class TrafficKillVpnService : VpnService(), Runnable {

    companion object {
        const val ACTION_START_KILL = "com.example.service.ACTION_START_KILL"
        const val ACTION_STOP_KILL = "com.example.service.ACTION_STOP_KILL"
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "traffic_kill_vpn_channel"
        private const val TAG = "TrafficKillVpn"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_KILL) {
            startForeground(NOTIFICATION_ID, createNotification())
            startVpnSinkhole()
        } else if (action == ACTION_STOP_KILL) {
            stopVpnSinkhole()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startVpnSinkhole() {
        if (isRunning) return
        isRunning = true

        try {
            val builder = Builder()
                .setSession("TrafficSwitchBlackhole")
                .addAddress("10.254.1.2", 32)
                .addRoute("0.0.0.0", 0) // Route all IPv4 traffic into blackhole
                .addRoute("::", 0)       // Route all IPv6 traffic into blackhole
                .setMtu(1500)
                .setBlocking(true)

            // Disallow our own app so we don't block our internal components if needed
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot exclude package: ${e.message}")
            }

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                vpnThread = Thread(this, "TrafficSinkholeThread").apply {
                    start()
                }
                Log.d(TAG, "VPN Sinkhole successfully established, traffic dropped to 00")
            } else {
                Log.e(TAG, "VPN establish() returned null. User may have revoked permission.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN sinkhole: ${e.message}", e)
        }
    }

    override fun run() {
        val pfd = vpnInterface ?: return
        val inStream = FileInputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32768)

        try {
            while (isRunning && !Thread.currentThread().isInterrupted) {
                val length = inStream.read(buffer)
                if (length > 0) {
                    // Intentionally blackhole / discard all packets without forwarding
                    // This causes all device upload/download to stall instantly at 00 KB/s
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "VPN stream closed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "VPN loop error: ${e.message}")
        } finally {
            try {
                inStream.close()
            } catch (_: Exception) {}
        }
    }

    private fun stopVpnSinkhole() {
        isRunning = false
        vpnThread?.interrupt()
        vpnThread = null

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null
        Log.d(TAG, "VPN Sinkhole stopped. Network traffic restored.")
    }

    override fun onDestroy() {
        stopVpnSinkhole()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traffic Cutoff Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the traffic kill switch"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Traffic Cutoff Active")
            .setContentText("All upload and download traffic dropped to 00 KB/s")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
