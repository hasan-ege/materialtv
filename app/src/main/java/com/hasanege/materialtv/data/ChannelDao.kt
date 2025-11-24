package com.hasanege.materialtv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("SELECT * FROM channels WHERE type = :type")
    fun getChannelsByType(type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE originalGroup = :group")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE virtualCategoryId = :categoryId")
    fun getChannelsByVirtualCategory(categoryId: String): Flow<List<ChannelEntity>>

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)
    
    @Query("SELECT * FROM channels WHERE isHidden = 0")
    fun getSafeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>
}
