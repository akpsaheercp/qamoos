package com.example.qamoos.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArabicDao {

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
    open suspend fun updateDictionaryOrders(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id ->
            updateDictionaryOrder(id, index)
        }
    }
}
