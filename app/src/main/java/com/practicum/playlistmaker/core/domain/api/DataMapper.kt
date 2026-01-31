package com.practicum.playlistmaker.core.domain.api

import com.practicum.playlistmaker.core.domain.models.Track

interface DataMapper {
    fun createJsonFromTracks(tracks: Array<Track>): String
    fun createTracksFromJson(json: String?): Array<Track>
    fun createJsonFromTrack(track: Track): String
    fun createTrackFromJson(json: String): Track?
}
