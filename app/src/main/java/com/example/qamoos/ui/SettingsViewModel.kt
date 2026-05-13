package com.example.qamoos.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.qamoos.data.ArabicDao
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.data.UserPreferences
import com.example.qamoos.utils.DictionaryManager
import com.example.qamoos.utils.DictionaryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val arabicDao: ArabicDao,
    private val userPreferences: UserPreferences,
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    val downloadProgress = dictionaryManager.downloadProgress
    val isPaused = dictionaryManager.isPaused
    val downloadingTables = dictionaryManager.downloadingTables
    private val _onlineDictNames = MutableStateFlow<List<String>>(emptyList())
    
    private val _startingDownloads = MutableStateFlow<Set<String>>(emptySet())
    val startingDownloads: StateFlow<Set<String>> = _startingDownloads

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        viewModelScope.launch {
            _onlineDictNames.value = dictionaryManager.getOnlineDictionaryNames()
        }
    }

    val dictionaries: StateFlow<List<DictionaryInfo>> = combine(
        arabicDao.getAllDictionaries(),
        _onlineDictNames,
        combine(userPreferences.selectedDictionaries, userPreferences.dictionaryOrder) { selected, order -> Pair(selected, order) }
    ) { list, onlineNames, prefPair ->
        val selectedSet = prefPair.first
        val savedOrder = prefPair.second
        
        val filteredList = if (onlineNames.isEmpty()) {
            list
        } else {
            list.filter { it.tableName?.lowercase() in onlineNames.map { n -> n.lowercase() } }
        }
        
        filteredList.map { 
            it.copy(
                displayName = DictionaryUtils.getNativeName(it.tableName, it.displayName),
                isSelected = if (selectedSet.contains(it.tableName)) 1 else 0,
                displayOrder = savedOrder.indexOf(it.tableName).let { idx -> if (idx == -1) 999 else idx }
            )
        }.sortedWith(
            compareBy<DictionaryInfo> { 
                when (it.tableName?.lowercase()) {
                    "taj" -> 1
                    "lisanularab" -> 2
                    "arabic" -> 3
                    "malayalam" -> 4
                    "english" -> 5
                    "misbah" -> 6
                    else -> 999
                }
            }.thenBy { it.displayOrder ?: 999 }
                .thenBy { it.displayName }
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
        Log.d("SettingsViewModel", "downloadDictionary requested for: $tableName")
        tableName?.let { table ->
            val downloadedCount = dictionaries.value.count { dict -> dictionaryManager.isDownloaded(dict.tableName ?: "") }
            if (downloadedCount >= 10) {
                _errorMessage.value = "Downloading more than 10 dictionaries will lead to error. If you want to use more than 10 dictionaries, use the offline version."
                return
            }
            
            _startingDownloads.value = _startingDownloads.value + table
            val nativeName = DictionaryUtils.getNativeName(table, table)
            viewModelScope.launch {
                try {
                    _errorMessage.value = null
                    Log.i("SettingsViewModel", "Starting download job for: $table")
                    dictionaryManager.downloadDictionary(table)
                    Log.i("SettingsViewModel", "Download job completed for: $table")
                    userPreferences.toggleDictionary(table, true)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Error in download job for: $table", e)
                    _errorMessage.value = "Error downloading $nativeName: ${e.message ?: "Unknown error"}"
                } finally {
                    _startingDownloads.value = _startingDownloads.value - table
                }
            }
        } ?: Log.e("SettingsViewModel", "tableName is null, cannot download")
    }

    fun clearError() {
        _errorMessage.value = null
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
                userPreferences.toggleDictionary(tableName, false)
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
        val tableName = dictionary.tableName ?: return
        viewModelScope.launch {
            userPreferences.toggleDictionary(tableName, dictionary.isSelected != 1)
        }
    }

    fun toggleAllDictionaries(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                userPreferences.updateSelectedDictionaries(dictionaries.value.mapNotNull { it.tableName }.toSet())
            } else {
                userPreferences.updateSelectedDictionaries(emptySet())
            }
        }
    }

    fun downloadAllDictionaries() {
        viewModelScope.launch {
            var currentDownloaded = dictionaries.value.count { dict -> dictionaryManager.isDownloaded(dict.tableName ?: "") }
            
            dictionaries.value.forEach { dict ->
                val tableName = dict.tableName ?: return@forEach
                if (!dictionaryManager.isDownloaded(tableName) && 
                    !dictionaryManager.downloadProgress.value.containsKey(tableName)) {
                    
                    if (currentDownloaded >= 10) {
                        _errorMessage.value = "Downloading more than 10 dictionaries will lead to error. If you want to use more than 10 dictionaries, use the offline version."
                        return@launch
                    }
                    
                    _startingDownloads.value = _startingDownloads.value + tableName
                    val nativeName = DictionaryUtils.getNativeName(tableName, tableName)
                    currentDownloaded++
                    launch {
                        try {
                            _errorMessage.value = null
                            dictionaryManager.downloadDictionary(tableName)
                            userPreferences.toggleDictionary(tableName, true)
                        } catch (e: Exception) {
                            Log.e("SettingsViewModel", "Error in download all for: $tableName", e)
                            _errorMessage.value = "Error downloading $nativeName: ${e.message ?: "Unknown error"}"
                        } finally {
                            _startingDownloads.value = _startingDownloads.value - tableName
                        }
                    }
                }
            }
        }
    }

    fun updateDictionaryOrders(newOrder: List<DictionaryInfo>) {
        viewModelScope.launch {
            userPreferences.updateDictionaryOrder(newOrder.mapNotNull { it.tableName })
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
