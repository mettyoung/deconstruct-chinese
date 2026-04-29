package com.mettyoung.deconstructchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.mettyoung.deconstructchinese.ui.theme.Background
import com.mettyoung.deconstructchinese.ui.theme.BluePrimary
import com.mettyoung.deconstructchinese.ui.theme.Divider
import com.mettyoung.deconstructchinese.ui.theme.PinyinColor
import com.mettyoung.deconstructchinese.ui.theme.Surface
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChineseWithPinyin(vocabulary: List<VocabularyItem>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        vocabulary.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = item.phonetic,
                    fontSize   = 13.sp,
                    color      = PinyinColor,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text       = item.word,
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Normal,
                    color      = TextPrimary,
                    lineHeight = 42.sp
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
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSaveWord: (VocabularyItem) -> Unit,
    onRemoveWord: (VocabularyItem) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxWidth()) {

        // Output header: Traditional Chinese label + actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Traditional Chinese",
                color = BluePrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Row {
                IconButton(
                    onClick  = { clipboardManager.setText(AnnotatedString(result.chineseText)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint     = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
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

        // Chinese characters with pinyin aligned above each word
        ChineseWithPinyin(
            vocabulary = result.vocabulary,
            modifier   = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(start = 14.dp, end = 14.dp, bottom = if (toEnglish) 8.dp else 16.dp)
        )

        // English translation (only in Chinese→English mode)
        if (toEnglish) {
            HorizontalDivider(color = Divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "English",
                    color = BluePrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text       = result.translatedText,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color      = TextPrimary,
                    lineHeight = 30.sp
                )
            }
        }

        HorizontalDivider(color = Divider)

        // Grammar note
        if (result.grammarNote.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F0FE))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text("Grammar Note", color = BluePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(result.grammarNote, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            HorizontalDivider(color = Divider)
        }

        // Vocabulary breakdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Background)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Word Breakdown",
                color      = TextSecondary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            result.vocabulary.forEach { item ->
                val isSaved = savedVocab.any { it.word == item.word }
                VocabularyCard(
                    item         = item,
                    isSaved      = isSaved,
                    onSpeak      = { onSpeakWord(item.word) },
                    onSaveToggle = { if (isSaved) onRemoveWord(item) else onSaveWord(item) }
                )
            }
        }
    }
}
