package com.hasanege.materialtv.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hasanege.materialtv.data.Converters

@Database(
    entities = [
        DownloadEntity::class,
        PlaylistEntity::class,
        WatchHistoryEntity::class,
        ChannelEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun channelDao(): ChannelDao
}
