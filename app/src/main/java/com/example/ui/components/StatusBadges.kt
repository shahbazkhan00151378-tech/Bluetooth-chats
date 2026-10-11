package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MessageDeliveryStatus
import com.example.model.PeerNode
import com.example.model.PeerStatus
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshGreenConnected
import com.example.ui.theme.MeshRedDisconnect

@Composable
fun HopRouteChip(peer: PeerNode, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (peer.status) {
        PeerStatus.DIRECT_NEIGHBOR -> Triple("1-Hop Direct BLE", MeshGreenConnected.copy(alpha = 0.15f), MeshGreenConnected)
        PeerStatus.MULTI_HOP_REACHABLE -> Triple("${peer.distanceHops} Hops (Relayed)", MeshAmberWarning.copy(alpha = 0.15f), MeshAmberWarning)
        PeerStatus.ROUTE_SEARCHING -> Triple("Discovering Route", MeshAmberWarning.copy(alpha = 0.15f), MeshAmberWarning)
        PeerStatus.OUT_OF_RANGE -> Triple("Offline / Unreachable", MeshRedDisconnect.copy(alpha = 0.15f), MeshRedDisconnect)
        PeerStatus.STANDBY -> Triple("Standby", Color.Gray.copy(alpha = 0.15f), Color.Gray)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DeliveryStatusIcon(status: MessageDeliveryStatus, modifier: Modifier = Modifier) {
    when (status) {
        MessageDeliveryStatus.PENDING,
        MessageDeliveryStatus.STORED_LOCALLY -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "Stored locally awaiting route",
                    tint = MeshAmberWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Pending", fontSize = 9.sp, color = MeshAmberWarning)
            }
        }
        MessageDeliveryStatus.SENT -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sent",
                    tint = MeshCyanPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Sent", fontSize = 9.sp, color = MeshCyanPrimary)
            }
        }
        MessageDeliveryStatus.DELIVERED,
        MessageDeliveryStatus.DELIVERED_PEER -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Delivered",
                    tint = MeshGreenConnected,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Delivered", fontSize = 9.sp, color = MeshGreenConnected)
            }
        }
        MessageDeliveryStatus.FAILED -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Delivery Failed",
                    tint = MeshRedDisconnect,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Failed", fontSize = 9.sp, color = MeshRedDisconnect)
            }
        }
        MessageDeliveryStatus.DISCOVERING_ROUTE -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = "Routing",
                    tint = MeshAmberWarning,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Routing...", fontSize = 9.sp, color = MeshAmberWarning)
            }
        }
        MessageDeliveryStatus.RELAYED_HOP -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Relayed",
                    tint = MeshCyanPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text("Relayed", fontSize = 9.sp, color = MeshCyanPrimary)
            }
        }
        MessageDeliveryStatus.BROADCAST_RECEIVED -> {
            Text("Broadcast", fontSize = 9.sp, color = MeshCyanPrimary, modifier = modifier)
        }
    }
}
