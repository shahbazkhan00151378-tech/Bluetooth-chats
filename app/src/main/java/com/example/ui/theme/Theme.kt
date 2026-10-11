package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MeshCyanPrimary,
    onPrimary = MeshCyanOnPrimary,
    primaryContainer = MeshCyanContainer,
    onPrimaryContainer = MeshCyanOnContainer,
    secondary = MeshSecondaryTeal,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF003833),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = MeshTertiaryBlue,
    onTertiary = Color(0xFF001F3F),
    background = MeshNavyDark,
    onBackground = TextPrimaryDark,
    surface = MeshSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = MeshSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = MeshCardBorderDark,
    error = MeshRedDisconnect,
    onError = Color(0xFF370000)
)

private val LightColorScheme = lightColorScheme(
    primary = MeshCyanPrimaryLight,
    onPrimary = MeshCyanOnPrimaryLight,
    primaryContainer = MeshCyanContainerLight,
    onPrimaryContainer = MeshCyanOnContainerLight,
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = MeshNavyLight,
    onBackground = TextPrimaryLight,
    surface = MeshSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = MeshSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = MeshCardBorderLight,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MeshLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep dedicated high-contrast radio-mesh theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // MeshLink defaults to high-contrast dark tactical aesthetic
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
