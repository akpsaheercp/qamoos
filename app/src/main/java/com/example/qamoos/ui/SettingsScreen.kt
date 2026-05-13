package com.example.qamoos.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.qamoos.R
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.ui.theme.Manjari
import com.example.qamoos.ui.theme.ScheherazadeNew
import com.example.qamoos.utils.DictionaryMetadata
import com.example.qamoos.utils.LanguageUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDictionaryClick: (Int) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit
) {
    val fontSizeMultiplier by viewModel.fontSizeMultiplier.collectAsState()
    val darkThemeConfig by viewModel.darkThemeConfig.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val dictionariesState by viewModel.dictionaries.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    
    var isReorderMode by remember { mutableStateOf(false) }
    
    // Local list for drag-and-drop to ensure smooth UI updates
    var localDictionaries by remember { mutableStateOf(dictionariesState) }
    
    // Update local list when database list changes, but not during reorder mode
    LaunchedEffect(dictionariesState) {
        if (!isReorderMode) {
            localDictionaries = dictionariesState
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = if (it.contains("offline version")) SnackbarDuration.Long else SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.settings), 
                            fontFamily = Manjari,
                            fontWeight = FontWeight.Bold
                        )
                        if (isFirstRun) {
                            Text(
                                "Setup your library to get started",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = Manjari,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (localDictionaries.isNotEmpty()) {
                        IconButton(onClick = { 
                            if (isReorderMode) {
                                // Save changes when exiting reorder mode
                                viewModel.updateDictionaryOrders(localDictionaries)
                            }
                            isReorderMode = !isReorderMode 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Icon(
                                if (isReorderMode) Icons.Default.Done else Icons.Default.SwapVert, 
                                contentDescription = "Reorder",
                                tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val listState = rememberLazyListState()
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- Library Tools ---
            if (!isReorderMode) {
                item { SettingsSectionHeader(title = "Tools") }
                item {
                    SettingsGroup {
                        SettingsClickableItem(
                            title = stringResource(R.string.history),
                            description = "Previously viewed meanings",
                            icon = Icons.Default.History,
                            onClick = onNavigateToHistory
                        )
                        SettingsClickableItem(
                            title = stringResource(R.string.favorites),
                            description = "Saved words and meanings",
                            icon = Icons.Default.Favorite,
                            onClick = onNavigateToFavorites
                        )
                    }
                }
            }

            // --- Dictionary Management ---
            item { 
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSectionHeader(title = if (isReorderMode) "Prioritize Dictionaries" else stringResource(R.string.manage_dictionaries))
                    if (!isReorderMode) {
                        TextButton(onClick = { viewModel.downloadAllDictionaries() }) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download All", fontFamily = Manjari, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReorderMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isReorderMode) Icons.Default.SwapVert else Icons.Default.Info, 
                                contentDescription = null, 
                                tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isReorderMode) "Drag the handles to prioritize dictionaries. Top dictionaries appear first in search results." else "Enable dictionaries to include them in search results. You can download up to 10 dictionaries.",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = Manjari,
                                color = if (isReorderMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = localDictionaries,
                key = { _, dict -> dict.tableName ?: dict.hashCode() }
            ) { index, dict ->
                DictionarySettingsItem(
                    dict = dict,
                    viewModel = viewModel,
                    isReorderMode = isReorderMode,
                    onMoveUp = {
                        if (index > 0) {
                            val newList = localDictionaries.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index - 1, item)
                            localDictionaries = newList
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onMoveDown = {
                        if (index < localDictionaries.size - 1) {
                            val newList = localDictionaries.toMutableList()
                            val item = newList.removeAt(index)
                            newList.add(index + 1, item)
                            localDictionaries = newList
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    onDownloadClick = { viewModel.downloadDictionary(dict.tableName) },
                    onPauseClick = { viewModel.pauseDownload(dict.tableName) },
                    onResumeClick = { viewModel.resumeDownload(dict.tableName) },
                    onCancelClick = { viewModel.cancelDownload(dict.tableName) },
                    onDeleteClick = { viewModel.deleteDictionary(dict) },
                    onToggle = { viewModel.toggleDictionary(dict) },
                    onClick = { dict.id?.let { onDictionaryClick(it) } }
                )
            }

            // --- Appearance Section ---
            if (!isReorderMode) {
                item { SettingsSectionHeader(title = stringResource(R.string.appearance)) }
                item {
                    SettingsGroup {
                        SettingsSelectionItem(
                            title = stringResource(R.string.theme),
                            icon = Icons.Default.Palette,
                            options = listOf("light", "dark", "system"),
                            selectedOption = darkThemeConfig,
                            onOptionSelected = { viewModel.updateTheme(it) },
                            optionLabels = mapOf(
                                "light" to stringResource(R.string.light),
                                "dark" to stringResource(R.string.dark),
                                "system" to stringResource(R.string.system)
                            )
                        )
                        SettingsSelectionItem(
                            title = stringResource(R.string.language),
                            icon = Icons.Default.Language,
                            options = listOf("en", "ml", "ar"),
                            selectedOption = appLanguage ?: "en",
                            onOptionSelected = { viewModel.updateLanguage(it) },
                            optionLabels = mapOf(
                                "en" to "English",
                                "ml" to "മലയാളം",
                                "ar" to "العربية"
                            )
                        )
                        SettingsSliderItem(
                            title = stringResource(R.string.font_size),
                            icon = Icons.Default.TextFields,
                            value = fontSizeMultiplier,
                            onValueChange = { viewModel.updateFontSize(it) },
                            range = 0.8f..1.5f,
                            steps = 7,
                            previewText = stringResource(R.string.preview_text)
                        )
                    }
                }

                // --- Community & Support ---
                item { SettingsSectionHeader(title = "Community & Support") }
                item {
                    SettingsGroup {
                        val uriHandler = LocalUriHandler.current
                        SettingsClickableItem(
                            title = "Telegram Community",
                            description = "Join for discussions and updates",
                            icon = Icons.AutoMirrored.Filled.Message,
                            onClick = { uriHandler.openUri("https://t.me/qamoos") }
                        )
                        SettingsClickableItem(
                            title = "Contact Developer",
                            description = "WhatsApp or Telegram for feedback",
                            icon = Icons.Default.Person,
                            onClick = { uriHandler.openUri("https://wa.me/917902520097") }
                        )
                    }
                }

                // --- Support the Project (Donation) ---
                item {
                    DonationCard(
                        onDonateClick = {
                            val upiUri = Uri.parse("upi://pay?pa=akpsaheer@okhdfcbank&pn=QamoosApp&cu=INR")
                            val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                            val chooser = Intent.createChooser(upiIntent, "Donate via UPI")
                            try {
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No UPI app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontFamily = Manjari
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DictionarySettingsItem(
    dict: DictionaryInfo,
    viewModel: SettingsViewModel,
    isReorderMode: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDownloaded = viewModel.isDownloaded(dict.tableName)
    val progressMap by viewModel.downloadProgress.collectAsState()
    val isPausedMap by viewModel.isPaused.collectAsState()
    val startingDownloads by viewModel.startingDownloads.collectAsState()
    
    val downloadProgress = progressMap[dict.tableName]
    val isPaused = isPausedMap[dict.tableName] ?: false
    val isStarting = startingDownloads.contains(dict.tableName)
    
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        headlineContent = {
            val isArabicName = LanguageUtils.isArabic(dict.displayName)
            Text(
                text = dict.displayName ?: "Unknown",
                fontFamily = if (isArabicName) com.example.qamoos.ui.theme.ScheherazadeNew else Manjari,
                fontWeight = FontWeight.Bold,
                fontSize = if (isArabicName) 18.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = when {
                    isDownloaded -> "Installed"
                    isStarting -> "Starting..."
                    downloadProgress != null -> if (isPaused) "Paused" else "Downloading ${downloadProgress.toInt()}%"
                    else -> DictionaryMetadata.getSize(dict.tableName)
                },
                fontFamily = Manjari,
                fontSize = 12.sp,
                color = when {
                    isDownloaded -> MaterialTheme.colorScheme.primary
                    isStarting || downloadProgress != null -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        leadingContent = {
            if (isReorderMode) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (dict.isSelected == 1 && isDownloaded) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.Book else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (dict.isSelected == 1 && isDownloaded) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailingContent = {
            if (isReorderMode) {
                Icon(
                    Icons.Default.DragHandle, 
                    contentDescription = "Reorder Handle", 
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDownloaded) {
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        
                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Dictionary", fontFamily = Manjari) },
                                text = { Text("Are you sure you want to delete ${dict.displayName}?", fontFamily = Manjari) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        onDeleteClick()
                                        showDeleteDialog = false
                                    }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error, fontFamily = Manjari)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Cancel", fontFamily = Manjari)
                                    }
                                }
                            )
                        }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                        
                        Switch(
                            checked = dict.isSelected == 1,
                            onCheckedChange = { 
                                onToggle()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    } else if (isStarting || downloadProgress != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isStarting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            } else {
                                IconButton(onClick = if (isPaused) onResumeClick else onPauseClick) {
                                    Icon(
                                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                IconButton(onClick = onCancelClick) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        IconButton(onClick = onDownloadClick) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    
    if ((isStarting || downloadProgress != null) && !isDownloaded) {
        if (isStarting) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            )
        } else {
            LinearProgressIndicator(
                progress = (downloadProgress ?: 0f) / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontFamily = Manjari,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontFamily = Manjari, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(description, fontFamily = Manjari, fontSize = 13.sp) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelectionItem(
    title: String,
    icon: ImageVector,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    optionLabels: Map<String, String>
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        ListItem(
            headlineContent = { Text(title, fontFamily = Manjari, fontWeight = FontWeight.SemiBold) },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { 
                        Text(
                            optionLabels[option] ?: option, 
                            fontFamily = Manjari, 
                            fontSize = 12.sp,
                            maxLines = 1
                        ) 
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    previewText: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        ListItem(
            headlineContent = { Text(title, fontFamily = Manjari, fontWeight = FontWeight.SemiBold) },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = previewText,
                fontSize = (16 * value).sp,
                fontFamily = Manjari,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun DonationCard(onDonateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.VolunteerActivism, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Support the Project",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Manjari,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ഈ സംരംഭം തുടർന്ന് കൊണ്ടുപോകുന്നതിനായി നിങ്ങളുടെ സഹായം അഭ്യർത്ഥിക്കുന്നു. ചെറിയൊരു തുക സംഭാവന ചെയ്യുന്നത് ആപ്ലിക്കേഷൻ അപ്‌ഡേറ്റുകൾ നൽകാൻ ഞങ്ങളെ സഹായിക്കും.",
                fontFamily = Manjari,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDonateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Donate via UPI", fontFamily = Manjari, fontWeight = FontWeight.Bold)
            }
        }
    }
}
