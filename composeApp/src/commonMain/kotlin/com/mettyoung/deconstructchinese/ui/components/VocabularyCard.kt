package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.theme.*

@Composable
fun VocabularyCard(
    item: VocabularyItem,
    isSaved: Boolean = false,
    useSimplified: Boolean = false,
    onSpeak: () -> Unit,
    onSaveToggle: () -> Unit
) {
    val simplified: String? = item.simplified?.takeIf { it != item.word }
    val mainWord = if (useSimplified) simplified ?: item.word else item.word
    val counterpartWord = if (simplified != null) {
        if (useSimplified) item.word else simplified
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Divider.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Character box
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BluePrimary.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = mainWord,
                        fontSize  = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color     = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
                if (item.frequency > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                        shape = CircleShape,
                        color = GoldAccent,
                        border = BorderStroke(2.dp, Surface)
                    ) {
                        Text(
                            text      = "${item.frequency}",
                            fontSize  = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color     = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Pinyin + meaning
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val pinyinDisplay = if (counterpartWord != null) {
                    "${item.phonetic} ($counterpartWord)"
                } else {
                    item.phonetic
                }
                Text(
                    pinyinDisplay, 
                    color = PinyinColor, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    item.meaning, 
                    color = TextSecondary, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSpeak, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp, 
                        contentDescription = "Pronounce", 
                        tint = TextSecondary.copy(alpha = 0.5f), 
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onSaveToggle, 
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove" else "Save",
                        tint     = if (isSaved) GoldAccent else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
