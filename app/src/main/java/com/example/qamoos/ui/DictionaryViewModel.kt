package com.example.qamoos.ui

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val dbCache = mutableMapOf<String, SQLiteDatabase>()

    override fun onCleared() {
        super.onCleared()
        dbCache.values.forEach { 
            try { it.close() } catch (_: Exception) {}
        }
        dbCache.clear()
    }

    private fun getDatabase(path: String): SQLiteDatabase? {
        return try {
            dbCache.getOrPut(path) {
                SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            }
        } catch (e: Exception) {
            Log.e("DictionaryViewModel", "Failed to open database at $path: ${e.message}")
            null
        }
    }

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
                
                // Search each dictionary independently using direct SQLite connections.
                // This bypasses the SQLite ATTACH limit (max 10) and avoids connection locking issues.
                dicts.forEach { dict ->
                    val tableName = dict.tableName ?: return@forEach
                    val displayName = dict.displayName ?: return@forEach
                    val path = dictionaryManager.getDictionaryPath(tableName) ?: return@forEach
                    
                    val db = getDatabase(path) ?: return@forEach
                    
                    try {
                        val results = queryExternalDictionary(db, tableName, displayName, normalizedQuery, exact, dict.displayOrder ?: 999)
                        allResults.addAll(results)
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

    private fun queryExternalDictionary(db: SQLiteDatabase, tableName: String, displayName: String, query: String, exact: Boolean, displayOrder: Int): List<UnifiedEntry> {
        val results = mutableListOf<UnifiedEntry>()
        val dbQuery = if (exact) query else "%$query%" 
        val operator = if (exact) "=" else "LIKE"
        
        val (idCol, wordCol, meaningCol, searchCol) = when (tableName.lowercase()) {
            "arabic" -> Quadruple("No", "word", "'[[MALAYALAM]]' || IFNULL(M_Malayalam, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word_no_harakah")
            "english" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_arabic, '') || '[[MALAYALAM]]' || IFNULL(M_malayalam, '')", "word")
            "malayalam" -> Quadruple("No", "word", "'[[ARABIC]]' || IFNULL(M_Arabic, '') || '[[ENGLISH]]' || IFNULL(M_English, '')", "word")
            "irab" -> Quadruple("id", "title", "'[[SUBHEADING]]' || IFNULL(konten, '') || '[[MEANING]]' || IFNULL(text, '')", "title")
            "maany", "afaal", "frooq", "maany_dict" -> Quadruple("id", "word", "meaning", "word_no_harakah")
            else -> Quadruple("_id", "word", "meaning", "word_no_harakah")
        }

        val sql = "SELECT $idCol as id, $wordCol as word, $searchCol as wordNoHarakah, $meaningCol as meaning FROM \"$tableName\" WHERE $searchCol $operator ? LIMIT 50"
        
        db.rawQuery(sql, arrayOf(dbQuery)).use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val wordIdx = cursor.getColumnIndex("word")
            val searchIdx = cursor.getColumnIndex("wordNoHarakah")
            val meaningIdx = cursor.getColumnIndex("meaning")
            
            while (cursor.moveToNext()) {
                results.add(UnifiedEntry(
                    id = if (idIdx != -1) cursor.getInt(idIdx) else null,
                    word = if (wordIdx != -1) cursor.getString(wordIdx) else null,
                    wordNoHarakah = if (searchIdx != -1) cursor.getString(searchIdx) else null,
                    meaning = if (meaningIdx != -1) cursor.getString(meaningIdx) else null,
                    dictionaryName = displayName,
                    displayOrder = if (tableName == "taj") -1 else displayOrder
                ))
            }
        }
        return results
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
