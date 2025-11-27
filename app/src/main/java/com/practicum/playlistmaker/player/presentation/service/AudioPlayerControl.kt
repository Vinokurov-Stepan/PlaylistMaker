package com.practicum.playlistmaker.player.presentation.service

import com.practicum.playlistmaker.core.domain.models.Playlist
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerControl {
    fun startPlayer()
    fun pausePlayer()
    fun safeReleasePlayer()
    fun onFavouriteClicked()
    suspend fun addTrackToPlaylist(playlist: Playlist): Boolean
    fun getPlayerState(): StateFlow<PlayerState>
    fun setAppInBackground()
    fun setAppInForeground()
}
