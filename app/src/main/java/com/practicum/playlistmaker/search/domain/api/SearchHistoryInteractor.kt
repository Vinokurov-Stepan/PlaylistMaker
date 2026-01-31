package com.practicum.playlistmaker.search.domain.api

import com.practicum.playlistmaker.core.domain.models.Track

interface SearchHistoryInteractor {
    fun loadTracks(): MutableList<Track>
    fun addTrack(track: Track)
    fun clearHistory()
    fun setListeningTrack(track: Track)
    fun getListeningTrack(): Track?
}
