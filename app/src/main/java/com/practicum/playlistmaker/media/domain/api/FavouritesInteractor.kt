package com.practicum.playlistmaker.media.domain.api

import com.practicum.playlistmaker.core.domain.models.Track
import kotlinx.coroutines.flow.Flow

interface FavouritesInteractor {

    suspend fun addTrack(track: Track)
    suspend fun deleteTrack(track: Track)
    fun getTracks(): Flow<List<Track>>
    suspend fun getFirstFavoriteTrack(): List<Track>
}
