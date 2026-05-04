package com.example.qamoos.data

import androidx.room.ColumnInfo

/**
 * Unified search result entry. 
 * Optimized with explicit ColumnInfo for surgical SQL mapping.
 * id is made nullable to support tables with optional primary keys.
 */
data class UnifiedEntry(
    @ColumnInfo(name = "id")
    val id: Int?,
    @ColumnInfo(name = "word")
    val word: String?,
    @ColumnInfo(name = "wordNoHarakah")
    val wordNoHarakah: String?,
    @ColumnInfo(name = "meaning")
    val meaning: String?,
    @ColumnInfo(name = "dictionaryName")
    val dictionaryName: String,
    @ColumnInfo(name = "displayOrder")
    val displayOrder: Int = 0
)
