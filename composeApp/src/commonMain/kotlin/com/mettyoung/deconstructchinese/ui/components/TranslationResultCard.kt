package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        vocabulary.forEach { item ->
            val simplified: String? = item.simplified?.takeIf { it != item.word }
                ?: ChineseScriptConverter.toSimplified(item.word).takeIf { it != item.word }
            
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
                    fontSize   = 12.sp,
                    color      = PinyinColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text       = displayWord,
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    lineHeight = 40.sp
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
    val scriptLabel = if (useSimplified) "Simplified" else "Traditional"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        scriptLabel,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Row {
                        IconButton(
                            onClick  = { clipboardManager.setText(AnnotatedString(displayChineseText)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick  = { if (isPlaying) onStop() else onSpeak() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isPlaying) "Stop" else "Listen",
                                tint     = if (isPlaying) BluePrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                ChineseWithPinyin(
                    vocabulary    = result.vocabulary,
                    useSimplified = useSimplified,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (toEnglish) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Divider.copy(alpha = 0.5f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "ENGLISH",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text       = result.translatedText,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color      = TextPrimary,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }

        if (result.grammarNote.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrammarBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        Text("GRAMMAR NOTE", color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(result.grammarNote, color = TextPrimary.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Text(
            "VOCABULARY BREAKDOWN",
            color      = TextSecondary,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
