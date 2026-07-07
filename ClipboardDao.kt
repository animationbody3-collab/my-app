package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isFavorite DESC, timestamp DESC")
    fun getAllItemsFlow(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteItemsFlow(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestItems(limit: Int): List<ClipboardItem>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestNonFavoriteItems(limit: Int): List<ClipboardItem>

    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM clipboard_items WHERE text LIKE :query ORDER BY timestamp DESC")
    fun searchItemsFlow(query: String): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getItemById(id: Long): ClipboardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItem): Long

    @Update
    suspend fun updateItem(item: ClipboardItem)

    @Delete
    suspend fun deleteItem(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()
}
