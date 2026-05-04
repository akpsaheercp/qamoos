package com.example.qamoos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.qamoos.data.ArabicDao
import com.example.qamoos.data.DictionaryInfo
import com.example.qamoos.data.UnifiedEntry
import com.example.qamoos.utils.DictionaryManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val arabicDao: ArabicDao,
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isExactMatch = MutableStateFlow(false)
    val isExactMatch: StateFlow<Boolean> = _isExactMatch

    private val _filterDictionary = MutableStateFlow<String?>(null)
    val filterDictionary: StateFlow<String?> = _filterDictionary

    val selectedDictionaries: StateFlow<List<DictionaryInfo>> = arabicDao.getSelectedDictionaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAnyDictionaryDownloaded = MutableStateFlow(false)
    val isAnyDictionaryDownloaded: StateFlow<Boolean> = _isAnyDictionaryDownloaded

    init {
        viewModelScope.launch {
            // Regularly check if dictionaries are downloaded
            // We can also trigger this on specific events if needed
            while(true) {
                val allDicts = arabicDao.getAllDictionariesInternal()
                val anyDownloaded = allDicts.any { dictionaryManager.isDownloaded(it.tableName ?: "") }
                _isAnyDictionaryDownloaded.value = anyDownloaded
                kotlinx.coroutines.delay(2000) // Check every 2 seconds
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<UnifiedEntry>> = combine(
        _searchQuery.debounce(250).distinctUntilChanged(),
        _isExactMatch,
        selectedDictionaries,
        dictionaryManager.downloadingTables,
        _filterDictionary
    ) { query, exact, dicts, _, filter ->
        // Filter: only use dictionaries that are BOTH selected by user AND downloaded
        var availableDicts = dicts.filter { 
            (it.isSelected == 1) && dictionaryManager.isDownloaded(it.tableName ?: "") 
        }
        
        // Apply UI filter if selected
        if (filter != null) {
            availableDicts = availableDicts.filter { it.displayName == filter }
        }
        
        Triple(query, exact, availableDicts)
    }.flatMapLatest { (originalQuery, exact, dicts) ->
        if (originalQuery.isEmpty() || dicts.isEmpty()) {
            flowOf(emptyList())
        } else {
            val normalizedQuery = normalizeArabic(originalQuery)

            // Perform search with transactional attachments
            flow {
                val attachments = dicts.mapNotNull { dict ->
                    val path = dictionaryManager.getDictionaryPath(dict.tableName ?: "")
                    if (path != null) Pair(path, dict.tableName!!) else null
                }

                if (attachments.isEmpty()) {
                    emit(emptyList<UnifiedEntry>())
                } else {
                    val sqliteQuery = buildMultiTableQuery(dicts, normalizedQuery, exact)
                    try {
                        val results = arabicDao.searchWithAttachments(sqliteQuery, attachments)
                        emit(results)
                    } catch (e: Exception) {
                        emit(emptyList<UnifiedEntry>())
                    }
                }
            }
            .map { list ->
                    // Sort results: 
                    // 1. Exact matches first
                    // 2. Then by Dictionary Display Order
                    // 3. Then by word length
                    list.sortedWith(
                        compareByDescending<UnifiedEntry> { 
                            it.wordNoHarakah == normalizedQuery || it.word == originalQuery 
                        }.thenBy { 
                            it.displayOrder 
                        }.thenBy { 
                            it.word?.length ?: Int.MAX_VALUE 
                        }
                    )
                }
        }
    }
.flowOn(kotlinx.coroutines.Dispatchers.IO) // Ensure DB work happens on IO thread
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Optimized SQL Builder for 30+ tables.
     * Uses prefix search for speed but maintains LIKE for flexibility.
     */
    private fun buildMultiTableQuery(dicts: List<DictionaryInfo>, query: String, exact: Boolean): SimpleSQLiteQuery {
        val validDicts = dicts.filter { it.tableName != null && it.displayName != null }
        if (validDicts.isEmpty()) return SimpleSQLiteQuery("SELECT 1 WHERE 0")
        
        val dbQuery = if (exact) query else "%$query%" 
        val operator = if (exact) "=" else "LIKE"
        
        val selectStatements = validDicts.map { dict ->
            val tableName = dict.tableName!!
            val displayName = dict.displayName!!
            // Force "Taj al-Arus" to have the highest priority (lowest order)
            val displayOrder = if (tableName == "taj") -1 else (dict.displayOrder ?: 0)
            
            // Map table-specific columns
            val (idCol, wordCol, meaningCol, searchCol) = when (tableName) {
                "Arabic" -> Quadruple("No", "word", "'[[MALAYALAM]]' || IFNULL(M_Malayalam, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word_no_harakah")
                "English" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_arabic, '') || '[[MALAYALAM]]' || IFNULL(M_malayalam, '')", "word")
                "Malayalam" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_Arabic, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word")
                "irab" -> Quadruple("id", "title", "'[[SUBHEADING]]' || IFNULL(konten, '') || '[[MEANING]]' || IFNULL(text, '')", "title")
                "maany", "afaal", "frooq", "maany_dict" -> Quadruple("id", "word", "meaning", "word_no_harakah")
                else -> Quadruple("_id", "word", "meaning", "word_no_harakah")
            }

            "SELECT $idCol as id, $wordCol as word, $searchCol as wordNoHarakah, $meaningCol as meaning, '${displayName.replace("'", "''")}' as dictionaryName, $displayOrder as displayOrder FROM $tableName.$tableName WHERE $searchCol $operator ?"
        }

        val finalQuery = selectStatements.joinToString(" UNION ALL ") + " LIMIT 200"
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
