package com.practicum.playlistmaker.media.presentation.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.media.domain.api.FavouritesInteractor
import com.practicum.playlistmaker.media.presentation.FavouritesState
import com.practicum.playlistmaker.search.domain.api.SearchHistoryInteractor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavouritesViewModel(
    private val searchHistory: SearchHistoryInteractor,
    private val favouritesInteractor: FavouritesInteractor
) : ViewModel() {

    private val _state =
        MutableStateFlow<FavouritesState>(FavouritesState.Content(persistentListOf()))
    val state: StateFlow<FavouritesState> = _state.asStateFlow()

    fun fillData() {
        viewModelScope.launch {
            favouritesInteractor.getTracks().collect { tracks ->
                processResult(tracks.toImmutableList())
            }
        }
    }

    fun onTrackClicked(track: Track) {
        viewModelScope.launch {
            searchHistory.setListeningTrack(track)
        }
    }

    private fun processResult(tracks: ImmutableList<Track>) {
        if (tracks.isEmpty()) {
            renderState(FavouritesState.Empty)
        } else {
            renderState(FavouritesState.Content(tracks))
        }
    }

    private fun renderState(state: FavouritesState) {
        _state.value = state
    }
}
