package com.practicum.playlistmaker.media.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.practicum.playlistmaker.media.data.dao.AddedTrackDao
import com.practicum.playlistmaker.media.data.dao.PlaylistDao
import com.practicum.playlistmaker.media.data.dao.TrackDao
import com.practicum.playlistmaker.media.data.entity.AddedTrackEntity
import com.practicum.playlistmaker.media.data.entity.PlaylistEntity
import com.practicum.playlistmaker.media.data.entity.TrackEntity

@Database(
    version = 1, entities = [TrackEntity::class, PlaylistEntity::class, AddedTrackEntity::class]
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun addedTrackDao(): AddedTrackDao
}
