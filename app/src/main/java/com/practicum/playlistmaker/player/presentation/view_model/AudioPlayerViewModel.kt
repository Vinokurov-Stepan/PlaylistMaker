package com.practicum.playlistmaker.player.presentation.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.media.domain.api.PlaylistsInteractor
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.player.presentation.service.AudioPlayerControl
import com.practicum.playlistmaker.player.presentation.service.PlayerState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class AudioPlayerViewModel(
    private val playlistsInteractor: PlaylistsInteractor?,
    private val trackAddMessage: String,
    private val trackAddedMessage: String
) : ViewModel() {

    private var audioPlayerControl: AudioPlayerControl? = null

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Default())
    open val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun setAudioPlayerControl(audioPlayerControl: AudioPlayerControl) {
        this.audioPlayerControl = audioPlayerControl
        viewModelScope.launch {
            audioPlayerControl.getPlayerState().collect {
                _playerState.value = it
            }
        }
    }

    fun onFavouriteClicked() {
        audioPlayerControl?.onFavouriteClicked()
    }

    fun removeAudioPlayerControl() {
        audioPlayerControl = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerControl?.safeReleasePlayer()
        audioPlayerControl = null
    }

    fun playbackControl() {
        val currentState = _playerState.value
        when (currentState) {
            is PlayerState.Playing -> audioPlayerControl?.pausePlayer()
            is PlayerState.Prepared, is PlayerState.Paused -> audioPlayerControl?.startPlayer()
            else -> {
                viewModelScope.launch {
                    audioPlayerControl?.startPlayer()
                }
            }
        }
    }

    fun setAppInBackground() {
        audioPlayerControl?.setAppInBackground()
    }

    fun setAppInForeground() {
        audioPlayerControl?.setAppInForeground()
    }

    private val _playlistState =
        MutableStateFlow<PlaylistsState>(PlaylistsState.Content(persistentListOf()))
    open val playlistState: StateFlow<PlaylistsState> = _playlistState.asStateFlow()

    fun fillData() {
        viewModelScope.launch {
            playlistsInteractor!!.getPlaylists().collect { playlists ->
                processResult(playlists)
            }
        }
    }

    fun onTrackAddToPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val result = audioPlayerControl?.addTrackToPlaylist(playlist)
            var message: String?
            val shouldHideBottomSheet: Boolean
            if (result == true) {
                message = "$trackAddMessage ${playlist.playlistName}"
                shouldHideBottomSheet = true
            } else {
                message = "$trackAddedMessage ${playlist.playlistName}"
                shouldHideBottomSheet = false
            }
            _playerState.value =
                _playerState.value
                    .updateMessageState(result == true)
                    .resetMessage(message)
                    .resetBottomSheetFlag(shouldHideBottomSheet)
        }
    }

    fun resetMessage() {
        _playerState.value = _playerState.value.resetMessage(null)
    }

    fun resetBottomSheetFlag() {
        _playerState.value = _playerState.value.resetBottomSheetFlag(false)
    }

    private fun processResult(playlists: List<Playlist>) {
        if (playlists.isEmpty()) {
            renderState(PlaylistsState.Empty)
        } else {
            renderState(PlaylistsState.Content(playlists))
        }
    }

    private fun renderState(state: PlaylistsState) {
        _playlistState.value = state
    }
}
