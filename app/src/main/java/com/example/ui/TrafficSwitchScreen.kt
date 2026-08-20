package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrafficManager
import com.example.ui.components.PermissionCard
import com.example.ui.components.SpeedGauge
import com.example.ui.components.TrafficGraph
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberPrimaryContainer
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficSwitchScreen(
    hasOverlayPermission: Boolean,
    hasVpnPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestVpn: () -> Unit,
    onRequestNotification: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOverlayRunning by TrafficManager.isOverlayRunning.collectAsState()
    val isKilled by TrafficManager.isTrafficKilled.collectAsState()
    val isRampingUp by TrafficManager.isRampingUp.collectAsState()
    val rampProgress by TrafficManager.rampProgress.collectAsState()
    val downloadSpeed by TrafficManager.downloadSpeedBps.collectAsState()
    val uploadSpeed by TrafficManager.uploadSpeedBps.collectAsState()
    val totalCutoffs by TrafficManager.totalCutoffsCount.collectAsState()
    val history by TrafficManager.speedHistory.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentAccentColor by animateColorAsState(
        targetValue = when {
            isKilled -> CyberDanger
            isRampingUp -> CyberWarning
            isOverlayRunning -> CyberPrimary
            else -> CyberSuccess
        },
        label = "screen_accent"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(currentAccentColor.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(if (isOverlayRunning) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(currentAccentColor)
                            )
                        }

                        Text(
                            text = "TRAFFIC SWITCH",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CyberBg
                )
            )
        },
        containerColor = CyberBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info & Subtitle
            item {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    Text(
                        text = "ফ্লোটিং ট্রাফিক কন্ট্রোলার",
                        color = CyberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Start চাপলে স্ক্রিনে ফ্লোটিং বাটন আসবে। বাটন ট্যাপ করলে সমস্ত আপলোড/ডাউনলোড ০ হয়ে যাবে, আবার ট্যাপ করলে ২ সেকেন্ডে রিকভার হবে।",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // PRIMARY CONTROLS: START & STOP BUTTONS
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    if (isOverlayRunning) CyberPrimary else CyberBorder,
                                    CyberSurfaceVariant
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "MAIN CONTROLS",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            // Status Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isOverlayRunning) CyberPrimary.copy(alpha = 0.15f)
                                        else CyberSurfaceVariant
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOverlayRunning) CyberPrimary else TextMuted)
                                )
                                Text(
                                    text = if (isOverlayRunning) "FLOATING ACTIVE" else "STOPPED",
                                    color = if (isOverlayRunning) CyberPrimary else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Start & Stop Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // START BUTTON
                            Button(
                                onClick = onStartService,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverlayRunning) CyberPrimaryContainer else CyberPrimary,
                                    contentColor = if (isOverlayRunning) CyberPrimary else Color(0xFF00151C)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .testTag("btn_start")
                                    .weight(1f)
                                    .height(56.dp)
                                    .shadow(
                                        elevation = if (!isOverlayRunning) 8.dp else 0.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = CyberPrimary
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Start",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "START",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // STOP BUTTON
                            Button(
                                onClick = onStopService,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOverlayRunning) CyberDanger else CyberSurfaceVariant,
                                    contentColor = if (isOverlayRunning) Color.White else TextMuted
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .testTag("btn_stop")
                                    .weight(1f)
                                    .height(56.dp)
                                    .shadow(
                                        elevation = if (isOverlayRunning) 8.dp else 0.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = CyberDanger
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "STOP",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Direct Manual Cutoff Toggle
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .testTag("direct_toggle_card")
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberSurfaceVariant)
                                .clickable {
                                    TrafficManager.toggleTraffic(context)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isKilled) CyberDanger.copy(alpha = 0.2f)
                                            else CyberSuccess.copy(alpha = 0.2f)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isKilled) Icons.Filled.PowerSettingsNew else Icons.Filled.Bolt,
                                        contentDescription = "Toggle",
                                        tint = if (isKilled) CyberDanger else CyberSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isKilled) "Restore Network (2s Ramp)" else "Instant Cutoff (< 1s to 00)",
                                        color = TextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isKilled) "Currently dropped to 00 KB/s" else "Simulate floating button tap directly",
                                        color = if (isKilled) CyberDanger else TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = if (isKilled) "RESTORE" else "KILL 00",
                                color = if (isKilled) CyberSuccess else CyberDanger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Real-Time Speedometer HUD
            item {
                SpeedGauge(
                    isKilled = isKilled,
                    isRampingUp = isRampingUp,
                    rampProgress = rampProgress,
                    downloadSpeedBps = downloadSpeed,
                    uploadSpeedBps = uploadSpeed
                )
            }

            // Real-Time Live Traffic Graph
            item {
                TrafficGraph(
                    history = history,
                    isKilled = isKilled
                )
            }

            // Permissions Status Checklist
            item {
                PermissionCard(
                    hasOverlayPermission = hasOverlayPermission,
                    hasVpnPermission = hasVpnPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestOverlay = onRequestOverlay,
                    onRequestVpn = onRequestVpn,
                    onRequestNotification = onRequestNotification
                )
            }

            // How It Works Guide
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "How to use",
                                tint = CyberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "HOW TO USE (ব্যবহার বিধি)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "1. সমস্ত পারমিশন দিন (Overlay + VPN + Notification)\n" +
                                    "2. START বাটনে চাপুন -> স্ক্রিনে একটি ফ্লোটিং বাটন চলে আসবে\n" +
                                    "3. ফ্লোটিং বাটনে ১ বার চাপলে সমস্ত ডাউনলোড ও আপলোড স্পিড সাথে সাথে 00 KB/s হয়ে যাবে\n" +
                                    "4. ফ্লোটিং বাটনে আবার চাপলে ঠিক ২ সেকেন্ডের মধ্যে ইন্টারনেট স্পিড স্বাভাবিক অবস্থায় ফিরে আসবে\n" +
                                    "5. STOP বাটনে চাপলে ফ্লোটিং বাটনটি বন্ধ হয়ে যাবে",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Cutoffs Triggered: $totalCutoffs",
                                color = CyberPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Ramp Duration: 2.0s",
                                color = CyberWarning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
