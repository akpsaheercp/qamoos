package com.example.qamoos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qamoos.data.ArabicDao
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.data.UserPreferences
import com.example.qamoos.utils.DictionaryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val arabicDao: ArabicDao,
    private val userPreferences: UserPreferences,
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    val downloadProgress = dictionaryManager.downloadProgress
    val isPaused = dictionaryManager.isPaused
    private val _onlineDictNames = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch {
            _onlineDictNames.value = dictionaryManager.getOnlineDictionaryNames()
        }
    }

    val dictionaries: StateFlow<List<DictionaryInfo>> = combine(
        arabicDao.getAllDictionaries(),
        downloadProgress, // Trigger update when download finishes
        isPaused,
        _onlineDictNames
    ) { list, _, _, onlineNames ->
        // Only show dictionaries that exist in our online storage
        val filteredList = if (onlineNames.isEmpty()) {
            list // Fallback to all if offline/error during fetch
        } else {
            list.filter { it.tableName?.lowercase() in onlineNames.map { n -> n.lowercase() } }
        }
        
        filteredList.map { it.copy() } // Just to trigger UI refresh if needed, but we'll use a better approach
        filteredList.sortedWith(
            compareByDescending<DictionaryInfo> { it.tableName == "taj" }
                .thenBy { it.displayOrder ?: 0 }
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun isDownloaded(tableName: String?): Boolean {
        return tableName?.let { dictionaryManager.isDownloaded(it) } ?: false
    }

    fun downloadDictionary(tableName: String?) {
        val dict = dictionaries.value.find { it.tableName == tableName }
        tableName?.let {
            viewModelScope.launch {
                try {
                    dictionaryManager.downloadDictionary(it)
                    // Automatically enable after successful download
                    dict?.id?.let { id ->
                        arabicDao.updateDictionarySelection(id, 1)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun pauseDownload(tableName: String?) {
        tableName?.let { dictionaryManager.pauseDownload(it) }
    }

    fun resumeDownload(tableName: String?) {
        tableName?.let { dictionaryManager.resumeDownload(it) }
    }

    fun cancelDownload(tableName: String?) {
        tableName?.let { dictionaryManager.cancelDownload(it) }
    }

    fun deleteDictionary(dictionary: DictionaryInfo) {
        val tableName = dictionary.tableName ?: return
        viewModelScope.launch {
            if (dictionaryManager.deleteDictionary(tableName)) {
                // Ensure selection is turned off if deleted
                arabicDao.updateDictionarySelection(dictionary.id ?: return@launch, 0)
            }
        }
    }

    val fontSizeMultiplier: StateFlow<Float> = userPreferences.fontSizeMultiplier
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val darkThemeConfig: StateFlow<String> = userPreferences.darkThemeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    val appLanguage: StateFlow<String?> = userPreferences.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isFirstRun: StateFlow<Boolean> = userPreferences.isFirstRun
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleDictionary(dictionary: DictionaryInfo) {
        val id = dictionary.id ?: return
        viewModelScope.launch {
            arabicDao.updateDictionarySelection(
                id,
                if (dictionary.isSelected == 1) 0 else 1
            )
        }
    }

    fun toggleAllDictionaries(enable: Boolean) {
        viewModelScope.launch {
            arabicDao.updateAllDictionariesSelection(if (enable) 1 else 0)
        }
    }

    fun downloadAllDictionaries() {
        viewModelScope.launch {
            dictionaries.value.forEach { dict ->
                val tableName = dict.tableName ?: return@forEach
                if (!dictionaryManager.isDownloaded(tableName) && 
                    !dictionaryManager.downloadProgress.value.containsKey(tableName)) {
                    launch {
                        try {
                            dictionaryManager.downloadDictionary(tableName)
                            // Automatically enable after successful download
                            dict.id?.let { id ->
                                arabicDao.updateDictionarySelection(id, 1)
                            }
                        } catch (e: Exception) {
                            // Individual failures don't stop others
                        }
                    }
                }
            }
        }
    }

    fun updateDictionaryOrders(newOrder: List<DictionaryInfo>) {
        viewModelScope.launch {
            arabicDao.updateDictionaryOrders(newOrder.mapNotNull { it.id })
        }
    }

    fun updateFontSize(multiplier: Float) {
        viewModelScope.launch {
            userPreferences.updateFontSizeMultiplier(multiplier)
        }
    }

    fun updateTheme(config: String) {
        viewModelScope.launch {
            userPreferences.updateDarkThemeConfig(config)
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            userPreferences.updateAppLanguage(languageCode)
        }
    }
}

class SettingsViewModelFactory(
    private val arabicDao: ArabicDao,
    private val userPreferences: UserPreferences,
    private val dictionaryManager: DictionaryManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(arabicDao, userPreferences, dictionaryManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
