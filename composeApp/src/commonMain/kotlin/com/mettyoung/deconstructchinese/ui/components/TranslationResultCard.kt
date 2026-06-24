package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.TranslationResult
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.util.ChineseScriptConverter
import com.mettyoung.deconstructchinese.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChineseWithPinyin(
    vocabulary: List<VocabularyItem>,
    useSimplified: Boolean,
    fallbackText: String,
    modifier: Modifier = Modifier
) {
    // Stage 1 (streaming): no per-word segmentation yet — show the raw Chinese
    // big without pinyin until the breakdown arrives.
    if (vocabulary.isEmpty()) {
        if (fallbackText.isNotBlank()) {
            Text(
                text = fallbackText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 44.sp,
                modifier = modifier
            )
        }
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        vocabulary.forEach { item ->
            val simplified: String? = item.simplified?.takeIf { it != item.word }
            
            val displayWord = if (useSimplified) simplified ?: item.word else item.word
            val counterpartWord = if (simplified != null) {
                if (useSimplified) item.word else simplified
            } else null

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                val pinyinDisplay = if (counterpartWord != null) {
                    "${item.phonetic} ($counterpartWord)"
                } else {
                    item.phonetic
                }
                Text(
                    text       = pinyinDisplay,
                    fontSize   = 13.sp,
                    color      = PinyinColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text       = displayWord,
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    lineHeight = 44.sp
                )
            }
        }
    }
}

@Composable
fun TranslationResultCard(
    result: TranslationResult,
    toEnglish: Boolean,
    isPlaying: Boolean,
    savedVocab: List<VocabularyItem>,
    useSimplified: Boolean = false,
    vocabLoading: Boolean = false,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSaveWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val displayChineseText = if (useSimplified) {
        ChineseScriptConverter.toSimplified(result.chineseText)
    } else {
        result.chineseText
    }
    val scriptLabel = if (useSimplified) "Simplified Chinese" else "Traditional Chinese"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Divider.copy(alpha = 0.5f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(scriptLabel.uppercase(), color = BluePrimary)
                    Row {
                        IconButton(
                            onClick  = { clipboardManager.setText(AnnotatedString(displayChineseText)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick  = { if (isPlaying) onStop() else onSpeak() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isPlaying) "Stop" else "Listen",
                                tint     = if (isPlaying) BluePrimary else TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                ChineseWithPinyin(
                    vocabulary    = result.vocabulary,
                    useSimplified = useSimplified,
                    fallbackText  = displayChineseText,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (toEnglish) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                        color = Divider.copy(alpha = 0.3f)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SectionLabel("ENGLISH TRANSLATION")
                        Text(
                            text       = result.translatedText,
                            style      = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                lineHeight = 28.sp
                            )
                        )
                    }
                }
            }
        }

        if (result.grammarNote.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = GrammarBg.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Column {
                        SectionLabel("LANGUAGE NOTES", color = BluePrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            result.grammarNote, 
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 14.sp,
                                color = TextPrimary.copy(alpha = 0.85f),
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel(
            "VOCABULARY BREAKDOWN",
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            color = TextSecondary.copy(alpha = 0.8f)
        )

        if (vocabLoading && result.vocabulary.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BluePrimary
                )
                Text(
                    "Loading breakdown…",
                    fontSize = 14.sp,
                    color = TextSecondary.copy(alpha = 0.8f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.vocabulary.forEach { item ->
                    val isSaved = savedVocab.any { it.word == item.word }
                    VocabularyCard(
                        item         = item,
                        isSaved      = isSaved,
                        useSimplified = useSimplified,
                        onSpeak      = { onSpeakWord(item.word) },
                        onSaveToggle = { if (isSaved) onRemoveWord(item) else onSaveWord(item) }
                    )
                }
            }
        }
    }
}
