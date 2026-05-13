package com.example.qamoos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.qamoos.data.AppDatabase
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.data.UserPreferences
import com.example.qamoos.ui.*
import com.example.qamoos.ui.theme.Manjari
import com.example.qamoos.ui.theme.QamoosTheme
import com.example.qamoos.ui.theme.ScheherazadeNew
import com.example.qamoos.utils.DictionaryManager
import com.example.qamoos.utils.LanguageUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val userPreferences = remember { UserPreferences(context) }
            val darkThemeConfig by userPreferences.darkThemeConfig.collectAsState(initial = "system")
            val fontSizeMultiplier by userPreferences.fontSizeMultiplier.collectAsState(initial = 1.0f)
            val appLanguage by userPreferences.appLanguage.collectAsState(initial = null)
            val isFirstRun by userPreferences.isFirstRun.collectAsState(initial = false)

            val coroutineScope = rememberCoroutineScope()

            // Handle Language Change
            LaunchedEffect(appLanguage) {
                appLanguage?.let {
                    val appLocales: LocaleListCompat = LocaleListCompat.forLanguageTags(it)
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }

            val darkTheme = when (darkThemeConfig) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            QamoosTheme(darkTheme = darkTheme) {
                val database = remember { AppDatabase.getDatabase(context) }
                val dictionaryManager = remember { DictionaryManager(context) }
                val dictionaryViewModel: DictionaryViewModel = viewModel(
                    factory = DictionaryViewModelFactory(database.arabicDao(), userPreferences, dictionaryManager)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(database.arabicDao(), userPreferences, dictionaryManager)
                )
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(database.historyDao())
                )
                val favoriteViewModel: FavoriteViewModel = viewModel(
                    factory = FavoriteViewModelFactory(database.favoriteDao())
                )

                val navController = rememberNavController()
                var showManualLanguageDialog by remember { mutableStateOf(false) }

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(navController = navController, startDestination = "dictionary") {
                        composable("dictionary") {
                            DictionaryScreen(
                                viewModel = dictionaryViewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToLibrary = { navController.navigate("settings") },
                                onShowLanguageDialog = { showManualLanguageDialog = true },
                                onResultClick = { index ->
                                    navController.navigate("detail/$index")
                                },
                                fontSizeMultiplier = fontSizeMultiplier
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() },
                                onDictionaryClick = { id ->
                                    navController.navigate("dictionary_detail/$id")
                                },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToFavorites = { navController.navigate("favorites") }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onBack = { navController.popBackStack() },
                                onEntryClick = { entry ->
                                    val encodedWord = java.net.URLEncoder.encode(entry.word ?: "", "UTF-8")
                                    val encodedDict = java.net.URLEncoder.encode(entry.dictionaryName, "UTF-8")
                                    navController.navigate("standalone_detail/$encodedWord/$encodedDict")
                                },
                                fontSizeMultiplier = fontSizeMultiplier
                            )
                        }
                        composable("favorites") {
                            FavoriteScreen(
                                viewModel = favoriteViewModel,
                                onBack = { navController.popBackStack() },
                                onEntryClick = { entry ->
                                    val encodedWord = java.net.URLEncoder.encode(entry.word ?: "", "UTF-8")
                                    val encodedDict = java.net.URLEncoder.encode(entry.dictionaryName, "UTF-8")
                                    navController.navigate("standalone_detail/$encodedWord/$encodedDict")
                                },
                                fontSizeMultiplier = fontSizeMultiplier
                            )
                        }
                        composable(
                            route = "standalone_detail/{word}/{dictName}",
                            arguments = listOf(
                                navArgument("word") { type = NavType.StringType },
                                navArgument("dictName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val word = backStackEntry.arguments?.getString("word") ?: ""
                            val dictName = backStackEntry.arguments?.getString("dictName") ?: ""
                            
                            val historyList by historyViewModel.historyEntries.collectAsState()
                            val favoritesList by favoriteViewModel.favoriteEntries.collectAsState()
                            
                            val entry = historyList.find { it.word == word && it.dictionaryName == dictName }?.let {
                                UnifiedEntry(it.id, it.word, it.word, it.meaning, it.dictionaryName)
                            } ?: favoritesList.find { it.word == word && it.dictionaryName == dictName }?.let {
                                UnifiedEntry(it.id, it.word, it.word, it.meaning, it.dictionaryName)
                            }

                            entry?.let {
                                DetailPagerScreen(
                                    results = listOf(it),
                                    initialIndex = 0,
                                    fontSizeMultiplier = fontSizeMultiplier,
                                    favoriteViewModel = favoriteViewModel,
                                    dictionaryViewModel = dictionaryViewModel,
                                    onDismiss = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(
                            route = "dictionary_detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: 0
                            DictionaryDetailScreen(
                                dictionaryId = id,
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = "detail/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getInt("index") ?: 0
                            val results = dictionaryViewModel.searchResults.collectAsState().value
                            
                            LaunchedEffect(index, results) {
                                results.getOrNull(index)?.let { entry ->
                                    historyViewModel.addHistory(entry.word ?: "", entry.meaning ?: "", entry.dictionaryName)
                                }
                            }

                            DetailPagerScreen(
                                results = results,
                                initialIndex = index,
                                fontSizeMultiplier = fontSizeMultiplier,
                                favoriteViewModel = favoriteViewModel,
                                dictionaryViewModel = dictionaryViewModel,
                                onDismiss = { navController.popBackStack() }
                            )
                        }
                    }

                    if (isFirstRun || showManualLanguageDialog) {
                        LanguageSelectionDialog(
                            onLanguageSelected = { languageCode ->
                                coroutineScope.launch {
                                    userPreferences.updateAppLanguage(languageCode)
                                    showManualLanguageDialog = false
                                }
                            },
                            isDismissible = !isFirstRun,
                            onDismiss = { showManualLanguageDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    onLanguageSelected: (String) -> Unit,
    isDismissible: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (isDismissible) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "App Language",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Manjari
                )
                Text(
                    text = "Select your preferred language\nഭാഷ തിരഞ്ഞെടുക്കുക",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontFamily = Manjari,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                LanguageOption(
                    language = "English",
                    subtitle = "English",
                    onClick = { onLanguageSelected("en") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "മലയാളം",
                    subtitle = "Malayalam",
                    onClick = { onLanguageSelected("ml") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(
                    language = "العربية",
                    subtitle = "Arabic",
                    onClick = { onLanguageSelected("ar") }
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    language: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (language == "العربية") ScheherazadeNew else Manjari
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (language == "العربية") ScheherazadeNew else Manjari
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Manjari
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onShowLanguageDialog: () -> Unit,
    onResultClick: (Int) -> Unit,
    fontSizeMultiplier: Float
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isExactMatch by viewModel.isExactMatch.collectAsState()
    val isAnyDictionaryDownloaded by viewModel.isAnyDictionaryDownloaded.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    val dictionaries by viewModel.dictionaries.collectAsState()
    val filterDictionary by viewModel.filterDictionary.collectAsState()
    
    var showFilterDropdown by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = Manjari
                            )
                        )
                        
                        if (isAnyDictionaryDownloaded || !isFirstRun) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                Surface(
                                    onClick = { showFilterDropdown = true },
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = filterDictionary ?: "All",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontFamily = Manjari,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 100.dp)
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showFilterDropdown,
                                    onDismissRequest = { showFilterDropdown = false },
                                    modifier = Modifier.heightIn(max = 400.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Dictionaries", fontFamily = Manjari) },
                                        onClick = {
                                            viewModel.setFilterDictionary(null)
                                            showFilterDropdown = false
                                        }
                                    )
                                    HorizontalDivider()
                                    dictionaries
                                        .filter { d -> d.isSelected == 1 && viewModel.isDownloaded(d.tableName ?: "") }
                                        .sortedBy { it.displayOrder ?: 999 }
                                        .forEach { dict ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    dict.displayName ?: "", 
                                                    fontFamily = Manjari,
                                                    color = if (dict.displayName == filterDictionary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                ) 
                                            },
                                            onClick = {
                                                viewModel.setFilterDictionary(dict.displayName)
                                                showFilterDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToLibrary) {
                        Icon(
                            Icons.Default.LibraryBooks,
                            contentDescription = "Manage Dictionaries",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { 
                        viewModel.toggleExactMatch()
                        coroutineScope.launch {
                            val msg = if (!isExactMatch) "Exact match enabled" else "Exact match disabled"
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = msg,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(
                            if (isExactMatch) Icons.Default.FilterList else Icons.Default.FilterListOff,
                            contentDescription = "Toggle Exact Match",
                            tint = if (isExactMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShowLanguageDialog) {
                        Icon(
                            Icons.Default.Language, 
                            contentDescription = "Change Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!isAnyDictionaryDownloaded && isFirstRun) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.no_dicts_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = Manjari,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                stringResource(R.string.no_dicts_desc),
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = Manjari,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateToLibrary,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.go_to_library), fontFamily = Manjari)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                ) {
                    val isQueryLtr = LanguageUtils.isMalayalam(searchQuery) || LanguageUtils.isEnglish(searchQuery)
                    CompositionLocalProvider(LocalLayoutDirection provides if (isQueryLtr) LayoutDirection.Ltr else LayoutDirection.Rtl) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(28.dp)),
                            placeholder = { 
                                Text(
                                    stringResource(R.string.search_placeholder),
                                    fontFamily = Manjari,
                                    fontSize = (18 * fontSizeMultiplier).sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = if (isQueryLtr) TextAlign.Left else TextAlign.Right,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Rounded.Search, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) 
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChange("") },
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear, 
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchResults.isNotEmpty()) {
                                        focusManager.clearFocus()
                                        onResultClick(0)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                            ),
                            textStyle = TextStyle(
                                fontFamily = if (isQueryLtr) Manjari else ScheherazadeNew,
                                fontSize = (if (isQueryLtr) 18 else 22).sp * fontSizeMultiplier,
                                textDirection = if (isQueryLtr) TextDirection.Ltr else TextDirection.Rtl,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                AnimatedContent(
                    targetState = searchResults.isEmpty() to searchQuery.isEmpty(),
                    label = "results_state"
                ) { (isEmpty, isQueryEmpty) ->
                    when {
                        isQueryEmpty -> {
                            EmptyStateView(
                                icon = Icons.Rounded.Search,
                                message = stringResource(R.string.enter_search_query)
                            )
                        }
                        isEmpty -> {
                            EmptyStateView(
                                icon = Icons.Default.Info,
                                message = stringResource(R.string.no_results_found, searchQuery)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(
                                    items = searchResults,
                                    key = { _, it -> "${it.dictionaryName}_${it.id ?: it.hashCode()}" }
                                ) { index, entry ->
                                    DictionaryResultCard(
                                        entry = entry,
                                        fontSizeMultiplier = fontSizeMultiplier,
                                        isAlternate = index % 2 != 0
                                    ) {
                                        focusManager.clearFocus()
                                        onResultClick(index)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DictionaryResultCardWrapper(
    entry: UnifiedEntry,
    fontSizeMultiplier: Float,
    isAlternate: Boolean = false,
    onClick: () -> Unit
) {
    DictionaryResultCard(entry, fontSizeMultiplier, isAlternate, onClick)
}


@Composable
fun MeaningSection(
    title: String?,
    content: String,
    isLtr: Boolean,
    containerColor: Color,
    contentColor: Color,
    fontSizeMultiplier: Float,
    useManjari: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    fontFamily = Manjari,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = content,
                style = TextStyle(
                    fontSize = (if (useManjari) 18 else 22).sp * fontSizeMultiplier,
                    lineHeight = (if (useManjari) 26 else 32).sp * fontSizeMultiplier,
                    textAlign = if (isLtr) TextAlign.Left else TextAlign.Right,
                    textDirection = if (isLtr) TextDirection.Ltr else TextDirection.Rtl,
                    fontFamily = if (useManjari) Manjari else ScheherazadeNew,
                    fontWeight = if (title == null && !useManjari) FontWeight.Medium else FontWeight.Normal
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPagerScreen(
    results: List<UnifiedEntry>,
    initialIndex: Int,
    fontSizeMultiplier: Float,
    favoriteViewModel: FavoriteViewModel,
    dictionaryViewModel: DictionaryViewModel,
    onDismiss: () -> Unit
) {
    if (results.isEmpty()) return
    
    val clipboardManager = LocalClipboardManager.current
    
    var headerHeight by remember { mutableFloatStateOf(0f) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val customNestedScrollConnection = remember(scrollBehavior) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                scrollBehavior.state.heightOffset += consumed.y
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(headerHeight) {
        if (headerHeight > 0f) {
            scrollBehavior.state.heightOffsetLimit = -headerHeight
        }
    }
    
    val dictionariesFlow by dictionaryViewModel.dictionaries.collectAsState()
    val uniqueDictionaries = remember(results, dictionariesFlow) { 
        results.map { it.dictionaryName }.distinct().sortedBy { name ->
            dictionariesFlow.find { it.displayName == name }?.displayOrder ?: 999
        }
    }
    var selectedDict by remember { mutableStateOf(results.getOrNull(initialIndex)?.dictionaryName ?: uniqueDictionaries[0]) }
    var expanded by remember { mutableStateOf(false) }

    val filteredResults = remember(results, selectedDict) {
        results.filter { it.dictionaryName == selectedDict }
    }

    val initialPage = remember(filteredResults, initialIndex, results) {
        val originalItem = results.getOrNull(initialIndex)
        val index = filteredResults.indexOf(originalItem)
        if (index >= 0) index else 0
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { filteredResults.size }
    )

    val context = LocalContext.current
    val window = (context as? android.app.Activity)?.window
    val insetsController = remember(window) {
        window?.let { WindowInsetsControllerCompat(it, it.decorView) }
    }

    LaunchedEffect(scrollBehavior.state.collapsedFraction) {
        if (scrollBehavior.state.collapsedFraction > 0.5f) {
            insetsController?.hide(WindowInsetsCompat.Type.statusBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // Ensure status bar is shown when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    LaunchedEffect(selectedDict) {
        if (pagerState.currentPage >= filteredResults.size) {
            pagerState.scrollToPage(0)
        }
    }

    val currentEntry = filteredResults.getOrNull(pagerState.currentPage) ?: filteredResults.getOrNull(0)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(customNestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> filteredResults.getOrNull(page)?.let { "${it.dictionaryName}_${it.id}" } ?: page }
            ) { page ->
                val pageEntry = filteredResults[page]
                val isLtr = LanguageUtils.isLtrContent(pageEntry)
                val rawMeaning = pageEntry.meaning ?: ""

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = if (isLtr) Alignment.Start else Alignment.End
                ) {
                    Spacer(modifier = Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { headerHeight.toDp() } + 16.dp))
                    
                    SelectionContainer {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (rawMeaning.contains("[[")) {
                                if (rawMeaning.contains("[[SUBHEADING]]")) {
                                    val sub = rawMeaning.substringAfter("[[SUBHEADING]]", "").substringBefore("[[MEANING]]").trim()
                                    val mean = cleanHtml(rawMeaning.substringAfter("[[MEANING]]", ""))
                                    
                                    if (sub.isNotEmpty()) {
                                        MeaningSection(
                                            title = null,
                                            content = sub,
                                            isLtr = false,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSizeMultiplier = fontSizeMultiplier,
                                            useManjari = false
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    
                                    MeaningSection(
                                        title = "Definition",
                                        content = mean,
                                        isLtr = isLtr,
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        fontSizeMultiplier = fontSizeMultiplier,
                                        useManjari = isLtr
                                    )
                                } else {
                                    val arPart = rawMeaning.substringAfter("[[ARABIC]]", "").substringBefore("[[MALAYALAM]]").substringBefore("[[ENGLISH]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                                    val malPart = rawMeaning.substringAfter("[[MALAYALAM]]", "").substringBefore("[[ENGLISH]]").substringBefore("[[ARABIC]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                                    val engPart = rawMeaning.substringAfter("[[ENGLISH]]", "").substringBefore("[[ARABIC]]").substringBefore("[[MALAYALAM]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()

                                    if (arPart.isNotEmpty()) {
                                        MeaningSection(
                                            title = "Arabic",
                                            content = arPart.split(",").joinToString("\n") { it.trim() },
                                            isLtr = false,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSizeMultiplier = fontSizeMultiplier,
                                            useManjari = false
                                        )
                                    }

                                    if (malPart.isNotEmpty()) {
                                        MeaningSection(
                                            title = "Malayalam",
                                            content = malPart.split(",").joinToString("\n") { it.trim() },
                                            isLtr = true,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSizeMultiplier = fontSizeMultiplier,
                                            useManjari = true
                                        )
                                    }

                                    if (engPart.isNotEmpty()) {
                                        MeaningSection(
                                            title = "English",
                                            content = engPart.split(",").joinToString("\n") { it.trim() },
                                            isLtr = true,
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontSizeMultiplier = fontSizeMultiplier,
                                            useManjari = true
                                        )
                                    }
                                }
                            } else {
                                val cleanedMeaning = cleanHtml(rawMeaning)
                                val displayMeaning = if (LanguageUtils.isMalayalam(cleanedMeaning) || LanguageUtils.isEnglish(cleanedMeaning)) {
                                    cleanedMeaning.split(",").joinToString("\n") { it.trim() }
                                } else {
                                    cleanedMeaning
                                }
                                
                                val useManjariForMeaning = LanguageUtils.isMalayalam(displayMeaning) || LanguageUtils.isEnglish(displayMeaning) || isLtr
                                
                                MeaningSection(
                                    title = null,
                                    content = displayMeaning,
                                    isLtr = isLtr,
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    fontSizeMultiplier = fontSizeMultiplier,
                                    useManjari = useManjariForMeaning
                                )
                            }
                        }
                    }
                }
            }

            val fraction = scrollBehavior.state.collapsedFraction
            val alpha = androidx.compose.animation.core.FastOutLinearInEasing.transform(1f - fraction)
            val translateY = -(fraction * (headerHeight * 0.1f))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { headerHeight = it.size.height.toFloat() }
                    .graphicsLayer { 
                        this.alpha = alpha 
                        this.translationY = translateY
                    }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha))
            ) {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalViewConfiguration provides androidx.compose.ui.platform.LocalViewConfiguration.current) {
                    TopAppBar(
                        title = {
                            val word = currentEntry?.word ?: ""
                            val useManjariForWord = LanguageUtils.isMalayalam(word) || (currentEntry?.dictionaryName?.contains("Malayalam", ignoreCase = true) ?: false)

                            Text(
                                text = word,
                                fontFamily = if (useManjariForWord) Manjari else ScheherazadeNew,
                                fontSize = (if (useManjariForWord) 20 else 24).sp * fontSizeMultiplier,
                                fontWeight = FontWeight.Bold,
                                textAlign = if (useManjariForWord) TextAlign.Left else TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                IconButton(onClick = onDismiss, enabled = alpha > 0.5f) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                                
                                Box {
                                    Surface(
                                        onClick = { if (alpha > 0.5f) expanded = true },
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.wrapContentSize(),
                                        enabled = alpha > 0.5f
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (selectedDict.length > 12) selectedDict.take(12) + "..." else selectedDict,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                                                fontFamily = Manjari,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alpha),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expanded && alpha > 0.5f,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.heightIn(max = 400.dp)
                                    ) {
                                        uniqueDictionaries.forEach { dictName ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Text(
                                                        dictName, 
                                                        fontFamily = Manjari,
                                                        color = if (dictName == selectedDict) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    ) 
                                                },
                                                onClick = {
                                                    selectedDict = dictName
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            val isFav by (currentEntry?.let { 
                                favoriteViewModel.isFavorite(it.word ?: "", it.dictionaryName).collectAsState(initial = false)
                            } ?: remember { mutableStateOf(false) })
                            
                            IconButton(
                                onClick = {
                                    currentEntry?.let { entry ->
                                        favoriteViewModel.toggleFavorite(
                                            entry.word ?: "",
                                            entry.meaning ?: "",
                                            entry.dictionaryName,
                                            isFav
                                        )
                                    }
                                },
                                enabled = alpha > 0.5f
                            ) {
                                Icon(
                                    if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    val rawMeaning = currentEntry?.meaning
                                    val dictName = currentEntry?.dictionaryName
                                    if (rawMeaning != null && dictName != null) {
                                        val finalMeaning = if (rawMeaning.contains("[[")) {
                                            val ar = rawMeaning.substringAfter("[[ARABIC]]", "").substringBefore("[[MALAYALAM]]").substringBefore("[[ENGLISH]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                                            val mal = rawMeaning.substringAfter("[[MALAYALAM]]", "").substringBefore("[[ENGLISH]]").substringBefore("[[ARABIC]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                                            val eng = rawMeaning.substringAfter("[[ENGLISH]]", "").substringBefore("[[ARABIC]]").substringBefore("[[MALAYALAM]]").replace(Regex("\\[\\[.*?\\]\\]"), "").trim()
                                            buildString {
                                                if (ar.isNotEmpty()) append("Arabic:\n${ar.split(",").joinToString("\n") { it.trim() }}\n\n")
                                                if (mal.isNotEmpty()) append("Malayalam:\n${mal.split(",").joinToString("\n") { it.trim() }}\n\n")
                                                if (eng.isNotEmpty()) append("English:\n${eng.split(",").joinToString("\n") { it.trim() }}")
                                            }.trim()
                                        } else if (LanguageUtils.isMalayalam(rawMeaning)) {
                                            rawMeaning.split(",").joinToString("\n") { it.trim() }
                                        } else {
                                            rawMeaning
                                        }
                                        val textToCopy = "$dictName\n\n$finalMeaning"
                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                    }
                                },
                                enabled = alpha > 0.5f
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
