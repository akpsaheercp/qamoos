package com.example.qamoos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.ui.theme.Manjari

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onEntryClick: (UnifiedEntry) -> Unit,
    fontSizeMultiplier: Float
) {
    val historyEntries by viewModel.historyEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History", fontFamily = Manjari) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyEntries.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (historyEntries.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.DeleteSweep,
                message = "Your search history is empty"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyEntries) { entry ->
                    val unifiedEntry = UnifiedEntry(
                        id = entry.id,
                        word = entry.word,
                        wordNoHarakah = entry.word,
                        meaning = entry.meaning,
                        dictionaryName = entry.dictionaryName
                    )
                    DictionaryResultCard(
                        entry = unifiedEntry,
                        fontSizeMultiplier = fontSizeMultiplier,
                        onClick = { onEntryClick(unifiedEntry) }
                    )
                }
            }
        }
    }
}
