package com.example.qamoos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.qamoos.data.ArabicDao
import com.example.qamoos.data.UserPreferences
import com.example.qamoos.utils.DictionaryManager

class DictionaryViewModelFactory(
    private val arabicDao: ArabicDao,
    private val userPreferences: UserPreferences,
    private val dictionaryManager: DictionaryManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DictionaryViewModel(arabicDao, userPreferences, dictionaryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
