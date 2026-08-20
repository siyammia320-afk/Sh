package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.TrafficManager
import com.example.ui.components.FloatingOverlayView
import com.example.ui.theme.MyApplicationTheme

class FloatingOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_OVERLAY"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_overlay_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        TrafficManager.startMonitoring(this)
        TrafficManager.setOverlayRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopFloatingOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        initFloatingWindow()

        return START_STICKY
    }

    private fun initFloatingWindow() {
        if (floatingView != null) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 60
                y = 350
            }
            layoutParams = params

            val owner = OverlayLifecycleOwner()
            owner.onCreate()
            lifecycleOwner = owner

            val composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)

                setContent {
                    MyApplicationTheme {
                        FloatingOverlayView(
                            onDragDelta = { dx, dy ->
                                updateWindowPosition(dx, dy)
                            },
                            onOpenApp = {
                                val intent = Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                            },
                            onCloseOverlay = {
                                stopFloatingOverlay()
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                stopSelf()
                            }
                        )
                    }
                }
            }

            floatingView = composeView
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowPosition(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        val wm = windowManager ?: return
        val view = floatingView ?: return

        params.x = (params.x + dx.toInt()).coerceAtLeast(0)
        params.y = (params.y + dy.toInt()).coerceAtLeast(0)

        try {
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {
            // ignore layout sync exceptions during quick drag
        }
    }

    private fun stopFloatingOverlay() {
        try {
            floatingView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (_: Exception) {}

        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        floatingView = null
        layoutParams = null
        TrafficManager.setOverlayRunning(false)
    }

    override fun onDestroy() {
        stopFloatingOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traffic Switch Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating traffic switch button active"
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
            .setContentTitle("Traffic Switch Active")
            .setContentText("Floating button is on screen over all apps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
