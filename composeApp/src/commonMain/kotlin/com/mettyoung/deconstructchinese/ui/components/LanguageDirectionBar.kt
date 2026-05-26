package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mettyoung.deconstructchinese.ui.theme.Background
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Surface
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun LanguageDirectionBar(
    toEnglish: Boolean,
    useSimplified: Boolean,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chineseLabel = if (useSimplified) "Simplified" else "Traditional"
    val sourceLabel = if (toEnglish) chineseLabel else "English"
    val targetLabel = if (toEnglish) "English" else chineseLabel

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DirectionSegment(text = sourceLabel, highlighted = true, onClick = onSwap)
            IconButton(
                onClick = onSwap,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Background)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap direction", tint = BluePrimary)
            }
            DirectionSegment(text = targetLabel, highlighted = false, onClick = onSwap)
        }
    }
}

@Composable
private fun RowScope.DirectionSegment(text: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.small)
            .background(if (highlighted) BluePrimary.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (highlighted) BluePrimary else TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
