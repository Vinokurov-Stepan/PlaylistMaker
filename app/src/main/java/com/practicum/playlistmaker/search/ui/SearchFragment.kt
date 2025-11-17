package com.practicum.playlistmaker.search.ui

import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.ui.root.RootActivity
import com.practicum.playlistmaker.core.util.NetworkStateBroadcastReceiver
import com.practicum.playlistmaker.core.util.debounce
import com.practicum.playlistmaker.databinding.FragmentSearchBinding
import com.practicum.playlistmaker.search.presentation.TracksState
import com.practicum.playlistmaker.search.presentation.view_model.SearchViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class SearchFragment : Fragment() {

    private val networkStateBroadcastReceiver = NetworkStateBroadcastReceiver()
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var errorMessage: String
    private lateinit var emptyMessage: String
    private lateinit var viewModel: SearchViewModel
    private lateinit var onTrackClickDebounce: (Track) -> Unit
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    private lateinit var tracksAdapter: TrackListAdapter
    private lateinit var storyTracksAdapter: TrackListAdapter
    private val tracks = mutableListOf<Track>()
    private val storyList = mutableListOf<Track>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        errorMessage = getString(R.string.something_went_wrong)
        emptyMessage = getString(R.string.nothing_found)

        viewModel = getViewModel {
            parametersOf(errorMessage, emptyMessage)
        }

        onTrackClickDebounce = debounce<Track>(
            CLICK_DEBOUNCE_DELAY, viewLifecycleOwner.lifecycleScope, false
        ) { track ->
            viewModel.addTrack(track)
            findNavController().navigate(
                R.id.action_searchFragment_to_audioFragment
            )
        }

        if (savedInstanceState != null) {
            viewModel.changedText = savedInstanceState.getString(TEXT_KEY, "")
            binding.searchEditText.setText(viewModel.changedText)
        }

        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == TRACK_ID) {
                viewModel.loadTracks(storyList)
                storyTracksAdapter.notifyDataSetChanged()
            }
        }
        viewModel.registerOnSharedPreferenceChangeListener(listener)

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        with(binding) {
            updateButton.visibility = View.GONE
            storyTracks.visibility = View.GONE

            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchClearIcon.isVisible = !s.isNullOrEmpty()
                    println(s.toString())
                    viewModel.searchDebounce(s.toString())
                    if (searchEditText.hasFocus() && s?.isEmpty() == true && storyList.isNotEmpty()) {
                        showHistory()
                    } else {
                        storyTracks.visibility = View.GONE
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            searchEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && searchEditText.text.isEmpty()) {
                    viewModel.loadTracks(storyList)
                    storyTracksAdapter.notifyDataSetChanged()
                    if (storyList.isNotEmpty()) {
                        showHistory()
                    }
                }
            }

            searchClearIcon.setOnClickListener {
                searchEditText.setText("")
                hideSoftKeyboard(it)
                tracks.clear()
                tracksAdapter.notifyDataSetChanged()
                viewModel.clearSearch()
            }

            clearStoryTracksButton.setOnClickListener {
                viewModel.clearHistory(storyList)
                storyTracksAdapter.notifyDataSetChanged()
                storyTracks.visibility = View.GONE
            }

            updateButton.setOnClickListener {
                viewModel.search()
            }

            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        tracksAdapter = TrackListAdapter(tracks, onTrackClick = { track ->
            (activity as RootActivity).animateBottomNavigationView()
            onTrackClickDebounce(track)
        }, onTrackLongClick = { track ->
        })
        binding.rvTrackList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvTrackList.adapter = tracksAdapter

        storyTracksAdapter = TrackListAdapter(storyList, onTrackClick = { track ->
            (activity as RootActivity).animateBottomNavigationView()
            onTrackClickDebounce(track)
        }, onTrackLongClick = { track ->
        })
        binding.storyTrackList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.storyTrackList.adapter = storyTracksAdapter
    }

    private fun setupObservers() {
        viewModel.getState().observe(viewLifecycleOwner) { state ->
            render(state)
        }
    }

    private fun showHistory() {
        this.tracks.clear()
        tracksAdapter.notifyDataSetChanged()
        with(binding) {
            placeholderMessage.visibility = View.GONE
            storyTracks.visibility = View.VISIBLE
        }
    }

    private fun render(state: TracksState) {
        when (state) {
            is TracksState.Loading -> showLoading()
            is TracksState.Content -> showContent(state.tracks)
            is TracksState.Empty -> showEmpty(state.emptyMessage)
            is TracksState.Error -> showError(state.errorMessage)
        }
    }

    private fun showLoading() {
        with(binding) {
            rvTrackList.visibility = View.GONE
            placeholderMessage.visibility = View.GONE
            storyTracks.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun showContent(tracks: List<Track>) {
        with(binding) {
            rvTrackList.visibility = View.VISIBLE
            placeholderMessage.visibility = View.GONE
            storyTracks.visibility = View.GONE
            progressBar.visibility = View.GONE
        }
        this.tracks.clear()
        this.tracks.addAll(tracks)
        tracksAdapter.notifyDataSetChanged()
    }

    private fun showEmpty(emptyMessage: String) {
        with(binding) {
            rvTrackList.visibility = View.GONE
            placeholderMessage.visibility = View.VISIBLE
            storyTracks.visibility = View.GONE
            progressBar.visibility = View.GONE
            tracks.clear()
            tracksAdapter.notifyDataSetChanged()
            placeholderText.text = emptyMessage
            placeholderImage.setImageResource(R.drawable.nothing_found)
            updateButton.visibility = View.GONE
        }
    }

    private fun showError(errorMessage: String) {
        with(binding) {
            rvTrackList.visibility = View.GONE
            placeholderMessage.visibility = View.VISIBLE
            storyTracks.visibility = View.GONE
            progressBar.visibility = View.GONE
            tracks.clear()
            tracksAdapter.notifyDataSetChanged()
            placeholderText.text = errorMessage
            placeholderImage.setImageResource(R.drawable.something_went_wrong)
            updateButton.visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(TEXT_KEY, viewModel.changedText)
    }

    private fun hideSoftKeyboard(view: View) {
        val manager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            requireContext(),
            networkStateBroadcastReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(networkStateBroadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
        private const val TRACK_ID = "TRACK_ID"
        private const val TEXT_KEY = "TEXT_KEY"
    }
}
