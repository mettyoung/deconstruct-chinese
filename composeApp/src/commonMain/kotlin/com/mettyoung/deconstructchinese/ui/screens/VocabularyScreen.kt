package com.mettyoung.deconstructchinese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.components.VocabularyCard
import com.mettyoung.deconstructchinese.ui.theme.Background
import com.mettyoung.deconstructchinese.ui.theme.Divider
import com.mettyoung.deconstructchinese.ui.theme.GoldAccent
import com.mettyoung.deconstructchinese.ui.theme.TextPrimary
import com.mettyoung.deconstructchinese.ui.theme.TextSecondary

@Composable
fun VocabularyScreen(
    modifier: Modifier = Modifier,
    vocabulary: List<VocabularyItem>,
    onDismiss: () -> Unit,
    onRemove: (VocabularyItem) -> Unit,
    onSpeak: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                "Saved",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${vocabulary.size} words · ranked by frequency",
                color = GoldAccent,
                fontSize = 13.sp
            )
        }

        HorizontalDivider(color = Divider)

        if (vocabulary.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No vocabulary saved yet.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vocabulary, key = { it.word }) { item ->
                    VocabularyCard(
                        item         = item,
                        isSaved      = true,
                        onSpeak      = { onSpeak(item.word) },
                        onSaveToggle = { onRemove(item) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
