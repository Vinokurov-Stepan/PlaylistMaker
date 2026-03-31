package com.practicum.playlistmaker.media.presentation

import androidx.compose.runtime.Immutable
import com.practicum.playlistmaker.core.domain.models.Playlist

@Immutable
interface PlaylistsState {
    data class Content(
        val playlists: List<Playlist>
    ) : PlaylistsState

    object Empty : PlaylistsState

    data class PlaylistContent(
        val playlist: Playlist?
    ) : PlaylistsState
}
