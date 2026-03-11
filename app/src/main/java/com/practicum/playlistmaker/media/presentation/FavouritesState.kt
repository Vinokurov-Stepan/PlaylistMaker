package com.practicum.playlistmaker.media.presentation

import com.practicum.playlistmaker.core.domain.models.Track
import kotlinx.collections.immutable.ImmutableList

sealed interface FavouritesState {

    data class Content(
        val tracks: ImmutableList<Track>
    ) : FavouritesState

    object Empty : FavouritesState
}
