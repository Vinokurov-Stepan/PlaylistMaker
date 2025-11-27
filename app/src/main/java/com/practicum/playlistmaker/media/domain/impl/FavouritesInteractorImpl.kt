package com.practicum.playlistmaker.media.domain.impl

import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.media.domain.api.FavouritesInteractor
import com.practicum.playlistmaker.media.domain.api.FavouritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FavouritesInteractorImpl(private val favouritesRepository: FavouritesRepository) :
    FavouritesInteractor {

    override suspend fun addTrack(track: Track) {
        favouritesRepository.addTrack(track)
    }

    override suspend fun deleteTrack(track: Track) {
        favouritesRepository.deleteTrack(track)
    }

    override fun getTracks(): Flow<List<Track>> {
        return favouritesRepository.getTracks()
    }

    override suspend fun getFirstFavoriteTrack(): List<Track> {
        return favouritesRepository.getTracks().first()
    }
}
