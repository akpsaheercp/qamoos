package com.example.qamoos.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Arabic",
    indices = [
        Index(value = ["word"], name = "idx_arabic_word"),
        Index(value = ["M_English"], name = "idx_arabic_english"),
        Index(value = ["M_Malayalam"], name = "idx_arabic_malayalam"),
        Index(value = ["word_no_harakah"], name = "idx_arabic_word_no_harakah")
    ]
)
data class ArabicEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "No")
    val id: Int,
    @ColumnInfo(name = "word")
    val word: String?,
    @ColumnInfo(name = "M_English")
    val meaningEnglish: String?,
    @ColumnInfo(name = "M_Malayalam")
    val meaningMalayalam: String?,
    @ColumnInfo(name = "word_no_harakah")
    val wordNoHarakah: String?
)
