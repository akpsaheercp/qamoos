package com.example.qamoos.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.ui.theme.Manjari
import com.example.qamoos.utils.DictionaryMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryListScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDictionaryClick: (Int) -> Unit
) {
    val dictionariesFlow by viewModel.dictionaries.collectAsState()
    var dictionaries by remember { mutableStateOf(emptyList<DictionaryInfo>()) }
    
    LaunchedEffect(dictionariesFlow) {
        dictionaries = dictionariesFlow
    }

    val gridState = rememberLazyGridState()
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }

    fun onGridDrag(offset: Offset) {
        draggingOffset += offset
        val draggedIndex = draggedItemIndex ?: return
        
        val layoutInfo = gridState.layoutInfo
        val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.index == draggedIndex } ?: return
        
        val currentX = draggedItemInfo.offset.x + draggedItemInfo.size.width / 2 + draggingOffset.x
        val currentY = draggedItemInfo.offset.y + draggedItemInfo.size.height / 2 + draggingOffset.y
        
        val targetItem = layoutInfo.visibleItemsInfo.find { item ->
            val itemLeft = item.offset.x
            val itemRight = item.offset.x + item.size.width
            val itemTop = item.offset.y
            val itemBottom = item.offset.y + item.size.height
            currentX > itemLeft && currentX < itemRight && currentY > itemTop && currentY < itemBottom
        }
        
        if (targetItem != null && targetItem.index != draggedIndex) {
            val newList = dictionaries.toMutableList()
            val item = newList.removeAt(draggedIndex)
            newList.add(targetItem.index, item)
            dictionaries = newList
            draggedItemIndex = targetItem.index
            draggingOffset = Offset.Zero
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Dictionaries", fontFamily = Manjari, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.downloadAllDictionaries() }) {
                        Icon(Icons.Default.Download, contentDescription = "Download All")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { viewModel.toggleAllDictionaries(true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable All", fontFamily = Manjari, fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = { viewModel.toggleAllDictionaries(false) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Default.RemoveDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disable All", fontFamily = Manjari, fontSize = 12.sp)
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Drag and drop dictionaries to set your search priority. Top dictionaries appear first.",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = Manjari,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(dictionaries.size, key = { dictionaries[it].id ?: dictionaries[it].hashCode() }) { index ->
                    val dict = dictionaries[index]
                    val isDragging = draggedItemIndex == index
                    val elevation by animateDpAsState(if (isDragging) 12.dp else 2.dp, label = "elevation")
                    val scale by animateDpAsState(if (isDragging) 1.05.dp else 1.dp, label = "scale")
                    
                    val isDownloaded = viewModel.isDownloaded(dict.tableName)
                    val progress by viewModel.downloadProgress.collectAsState()
                    val isPausedMap by viewModel.isPaused.collectAsState()
                    val currentProgress = progress[dict.tableName]
                    val isPaused = isPausedMap[dict.tableName] ?: false

                    DictionaryGridItem(
                        dict = dict,
                        index = index + 1,
                        isDownloaded = isDownloaded,
                        downloadProgress = currentProgress,
                        isPaused = isPaused,
                        onDownloadClick = { viewModel.downloadDictionary(dict.tableName) },
                        onPauseClick = { viewModel.pauseDownload(dict.tableName) },
                        onResumeClick = { viewModel.resumeDownload(dict.tableName) },
                        onCancelClick = { viewModel.cancelDownload(dict.tableName) },
                        onDeleteClick = { viewModel.deleteDictionary(dict) },
                        onToggle = { viewModel.toggleDictionary(dict) },
                        onClick = { dict.id?.let { onDictionaryClick(it) } },
                        modifier = Modifier
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                val s = scale.value
                                scaleX = s
                                scaleY = s
                                shadowElevation = elevation.toPx()
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                                
                                if (isDragging) {
                                    translationX = draggingOffset.x.dp.toPx()
                                    translationY = draggingOffset.y.dp.toPx()
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        draggedItemIndex = index
                                        draggingOffset = Offset.Zero
                                    },
                                    onDragEnd = {
                                        viewModel.updateDictionaryOrders(dictionaries)
                                        draggedItemIndex = null
                                        draggingOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        draggingOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onGridDrag(dragAmount)
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun DictionaryGridItem(
    dict: DictionaryInfo,
    index: Int,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    isPaused: Boolean,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        downloadProgress != null -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        dict.isSelected == 1 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    }

    val contentColor = when {
        downloadProgress != null -> MaterialTheme.colorScheme.onTertiaryContainer
        dict.isSelected == 1 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Position Badge
            Surface(
                color = if (dict.isSelected == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = if (dict.isSelected == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                    fontFamily = Manjari,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dict.displayName ?: "Unknown",
                    fontFamily = Manjari,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 4.dp)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDownloaded) {
                        IconButton(
                            onClick = onDeleteClick, 
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Switch(
                            checked = dict.isSelected == 1,
                            onCheckedChange = { onToggle() },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    } else if (downloadProgress != null) {
                        IconButton(onClick = if (isPaused) onResumeClick else onPauseClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.tertiary
                            )
                            Text("${downloadProgress.toInt()}%", fontSize = 9.sp, fontFamily = Manjari, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = onCancelClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Surface(
                            onClick = onDownloadClick,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Download, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = when {
                        isDownloaded -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        downloadProgress != null -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when {
                            isDownloaded -> "Installed"
                            downloadProgress != null -> if (isPaused) "Paused" else "Downloading..."
                            else -> DictionaryMetadata.getSize(dict.tableName)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isDownloaded -> MaterialTheme.colorScheme.primary
                            downloadProgress != null -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontFamily = Manjari,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
