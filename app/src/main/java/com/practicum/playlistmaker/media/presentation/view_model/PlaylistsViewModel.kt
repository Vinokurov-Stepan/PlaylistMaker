package com.practicum.playlistmaker.media.presentation.view_model

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.media.domain.api.PlaylistsInteractor
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.search.domain.api.SearchHistoryInteractor
import com.practicum.playlistmaker.search.presentation.TracksState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    val playlistsInteractor: PlaylistsInteractor,
    private val searchHistory: SearchHistoryInteractor,
) : ViewModel() {

    private val _playlistState =
        MutableStateFlow<PlaylistsState>(PlaylistsState.Content(persistentListOf()))
    val playlistState: StateFlow<PlaylistsState> = _playlistState.asStateFlow()

    private val _tracksStateLiveData = MutableLiveData<TracksState>()
    fun tracksStateLiveData(): LiveData<TracksState> = _tracksStateLiveData

    private val _playlistLength = MutableLiveData<Int>()
    val playlistLength: LiveData<Int> = _playlistLength

    private val _playlistTracksCount = MutableLiveData<Int>()
    val playlistTracksCount: LiveData<Int> = _playlistTracksCount

    private var currentTracks: ImmutableList<Track> = persistentListOf()

    fun fillData() {
        viewModelScope.launch {
            playlistsInteractor.getPlaylists().collect { playlists ->
                processResult(playlists.toImmutableList())
            }
        }
    }

    private fun processResult(playlists: ImmutableList<Playlist>) {
        if (playlists.isEmpty()) {
            renderState(PlaylistsState.Empty)
        } else {
            renderState(PlaylistsState.Content(playlists))
        }
    }

    fun onPlaylistCreate(playlist: Playlist, imageUri: Uri?) {
        viewModelScope.launch {
            playlistsInteractor.addPlaylist(playlist, imageUri)
        }
    }

    fun loadPlaylistById(playlistId: Int) {
        viewModelScope.launch {
            val playlist = playlistsInteractor.getPlaylistById(playlistId)
            if (playlist != null) {
                renderState(PlaylistsState.PlaylistContent(playlist))
                _playlistTracksCount.value = playlist.numberOfTracks
                loadPlaylistTracks(playlistId)
                playlistsInteractor.getTracksByPlaylistId(playlistId).collect { tracks ->
                    renderState(TracksState.Content(tracks.toImmutableList()))
                }
            }
        }
    }

    fun updatePlaylist(playlist: Playlist, imageUri: Uri?) {
        viewModelScope.launch {
            playlistsInteractor.updatePlaylist(playlist, imageUri)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            playlistsInteractor.deletePlaylist(playlistId)
        }
    }

    fun deleteTrackFromPlaylist(trackId: Int, playlistId: Int) {
        viewModelScope.launch {
            playlistsInteractor.deleteTrackFromPlaylist(trackId, playlistId)
            updateUIAfterTrackDeletion(trackId, playlistId)
        }
    }

    private fun updateUIAfterTrackDeletion(trackId: Int, playlistId: Int) {
        val updatedTracks = currentTracks.filter { it.trackId != trackId }
        currentTracks = updatedTracks.toImmutableList()
        renderState(TracksState.Content(currentTracks))
        _playlistTracksCount.value = updatedTracks.size
        calculateTotalDuration(currentTracks)
        viewModelScope.launch {
            playlistsInteractor.getTracksByPlaylistId(playlistId).collect { freshTracks ->
                if (freshTracks.size != updatedTracks.size) {
                    currentTracks = freshTracks.toImmutableList()
                    renderState(TracksState.Content(currentTracks))
                    calculateTotalDuration(currentTracks)
                }
            }
        }
    }

    private fun loadPlaylistTracks(playlistId: Int) {
        viewModelScope.launch {
            playlistsInteractor.getTracksByPlaylistId(playlistId).collect { tracks ->
                currentTracks = tracks.toImmutableList()
                renderState(TracksState.Content(currentTracks))
                calculateTotalDuration(currentTracks)
            }
        }
    }

    private fun calculateTotalDuration(tracks: ImmutableList<Track>) {
        val totalMillis = tracks.sumOf { track ->
            parseTimeToMillis(track.trackTimeMillis)
        }
        val totalMinutes = (totalMillis / 1000 / 60).toInt()
        _playlistLength.value = totalMinutes
    }

    private fun parseTimeToMillis(timeString: String): Long {
        val parts = timeString.split(":")
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                (minutes * 60 + seconds) * 1000
            }

            3 -> {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toLong()
                (hours * 3600 + minutes * 60 + seconds) * 1000
            }

            else -> 0L
        }
    }

    fun addTrack(track: Track) {
        searchHistory.addTrack(track)
    }

    fun sharePlaylist(message: String) {
        playlistsInteractor.sharePlaylist(message)
    }

    private fun renderState(state: PlaylistsState) {
        _playlistState.value = state
    }

    private fun renderState(state: TracksState) {
        _tracksStateLiveData.postValue(state)
    }
}
