package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TechnicalSpecItem
import com.example.ui.components.PrototypeBadge
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary
import com.example.ui.theme.MeshGreenConnected
import com.example.ui.theme.MeshNavyDark
import com.example.ui.theme.MeshRedDisconnect
import com.example.ui.theme.MeshSurfaceDark
import com.example.ui.theme.MeshSurfaceVariantDark

@Composable
fun ArchitectureSpecScreen(
    specs: List<TechnicalSpecItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Native Android Implementation Path",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Phase 1 UI Prototype vs Native Production Roadmap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PrototypeBadge("Engineering Spec")
            }
        }

        // Implementation Status Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshCyanPrimary.copy(alpha = 0.4f)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MeshCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phase 1 Architecture Boundary",
                            style = MaterialTheme.typography.titleSmall,
                            color = MeshCyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Per project specification, this build represents Phase 1: a clean, modular mobile-first UI prototype. No fake mesh routing or simulated packet deliveries are masqueraded as real RF traffic. Below is the validated native Android implementation roadmap for subsequent phases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Native Android Permission Requirements Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MeshNavyDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshSurfaceVariantDark))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MeshAmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Native Android Permissions (Declared in Manifest)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    PermissionRow("BLUETOOTH_SCAN", "neverForLocation flag (API 31+)", "Scans for 128-bit mesh UUID advertisements")
                    PermissionRow("BLUETOOTH_ADVERTISE", "Normal runtime (API 31+)", "Broadcasts local node presence & multi-hop packets")
                    PermissionRow("BLUETOOTH_CONNECT", "Normal runtime (API 31+)", "Maintains L2CAP sockets & GATT server connections")
                    PermissionRow("FOREGROUND_SERVICE", "CONNECTED_DEVICE (API 34+)", "Prevents OS from killing mesh relay daemon in background")
                }
            }
        }

        // Specification Cards
        items(specs, key = { it.title }) { spec ->
            SpecItemCard(spec)
        }

        // Engineering Limitations & Hardware Realities Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MeshSurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MeshAmberWarning.copy(alpha = 0.4f)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MeshAmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Key Hardware & OS Constraints in Native Android",
                            style = MaterialTheme.typography.titleSmall,
                            color = MeshAmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ConstraintBullet("BLE Connection Caps", "Android phones typically support at most 4–7 simultaneous BLE connections. MeshLink must use stateless connection-oriented bursts (connect, exchange, close) rather than persistent topologies.")
                    ConstraintBullet("Doze Mode & Throttling", "Android aggressive battery optimization suspends background scanning. A Foreground Service with ongoing notification is mandatory for continuous relay participation.")
                    ConstraintBullet("MTU Packet Limits", "Default BLE MTU is 23 bytes (3 bytes header, 20 bytes payload). Modern Android negotiates up to 512 bytes MTU, but messages larger than 512 bytes require application-layer chunking and reassembly.")
                    ConstraintBullet("RF Interference & Collisions", "Uncoordinated flood broadcasts saturate the 2.4 GHz channel. Phase 2 requires randomized backoff delays (50–150ms) and Bloom filter suppression.")
                }
            }
        }
    }
}

@Composable
fun SpecItemCard(spec: TechnicalSpecItem) {
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
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeshSurfaceVariantDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = spec.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshCyanPrimary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Phase 1: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = spec.phase1Status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshAmberWarning,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MeshNavyDark)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "Native Android Stack (Phase 2):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = spec.nativeAndroidStack,
                        style = MaterialTheme.typography.labelMedium,
                        color = MeshCyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = spec.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MeshSurfaceVariantDark
            )

            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "Challenges & Limits: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeshAmberWarning,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = spec.limitationsAndChallenges,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(perm: String, api: String, purpose: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = perm,
                style = MaterialTheme.typography.labelMedium,
                color = MeshCyanPrimary
            )
            Text(
                text = api,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = purpose,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ConstraintBullet(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = MeshAmberWarning, fontWeight = FontWeight.Bold)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
