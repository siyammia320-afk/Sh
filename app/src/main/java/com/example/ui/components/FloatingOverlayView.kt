package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FloatingOverlayView(
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKilled by TrafficManager.isTrafficKilled.collectAsState()
    val isRampingUp by TrafficManager.isRampingUp.collectAsState()
    val rampProgress by TrafficManager.rampProgress.collectAsState()
    val downloadSpeed by TrafficManager.downloadSpeedBps.collectAsState()
    val uploadSpeed by TrafficManager.uploadSpeedBps.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    // Pulsing animation for killed / active state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentAccentColor by animateColorAsState(
        targetValue = when {
            isKilled -> CyberDanger
            isRampingUp -> CyberWarning
            else -> CyberSuccess
        },
        label = "accent_color"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CyberBg.copy(alpha = 0.92f),
            shadowElevation = 10.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = currentAccentColor,
                    ambientColor = currentAccentColor
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            currentAccentColor,
                            currentAccentColor.copy(alpha = 0.4f),
                            CyberBorder
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Main Clickable Floating Toggle Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .testTag("floating_toggle_pill")
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberSurface.copy(alpha = 0.8f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = currentAccentColor)
                        ) {
                            if (isKilled) {
                                TrafficManager.restoreTraffic(com.example.MainActivity.appContext)
                            } else {
                                TrafficManager.killTraffic(com.example.MainActivity.appContext)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Status Beacon Icon / Indicator
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        currentAccentColor.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        if (isRampingUp) {
                            CircularProgressIndicator(
                                progress = { rampProgress },
                                modifier = Modifier.size(28.dp),
                                color = CyberWarning,
                                strokeWidth = 2.5.dp,
                                trackColor = CyberBorder
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .scale(if (isKilled) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(currentAccentColor)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            )
                        }

                        Icon(
                            imageVector = when {
                                isKilled -> Icons.Filled.PowerOff
                                isRampingUp -> Icons.Filled.ElectricBolt
                                else -> Icons.Filled.PowerSettingsNew
                            },
                            contentDescription = "Toggle Status",
                            tint = if (isRampingUp) CyberWarning else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Speed & Status Readout
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when {
                                    isKilled -> "00"
                                    isRampingUp -> String.format("%.0f%%", rampProgress * 100)
                                    else -> TrafficManager.formatSpeedValue(downloadSpeed + uploadSpeed)
                                },
                                color = if (isKilled) CyberDanger else TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = when {
                                    isKilled -> "KB/s"
                                    isRampingUp -> "RAMP"
                                    else -> TrafficManager.formatSpeedUnit(downloadSpeed + uploadSpeed)
                                },
                                color = currentAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = when {
                                isKilled -> "TAP TO RESTORE (2s)"
                                isRampingUp -> "RESTORING 2s..."
                                else -> "TAP TO KILL (00)"
                            },
                            color = if (isKilled) CyberDanger.copy(alpha = 0.9f) else TextSecondary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp
                        )
                    }

                    // Mini expand/menu chevron
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { isExpanded = !isExpanded }
                    ) {
                        Text(
                            text = if (isExpanded) "▲" else "▼",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Expandable Quick Action Bar
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(160.dp)
                        ) {
                            // Open App Button
                            IconButton(
                                onClick = {
                                    isExpanded = false
                                    onOpenApp()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInFull,
                                    contentDescription = "Open App",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Close Overlay Button
                            IconButton(
                                onClick = {
                                    isExpanded = false
                                    onCloseOverlay()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close Overlay",
                                    tint = CyberDanger,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Detailed DL / UL readout in expanded mode
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .width(150.dp)
                                .padding(top = 2.dp)
                        ) {
                            Text(
                                text = "⬇ ${TrafficManager.formatSpeed(downloadSpeed)}",
                                color = CyberPrimary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "⬆ ${TrafficManager.formatSpeed(uploadSpeed)}",
                                color = CyberAccent,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
