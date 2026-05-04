package com.example.qamoos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qamoos.data.FavoriteDao
import com.example.qamoos.data.FavoriteEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteViewModel(private val favoriteDao: FavoriteDao) : ViewModel() {

    val favoriteEntries: StateFlow<List<FavoriteEntry>> = favoriteDao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleFavorite(word: String, meaning: String, dictionaryName: String, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) {
                favoriteDao.deleteFavorite(word, dictionaryName)
            } else {
                favoriteDao.insertFavorite(
                    FavoriteEntry(
                        word = word,
                        meaning = meaning,
                        dictionaryName = dictionaryName
                    )
                )
            }
        }
    }

    fun isFavorite(word: String, dictionaryName: String): Flow<Boolean> {
        return favoriteDao.isFavorite(word, dictionaryName)
    }
}

class FavoriteViewModelFactory(private val favoriteDao: FavoriteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(favoriteDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
