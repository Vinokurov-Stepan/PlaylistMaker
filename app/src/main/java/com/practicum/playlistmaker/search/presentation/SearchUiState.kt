package com.practicum.playlistmaker.search.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class SearchUiState(
    val searchText: String = "",
    val isSearchFocused: Boolean = false,
    val showHistory: Boolean = false
)
