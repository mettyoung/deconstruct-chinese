package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.util.ChineseScriptConverter
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Card
import com.mettyoung.deconstructchinese.ui.theme.GoldAccent
import com.mettyoung.deconstructchinese.ui.theme.PinyinColor
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun VocabularyCard(
    item: VocabularyItem,
    isSaved: Boolean = false,
    useSimplified: Boolean = false,
    onSpeak: () -> Unit,
    onSaveToggle: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    // item.word is always traditional; resolve simplified from API field or converter fallback
    val simplified: String? = item.simplified
        ?: ChineseScriptConverter.toSimplified(item.word).takeIf { it != item.word }
    val mainWord = if (useSimplified) simplified ?: item.word else item.word
    val counterpartWord = if (simplified != null) {
        if (useSimplified) item.word else simplified
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Character box
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BluePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = mainWord,
                    fontSize  = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color     = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            if (item.frequency > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "${item.frequency}",
                        fontSize  = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color     = Color(0xFF202124)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Pinyin + meaning
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val pinyinDisplay = if (counterpartWord != null) {
                "${item.phonetic} ($counterpartWord)"
            } else {
                item.phonetic
            }
            Text(pinyinDisplay, color = PinyinColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(item.meaning, color = TextSecondary, fontSize = 13.sp)
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick  = { clipboardManager.setText(AnnotatedString(mainWord)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onSaveToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isSaved) "Remove from saved" else "Save vocabulary",
                    tint     = if (isSaved) GoldAccent else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
