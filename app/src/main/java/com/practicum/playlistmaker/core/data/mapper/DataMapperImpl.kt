package com.practicum.playlistmaker.core.data.mapper

import com.google.gson.Gson
import com.practicum.playlistmaker.core.domain.api.DataMapper
import com.practicum.playlistmaker.core.domain.models.Track

class DataMapperImpl(private val gson: Gson) : DataMapper {
    override fun createJsonFromTracks(tracks: Array<Track>): String {
        return gson.toJson(tracks)
    }

    override fun createTracksFromJson(json: String?): Array<Track> {
        return (if (json != null) {
            return gson.fromJson(json, Array<Track>::class.java)
        } else emptyList<Track>()).toTypedArray()
    }

    override fun createJsonFromTrack(track: Track): String {
        return gson.toJson(track)
    }

    override fun createTrackFromJson(json: String): Track {
        return gson.fromJson(json, Track::class.java)
    }
}
