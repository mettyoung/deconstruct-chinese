package com.mettyoung.deconstructchinese.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.deconstructchinese.model.VocabularyItem
import com.mettyoung.deconstructchinese.ui.components.VocabularyCard
import com.mettyoung.deconstructchinese.ui.theme.*

@Composable
fun VocabularyScreen(
    modifier: Modifier = Modifier,
    vocabulary: List<VocabularyItem>,
    useSimplified: Boolean = false,
    onDismiss: () -> Unit,
    onRemove: (VocabularyItem) -> Unit,
    onSpeak: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                "Saved Words",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = GoldAccent.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "${vocabulary.size} items",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ranked by frequency",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (vocabulary.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Bookmark, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp),
                        tint = Divider
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No vocabulary saved yet.", 
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Words you save will appear here.", 
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vocabulary, key = { it.word }) { item ->
                    VocabularyCard(
                        item         = item,
                        isSaved      = true,
                        useSimplified = useSimplified,
                        onSpeak      = { onSpeak(item.word) },
                        onSaveToggle = { onRemove(item) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
