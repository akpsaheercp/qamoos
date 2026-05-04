package com.example.qamoos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_favorites")
data class FavoriteEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val word: String,
    val meaning: String,
    val dictionaryName: String,
    val timestamp: Long = System.currentTimeMillis()
)
