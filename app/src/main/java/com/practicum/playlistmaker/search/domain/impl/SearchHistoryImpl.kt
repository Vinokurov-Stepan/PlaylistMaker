package com.practicum.playlistmaker.search.domain.impl

import com.practicum.playlistmaker.core.domain.api.DataMapper
import com.practicum.playlistmaker.search.domain.api.SearchHistoryInteractor
import com.practicum.playlistmaker.core.domain.api.SharedPreferencesRepository
import com.practicum.playlistmaker.core.domain.models.Track

class SearchHistoryImpl(
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val dataMapper: DataMapper,
) : SearchHistoryInteractor {

    companion object {
        private const val TRACK_ID = "TRACK_ID"
        private const val LISTENING_TRACK = "LISTENING_TRACK"
        private const val STORY_SIZE = 10
    }

    override fun loadTracks(): MutableList<Track> {
        val historyTracks = mutableListOf<Track>()
        val track = sharedPreferencesRepository.getStrItem(TRACK_ID)
        if (track != null) {
            historyTracks.addAll(dataMapper.createTracksFromJson(track))
        }
        return historyTracks
    }

    override fun addTrack(track: Track) {
        val tracks = mutableListOf<Track>()
        tracks.addAll(
            dataMapper.createTracksFromJson(sharedPreferencesRepository.getStrItem(TRACK_ID))
        )
        val existingTrack = tracks.find { it.trackId == track.trackId }
        if (existingTrack != null) {
            tracks.remove(existingTrack)
        }
        tracks.add(0, track)
        if (tracks.size > STORY_SIZE) {
            tracks.removeAt(tracks.size - 1)
        }
        sharedPreferencesRepository.removeItem(TRACK_ID)
        sharedPreferencesRepository.putStrItem(
            TRACK_ID, dataMapper.createJsonFromTracks(tracks.toTypedArray())
        )
    }

    override fun clearHistory() {
        sharedPreferencesRepository.removeItem(TRACK_ID)
    }

    override fun setListeningTrack(track: Track) {
        val trackJson = dataMapper.createJsonFromTrack(track)
        sharedPreferencesRepository.putStrItem(LISTENING_TRACK, trackJson)
    }

    override fun getListeningTrack(): Track? {
        val trackJson = sharedPreferencesRepository.getStrItem(LISTENING_TRACK)
        return if (trackJson.isNullOrEmpty()) {
            null
        } else {
            dataMapper.createTrackFromJson(trackJson)
        }
    }
}
