package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ble.BleConstants
import com.example.ble.BleHardwareState
import com.example.ble.DiscoveredPeerDevice
import com.example.ble.NativeBleMessage
import com.example.ui.components.PrototypeBadge
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshGreenConnected
import com.example.ui.theme.MeshNavyDark
import com.example.ui.theme.MeshRedDisconnect
import com.example.ui.theme.MeshSurfaceDark
import com.example.ui.theme.MeshSurfaceVariantDark
import com.example.viewmodel.MeshViewModel

@Composable
fun NativeBleControlScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val hardwareState by viewModel.bleHardwareState.collectAsState()
    val isAdvertising by viewModel.isBleAdvertising.collectAsState()
    val isScanning by viewModel.isBleScanning.collectAsState()
    val discoveredPeers by viewModel.discoveredBlePeers.collectAsState()
    val connectedPeerAddress by viewModel.connectedPeerAddress.collectAsState()
    val nativeMessages by viewModel.nativeBleMessages.collectAsState()
    val lastError by viewModel.bleLastError.collectAsState()
    val statusLog by viewModel.bleStatusLog.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var pingDraftText by remember { mutableStateOf("PING") }
    var selectedTargetDevice by remember { mutableStateOf<DiscoveredPeerDevice?>(null) }

    // Android runtime permissions launcher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBleState()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Native BLE Direct Link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Milestone 2.1: Physical 2-Device BLE Ping (Zero Internet)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeshCyanPrimary.copy(alpha = 0.15f))
                        .border(1.dp, MeshCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NATIVE HARDWARE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshCyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hardware State Status Banner
        item {
            HardwareStateCard(
                state = hardwareState,
                onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
                onRefresh = { viewModel.refreshBleState() }
            )
        }

        // Error Banner (if any)
        if (lastError != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MeshRedDisconnect.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshRedDisconnect.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = MeshRedDisconnect, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lastError ?: "", style = MaterialTheme.typography.bodySmall, color = MeshRedDisconnect, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.clearBleError() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = MeshRedDisconnect, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Physical Testing Guide Card (Airplane mode instructions)
        item {
            PhysicalTestingGuideCard()
        }

        // Control Panel: Advertiser & Scanner Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Real BLE Radio Controls",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Advertising Toggle Button
                        Button(
                            onClick = {
                                if (isAdvertising) viewModel.stopBleAdvertising() else viewModel.startBleAdvertising()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_ble_advertise_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdvertising) MeshAmberWarning else MeshCyanPrimary,
                                contentColor = MeshNavyDark
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isAdvertising) Icons.Default.CellTower else Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAdvertising) "Stop Beacon" else "Start Beacon",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Scanning Toggle Button
                        Button(
                            onClick = {
                                if (isScanning) viewModel.stopBleScanning() else viewModel.startBleScanning()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_ble_scan_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanning) MeshAmberWarning else MeshSurfaceVariantDark,
                                contentColor = if (isScanning) MeshNavyDark else MeshCyanPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Close else Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isScanning) "Stop Scan" else "Scan Peers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (statusLog != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MeshNavyDark)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Telemetry: $statusLog",
                                style = MaterialTheme.typography.labelSmall,
                                color = MeshCyanPrimary,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Discovered Devices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, tint = MeshCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Discovered BLE Devices",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${discoveredPeers.size} Found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (discoveredPeers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isScanning) "Actively scanning 2.4 GHz spectrum for Service 0xFD42..." else "No devices discovered yet. Tap 'Scan Peers' above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(discoveredPeers, key = { it.address }) { peer ->
                DiscoveredPeerCard(
                    peer = peer,
                    isSelected = selectedTargetDevice?.address == peer.address,
                    onSelect = { selectedTargetDevice = peer }
                )
            }
        }

        // Live Direct BLE Transmit & Receive Thread
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BluetoothConnected, contentDescription = null, tint = MeshCyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Direct BLE Message Exchange",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (connectedPeerAddress != null) {
                            Text(text = "Connected: $connectedPeerAddress", style = MaterialTheme.typography.labelSmall, color = MeshGreenConnected, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (nativeMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MeshNavyDark)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No live BLE messages yet. Select a discovered device below and send a ping payload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            nativeMessages.forEach { msg ->
                                NativeMessageBubble(msg)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compose bar for sending direct ping payload
                    val payloadBytes = pingDraftText.toByteArray().size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedTargetDevice != null) "Target: ${selectedTargetDevice?.name} (${selectedTargetDevice?.address})" else "Select a discovered peer above",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTargetDevice != null) MeshCyanPrimary else MeshAmberWarning,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$payloadBytes / ${BleConstants.DEFAULT_MAX_PAYLOAD_BYTES} bytes (Single BLE Packet)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (payloadBytes > BleConstants.DEFAULT_MAX_PAYLOAD_BYTES) MeshAmberWarning else MeshCyanPrimary,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pingDraftText,
                            onValueChange = { if (it.length <= 50) pingDraftText = it },
                            placeholder = { Text("Short text (<20 bytes)", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ble_ping_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeshCyanPrimary,
                                unfocusedBorderColor = MeshSurfaceVariantDark,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MeshNavyDark,
                                unfocusedContainerColor = MeshNavyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val target = selectedTargetDevice
                                if (target != null && pingDraftText.isNotBlank()) {
                                    viewModel.sendNativeBlePing(target.device, pingDraftText)
                                }
                            },
                            enabled = selectedTargetDevice != null && pingDraftText.isNotBlank(),
                            modifier = Modifier.testTag("send_ble_ping_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MeshCyanPrimary, contentColor = MeshNavyDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ping", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareStateCard(
    state: BleHardwareState,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit
) {
    val (title, subtitle, color, icon) = when (state) {
        BleHardwareState.CHECKING -> Quad("Checking Radio...", "Verifying Bluetooth subsystem", MeshAmberWarning, Icons.Default.HourglassTop)
        BleHardwareState.HARDWARE_UNAVAILABLE -> Quad("No BLE Controller Detected", "Cloud streaming container or emulator lacks 2.4 GHz physical radio. Export APK to test on two real Android devices.", MeshAmberWarning, Icons.Default.BluetoothDisabled)
        BleHardwareState.BLUETOOTH_DISABLED -> Quad("Bluetooth Radio Disabled", "Turn Bluetooth ON in Android system settings.", MeshRedDisconnect, Icons.Default.BluetoothDisabled)
        BleHardwareState.PERMISSIONS_REQUIRED -> Quad("Permissions Required", "Android 12+ requires BLUETOOTH_SCAN, ADVERTISE, CONNECT.", MeshAmberWarning, Icons.Default.Security)
        BleHardwareState.READY_IDLE -> Quad("Physical BLE Radio Ready", "Bluetooth is active and permitted. Ready for advertising or scanning.", MeshGreenConnected, Icons.Default.Bluetooth)
        BleHardwareState.ADVERTISING -> Quad("Broadcasting Mesh Beacon", "Advertising Service UUID 0xFD42 on physical radio.", MeshCyanPrimary, Icons.Default.CellTower)
        BleHardwareState.SCANNING -> Quad("Actively Scanning BLE", "Scanning nearby 2.4 GHz advertisements.", MeshCyanPrimary, Icons.AutoMirrored.Filled.BluetoothSearching)
        BleHardwareState.CONNECTING -> Quad("Connecting GATT...", "Establishing point-to-point BLE link.", MeshAmberWarning, Icons.Default.BluetoothConnected)
        BleHardwareState.CONNECTED -> Quad("GATT Link Active", "Direct device-to-device BLE channel connected.", MeshGreenConnected, Icons.Default.BluetoothConnected)
        BleHardwareState.TRANSMITTING -> Quad("Transmitting Frame...", "Writing characteristic bytes over BLE link.", MeshCyanPrimary, Icons.Default.Sensors)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.5f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }

                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            if (state == BleHardwareState.PERMISSIONS_REQUIRED) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth().testTag("grant_ble_permissions_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MeshAmberWarning, contentColor = MeshNavyDark)
                ) {
                    Text("Grant Bluetooth Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PhysicalTestingGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeshNavyDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AirplanemodeActive, contentDescription = null, tint = MeshCyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Real Physical Device Testing Protocol (Airplane Mode)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "1. Install this APK on Phone A and Phone B.\n" +
                        "2. Enable Airplane Mode on both phones (Wi-Fi OFF, Cellular OFF).\n" +
                        "3. Turn Bluetooth ON on both phones.\n" +
                        "4. Phone A: Tap 'Start Beacon' (hosts GATT server for Service 0xFD42).\n" +
                        "5. Phone B: Tap 'Scan Peers', tap Phone A's entry, then tap 'Ping'.\n" +
                        "6. Observe direct BLE delivery confirmation and write-acknowledgement ticks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DiscoveredPeerCard(
    peer: DiscoveredPeerDevice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("peer_item_${peer.address}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MeshSurfaceVariantDark else MeshSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MeshCyanPrimary else MeshSurfaceVariantDark)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MeshCyanPrimary.copy(alpha = 0.2f) else MeshNavyDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isSelected) MeshCyanPrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = peer.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(text = peer.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${peer.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (peer.rssi > -70) MeshGreenConnected else MeshAmberWarning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isSelected) "Selected" else "Tap to Select",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MeshCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun NativeMessageBubble(message: NativeBleMessage) {
    val isOut = message.isOutgoing
    val alignment = if (isOut) Alignment.End else Alignment.Start
    val bg = if (isOut) MeshSurfaceVariantDark else MeshSurfaceDark
    val border = if (isOut) MeshCyanPrimary.copy(alpha = 0.4f) else MeshSurfaceVariantDark

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(border))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOut) "To: Peer" else "From: ${message.senderName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshCyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (message.isAckReceived) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (message.isAckReceived) MeshGreenConnected else MeshAmberWarning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.deliveryStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isAckReceived) MeshGreenConnected else MeshAmberWarning,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
