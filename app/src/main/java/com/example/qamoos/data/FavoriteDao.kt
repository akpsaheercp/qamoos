package com.example.qamoos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM user_favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entry: FavoriteEntry)

    @Delete
    suspend fun deleteFavorite(entry: FavoriteEntry)

    @Query("DELETE FROM user_favorites WHERE word = :word AND dictionaryName = :dictionaryName")
    suspend fun deleteFavorite(word: String, dictionaryName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM user_favorites WHERE word = :word AND dictionaryName = :dictionaryName)")
    fun isFavorite(word: String, dictionaryName: String): Flow<Boolean>
}
