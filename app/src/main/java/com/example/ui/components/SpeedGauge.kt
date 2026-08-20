package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TrafficManager
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SpeedGauge(
    isKilled: Boolean,
    isRampingUp: Boolean,
    rampProgress: Float,
    downloadSpeedBps: Long,
    uploadSpeedBps: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gauge_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val currentStatusColor by animateColorAsState(
        targetValue = when {
            isKilled -> CyberDanger
            isRampingUp -> CyberWarning
            else -> CyberSuccess
        },
        label = "status_color"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .testTag("speed_gauge_card")
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(currentStatusColor.copy(alpha = 0.6f), CyberBorder)
                ),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Status Header Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(currentStatusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(currentStatusColor)
                    )
                    Text(
                        text = when {
                            isKilled -> "TRAFFIC KILLED • 00 KB/s"
                            isRampingUp -> "RAMPING UP • ${(rampProgress * 100).toInt()}%"
                            else -> "NETWORK FLOW • ACTIVE"
                        },
                        color = currentStatusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (isKilled) "OFFLINE" else if (isRampingUp) "RAMP 2s" else "ONLINE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Digital Speed Displays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Speed Block
                SpeedMetricBox(
                    title = "DOWNLOAD",
                    speedValue = TrafficManager.formatSpeedValue(downloadSpeedBps),
                    speedUnit = TrafficManager.formatSpeedUnit(downloadSpeedBps),
                    icon = Icons.Filled.ArrowDownward,
                    accentColor = CyberPrimary,
                    isKilled = isKilled,
                    modifier = Modifier.weight(1f)
                )

                // Upload Speed Block
                SpeedMetricBox(
                    title = "UPLOAD",
                    speedValue = TrafficManager.formatSpeedValue(uploadSpeedBps),
                    speedUnit = TrafficManager.formatSpeedUnit(uploadSpeedBps),
                    icon = Icons.Filled.ArrowUpward,
                    accentColor = CyberAccent,
                    isKilled = isKilled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Ramping visual progress bar (when restoring traffic over 2s)
            if (isRampingUp) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Restoring Network Throughput (2s)",
                            color = CyberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(rampProgress * 100).toInt()}%",
                            color = CyberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { rampProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberWarning,
                        trackColor = CyberSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedMetricBox(
    title: String,
    speedValue: String,
    speedUnit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isKilled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = speedValue,
                    color = if (isKilled) CyberDanger else TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = speedUnit,
                    color = if (isKilled) CyberDanger.copy(alpha = 0.8f) else accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
