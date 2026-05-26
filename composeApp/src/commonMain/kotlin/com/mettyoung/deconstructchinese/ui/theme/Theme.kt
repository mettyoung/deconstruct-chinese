package com.mettyoung.deconstructchinese.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppColorScheme = lightColorScheme(
    primary        = BluePrimary,
    secondary      = GoldAccent,
    background      = Background,
    surface        = Surface,
    onPrimary      = Color.White,
    onBackground   = TextPrimary,
    onSurface      = TextPrimary,
    surfaceVariant = Card
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp),
        titleLarge    = titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        bodyLarge     = bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelSmall    = labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
    )
}

private val AppShapes = Shapes(
    small  = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large  = RoundedCornerShape(24.dp)
)

@Composable
fun DeconstructTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
