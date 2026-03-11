package com.practicum.playlistmaker.media.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.ui.root.RootActivity
import com.practicum.playlistmaker.core.util.debounce
import com.practicum.playlistmaker.databinding.FragmentPlaylistInfoBinding
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.media.presentation.view_model.PlaylistsViewModel
import com.practicum.playlistmaker.search.presentation.TracksState
import com.practicum.playlistmaker.search.ui.TrackListAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

class PlaylistInfoFragment : Fragment() {

    private lateinit var viewModel: PlaylistsViewModel
    private var _binding: FragmentPlaylistInfoBinding? = null
    private val binding get() = _binding!!
    private lateinit var onTrackClickDebounce: (Track) -> Unit
    private lateinit var playlist: Playlist
    private lateinit var tracksAdapter: TrackListAdapter
    private val tracks = mutableListOf<Track>()
    private var currentPlaylistId = -1
    private var currentDuration = 0
    private var currentTracksCount = 0

    private val playlistId: Int by lazy {
        arguments?.getInt("playlistId", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaylistInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = getViewModel()

        onTrackClickDebounce = debounce<Track>(
            CLICK_DEBOUNCE_DELAY, viewLifecycleOwner.lifecycleScope, false
        ) { track ->
            viewModel.addTrack(track)
            findNavController().navigate(
                R.id.action_playlistInfoFragment_to_audioFragment
            )
        }

        setupViews()
        setupObservers()

        currentPlaylistId = playlistId
        viewModel.loadPlaylistById(currentPlaylistId)
    }

    private fun setupViews() {
        with(binding) {
            backButton.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            shareButton.setOnClickListener {
                if (tracks.isEmpty()) {
                    Toast.makeText(
                        requireContext(), getString(R.string.playlist_is_empty), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.sharePlaylist(generateShareText(playlist, tracks))
                }
            }

            val bottomSheetBehavior = BottomSheetBehavior.from(actionsBottomSheet).apply {
                state = BottomSheetBehavior.STATE_HIDDEN

                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                overlay.isVisible = false
                            }

                            else -> {
                                Glide.with(requireContext()).load(playlist.pathToPlaylistIcon)
                                    .placeholder(R.drawable.track_icon_placeholder).transform(
                                        CenterCrop(), RoundedCorners(
                                            TypedValue.applyDimension(
                                                TypedValue.COMPLEX_UNIT_DIP,
                                                2F,
                                                requireContext().resources.displayMetrics
                                            ).toInt()
                                        )
                                    ).diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true).into(playlistIcon)
                                playlistNameItem.text = playlist.playlistName
                                playlistInfoItem.text =
                                    requireContext().resources.getQuantityString(
                                        R.plurals.tracks_count,
                                        playlist.numberOfTracks,
                                        playlist.numberOfTracks
                                    )
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
            }

            actionsButton.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            changePlaylistInfo.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                val bundle = Bundle().apply {
                    putInt("playlistId", playlist.playlistId)
                }
                findNavController().navigate(
                    R.id.action_playlistInfoFragment_to_playlistMakerFragment,
                    bundle
                )
            }

            deletePlaylist.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                showDeletePlaylistDialog()
            }

            sharePlaylist.setOnClickListener {
                if (tracks.isEmpty()) {
                    Toast.makeText(
                        requireContext(), getString(R.string.playlist_is_empty), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.sharePlaylist(generateShareText(playlist, tracks))
                }
            }
        }

        tracksAdapter = TrackListAdapter(tracks, onTrackClick = { track ->
            (activity as RootActivity).animateBottomNavigationView()
            onTrackClickDebounce(track)
        }, onTrackLongClick = { track ->
            showDeleteTrackDialog(track)
        })
        binding.rvTracks.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvTracks.adapter = tracksAdapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.playlistState.collect { state ->
                render(state)
            }
        }

        viewModel.tracksStateLiveData().observe(viewLifecycleOwner) { state ->
            render(state)
        }

        viewModel.playlistLength.observe(viewLifecycleOwner) { minutes ->
            currentDuration = minutes
            updatePlaylistInfo()
        }

        viewModel.playlistTracksCount.observe(viewLifecycleOwner) { count ->
            currentTracksCount = count
            updatePlaylistInfo()
        }
    }

    private fun render(state: PlaylistsState) {
        when (state) {
            is PlaylistsState.PlaylistContent -> loadPlaylist(state.playlist)
        }
    }

    private fun render(state: TracksState) {
        when (state) {
            is TracksState.Content -> loadTracks(state.tracks)
        }
    }

    private fun loadPlaylist(playlist: Playlist?) {
        this.playlist = playlist!!
        if (!playlist.pathToPlaylistIcon.isNullOrEmpty()) {
            Glide.with(requireContext()).load(playlist.pathToPlaylistIcon).centerCrop()
                .placeholder(R.drawable.track_icon_placeholder).diskCacheStrategy(
                    DiskCacheStrategy.NONE
                ).skipMemoryCache(true).into(binding.playlistImage)
            binding.playlistImage.isVisible = true
            binding.playlistPlaceholder.isVisible = false
        } else {
            Glide.with(requireContext()).load(playlist.pathToPlaylistIcon).centerCrop()
                .placeholder(R.drawable.track_icon_placeholder).diskCacheStrategy(
                    DiskCacheStrategy.NONE
                ).skipMemoryCache(true).into(binding.playlistPlaceholder)
            binding.playlistImage.isVisible = false
            binding.playlistPlaceholder.isVisible = true
        }
        binding.playlistName.text = playlist.playlistName
        binding.playlistDescription.text = playlist.playlistDescription
    }

    private fun updatePlaylistInfo() {
        val playlistLengthText = requireContext().resources.getQuantityString(
            R.plurals.minutes_count, currentDuration, currentDuration
        )
        val playlistTracksText = requireContext().resources.getQuantityString(
            R.plurals.tracks_count, currentTracksCount, currentTracksCount
        )
        binding.playlistInfo.text = requireContext().getString(
            R.string.doubleInfo, playlistLengthText, playlistTracksText
        )
    }

    private fun loadTracks(tracks: List<Track>) {
        this.tracks.clear()
        this.tracks.addAll(tracks)
        if (tracks.isNotEmpty()) {
            binding.placeholderText.isVisible = false
        }
        tracksAdapter.notifyDataSetChanged()
    }

    private fun showDeleteTrackDialog(track: Track) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(
            getString(R.string.to_delete_track_from_playlist)
        ).setPositiveButton(getString(R.string.yes)) { dialog, which ->
            viewModel.deleteTrackFromPlaylist(track.trackId, currentPlaylistId)
        }.setNegativeButton(getString(R.string.no), null).show()
    }

    private fun showDeletePlaylistDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Хотите удалить плейлист «${playlist.playlistName}»?")
            .setPositiveButton(getString(R.string.yes)) { dialog, which ->
                deletePlaylistAndNavigateBack()
            }.setNegativeButton(getString(R.string.no)) { dialog, which ->
            }.show()
    }

    private fun deletePlaylistAndNavigateBack() {
        viewModel.deletePlaylist(currentPlaylistId)
        findNavController().popBackStack()
    }

    private fun generateShareText(playlist: Playlist, tracks: List<Track>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(playlist.playlistName).append("\n")
        playlist.playlistDescription?.let {
            stringBuilder.append(it).append("\n")
        }
        val tracksCountText = requireContext().resources.getQuantityString(
            R.plurals.tracks_count, playlist.numberOfTracks, playlist.numberOfTracks
        )
        stringBuilder.append(tracksCountText).append("\n\n")
        tracks.forEachIndexed { index, track ->
            val trackNumber = index + 1
            val trackInfo = "${track.artistName} - ${track.trackName} (${track.trackTimeMillis})"
            stringBuilder.append("$trackNumber. $trackInfo\n")
        }
        return stringBuilder.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
    }
}
