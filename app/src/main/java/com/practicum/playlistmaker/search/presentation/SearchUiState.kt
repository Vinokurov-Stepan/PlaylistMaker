package com.practicum.playlistmaker.search.presentation

data class SearchUiState(
    val searchText: String = "",
    val isSearchFocused: Boolean = false,
    val showHistory: Boolean = false
)
