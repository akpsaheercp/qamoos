package com.example.qamoos.utils

import com.example.qamoos.data.UnifiedEntry

object LanguageUtils {
    fun isMalayalam(text: String?): Boolean {
        return text?.any { it in '\u0D00'..'\u0D7F' } ?: false
    }

    fun isArabic(text: String?): Boolean {
        return text?.any { it in '\u0600'..'\u06FF' } ?: false
    }

    fun isEnglish(text: String?): Boolean {
        val cleaned = text?.replace(Regex("<[^>]*>"), "") ?: ""
        return cleaned.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    fun isLtrContent(entry: UnifiedEntry): Boolean {
        val isMalayalamDict = entry.dictionaryName.contains("Malayalam", ignoreCase = true)
        val isEnglishDict = entry.dictionaryName.contains("English", ignoreCase = true)
        val word = entry.word ?: ""
        val meaning = entry.meaning ?: ""
        
        if (isArabic(word) || isArabic(meaning.replace(Regex("<[^>]*>"), ""))) {
            if (!isMalayalamDict && !isEnglishDict) return false
        }
        
        return isMalayalamDict || isEnglishDict || 
               isMalayalam(meaning) || isEnglish(meaning) ||
               isMalayalam(word) || isEnglish(word)
    }
}
