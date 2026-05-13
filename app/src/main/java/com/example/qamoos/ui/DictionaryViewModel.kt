package com.example.qamoos.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.qamoos.data.ArabicDao
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.utils.DictionaryManager
import com.example.qamoos.utils.DictionaryUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val arabicDao: ArabicDao,
    private val userPreferences: com.example.qamoos.data.UserPreferences,
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    val isFirstRun: StateFlow<Boolean> = userPreferences.isFirstRun
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isExactMatch = MutableStateFlow(false)
    val isExactMatch: StateFlow<Boolean> = _isExactMatch

    private val _filterDictionary = MutableStateFlow<String?>(null)
    val filterDictionary: StateFlow<String?> = _filterDictionary

    val dictionaries: StateFlow<List<DictionaryInfo>> = combine(
        arabicDao.getAllDictionaries(),
        userPreferences.selectedDictionaries,
        userPreferences.dictionaryOrder
    ) { list, selectedSet, savedOrder ->
        list.map { 
            it.copy(
                displayName = DictionaryUtils.getNativeName(it.tableName, it.displayName),
                isSelected = if (selectedSet.contains(it.tableName)) 1 else 0,
                displayOrder = savedOrder.indexOf(it.tableName).let { idx -> if (idx == -1) 999 else idx }
            )
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isAnyDictionaryDownloaded: StateFlow<Boolean> = dictionaries
        .map { list -> list.any { dictionaryManager.isDownloaded(it.tableName ?: "") } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        // No longer need the manual polling loop as it's handled by the Flow above
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<UnifiedEntry>> = combine(
        _searchQuery.debounce(300).distinctUntilChanged(),
        _isExactMatch,
        dictionaries,
        _filterDictionary
    ) { query, exact, dicts, filter ->
        var availableDicts = dicts.filter { 
            (it.isSelected == 1) && dictionaryManager.isDownloaded(it.tableName ?: "") 
        }
        
        if (filter != null) {
            availableDicts = availableDicts.filter { it.displayName == filter }
        }
        
        Triple(query, exact, availableDicts)
    }.flatMapLatest { (originalQuery: String, exact: Boolean, dicts: List<DictionaryInfo>) ->
        if (originalQuery.isEmpty() || dicts.isEmpty()) {
            flowOf(emptyList<UnifiedEntry>())
        } else {
            val normalizedQuery = normalizeArabic(originalQuery)
            
            flow {
                val allResults = mutableListOf<UnifiedEntry>()
                
                // Search each dictionary individually to ensure that a failure in one 
                // (e.g., due to different table structure) doesn't prevent results from others.
                dicts.forEach { dict ->
                    val tableName = dict.tableName ?: return@forEach
                    val path = dictionaryManager.getDictionaryPath(tableName) ?: return@forEach
                    
                    val attachments = listOf(Pair(path, tableName))
                    val sqliteQuery = buildMultiTableQuery(listOf(dict), normalizedQuery, exact)
                    
                    try {
                        val result = arabicDao.searchWithAttachments(sqliteQuery, attachments)
                        allResults.addAll(result)
                    } catch (e: Exception) {
                        Log.e("DictionaryViewModel", "Search failed for $tableName: ${e.message}")
                    }
                }
                
                // Sort results: matches to the exact query first, then by dictionary priority
                val sortedResults = allResults.sortedWith(
                    compareByDescending<UnifiedEntry> { 
                        (it.wordNoHarakah == normalizedQuery || it.word == originalQuery)
                    }.thenBy { 
                        it.displayOrder ?: 999
                    }.thenBy { 
                        it.word?.length ?: Int.MAX_VALUE 
                    }
                )
                emit(sortedResults)
            }
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.IO)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun buildMultiTableQuery(dicts: List<DictionaryInfo>, query: String, exact: Boolean): SimpleSQLiteQuery {
        val validDicts = dicts.filter { it.tableName != null && it.displayName != null }
        if (validDicts.isEmpty()) return SimpleSQLiteQuery("SELECT 1 WHERE 0")
        
        val dbQuery = if (exact) query else "%$query%" 
        val operator = if (exact) "=" else "LIKE"
        
        val selectStatements = validDicts.map { dict ->
            val tableName = dict.tableName!!
            val displayName = dict.displayName!!
            val displayOrder = if (tableName == "taj") -1 else (dict.displayOrder ?: 0)
            
            val (idCol, wordCol, meaningCol, searchCol) = when (tableName.lowercase()) {
                "arabic" -> Quadruple("No", "word", "'[[MALAYALAM]]' || IFNULL(M_Malayalam, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word_no_harakah")
                "english" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_arabic, '') || '[[MALAYALAM]]' || IFNULL(M_malayalam, '')", "word")
                "malayalam" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_Arabic, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word")
                "irab" -> Quadruple("id", "title", "'[[SUBHEADING]]' || IFNULL(konten, '') || '[[MEANING]]' || IFNULL(text, '')", "title")
                "maany", "afaal", "frooq", "maany_dict" -> Quadruple("id", "word", "meaning", "word_no_harakah")
                else -> Quadruple("_id", "word", "meaning", "word_no_harakah")
            }

            // Use subqueries to allow per-table LIMIT when used with UNION ALL
            "SELECT * FROM (SELECT $idCol as id, $wordCol as word, $searchCol as wordNoHarakah, $meaningCol as meaning, '${displayName.replace("'", "''")}' as dictionaryName, $displayOrder as displayOrder FROM \"$tableName\".$tableName WHERE $searchCol $operator ? LIMIT 50)"
        }

        val finalQuery = selectStatements.joinToString(" UNION ALL ")
        val args = Array(selectStatements.size) { dbQuery }

        return SimpleSQLiteQuery(finalQuery, args)
    }

    private fun normalizeArabic(text: String): String {
        if (text.isBlank()) return text
        // Remove diacritics and tatweel
        val regex = Regex("[\u064B-\u0652\u0640]")
        return text.replace(regex, "")
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun isDownloaded(tableName: String): Boolean {
        return dictionaryManager.isDownloaded(tableName)
    }

    fun setFilterDictionary(name: String?) {
        _filterDictionary.value = name
    }

    fun toggleExactMatch() {
        _isExactMatch.value = !_isExactMatch.value
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
