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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshNavyDark
import com.example.ui.theme.MeshSurfaceDark
import com.example.ui.theme.MeshSurfaceVariantDark

@Composable
fun NewPairDialog(
    onDismiss: () -> Unit
) {
    var manualNodeId by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: BLE Beacon, 1: QR Exchange, 2: Manual ID

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MeshCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            color = MeshSurfaceDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MeshCyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = "Discover Peer",
                                tint = MeshCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pair Nearby Mesh Node",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Phase 1 Architecture Preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Phase 1 Warning Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MeshSurfaceVariantDark)
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PrototypeBadge("Phase 1 UI Only")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Radio Scanning Not Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeshAmberWarning,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Browser & emulator environments do not provide direct hardware Bluetooth LE mesh radios. In Phase 2, native Android APIs will discover devices over physical 2.4 GHz BLE beacons.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DISCOVERY METHODS IN NATIVE ANDROID (PHASE 2):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                DiscoveryMethodCard(
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    title = "1. Proximity BLE Beacon",
                    subtitle = "Android BluetoothLeScanner scans for MeshLink Service UUID (0xFD42). Automatically registers direct 1-hop neighbors.",
                    nativeTag = "BluetoothLeScanner"
                )

                Spacer(modifier = Modifier.height(8.dp))

                DiscoveryMethodCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "2. QR Code Out-of-Band Exchange",
                    subtitle = "Phones scan each other's Ed25519 public key QR code offline. Prevents Man-in-the-Middle (MITM) attacks without a server.",
                    nativeTag = "CameraX + ML Kit / ZXing"
                )

                Spacer(modifier = Modifier.height(8.dp))

                DiscoveryMethodCard(
                    icon = Icons.Default.VpnKey,
                    title = "3. Static Node ID / Call Sign",
                    subtitle = "Exchange 6-character node hashes (e.g., NODE_9A4C) or pre-shared tactical keys for outposts.",
                    nativeTag = "Ad-hoc Route Discovery"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("understand_pairing_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MeshCyanPrimary, contentColor = MeshNavyDark)
                ) {
                    Text("Understood (Return to App)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiscoveryMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    nativeTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MeshNavyDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MeshCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeshSurfaceVariantDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = nativeTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshCyanPrimary,
                        fontSize = 9.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
