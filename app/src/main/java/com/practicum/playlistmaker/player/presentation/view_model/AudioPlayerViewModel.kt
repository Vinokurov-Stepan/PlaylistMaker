package com.practicum.playlistmaker.player.presentation.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.media.domain.api.PlaylistsInteractor
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.player.presentation.service.AudioPlayerControl
import com.practicum.playlistmaker.player.presentation.service.PlayerState
import kotlinx.coroutines.launch

class AudioPlayerViewModel(
    private val playlistsInteractor: PlaylistsInteractor,
    private val trackAddMessage: String,
    private val trackAddedMessage: String
) : ViewModel() {

    private var audioPlayerControl: AudioPlayerControl? = null

    private val playerState = MutableLiveData<PlayerState>(PlayerState.Default())
    fun observePlayerState(): LiveData<PlayerState> = playerState

    fun setAudioPlayerControl(audioPlayerControl: AudioPlayerControl) {
        this.audioPlayerControl = audioPlayerControl
        viewModelScope.launch {
            audioPlayerControl.getPlayerState().collect {
                playerState.postValue(it)
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
        val currentState = playerState.value
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

    private val stateLiveData = MutableLiveData<PlaylistsState>()
    fun observeState(): LiveData<PlaylistsState> = stateLiveData

    fun fillData() {
        viewModelScope.launch {
            playlistsInteractor.getPlaylists().collect { playlists ->
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
            playerState.value =
                playerState.value?.updateMessageState(result == true)?.resetMessage(message)
                    ?.resetBottomSheetFlag(shouldHideBottomSheet)
        }
    }

    fun resetMessage() {
        playerState.value = playerState.value?.resetMessage(null)
    }

    fun resetBottomSheetFlag() {
        playerState.value = playerState.value?.resetBottomSheetFlag(false)
    }

    private fun processResult(playlists: List<Playlist>) {
        if (playlists.isEmpty()) {
            renderState(PlaylistsState.Empty)
        } else {
            renderState(PlaylistsState.Content(playlists))
        }
    }

    private fun renderState(state: PlaylistsState) {
        stateLiveData.postValue(state)
    }
}
