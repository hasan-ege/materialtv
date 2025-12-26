package com.hasanege.materialtv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity): Long

    @Update
    suspend fun update(favorite: FavoriteEntity)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)

    @Query("SELECT * FROM favorites ORDER BY orderIndex ASC, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE listId = :listId ORDER BY orderIndex ASC, addedAt DESC")
    fun getFavoritesByList(listId: Long): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentType = :type ORDER BY orderIndex ASC, addedAt DESC")
    fun getFavoritesByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentId = :contentId AND contentType = :type LIMIT 1")
    suspend fun getFavoriteByContent(contentId: Int, type: String): FavoriteEntity?

    @Query("SELECT * FROM favorites WHERE genre LIKE '%' || :genre || '%' ORDER BY orderIndex ASC, addedAt DESC")
    fun getFavoritesByGenre(genre: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE isWatched = :watched ORDER BY orderIndex ASC, addedAt DESC")
    fun getFavoritesByWatchedStatus(watched: Boolean): Flow<List<FavoriteEntity>>

    @Query("DELETE FROM favorites WHERE contentId = :contentId AND contentType = :type")
    suspend fun deleteByContent(contentId: Int, type: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contentId = :contentId AND contentType = :type LIMIT 1)")
    suspend fun isFavorite(contentId: Int, type: String): Boolean

    @Query("UPDATE favorites SET orderIndex = :newIndex WHERE id = :favoriteId")
    suspend fun updateOrderIndex(favoriteId: Long, newIndex: Int)

    @Query("SELECT COUNT(*) FROM favorites WHERE listId = :listId")
    suspend fun getCountByList(listId: Long): Int

    @Query("DELETE FROM favorites WHERE listId = :listId")
    suspend fun deleteAllByList(listId: Long)
}
