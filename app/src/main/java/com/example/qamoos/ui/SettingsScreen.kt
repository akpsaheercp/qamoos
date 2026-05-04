package com.example.qamoos.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qamoos.R
import com.example.qamoos.ui.theme.Manjari

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDictionaryClick: (Int) -> Unit,
    onNavigateToDictionaryList: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val fontSizeMultiplier by viewModel.fontSizeMultiplier.collectAsState()
    val darkThemeConfig by viewModel.darkThemeConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontFamily = Manjari) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
        ) {
            item {
                Text(stringResource(R.string.library), style = MaterialTheme.typography.titleLarge, fontFamily = Manjari)
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.manage_dictionaries), fontFamily = Manjari) },
                            supportingContent = { Text("Download and reorder dictionaries", fontFamily = Manjari) },
                            leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.clickable(onClick = onNavigateToDictionaryList)
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.history), fontFamily = Manjari) },
                            supportingContent = { Text("Previously viewed meanings", fontFamily = Manjari) },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                            modifier = Modifier.clickable(onClick = onNavigateToHistory)
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.favorites), fontFamily = Manjari) },
                            supportingContent = { Text("Saved words and meanings", fontFamily = Manjari) },
                            leadingContent = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            modifier = Modifier.clickable(onClick = onNavigateToFavorites)
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge, fontFamily = Manjari)
            }

            item {
                val appLanguage by viewModel.appLanguage.collectAsState()
                Card {
                    Column(modifier = Modifier.padding(8.dp)) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.theme), fontFamily = Manjari) },
                            supportingContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    FilterChip(
                                        selected = darkThemeConfig == "light",
                                        onClick = { viewModel.updateTheme("light") },
                                        label = { Text(stringResource(R.string.light), fontFamily = Manjari) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = darkThemeConfig == "dark",
                                        onClick = { viewModel.updateTheme("dark") },
                                        label = { Text(stringResource(R.string.dark), fontFamily = Manjari) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = darkThemeConfig == "system",
                                        onClick = { viewModel.updateTheme("system") },
                                        label = { Text(stringResource(R.string.system), fontFamily = Manjari) }
                                    )
                                }
                            },
                            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.language), fontFamily = Manjari) },
                            supportingContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    FilterChip(
                                        selected = appLanguage == "en",
                                        onClick = { viewModel.updateLanguage("en") },
                                        label = { Text(stringResource(R.string.english), fontFamily = Manjari) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = appLanguage == "ml",
                                        onClick = { viewModel.updateLanguage("ml") },
                                        label = { Text(stringResource(R.string.malayalam), fontFamily = Manjari) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilterChip(
                                        selected = appLanguage == "ar",
                                        onClick = { viewModel.updateLanguage("ar") },
                                        label = { Text(stringResource(R.string.arabic), fontFamily = Manjari) }
                                    )
                                }
                            },
                            leadingContent = { Icon(Icons.Default.Language, contentDescription = null) }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.font_size), fontFamily = Manjari) },
                            supportingContent = {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Slider(
                                        value = fontSizeMultiplier,
                                        onValueChange = { viewModel.updateFontSize(it) },
                                        valueRange = 0.8f..1.5f,
                                        steps = 7
                                    )
                                    Text(
                                        stringResource(R.string.preview_text),
                                        fontSize = (16 * fontSizeMultiplier).sp,
                                        fontFamily = Manjari,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            },
                            leadingContent = { Icon(Icons.Default.TextFields, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.support_community), style = MaterialTheme.typography.titleLarge, fontFamily = Manjari)
            }

            item {
                val uriHandler = LocalUriHandler.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Join our community for discussions, updates, and feedback.",
                            fontFamily = Manjari,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { uriHandler.openUri("https://t.me/qamoos") },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join for Discussion & Updates", fontFamily = Manjari)
                        }
                    }
                }
            }

            item {
                val context = LocalContext.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Support the Project",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = Manjari,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ഈ ആപ്ലിക്കേഷൻ പ്ലേസ്റ്റോറിൽ നിലനിർത്തുന്നതിനും പുതിയ അപ്‌ഡേറ്റുകൾ നൽകുന്നതിനും ഗൂഗിളിന് നിശ്ചിത തുക നൽകേണ്ടതുണ്ട്. നിങ്ങളുടെ ചെറിയൊരു സഹായം ഈ സംരംഭത്തെ മുന്നോട്ട് കൊണ്ടുപോകാൻ സഹായിക്കും.",
                            fontFamily = Manjari,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val upiUri = Uri.parse("upi://pay?pa=akpsaheer@okhdfcbank&pn=QamoosApp&cu=INR")
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                val chooser = Intent.createChooser(upiIntent, "Donate via UPI")
                                try {
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Donate via UPI", fontFamily = Manjari)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Contact Developer", style = MaterialTheme.typography.titleLarge, fontFamily = Manjari)
            }

            item {
                val uriHandler = LocalUriHandler.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Feel free to reach out personally for queries or suggestions.",
                            fontFamily = Manjari,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { uriHandler.openUri("https://wa.me/917902520097") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text("WhatsApp", fontFamily = Manjari)
                            }
                            Button(
                                onClick = { uriHandler.openUri("https://t.me/+917902520097") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Text("Telegram", fontFamily = Manjari)
                            }
                        }
                    }
                }
            }
        }
    }
}
