package com.example.qamoos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.ui.theme.Manjari
import com.example.qamoos.utils.DictionaryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    viewModel: FavoriteViewModel,
    onBack: () -> Unit,
    onEntryClick: (UnifiedEntry) -> Unit,
    fontSizeMultiplier: Float
) {
    val favoriteEntries by viewModel.favoriteEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites", fontFamily = Manjari) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (favoriteEntries.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Favorite,
                message = "No favorites saved yet"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteEntries) { entry ->
                    val unifiedEntry = UnifiedEntry(
                        id = entry.id,
                        word = entry.word,
                        wordNoHarakah = entry.word,
                        meaning = entry.meaning,
                        dictionaryName = DictionaryUtils.getNativeName(null, entry.dictionaryName)
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
