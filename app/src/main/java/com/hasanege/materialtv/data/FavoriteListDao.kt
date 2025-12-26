package com.hasanege.materialtv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: FavoriteListEntity): Long

    @Update
    suspend fun update(list: FavoriteListEntity)

    @Delete
    suspend fun delete(list: FavoriteListEntity)

    @Query("SELECT * FROM favorite_lists ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllLists(): Flow<List<FavoriteListEntity>>

    @Query("SELECT * FROM favorite_lists WHERE listId = :listId LIMIT 1")
    suspend fun getListById(listId: Long): FavoriteListEntity?

    @Query("UPDATE favorite_lists SET orderIndex = :newIndex WHERE listId = :listId")
    suspend fun updateOrderIndex(listId: Long, newIndex: Int)

    @Query("SELECT COUNT(*) FROM favorite_lists")
    suspend fun getListCount(): Int
}
