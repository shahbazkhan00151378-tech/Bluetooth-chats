package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MeshPacketViewerDialog
import com.example.ui.components.NewPairDialog
import com.example.ui.components.OfflineStatusBar
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshNavyDark
import com.example.ui.theme.MeshSurfaceDark
import com.example.viewmodel.HomeSection
import com.example.viewmodel.MeshViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val groupChannels by viewModel.groupChannels.collectAsState()
    val bleHardwareState by viewModel.bleHardwareState.collectAsState()
    val isAdvertising by viewModel.isBleAdvertising.collectAsState()
    val isScanning by viewModel.isBleScanning.collectAsState()
    val discoveredBlePeers by viewModel.discoveredBlePeers.collectAsState()
    val nativeMessages by viewModel.nativeBleMessages.collectAsState()
    val connectedPeerAddress by viewModel.connectedPeerAddress.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permission launcher for Bluetooth
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
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.refreshBleState()
        if (!allGranted) {
            viewModel.onPermissionsDenied()
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth permissions were denied.")
            }
        }
    }

    // Lifecycle Observer: Automatically stop active BLE scan when app goes to background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.stopBleScanning()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.lastSentDraftNotice) {
        uiState.lastSentDraftNotice?.let { notice ->
            scope.launch {
                snackbarHostState.showSnackbar(notice)
                viewModel.clearNotice()
            }
        }
    }

    // Modal Packet Inspector Dialog
    uiState.inspectedPacket?.let { packet ->
        MeshPacketViewerDialog(
            packet = packet,
            onDismiss = { viewModel.dismissPacketInspection() }
        )
    }

    // Modal New Pair Dialog
    if (uiState.showNewPairDialog) {
        NewPairDialog(
            onDismiss = { viewModel.closeNewPairDialog() }
        )
    }

    // Hidden Developer Diagnostics Screen
    if (uiState.showDiagnosticsScreen) {
        DeveloperDiagnosticsScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeDiagnostics() }
        )
        return
    }

    // Active Detail Screen Handling
    val activeChatId = uiState.selectedPersonalChatId
    if (activeChatId != null) {
        val selectedConv = conversations.find { it.id == activeChatId }
        val messages by viewModel.getMessagesFlow(activeChatId).collectAsState(
            initial = viewModel.getMessagesForConversation(activeChatId)
        )
        if (selectedConv != null) {
            ChatDetailScreen(
                peer = selectedConv.peer,
                messages = messages,
                onBack = { viewModel.closeActiveChat() },
                onSendMessage = { text ->
                    viewModel.sendMessage(activeChatId, text, isGroup = false)
                },
                onInspectMessage = { msg ->
                    viewModel.inspectMessagePacket(msg)
                },
                noticeText = uiState.lastSentDraftNotice,
                onClearNotice = { viewModel.clearNotice() }
            )
            return
        }
    }

    val activeGroupId = uiState.selectedGroupId
    if (activeGroupId != null) {
        val selectedGroup = groupChannels.find { it.id == activeGroupId }
        val messages = viewModel.repository.getMessagesForConversation(activeGroupId)
        if (selectedGroup != null) {
            GroupDetailScreen(
                group = selectedGroup,
                messages = messages,
                onBack = { viewModel.closeActiveChat() },
                onSendMessage = { text ->
                    viewModel.sendLocalDraft(activeGroupId, text, isGroup = true)
                },
                onInspectMessage = { msg ->
                    viewModel.inspectMessagePacket(msg)
                },
                noticeText = uiState.lastSentDraftNotice,
                onClearNotice = { viewModel.clearNotice() }
            )
            return
        }
    }

    // Main User Interface with 3-Tab Bottom Navigation
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            OfflineStatusBar(
                bleHardwareState = bleHardwareState,
                isAdvertising = isAdvertising,
                isScanning = isScanning,
                connectedPeerCount = if (connectedPeerAddress != null) 1 else 0,
                onRequestPermissions = { permissionLauncher.launch(permissionsToRequest) },
                onOpenDiagnostics = { viewModel.openDiagnostics() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MeshSurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("home_bottom_navigation")
            ) {
                NavigationItem(
                    selected = uiState.currentSection == HomeSection.PERSONAL_CHATS,
                    onClick = { viewModel.selectSection(HomeSection.PERSONAL_CHATS) },
                    icon = Icons.Default.Person,
                    label = "Chats",
                    testTag = "nav_tab_personal"
                )

                NavigationItem(
                    selected = uiState.currentSection == HomeSection.NEARBY_GROUPS,
                    onClick = { viewModel.selectSection(HomeSection.NEARBY_GROUPS) },
                    icon = Icons.Default.Groups,
                    label = "Groups",
                    testTag = "nav_tab_groups"
                )

                NavigationItem(
                    selected = uiState.currentSection == HomeSection.MESH_NETWORK,
                    onClick = { viewModel.selectSection(HomeSection.MESH_NETWORK) },
                    icon = Icons.Default.Hub,
                    label = "Network",
                    testTag = "nav_tab_network"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MeshNavyDark)
                .padding(innerPadding)
        ) {
            when (uiState.currentSection) {
                HomeSection.PERSONAL_CHATS -> {
                    PersonalChatSection(
                        conversations = conversations,
                        nativeMessages = nativeMessages,
                        onSelectChat = { id -> viewModel.openPersonalChat(id) },
                        onNavigateToNetwork = { viewModel.selectSection(HomeSection.MESH_NETWORK) }
                    )
                }

                HomeSection.NEARBY_GROUPS -> {
                    NearbyGroupsSection(
                        groups = groupChannels,
                        onSelectGroup = { id -> viewModel.openGroupChat(id) }
                    )
                }

                HomeSection.MESH_NETWORK -> {
                    MeshNetworkSection(
                        bleHardwareState = bleHardwareState,
                        isAdvertising = isAdvertising,
                        isScanning = isScanning,
                        discoveredPeers = discoveredBlePeers,
                        onToggleAdvertising = {
                            if (!viewModel.hasBlePermissions()) {
                                permissionLauncher.launch(permissionsToRequest)
                            } else if (!viewModel.isBluetoothEnabled()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please turn on Bluetooth in settings.")
                                }
                                viewModel.refreshBleState()
                            } else {
                                if (isAdvertising) viewModel.stopBleAdvertising() else viewModel.startBleAdvertising()
                            }
                        },
                        onToggleScanning = {
                            if (!viewModel.hasBlePermissions()) {
                                permissionLauncher.launch(permissionsToRequest)
                            } else if (!viewModel.isBluetoothEnabled()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please turn on Bluetooth in settings.")
                                }
                                viewModel.refreshBleState()
                            } else {
                                if (isScanning) viewModel.stopBleScanning() else viewModel.startBleScanning()
                            }
                        },
                        onRequestPermissions = {
                            permissionLauncher.launch(permissionsToRequest)
                        },
                        onSelectPeerForMessage = { peer ->
                            viewModel.selectBlePeer(peer)
                            viewModel.startChatWithBlePeer(peer)
                        },
                        onOpenDiagnostics = { viewModel.openDiagnostics() }
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MeshNavyDark else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MeshCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MeshCyanPrimary,
            selectedIconColor = MeshNavyDark,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag(testTag)
    )
}
