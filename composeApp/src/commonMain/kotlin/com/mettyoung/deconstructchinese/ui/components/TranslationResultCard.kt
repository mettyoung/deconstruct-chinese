package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.theme.CardDark
import com.mettyoung.deconstructchinese.ui.theme.GoldAccent
import com.mettyoung.deconstructchinese.ui.theme.PinyinColor
import com.mettyoung.deconstructchinese.ui.theme.RedPrimary
import com.mettyoung.deconstructchinese.ui.theme.SurfaceDark
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun TranslationResultCard(
    result: TranslationResult,
    isPlaying: Boolean,
    savedVocab: List<VocabularyItem>,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSaveWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Main translation
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Chinese", color = GoldAccent,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(result.chineseText))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Chinese text",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = { if (isPlaying) onStop() else onSpeak() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isPlaying)
                                    RedPrimary
                                else
                                    GoldAccent.copy(alpha = 0.2f),
                                contentColor = if (isPlaying)
                                    Color.White
                                else
                                    GoldAccent
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop
                                else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (isPlaying) "Stop" else "Listen",
                                fontSize = 13.sp)
                        }
                    }
                }

                // Chinese characters (large)
                Text(
                    text = result.chineseText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 44.sp
                )

                // Pinyin
                Text(
                    text = result.pinyinText,
                    fontSize = 18.sp,
                    color = PinyinColor,
                    fontWeight = FontWeight.Medium
                )

                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))

                // Original English
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EN", color = TextSecondary, fontSize = 12.sp)
                    Text(result.originalText,
                        color = TextSecondary, fontSize = 15.sp)
                }
            }
        }

        // Grammar note
        if (result.grammarNote.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B3A4B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text("Grammar Note",
                            color = Color(0xFF4FC3F7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(result.grammarNote,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp)
                    }
                }
            }
        }

        // Vocabulary breakdown
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Vocabulary Breakdown",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp)

                Spacer(Modifier.height(4.dp))

                result.vocabulary.forEach { item ->
                    val isSaved = savedVocab.any { it.character == item.character }
                    VocabularyCard(
                        item = item,
                        isSaved = isSaved,
                        onSpeak = { onSpeakWord(item.character) },
                        onSaveToggle = {
                            if (isSaved) onRemoveWord(item) else onSaveWord(item)
                        }
                    )
                }
            }
        }
    }
}
