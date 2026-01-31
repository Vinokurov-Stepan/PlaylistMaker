package com.practicum.playlistmaker.media.presentation

import com.practicum.playlistmaker.core.domain.models.Playlist

interface PlaylistsState {

    data class Content(
        val playlists: List<Playlist>
    ) : PlaylistsState

    object Empty : PlaylistsState

    data class PlaylistContent(
        val playlist: Playlist?
    ) : PlaylistsState
}
