package com.practicum.playlistmaker.di

import android.content.Context
import android.media.MediaPlayer
import androidx.room.Room
import com.google.gson.Gson
import com.practicum.playlistmaker.BuildConfig
import com.practicum.playlistmaker.media.data.AppDatabase
import com.practicum.playlistmaker.media.data.dao.AddedTrackDao
import com.practicum.playlistmaker.media.data.dao.PlaylistDao
import com.practicum.playlistmaker.media.data.dao.TrackDao
import com.practicum.playlistmaker.search.data.NetworkClient
import com.practicum.playlistmaker.search.data.network.ITunesAPI
import com.practicum.playlistmaker.search.data.network.RetrofitNetworkClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val dataModule = module {
    single<ITunesAPI> {
        val retrofit = Retrofit.Builder().baseUrl(BuildConfig.SERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
        retrofit.create(ITunesAPI::class.java)
    }

    single {
        androidContext().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
    }

    factory { Gson() }

    single<NetworkClient> {
        RetrofitNetworkClient(androidContext(), get())
    }

    factory { MediaPlayer() }

    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database.db")
            .fallbackToDestructiveMigration().build()
    }

    single<TrackDao> {
        get<AppDatabase>().trackDao()
    }

    single<PlaylistDao> {
        get<AppDatabase>().playlistDao()
    }

    single<AddedTrackDao> {
        get<AppDatabase>().addedTrackDao()
    }
}
