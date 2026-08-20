package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SpeedDataPoint
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun TrafficGraph(
    history: List<SpeedDataPoint>,
    isKilled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .testTag("traffic_graph_card")
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "REAL-TIME TRAFFIC FLOW",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DL Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isKilled) CyberDanger else CyberPrimary)
                        )
                        Text(
                            text = "Download",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // UL Legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isKilled) CyberDanger.copy(alpha = 0.5f) else CyberAccent)
                        )
                        Text(
                            text = "Upload",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0C121E))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    val gridLinesCount = 3
                    for (i in 1..gridLinesCount) {
                        val y = height * (i.toFloat() / (gridLinesCount + 1))
                        drawLine(
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    if (history.size < 2) return@Canvas

                    val maxSpeed = (history.maxOfOrNull { maxOf(it.downloadSpeedBps, it.uploadSpeedBps) } ?: 1024L)
                        .coerceAtLeast(1024L) // at least 1 KB/s for scale

                    val stepX = width / (history.size - 1).toFloat()

                    // Draw Download curve
                    val dlPath = Path()
                    val dlFillPath = Path()
                    dlFillPath.moveTo(0f, height)

                    history.forEachIndexed { index, point ->
                        val x = index * stepX
                        val normalizedY = 1f - (point.downloadSpeedBps.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
                        val y = normalizedY * (height - 12f) + 6f

                        if (index == 0) {
                            dlPath.moveTo(x, y)
                            dlFillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevY = (1f - (history[index - 1].downloadSpeedBps.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)) * (height - 12f) + 6f
                            val midX = (prevX + x) / 2
                            dlPath.cubicTo(midX, prevY, midX, y, x, y)
                            dlFillPath.cubicTo(midX, prevY, midX, y, x, y)
                        }
                    }
                    dlFillPath.lineTo(width, height)
                    dlFillPath.close()

                    // Draw DL gradient fill
                    val dlColor = if (isKilled) CyberDanger else CyberPrimary
                    drawPath(
                        path = dlFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                dlColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw DL stroke
                    drawPath(
                        path = dlPath,
                        color = dlColor,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )

                    // Draw UL curve
                    val ulColor = if (isKilled) CyberDanger.copy(alpha = 0.5f) else CyberAccent
                    val ulPath = Path()
                    history.forEachIndexed { index, point ->
                        val x = index * stepX
                        val normalizedY = 1f - (point.uploadSpeedBps.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
                        val y = normalizedY * (height - 12f) + 6f

                        if (index == 0) {
                            ulPath.moveTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevY = (1f - (history[index - 1].uploadSpeedBps.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)) * (height - 12f) + 6f
                            val midX = (prevX + x) / 2
                            ulPath.cubicTo(midX, prevY, midX, y, x, y)
                        }
                    }
                    drawPath(
                        path = ulPath,
                        color = ulColor,
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
