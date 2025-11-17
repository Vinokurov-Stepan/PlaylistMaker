package com.practicum.playlistmaker.player.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.ui.root.RootActivity
import com.practicum.playlistmaker.core.util.NetworkStateBroadcastReceiver
import com.practicum.playlistmaker.core.util.debounce
import com.practicum.playlistmaker.databinding.FragmentAudioBinding
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.media.ui.PlaylistAudioAdapter
import com.practicum.playlistmaker.player.presentation.service.MusicService
import com.practicum.playlistmaker.player.presentation.service.PlayerState
import com.practicum.playlistmaker.player.presentation.view_model.AudioPlayerViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class AudioFragment : Fragment() {

    private val networkStateBroadcastReceiver = NetworkStateBroadcastReceiver()
    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!
    private var track: Track? = null
    private var isTrackLicked = false
    private lateinit var viewModel: AudioPlayerViewModel
    private lateinit var playlistsAdapter: PlaylistAudioAdapter
    private val playlists = mutableListOf<Playlist>()
    private lateinit var onPlaylistClickDebounce: (Playlist) -> Unit
    private var isTrackAdded: Boolean = true
    private var toastText: String? = null
    private lateinit var trackAddMessage: String
    private lateinit var trackAddedMessage: String

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            viewModel.setAudioPlayerControl(binder.getService())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.removeAudioPlayerControl()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackAddMessage = getString(R.string.trackAdd)
        trackAddedMessage = getString(R.string.trackAdded)

        viewModel = getViewModel { parametersOf(trackAddMessage, trackAddedMessage) }

        onPlaylistClickDebounce = debounce<Playlist>(
            CLICK_DEBOUNCE_DELAY, viewLifecycleOwner.lifecycleScope, false
        ) { playlist ->
            viewModel.onTrackAddToPlaylist(playlist)
        }

        setupObservers()
        bindMusicService()
        setupViews()
    }

    private fun setupObservers() {
        viewModel.observePlayerState().observe(viewLifecycleOwner) { playerState ->
            playerState?.let { state ->
                if (track != state.track && state.track != null) {
                    setupTrackInfo(state.track!!, this)
                    track = state.track
                }
                render(state)
                binding.trackTimeToEnd.text = state.timer
                binding.playTrackButton.isEnabled = state.isPlayButtonEnabled
                if (isTrackLicked != state.isTrackLicked) {
                    binding.toLikeTrackButton.setLickedState(state.isTrackLicked)
                    changeLickedButtonStyle(state.isTrackLicked)
                    isTrackLicked = state.isTrackLicked
                }
                isTrackAdded = state.addedTrackState
                state.message?.let { message ->
                    if (message.isNotEmpty()) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                    viewModel.resetMessage()
                }
                if (state.shouldHideBottomSheet) {
                    val bottomSheetBehavior = BottomSheetBehavior.from(binding.playlistsBottomSheet)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    viewModel.resetBottomSheetFlag()
                }
            }
        }

        viewModel.observeState().observe(viewLifecycleOwner) { state ->
            if (state is PlaylistsState.Content) {
                with(binding) {
                    rvPlaylists.isVisible = true
                }
                this.playlists.clear()
                this.playlists.addAll(state.playlists)
                playlistsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupTrackInfo(track: Track, audioPlayer: AudioFragment) {
        with(binding) {
            Glide.with(audioPlayer).load(track.getCoverArtwork())
                .placeholder(R.drawable.track_icon_placeholder).centerCrop().transform(
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics
                        ).toInt()
                    )
                ).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(trackIcon)
            trackName.text = track.trackName
            artistName.text = track.artistName
            trackTimeInfo.text = track.trackTimeMillis
            trackAlbumInfo.text = track.collectionName
            trackYearInfo.text = track.releaseDate.substringBefore("-")
            trackGenreInfo.text = track.primaryGenreName
            trackCountryInfo.text = track.country
        }
    }

    private fun setupViews() {
        with(binding) {
            val bottomSheetBehavior = BottomSheetBehavior.from(playlistsBottomSheet).apply {
                state = BottomSheetBehavior.STATE_HIDDEN
            }

            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {

                override fun onStateChanged(bottomSheet: View, newState: Int) {

                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            overlay.isVisible = false
                        }

                        else -> {
                            overlay.isVisible = true
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val top = bottomSheet.top
                    val screenHeight = resources.displayMetrics.heightPixels

                    val alpha = 1f - (top.toFloat() / screenHeight.toFloat())

                    overlay.alpha = alpha.coerceIn(0f, 1f)
                    overlay.isVisible = alpha > 0f
                }
            })

            playlistsAdapter = PlaylistAudioAdapter(playlists) { playlist ->
                (activity as RootActivity).animateBottomNavigationView()
                onPlaylistClickDebounce(playlist)
                toastText?.let { text ->
                    if (text.isNotEmpty()) {
                        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            rvPlaylists.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            rvPlaylists.adapter = playlistsAdapter

            searchButton.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            playTrackButton.setOnClickListener {
                viewModel.playbackControl()
            }

            toLikeTrackButton.setOnClickListener {
                viewModel.onFavouriteClicked()
            }

            addToPlaylistButton.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                viewModel.fillData()
            }

            createPlaylistButton.setOnClickListener {
                findNavController().navigate(
                    R.id.action_audioFragment_to_playlistMakerFragment
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAppInForeground()
        ContextCompat.registerReceiver(
            requireContext(),
            networkStateBroadcastReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.setAppInBackground()
        requireContext().unregisterReceiver(networkStateBroadcastReceiver)
    }

    override fun onStop() {
        super.onStop()
        val isAppInBackground =
            !requireActivity().isChangingConfigurations && !requireActivity().isFinishing
        if (isAppInBackground) {
            viewModel.setAppInBackground()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        track = null
        isTrackLicked = false
        binding.playTrackButton.cleanup()
        binding.toLikeTrackButton.cleanup()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindMusicService()
    }

    private fun bindMusicService() {
        val stopIntent = Intent(requireContext(), MusicService::class.java)
        requireContext().stopService(stopIntent)
        val intent = Intent(requireContext(), MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(requireContext(), intent)
        } else {
            requireContext().startService(intent)
        }
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindMusicService() {
        requireContext().unbindService(serviceConnection)
    }

    private fun render(state: PlayerState) {
        when (state) {
            is PlayerState.Playing -> playPlayer()
            is PlayerState.Paused -> pausePlayer()
            is PlayerState.Prepared -> pausePlayer()
            is PlayerState.Default -> {
                pausePlayer()
            }
        }
    }

    private fun playPlayer() {
        binding.playTrackButton.setIcon(true)
    }

    private fun pausePlayer() {
        binding.playTrackButton.setIcon(false)
    }

    private fun changeLickedButtonStyle(isTrackLicked: Boolean) {
        if (isTrackLicked) {
            binding.toLikeTrackButton.setIcon(true)
        } else {
            binding.toLikeTrackButton.setIcon(false)
        }
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
    }
}
