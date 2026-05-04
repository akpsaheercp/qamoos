package com.example.qamoos.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArabicDao {
    /**
     * Surgical search across dynamic tables.
     */
    @RawQuery(observedEntities = [DictionaryInfo::class])
    abstract fun searchAllDictionaries(query: SupportSQLiteQuery): Flow<List<UnifiedEntry>>

    /**
     * One-shot search for transactional use.
     */
    @RawQuery(observedEntities = [DictionaryInfo::class])
    abstract suspend fun searchAllDictionariesInternal(query: SupportSQLiteQuery): List<UnifiedEntry>

    @Query("SELECT * FROM dictionary_info WHERE is_selected = 1 ORDER BY display_order ASC")
    abstract fun getSelectedDictionaries(): Flow<List<DictionaryInfo>>

    @Query("SELECT * FROM dictionary_info ORDER BY display_order ASC")
    abstract fun getAllDictionaries(): Flow<List<DictionaryInfo>>

    @Query("SELECT * FROM dictionary_info ORDER BY display_order ASC")
    abstract suspend fun getAllDictionariesInternal(): List<DictionaryInfo>

    @Query("UPDATE dictionary_info SET is_selected = :isSelected WHERE id = :id")
    abstract suspend fun updateDictionarySelection(id: Int, isSelected: Int)

    @Query("UPDATE dictionary_info SET is_selected = :isSelected")
    abstract suspend fun updateAllDictionariesSelection(isSelected: Int)

    @Query("UPDATE dictionary_info SET display_order = :displayOrder WHERE id = :id")
    abstract suspend fun updateDictionaryOrder(id: Int, displayOrder: Int)

    @Transaction
    open suspend fun attachDatabase(path: String, alias: String) {
        try {
            execRawSql("ATTACH DATABASE '$path' AS $alias")
        } catch (e: Exception) {
            // Usually occurs if already attached
        }
    }

    /**
     * Attaches multiple databases and performs search in a single transaction.
     * This ensures all operations use the same connection.
     */
    @Transaction
    open suspend fun searchWithAttachments(
        query: SupportSQLiteQuery,
        attachments: List<Pair<String, String>>
    ): List<UnifiedEntry> {
        attachments.forEach { (path, alias) ->
            try {
                // Use double quotes for alias to avoid issues with reserved words or special characters
                execRawSql("ATTACH DATABASE '$path' AS \"$alias\"")
            } catch (e: Exception) {
                // Ignore if already attached or other attachment issues
                // If it fails, the subsequent query might fail, but it's caught in the ViewModel
            }
        }
        return searchAllDictionariesInternal(query)
    }

    @RawQuery
    abstract suspend fun performRawQuery(query: SupportSQLiteQuery): Int

    suspend fun execRawSql(sql: String) {
        performRawQuery(SimpleSQLiteQuery(sql))
    }

    @Transaction
    open suspend fun updateDictionaryOrders(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id ->
            updateDictionaryOrder(id, index)
        }
    }
}
