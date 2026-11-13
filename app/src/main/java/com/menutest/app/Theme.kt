package com.menutest.app

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

@Composable
fun MenuTestTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(
            primary = Color(0xFF9ECAFF), onPrimary = Color(0xFF003258),
            primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
            secondary = Color(0xFF82D3D5), tertiary = Color(0xFF82D3D5),
        )
        else -> lightColorScheme(
            primary = Color(0xFF0061A4), onPrimary = Color.White,
            primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
            secondary = Color(0xFF00696B), tertiary = Color(0xFF00696B),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
