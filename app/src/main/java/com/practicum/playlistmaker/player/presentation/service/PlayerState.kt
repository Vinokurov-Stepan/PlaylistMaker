package com.practicum.playlistmaker.player.presentation.service

import androidx.compose.runtime.Immutable
import com.practicum.playlistmaker.core.domain.models.Track

@Immutable
sealed class PlayerState {
    abstract val track: Track?
    abstract val timer: String?
    abstract val isPlayButtonEnabled: Boolean
    abstract val isTrackLicked: Boolean
    abstract val addedTrackState: Boolean
    abstract val message: String?
    abstract val shouldHideBottomSheet: Boolean

    fun updateFavoriteState(isFavorite: Boolean): PlayerState =
        when (this) {
            is Default -> copy(isTrackLicked = isFavorite)
            is Prepared -> copy(isTrackLicked = isFavorite)
            is Playing -> copy(isTrackLicked = isFavorite)
            is Paused -> copy(isTrackLicked = isFavorite)
        }

    fun updateMessageState(addedTrackState: Boolean): PlayerState =
        when (this) {
            is Default -> copy(addedTrackState = addedTrackState)
            is Prepared -> copy(addedTrackState = addedTrackState)
            is Playing -> copy(addedTrackState = addedTrackState)
            is Paused -> copy(addedTrackState = addedTrackState)
        }

    fun resetMessage(message: String?): PlayerState =
        when (this) {
            is Default -> copy(message = message)
            is Prepared -> copy(message = message)
            is Playing -> copy(message = message)
            is Paused -> copy(message = message)
        }

    fun resetBottomSheetFlag(flag: Boolean): PlayerState =
        when (this) {
            is Default -> copy(shouldHideBottomSheet = flag)
            is Prepared -> copy(shouldHideBottomSheet = flag)
            is Playing -> copy(shouldHideBottomSheet = flag)
            is Paused -> copy(shouldHideBottomSheet = flag)
        }

    @Immutable
    data class Default(
        override val track: Track? = null,
        override val timer: String? = "00:00",
        override val isPlayButtonEnabled: Boolean = false,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState()

    @Immutable
    data class Prepared(
        override val track: Track?,
        override val timer: String? = "00:00",
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState()

    @Immutable
    data class Playing(
        override val track: Track?,
        override val timer: String?,
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState()

    @Immutable
    data class Paused(
        override val track: Track?,
        override val timer: String?,
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState()
}
