package com.practicum.playlistmaker.media.presentation

import androidx.compose.runtime.Immutable
import com.practicum.playlistmaker.core.domain.models.Track
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface FavouritesState {
    data class Content(
        val tracks: ImmutableList<Track>
    ) : FavouritesState

    object Empty : FavouritesState
}
