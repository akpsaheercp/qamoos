package com.example.qamoos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qamoos.ui.theme.Manjari

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryDetailScreen(
    dictionaryId: Int,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val dictionaries by viewModel.dictionaries.collectAsState()
    val dictionary = dictionaries.find { it.id == dictionaryId }

    val details = getDictionaryDetails(dictionary?.tableName ?: "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary Details", fontFamily = Manjari) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = dictionary?.displayName ?: "Unknown Dictionary",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = Manjari,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "ID: $dictionaryId | Table: ${dictionary?.tableName ?: "N/A"}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = Manjari,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DetailSection("Description", details.description)
            DetailSection("Author", details.author)
            DetailSection("Era", details.era)
            DetailSection("Scope", details.scope)
            
            if (details.isClassical) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "This is a classical Arabic dictionary, highly regarded in linguistic studies.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = Manjari
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = Manjari
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = Manjari,
            lineHeight = 24.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
        )
    }
}

data class DictionaryDetails(
    val description: String,
    val author: String = "Unknown",
    val era: String = "N/A",
    val scope: String = "General",
    val isClassical: Boolean = false
)

fun getDictionaryDetails(tableName: String): DictionaryDetails {
    return when (tableName.lowercase()) {
        "lisanularab" -> DictionaryDetails(
            description = "Lisan al-Arab is one of the most comprehensive and famous dictionaries of the Arabic language. It combines the content of five earlier major works.",
            author = "Ibn Manzur",
            era = "13th Century (Mamluk era)",
            scope = "Extensive linguistic and literary coverage",
            isClassical = true
        )
        "taj" -> DictionaryDetails(
            description = "Taj al-Arus is a massive dictionary based on Al-Qamoos al-Muheet, expanding it significantly with explanations and citations.",
            author = "Murtada al-Zabidi",
            era = "18th Century",
            scope = "Encyclopedia of Arabic linguistics",
            isClassical = true
        )
        "qamoos_moheet" -> DictionaryDetails(
            description = "Al-Qamoos al-Muheet was for centuries the most popular Arabic dictionary, known for its concise yet comprehensive entries.",
            author = "Al-Fayruzabadi",
            era = "14th-15th Century",
            scope = "Standard linguistic reference",
            isClassical = true
        )
        "mujamul_waseet" -> DictionaryDetails(
            description = "Al-Mu'jam al-Waseet is a modern dictionary produced by the Academy of the Arabic Language in Cairo, focusing on contemporary usage while maintaining classical standards.",
            author = "Academy of the Arabic Language in Cairo",
            era = "20th Century",
            scope = "Modern and classical Arabic",
            isClassical = false
        )
        "sehah" -> DictionaryDetails(
            description = "As-Sehah (The Correct One) is a landmark dictionary that revolutionized Arabic lexicography by using an alphabetical order based on the last letter of the root.",
            author = "Al-Jawhari",
            era = "10th Century",
            scope = "Linguistic correctness and purity",
            isClassical = true
        )
        "maqayys" -> DictionaryDetails(
            description = "Maqayys al-Lugha focuses on the etymology and the 'measure' of words, explaining how different meanings of a root are interconnected.",
            author = "Ibn Faris",
            era = "10th Century",
            scope = "Etymological and conceptual linguistics",
            isClassical = true
        )
        "mufradatquran" -> DictionaryDetails(
            description = "Mufradat al-Quran is the most famous dictionary dedicated to the vocabulary of the Holy Quran, explaining terms in their theological and linguistic context.",
            author = "Al-Raghib al-Isfahani",
            era = "11th Century",
            scope = "Quranic vocabulary",
            isClassical = true
        )
        "misbah" -> DictionaryDetails(
            description = "Al-Misbah al-Muneer is a concise dictionary initially written to explain the difficult words found in a specific Shafi'i jurisprudence book.",
            author = "Al-Fayyumi",
            era = "14th Century",
            scope = "Linguistic and legal terminology",
            isClassical = true
        )
        "nahwa" -> DictionaryDetails(
            description = "A specialized dictionary focusing on Arabic grammar (Nahw) and its various terminologies and rules.",
            author = "Various scholars",
            era = "Modern compilation",
            scope = "Grammar and Syntax",
            isClassical = false
        )
        "sarf" -> DictionaryDetails(
            description = "A specialized dictionary focusing on Arabic morphology (Sarf), explaining word derivations and forms.",
            author = "Various scholars",
            era = "Modern compilation",
            scope = "Morphology",
            isClassical = false
        )
        "english" -> DictionaryDetails(
            description = "A comprehensive English-Arabic and Arabic-English translation layer providing meanings in English.",
            author = "Compiled from various sources",
            era = "Modern",
            scope = "Translation and modern usage",
            isClassical = false
        )
        "malayalam" -> DictionaryDetails(
            description = "A specialized dictionary providing Arabic-Malayalam translations, widely used in Kerala, India.",
            author = "Various contributors",
            era = "Modern",
            scope = "Regional translation (Malayalam)",
            isClassical = false
        )
        "irab" -> DictionaryDetails(
            description = "Kamus Irab is a specialized dictionary for the grammatical parsing (Irab) of Arabic words, especially in the context of the Quran.",
            author = "Modern scholars",
            era = "Contemporary",
            scope = "Grammatical parsing (Irab)",
            isClassical = false
        )
        "muasirah" -> DictionaryDetails(
            description = "Al-Lugha al-Muasirah focuses on contemporary Arabic usage, including modern terminology not found in classical works.",
            author = "Modern lexicographers",
            era = "20th-21st Century",
            scope = "Contemporary Arabic",
            isClassical = false
        )
        "boldan" -> DictionaryDetails(
            description = "Mujam al-Boldan is a famous geographical dictionary describing cities, countries, and geographical features known at the time.",
            author = "Yaqut al-Hamawi",
            era = "13th Century",
            scope = "Geography and History",
            isClassical = true
        )
        else -> DictionaryDetails(
            description = "This dictionary provides specialized meanings and linguistic insights for the Arabic language. It is part of the comprehensive Qamoos collection.",
            author = "Classical/Modern Lexicographers",
            era = "Historical/Modern",
            scope = "Linguistic reference",
            isClassical = tableName.isNotEmpty()
        )
    }
}
