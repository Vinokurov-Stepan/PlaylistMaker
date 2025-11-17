package com.practicum.playlistmaker.player.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.media.domain.api.FavouritesInteractor
import com.practicum.playlistmaker.media.domain.api.PlaylistsInteractor
import com.practicum.playlistmaker.player.domain.api.PlayerInteractor
import com.practicum.playlistmaker.search.domain.api.SearchHistoryInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MusicService() : Service(), AudioPlayerControl {

    private val player: PlayerInteractor by inject()
    private val searchHistory: SearchHistoryInteractor by inject()
    private val favouritesInteractor: FavouritesInteractor by inject()
    private val playlistsInteractor: PlaylistsInteractor by inject()

    private val binder = MusicServiceBinder()
    private var timerJob: Job? = null
    private var track: Track? = null
    private var isPlayerInitialized = false
    private var isAppInBackground = false
    private var isOnAudioScreen = true

    private val _servicePlayerState = MutableStateFlow<PlayerState>(PlayerState.Default())
    private val servicePlayerState = _servicePlayerState.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val initialNotification = createInitialNotification()
        ServiceCompat.startForeground(
            this, SERVICE_NOTIFICATION_ID, initialNotification, getForegroundServiceTypeConstant()
        )
        track = searchHistory.getListeningTrack()
        preparePlayer()
    }

    private fun createInitialNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setSilent(true).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, LOG_TAG, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Service for playing music"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getForegroundServiceTypeConstant(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }

    private fun preparePlayer() {
        val currentTrack = track
        player.reset()
        player.prepare(track!!.previewUrl)
        player.getMediaPlayer().setOnPreparedListener {
            isPlayerInitialized = true
            _servicePlayerState.value = PlayerState.Prepared(
                track = track,
                timer = player.resetTimer(),
                isTrackLicked = _servicePlayerState.value.isTrackLicked
            )
        }
        player.getMediaPlayer().setOnCompletionListener {
            timerJob?.cancel()
            _servicePlayerState.value = PlayerState.Prepared(
                track = track,
                timer = player.resetTimer(),
                isTrackLicked = _servicePlayerState.value.isTrackLicked
            )
            hideNotification()
            stopSelf()
        }
        checkIfTrackIsFavorite(currentTrack)
    }

    private fun checkIfTrackIsFavorite(currentTrack: Track?) {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            val favoriteTracks = player.getIdTracks()
            val isFavorite = favoriteTracks.contains(currentTrack!!.trackId)
            _servicePlayerState.value = _servicePlayerState.value.updateFavoriteState(isFavorite)
        }
    }

    override fun startPlayer() {
        player.play()
        _servicePlayerState.value = PlayerState.Playing(
            track = track,
            timer = player.getCurrentPosition(),
            isTrackLicked = _servicePlayerState.value.isTrackLicked
        )
        startTimerUpdate()
        if (isAppInBackground && !isOnAudioScreen) {
            showNotification()
        }
    }

    override fun pausePlayer() {
        player.pause()
        timerJob?.cancel()
        _servicePlayerState.value = PlayerState.Paused(
            track = track,
            timer = player.getCurrentPosition(),
            isTrackLicked = _servicePlayerState.value.isTrackLicked
        )
        hideNotification()
    }

    private fun showNotification() {
        val notification = createMusicNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
    }

    private fun createMusicNotification(): Notification {
        val trackName = track?.trackName
        val artistName = track?.artistName
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("$artistName - $trackName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setOngoing(true).setSilent(true)
            .build()
    }

    private fun hideNotification() {
        val initialNotification = createInitialNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(SERVICE_NOTIFICATION_ID, initialNotification)
    }

    override fun safeReleasePlayer() {
        if (isPlayerInitialized) {
            player.pause()
        }
        player.release()
        isPlayerInitialized = false
        _servicePlayerState.value = PlayerState.Default(timer = player.resetTimer())
        timerJob?.cancel()
        hideNotification()
        stopSelf()
    }

    override fun setAppInBackground() {
        isAppInBackground = true
        isOnAudioScreen = false
        val currentState = _servicePlayerState.value
        if (currentState is PlayerState.Playing) {
            showNotification()
        }
    }

    override fun setAppInForeground() {
        isAppInBackground = false
        isOnAudioScreen = true
        hideNotification()
    }

    private fun startTimerUpdate() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_servicePlayerState.value is PlayerState.Playing) {
                delay(REFRESH_SECONDS_VALUE_MILLIS)
                val currentState = _servicePlayerState.value
                if (currentState is PlayerState.Playing) {
                    _servicePlayerState.value =
                        currentState.copy(timer = player.getCurrentPosition())
                }
            }
        }
    }

    override fun onFavouriteClicked() {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            val currentState = _servicePlayerState.value
            if (currentState.isTrackLicked) {
                favouritesInteractor.deleteTrack(track!!)
                _servicePlayerState.value = currentState.updateFavoriteState(false)
            } else {
                favouritesInteractor.addTrack(track!!)
                _servicePlayerState.value = currentState.updateFavoriteState(true)
            }
        }
    }

    override suspend fun addTrackToPlaylist(playlist: Playlist): Boolean {
        return playlistsInteractor.addTrackToPlaylist(track!!, playlist)
    }

    override fun getPlayerState(): StateFlow<PlayerState> {
        return servicePlayerState
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        safeReleasePlayer()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        safeReleasePlayer()
        timerJob?.cancel()
    }

    companion object {
        private const val REFRESH_SECONDS_VALUE_MILLIS = 300L
        private const val SERVICE_NOTIFICATION_ID = 100
        private const val LOG_TAG = "Music Service"
        private const val NOTIFICATION_CHANNEL_ID = "music_service_channel"
    }

    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}
