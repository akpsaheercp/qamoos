package com.example.qamoos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM user_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("DELETE FROM user_history WHERE id NOT IN (SELECT id FROM user_history ORDER BY timestamp DESC LIMIT 50)")
    suspend fun deleteOldHistory()
    
    @Transaction
    suspend fun insertAndTrim(entry: HistoryEntry) {
        insertHistory(entry)
        deleteOldHistory()
    }

    @Query("DELETE FROM user_history")
    suspend fun clearHistory()
}
