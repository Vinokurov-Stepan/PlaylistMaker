package com.practicum.playlistmaker.search.presentation.view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.util.debounce
import com.practicum.playlistmaker.search.presentation.TracksState
import com.practicum.playlistmaker.search.domain.api.SearchHistoryInteractor
import com.practicum.playlistmaker.search.domain.api.TracksInteractor
import com.practicum.playlistmaker.search.presentation.SearchUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val tracksInteractor: TracksInteractor,
    private val searchHistory: SearchHistoryInteractor,
    private val errorStr: String,
    private val emptyStr: String
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_DELAY = 2_000L
    }

    private val _state = MutableStateFlow<TracksState>(TracksState.Content(persistentListOf()))
    val state: StateFlow<TracksState> = _state.asStateFlow()

    private val _historyState = MutableStateFlow<ImmutableList<Track>>(persistentListOf())
    val historyState: StateFlow<ImmutableList<Track>> = _historyState.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var lastSearchedText: String? = null

    init {
        loadHistory()
    }

    fun onUpdateClicked() {
        performSearch()
    }

    private fun searchDebounce(changedText: String) {
        trackSearchDebounce(changedText)
    }

    private val trackSearchDebounce =
        debounce<String>(SEARCH_DEBOUNCE_DELAY, viewModelScope, true) {
            performSearch()
        }

    private fun performSearch(force: Boolean = false) {
        val searchText = _uiState.value.searchText
        if (searchText.isEmpty()) {
            _state.value = TracksState.Content(persistentListOf())
            lastSearchedText = null
            return
        }
        if (!force && lastSearchedText == searchText) {
            return
        }
        if (searchText.isNotEmpty()) {
            _state.value = TracksState.Loading
            viewModelScope.launch {
                tracksInteractor.searchTracks(searchText).collect { pair ->
                    lastSearchedText = searchText
                    processSearchResult(searchText, pair.first, pair.second)
                }
            }
        }
    }

    private fun processSearchResult(
        searchText: String,
        foundTracks: List<Track>?,
        errorMessage: String?
    ) {
        val tracks = foundTracks?.toImmutableList() ?: persistentListOf()
        when {
            errorMessage != null -> {
                renderState(
                    TracksState.Error(
                        errorStr
                    )
                )
                lastSearchedText = null
            }

            tracks.isEmpty() -> {
                renderState(
                    TracksState.Empty(
                        emptyStr
                    )
                )
                lastSearchedText = searchText
            }

            else -> {
                renderState(TracksState.Content(tracks))
                lastSearchedText = searchText
            }
        }
    }

    fun onTrackClicked(track: Track) {
        viewModelScope.launch {
            searchHistory.setListeningTrack(track)
        }
    }

    fun addTrackInHistory(track: Track) {
        viewModelScope.launch {
            searchHistory.addTrack(track)
            loadHistory()
        }
    }

    fun onSearchTextChanged(text: String) {
        val currentUiState = _uiState.value
        val shouldShowHistory = text.isEmpty() &&
                currentUiState.isSearchFocused &&
                _historyState.value.isNotEmpty()
        _uiState.value = currentUiState.copy(
            searchText = text,
            showHistory = shouldShowHistory
        )
        if (text.isEmpty()) {
            _state.value = TracksState.Content(persistentListOf())
            lastSearchedText = null
        } else {
            searchDebounce(text)
        }
        if (text.isEmpty() && currentUiState.isSearchFocused && _historyState.value.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showHistory = true)
        }
    }

    fun onSearchFocusChanged(focused: Boolean) {
        val currentUiState = _uiState.value
        val shouldShowHistory = currentUiState.searchText.isEmpty() &&
                focused &&
                _historyState.value.isNotEmpty()
        _uiState.value = currentUiState.copy(
            isSearchFocused = focused,
            showHistory = shouldShowHistory
        )
        if (!focused) {
            _uiState.value = _uiState.value.copy(showHistory = false)
        }
    }

    fun onClearSearch() {
        _uiState.value = SearchUiState(
            searchText = "",
            isSearchFocused = true,
            showHistory = _historyState.value.isNotEmpty()
        )
        lastSearchedText = null
        renderState(TracksState.Content(persistentListOf()))
    }

    fun onClearHistory() {
        viewModelScope.launch {
            searchHistory.clearHistory()
            renderHistoryState(persistentListOf())
            _uiState.value = _uiState.value.copy(showHistory = false)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val tracks = searchHistory.loadTracks()
            renderHistoryState(tracks)
            val currentUiState = _uiState.value
            val shouldShowHistory = currentUiState.searchText.isEmpty() &&
                    currentUiState.isSearchFocused &&
                    tracks.isNotEmpty()
            if (currentUiState.showHistory != shouldShowHistory) {
                _uiState.value = currentUiState.copy(showHistory = shouldShowHistory)
            }
        }
    }

    private fun renderState(state: TracksState) {
        _state.value = state
    }

    private fun renderHistoryState(historyTracks: List<Track>) {
        _historyState.value = historyTracks.toImmutableList()
    }
}
