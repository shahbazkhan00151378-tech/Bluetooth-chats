package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeshAmberWarning
import com.example.ui.theme.MeshCyanPrimary

@Composable
fun PrototypeBadge(
    text: String = "UI Prototype",
    isAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tintColor = if (isAccent) MeshCyanPrimary else MeshAmberWarning
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(0.8.dp, tintColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(tintColor.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
