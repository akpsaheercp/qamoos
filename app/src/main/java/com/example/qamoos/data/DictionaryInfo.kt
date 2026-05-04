package com.example.qamoos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_info")
data class DictionaryInfo(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int?,
    @ColumnInfo(name = "table_name")
    val tableName: String?,
    @ColumnInfo(name = "display_name")
    val displayName: String?,
    @ColumnInfo(name = "is_selected")
    val isSelected: Int? = 1,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int? = 0
)
