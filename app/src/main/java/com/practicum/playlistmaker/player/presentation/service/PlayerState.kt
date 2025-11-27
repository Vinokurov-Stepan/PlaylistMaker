package com.practicum.playlistmaker.player.presentation.service

import com.practicum.playlistmaker.core.domain.models.Track

sealed class PlayerState {

    abstract val track: Track?
    abstract val timer: String?
    abstract val isPlayButtonEnabled: Boolean
    abstract val isTrackLicked: Boolean
    abstract val addedTrackState: Boolean
    abstract val message: String?
    abstract val shouldHideBottomSheet: Boolean

    abstract fun updateFavoriteState(isFavorite: Boolean): PlayerState
    abstract fun updateMessageState(addedTrackState: Boolean): PlayerState
    abstract fun resetMessage(message: String?): PlayerState
    abstract fun resetBottomSheetFlag(flag: Boolean): PlayerState

    data class Default(
        override val track: Track? = null,
        override val timer: String? = "00:00",
        override val isPlayButtonEnabled: Boolean = false,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState() {
        override fun updateFavoriteState(isFavorite: Boolean): PlayerState =
            copy(isTrackLicked = isFavorite)

        override fun updateMessageState(addedTrackState: Boolean): PlayerState =
            copy(addedTrackState = addedTrackState)

        override fun resetMessage(message: String?): PlayerState = copy(message = message)
        override fun resetBottomSheetFlag(flag: Boolean): PlayerState =
            copy(shouldHideBottomSheet = flag)
    }

    data class Prepared(
        override val track: Track?,
        override val timer: String? = "00:00",
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState() {
        override fun updateFavoriteState(isFavorite: Boolean): PlayerState =
            copy(isTrackLicked = isFavorite)

        override fun updateMessageState(addedTrackState: Boolean): PlayerState =
            copy(addedTrackState = addedTrackState)

        override fun resetMessage(message: String?): PlayerState = copy(message = message)
        override fun resetBottomSheetFlag(flag: Boolean): PlayerState =
            copy(shouldHideBottomSheet = flag)
    }

    data class Playing(
        override val track: Track?,
        override val timer: String?,
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState() {
        override fun updateFavoriteState(isFavorite: Boolean): PlayerState =
            copy(isTrackLicked = isFavorite)

        override fun updateMessageState(addedTrackState: Boolean): PlayerState =
            copy(addedTrackState = addedTrackState)

        override fun resetMessage(message: String?): PlayerState = copy(message = message)
        override fun resetBottomSheetFlag(flag: Boolean): PlayerState =
            copy(shouldHideBottomSheet = flag)
    }

    data class Paused(
        override val track: Track?,
        override val timer: String?,
        override val isPlayButtonEnabled: Boolean = true,
        override val isTrackLicked: Boolean = false,
        override val addedTrackState: Boolean = false,
        override val message: String? = null,
        override val shouldHideBottomSheet: Boolean = false
    ) : PlayerState() {
        override fun updateFavoriteState(isFavorite: Boolean): PlayerState =
            copy(isTrackLicked = isFavorite)

        override fun updateMessageState(addedTrackState: Boolean): PlayerState =
            copy(addedTrackState = addedTrackState)

        override fun resetMessage(message: String?): PlayerState = copy(message = message)
        override fun resetBottomSheetFlag(flag: Boolean): PlayerState =
            copy(shouldHideBottomSheet = flag)
    }
}
