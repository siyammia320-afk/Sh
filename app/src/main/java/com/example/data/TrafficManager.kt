package com.example.data

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.service.TrafficKillVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class SpeedDataPoint(
    val timestamp: Long,
    val downloadSpeedBps: Long,
    val uploadSpeedBps: Long
)

object TrafficManager {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null
    private var rampUpJob: Job? = null

    // State flows
    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning = _isOverlayRunning.asStateFlow()

    private val _isTrafficKilled = MutableStateFlow(false)
    val isTrafficKilled = _isTrafficKilled.asStateFlow()

    private val _isRampingUp = MutableStateFlow(false)
    val isRampingUp = _isRampingUp.asStateFlow()

    private val _rampProgress = MutableStateFlow(1f)
    val rampProgress = _rampProgress.asStateFlow()

    private val _downloadSpeedBps = MutableStateFlow(0L)
    val downloadSpeedBps = _downloadSpeedBps.asStateFlow()

    private val _uploadSpeedBps = MutableStateFlow(0L)
    val uploadSpeedBps = _uploadSpeedBps.asStateFlow()

    private val _totalCutoffsCount = MutableStateFlow(0)
    val totalCutoffsCount = _totalCutoffsCount.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedDataPoint>>(emptyList())
    val speedHistory = _speedHistory.asStateFlow()

    private var lastRxBytes: Long = 0L
    private var lastTxBytes: Long = 0L
    private var lastTime: Long = 0L

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun startMonitoring(context: Context) {
        if (monitorJob?.isActive == true) return

        lastRxBytes = TrafficStats.getTotalRxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L
        lastTxBytes = TrafficStats.getTotalTxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L
        lastTime = SystemClock.elapsedRealtime()

        monitorJob = scope.launch {
            while (isActive) {
                delay(400)
                updateSpeed()
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun updateSpeed() {
        val currentTime = SystemClock.elapsedRealtime()
        val timeDiff = (currentTime - lastTime).coerceAtLeast(1) // in ms

        val currentRx = TrafficStats.getTotalRxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L
        val currentTx = TrafficStats.getTotalTxBytes().takeIf { it != TrafficStats.UNSUPPORTED.toLong() } ?: 0L

        val rxDiff = (currentRx - lastRxBytes).coerceAtLeast(0)
        val txDiff = (currentTx - lastTxBytes).coerceAtLeast(0)

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTime = currentTime

        if (_isTrafficKilled.value) {
            // Traffic is completely dropped to 00 immediately (< 1s)
            _downloadSpeedBps.value = 0L
            _uploadSpeedBps.value = 0L
        } else {
            val rawRxBps = (rxDiff * 1000L) / timeDiff
            val rawTxBps = (txDiff * 1000L) / timeDiff

            val multiplier = if (_isRampingUp.value) _rampProgress.value else 1f
            val finalRx = (rawRxBps * multiplier).toLong()
            val finalTx = (rawTxBps * multiplier).toLong()

            _downloadSpeedBps.value = finalRx
            _uploadSpeedBps.value = finalTx
        }

        // Keep 30 data points for smooth visual graph
        val currentList = _speedHistory.value.toMutableList()
        if (currentList.size > 30) {
            currentList.removeAt(0)
        }
        currentList.add(
            SpeedDataPoint(
                timestamp = System.currentTimeMillis(),
                downloadSpeedBps = _downloadSpeedBps.value,
                uploadSpeedBps = _uploadSpeedBps.value
            )
        )
        _speedHistory.value = currentList
    }

    /**
     * Instantly cuts all download and upload traffic to 00 (< 1s)
     */
    fun killTraffic(context: Context) {
        rampUpJob?.cancel()
        _isRampingUp.value = false
        _rampProgress.value = 0f
        _isTrafficKilled.value = true
        _downloadSpeedBps.value = 0L
        _uploadSpeedBps.value = 0L
        _totalCutoffsCount.value += 1

        vibrateFeedback(context, isKill = true)

        // Start TrafficKillVpnService to sinkhole all packets
        val intent = Intent(context, TrafficKillVpnService::class.java).apply {
            action = TrafficKillVpnService.ACTION_START_KILL
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // fallback
        }
    }

    /**
     * Restores traffic with a 2-second gradual ramp-up as requested by user
     */
    fun restoreTraffic(context: Context) {
        _isTrafficKilled.value = false
        _isRampingUp.value = true
        _rampProgress.value = 0.05f

        vibrateFeedback(context, isKill = false)

        // Stop the VPN Sinkhole to allow normal network traffic
        val intent = Intent(context, TrafficKillVpnService::class.java).apply {
            action = TrafficKillVpnService.ACTION_STOP_KILL
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {
            // ignore
        }

        // Perform 2-second smooth ramp-up (2000ms total, updating every 50ms = 40 steps)
        rampUpJob?.cancel()
        rampUpJob = scope.launch {
            val totalDurationMs = 2000L
            val stepMs = 50L
            val totalSteps = (totalDurationMs / stepMs).toInt()

            for (i in 1..totalSteps) {
                delay(stepMs)
                val progress = (i.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                // Ease out cubic ramp for smooth natural recovery
                val easedProgress = 1f - Math.pow((1.0 - progress), 2.0).toFloat()
                _rampProgress.value = easedProgress
            }

            _isRampingUp.value = false
            _rampProgress.value = 1f
        }
    }

    fun toggleTraffic(context: Context) {
        if (_isTrafficKilled.value) {
            restoreTraffic(context)
        } else {
            killTraffic(context)
        }
    }

    private fun vibrateFeedback(context: Context, isKill: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isKill) {
                    // Strong single buzz on kill
                    vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // Double pulse buzz on restore
                    val timings = longArrayOf(0, 40, 60, 60)
                    val amplitudes = intArrayOf(0, 180, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isKill) 80 else 120)
            }
        } catch (_: Exception) {
            // Ignore vibration errors if permission not granted
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (_isTrafficKilled.value) return "00 KB/s"
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec B/s"
            bytesPerSec < 1024 * 1024 -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
            else -> String.format(Locale.US, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
        }
    }

    fun formatSpeedValue(bytesPerSec: Long): String {
        if (_isTrafficKilled.value) return "0.00"
        return when {
            bytesPerSec < 1024 -> "$bytesPerSec"
            bytesPerSec < 1024 * 1024 -> String.format(Locale.US, "%.1f", bytesPerSec / 1024.0)
            else -> String.format(Locale.US, "%.2f", bytesPerSec / (1024.0 * 1024.0))
        }
    }

    fun formatSpeedUnit(bytesPerSec: Long): String {
        if (_isTrafficKilled.value) return "KB/s"
        return when {
            bytesPerSec < 1024 -> "B/s"
            bytesPerSec < 1024 * 1024 -> "KB/s"
            else -> "MB/s"
        }
    }
}
