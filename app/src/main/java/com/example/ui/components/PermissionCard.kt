package com.example.ui.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PermissionCard(
    hasOverlayPermission: Boolean,
    hasVpnPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestVpn: () -> Unit,
    onRequestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGranted = hasOverlayPermission && hasVpnPermission && hasNotificationPermission

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .testTag("permission_card")
            .fillMaxWidth()
            .border(
                1.dp,
                if (allGranted) CyberSuccess.copy(alpha = 0.5f) else CyberWarning.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
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
                    text = "REQUIRED PERMISSIONS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (allGranted) CyberSuccess.copy(alpha = 0.15f)
                            else CyberWarning.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (allGranted) "ALL GRANTED" else "ACTION NEEDED",
                        color = if (allGranted) CyberSuccess else CyberWarning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Item 1: Overlay
            PermissionRowItem(
                title = "Display Over Other Apps",
                subtitle = "Enables floating button over games & apps",
                icon = Icons.Filled.Layers,
                isGranted = hasOverlayPermission,
                onRequest = onRequestOverlay,
                testTag = "btn_grant_overlay"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Item 2: VPN / Traffic Control
            PermissionRowItem(
                title = "Traffic Cutoff (VPN Tunnel)",
                subtitle = "Allows instant packet drop to 00 KB/s",
                icon = Icons.Filled.VpnKey,
                isGranted = hasVpnPermission,
                onRequest = onRequestVpn,
                testTag = "btn_grant_vpn"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Item 3: Notification
            PermissionRowItem(
                title = "Background Notification",
                subtitle = "Keeps the floating switch active in background",
                icon = Icons.Filled.Notifications,
                isGranted = hasNotificationPermission,
                onRequest = onRequestNotification,
                testTag = "btn_grant_notification"
            )
        }
    }
}

@Composable
private fun PermissionRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) CyberSuccess.copy(alpha = 0.15f)
                        else CyberPrimary.copy(alpha = 0.15f)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) CyberSuccess else CyberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CyberSuccess.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Granted",
                    tint = CyberSuccess,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 4.dp
                ),
                modifier = Modifier
                    .testTag(testTag)
                    .height(30.dp)
            ) {
                Text(
                    text = "Grant",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
