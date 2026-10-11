package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleHardwareState
import com.example.ble.DiscoveredPeerDevice
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshGreenConnected
import com.example.ui.theme.MeshNavyDark
import com.example.ui.theme.MeshRedDisconnect
import com.example.ui.theme.MeshSurfaceDark
import com.example.ui.theme.MeshSurfaceVariantDark

@Composable
fun MeshNetworkSection(
    bleHardwareState: BleHardwareState,
    isAdvertising: Boolean,
    isScanning: Boolean,
    discoveredPeers: List<DiscoveredPeerDevice>,
    onToggleAdvertising: () -> Unit,
    onToggleScanning: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSelectPeerForMessage: (DiscoveredPeerDevice) -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Warning / Status Alert Card (Permissions, Bluetooth Disabled, Hardware Missing)
        when (bleHardwareState) {
            BleHardwareState.HARDWARE_UNAVAILABLE -> {
                item {
                    AlertStatusCard(
                        icon = Icons.Default.ErrorOutline,
                        iconTint = MeshRedDisconnect,
                        title = "BLE Hardware Not Available",
                        message = "This device or virtual container does not have Bluetooth Low Energy hardware. Real phone-to-phone discovery requires a physical Android device.",
                        actionButtonText = null,
                        onAction = null
                    )
                }
            }
            BleHardwareState.PERMISSIONS_REQUIRED -> {
                item {
                    AlertStatusCard(
                        icon = Icons.Default.Security,
                        iconTint = MeshAmberWarning,
                        title = "Bluetooth Permissions Required",
                        message = "Android requires Nearby Devices (Bluetooth) permissions to find other phones and broadcast your mesh beacon.",
                        actionButtonText = "Grant Permissions",
                        onAction = onRequestPermissions
                    )
                }
            }
            BleHardwareState.BLUETOOTH_DISABLED -> {
                item {
                    AlertStatusCard(
                        icon = Icons.Default.BluetoothDisabled,
                        iconTint = MeshAmberWarning,
                        title = "Bluetooth Radio is Turned Off",
                        message = "Turn on Bluetooth in your device settings to discover nearby phones without internet or mobile data.",
                        actionButtonText = null,
                        onAction = null
                    )
                }
            }
            else -> {
                // Radio operational
            }
        }

        // Your Local Device Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MeshCyanPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MeshCyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Your Phone (Local Node)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusText = when {
                                    isAdvertising && isScanning -> "Beacon Active & Scanning..."
                                    isAdvertising -> "Discoverable to nearby phones (Beacon Active)"
                                    isScanning -> "Scanning radio spectrum (20s scan active)..."
                                    bleHardwareState == BleHardwareState.BLUETOOTH_DISABLED -> "Bluetooth turned off"
                                    bleHardwareState == BleHardwareState.PERMISSIONS_REQUIRED -> "Permissions needed"
                                    else -> "Offline • Ready to connect"
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAdvertising || isScanning) MeshCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        // Beacon Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isAdvertising) MeshCyanPrimary.copy(alpha = 0.15f)
                                    else MeshSurfaceVariantDark
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isAdvertising) "BEACON ON" else "STANDBY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdvertising) MeshCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Controls: Make Discoverable & Find Phones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleAdvertising,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("toggle_discoverable_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvertising) MeshAmberWarning else MeshCyanPrimary,
                                contentColor = MeshNavyDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAdvertising) Icons.Default.Close else Icons.Default.CellTower,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdvertising) "Stop Beacon" else "Make Discoverable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onToggleScanning,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("toggle_scan_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) MeshAmberWarning else MeshSurfaceVariantDark,
                                contentColor = if (isScanning) MeshNavyDark else MeshCyanPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MeshNavyDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "Stop Search" else "Find Phones",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mesh Service UUID: 0000FD42-0000-1000-8000-00805F9B34FB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section Title: Nearby Devices Found
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nearby Devices Found",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MeshSurfaceVariantDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${discoveredPeers.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isScanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MeshCyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanning (20s)...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshCyanPrimary,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Text(
                        text = "Real BLE Discovery",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Real Discovered Devices List
        if (discoveredPeers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (isScanning) MeshCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isScanning) "Actively scanning for phones broadcasting MeshLink..." else "No nearby phones detected yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isScanning) {
                                "Scanning radio frequencies for Service UUID 0xFD42. Tap 'Stop Search' anytime."
                            } else {
                                "Tap 'Find Phones' to scan. Make sure the other phone has MeshLink open and 'Make Discoverable' turned on."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(discoveredPeers, key = { it.address }) { peer ->
                RealDiscoveredPeerCard(
                    peer = peer,
                    onMessage = { onSelectPeerForMessage(peer) }
                )
            }
        }

        // Educational / Mesh Concept Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MeshCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How MeshLink Discovery Works",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ConceptExplanationRow(
                        step = "1. Dedicated Service Beacon",
                        desc = "MeshLink devices advertise a unique 128-bit Service UUID (0xFD42) so only MeshLink nodes detect each other."
                    )
                    ConceptExplanationRow(
                        step = "2. Zero Internet Required",
                        desc = "Discovery happens entirely on local Bluetooth Low Energy channels without cellular data or Wi-Fi."
                    )
                    ConceptExplanationRow(
                        step = "3. Discovery vs. Connection",
                        desc = "Discovered devices are in radio range (~30m). A GATT connection is opened on-demand when sending a message."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenDiagnostics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_developer_spec_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MeshCyanPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Technical Diagnostics & Specs", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RealDiscoveredPeerCard(
    peer: DiscoveredPeerDevice,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MeshCyanPrimary.copy(alpha = 0.35f))
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MeshCyanPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MeshCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = peer.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = peer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = onMessage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MeshCyanPrimary,
                        contentColor = MeshNavyDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("message_peer_${peer.address}")
                ) {
                    Text("Direct Ping", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Row: Signal strength & Unconnected status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Quality
                val signalQuality = when {
                    peer.rssi > -65 -> "Strong Signal"
                    peer.rssi > -80 -> "Good Signal"
                    else -> "Weak Signal"
                }
                val signalColor = when {
                    peer.rssi > -65 -> MeshGreenConnected
                    peer.rssi > -80 -> MeshCyanPrimary
                    else -> MeshAmberWarning
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(signalColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$signalQuality (${peer.rssi} dBm)",
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor,
                        fontSize = 11.sp
                    )
                }

                // Explicit Connection State: "Discovered • Unconnected"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeshSurfaceVariantDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Discovered (In Range)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    actionButtonText: String?,
    onAction: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(iconTint.copy(alpha = 0.5f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = iconTint,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (actionButtonText != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iconTint,
                        contentColor = MeshNavyDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = actionButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConceptExplanationRow(step: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelSmall,
            color = MeshCyanPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
        )
    }
}
