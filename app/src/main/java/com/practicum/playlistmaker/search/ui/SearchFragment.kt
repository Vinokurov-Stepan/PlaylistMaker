package com.practicum.playlistmaker.search.ui

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.ui.root.RootActivity
import com.practicum.playlistmaker.core.util.NetworkStateBroadcastReceiver
import com.practicum.playlistmaker.search.presentation.TracksState
import com.practicum.playlistmaker.search.presentation.view_model.SearchViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.collections.listOf

class SearchFragment : Fragment() {

    private lateinit var errorMessage: String
    private lateinit var emptyMessage: String
    private val networkStateBroadcastReceiver = NetworkStateBroadcastReceiver()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            errorMessage = getString(R.string.something_went_wrong)
            emptyMessage = getString(R.string.nothing_found)
            setContent {
                SearchScreen(
                    koinViewModel(parameters = {
                        parametersOf(
                            errorMessage, emptyMessage
                        )
                    }),
                    onNavigateToAudio = { track ->
                        (activity as RootActivity).animateBottomNavigationView()
                        findNavController().navigate(
                            R.id.action_searchFragment_to_audioFragment
                        )
                    },
                    clickDebounceDelay = CLICK_DEBOUNCE_DELAY
                )
            }
        }
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

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
    }
}

@Composable
private fun SearchScreen(
    searchViewModel: SearchViewModel,
    onNavigateToAudio: (Track) -> Unit,
    clickDebounceDelay: Long
) {
    val state = searchViewModel.state.collectAsState().value
    val historyState = searchViewModel.historyState.collectAsState().value
    val uiState = searchViewModel.uiState.collectAsState().value

    var clickTimer by remember { mutableLongStateOf(0L) }
    val debouncedOnTrackClick = remember(clickDebounceDelay) {
        { track: Track, isAddedInHistory: Boolean ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - clickTimer >= clickDebounceDelay) {
                clickTimer = currentTime
                searchViewModel.onTrackClicked(track)
                if (isAddedInHistory) {
                    searchViewModel.addTrackInHistory(track)
                }
                onNavigateToAudio(track)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        SearchTittle()
        SearchTextField(
            searchViewModel,
            uiState.searchText
        )
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.showHistory && historyState.isNotEmpty() -> {
                    HistoryContent(
                        tracks = historyState,
                        onClearHistoryClick = searchViewModel::onClearHistory,
                        onTrackClick = debouncedOnTrackClick
                    )
                }

                else -> {
                    SearchContent(
                        state = state,
                        onUpdateClick = searchViewModel::onUpdateClicked,
                        onTrackClick = debouncedOnTrackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTittle() {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Text(
        text = stringResource(R.string.search),
        modifier = Modifier
            .height(56.dp)
            .padding(start = 16.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        fontSize = 22.sp,
        fontFamily = FontFamily(Font(R.font.ys_display_medium)),
        color = textColor
    )
}

@Composable
private fun SearchTextField(
    searchViewModel: SearchViewModel?,
    searchText: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) {
            colorResource(R.color.white)
        } else {
            colorResource(R.color.grey)
        }
    )
    val placeholderColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.black) else colorResource(R.color.light_grey)
    )
    val interactionSource = remember { MutableInteractionSource() }

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        BasicTextField(
            value = searchText,
            onValueChange = { newText ->
                searchViewModel?.onSearchTextChanged(newText)
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
                .height(36.dp)
                .onFocusChanged { focusState ->
                    searchViewModel?.onSearchFocusChanged(focusState.isFocused)
                },
            interactionSource = interactionSource,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = colorResource(R.color.black),
                fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            singleLine = true,
            cursorBrush = SolidColor(colorResource(R.color.blue)),
            decorationBox = { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = searchText,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(0.dp),
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.loupe),
                            contentDescription = null,
                            tint = placeholderColor
                        )
                    },
                    placeholder = {
                        Text(
                            modifier = Modifier.padding(start = 0.dp),
                            text = stringResource(R.string.search),
                            fontSize = 16.sp,
                            color = placeholderColor,
                            fontFamily = FontFamily(Font(R.font.ys_display_regular))
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        )
        if (searchText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .align(Alignment.CenterVertically)
            ) {
                IconButton(
                    onClick = {
                        searchViewModel?.onClearSearch()
                        val imm =
                            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        val activity = context as? Activity
                        activity?.currentFocus?.let { view ->
                            imm.hideSoftInputFromWindow(view.windowToken, 0)
                        }
                    },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.clear_edit_text),
                        contentDescription = null,
                        tint = placeholderColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchContent(
    state: TracksState,
    onUpdateClick: () -> Unit,
    onTrackClick: (Track, Boolean) -> Unit
) {
    when (state) {
        is TracksState.Loading -> {
            LoadingView()
        }

        is TracksState.Content -> {
            TracksListView(
                tracks = state.tracks.toImmutableList(),
                onTrackClick = onTrackClick
            )
        }

        is TracksState.Empty -> {
            PlaceholderView(
                message = state.emptyMessage,
                imageRes = R.drawable.nothing_found,
                showButton = false,
                onButtonClick = {}
            )
        }

        is TracksState.Error -> {
            PlaceholderView(
                message = state.errorMessage,
                imageRes = R.drawable.something_went_wrong,
                showButton = true,
                onButtonClick = onUpdateClick
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(44.dp)
                .padding(top = 124.dp),
            color = colorResource(R.color.blue),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun TracksListView(
    tracks: ImmutableList<Track>,
    onTrackClick: ((Track, Boolean) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(tracks) { track ->
            TrackItem(
                track = track,
                onClick = { onTrackClick?.let { it(track, true) } }
            )
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )
    val infoTextColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.light_grey)
    )
    val placeholderColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.light_grey)
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(61.dp)
            .combinedClickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(track.artworkUrl100)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
                error = painterResource(R.drawable.track_icon_placeholder),
                placeholder = painterResource(R.drawable.track_icon_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 13.dp)
                    .size(45.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 44.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.trackName,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .padding(top = 1.dp),
                    text = stringResource(
                        R.string.doubleInfo,
                        track.artistName,
                        track.trackTimeMillis
                    ),
                    fontSize = 11.sp,
                    fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                    color = infoTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.arrowforward),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(24.dp),
            tint = placeholderColor
        )
    }
}

@Composable
private fun PlaceholderView(
    message: String,
    imageRes: Int,
    showButton: Boolean,
    onButtonClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.black) else colorResource(R.color.white)
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 86.dp)
                .size(120.dp)
        )
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            fontSize = 19.sp,
            color = textColor,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            textAlign = TextAlign.Center
        )
        if (showButton) {
            Button(
                onClick = onButtonClick,
                modifier = Modifier.padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                shape = RoundedCornerShape(54.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    text = stringResource(R.string.update),
                    color = buttonTextColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily(Font(R.font.ys_display_medium))
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    tracks: ImmutableList<Track>,
    onClearHistoryClick: () -> Unit,
    onTrackClick: ((Track, Boolean) -> Unit)?
) {

    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.black) else colorResource(R.color.white)
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.story_tracks),
            modifier = Modifier
                .padding(
                    top = 26.dp,
                    bottom = 20.dp
                ),
            color = textColor,
            fontSize = 19.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium))
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(tracks) { track ->
                TrackItem(
                    track = track,
                    onClick = { onTrackClick?.let { it(track, false) } }
                )
            }
            item {
                Button(
                    onClick = onClearHistoryClick,
                    modifier = Modifier
                        .padding(top = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    shape = RoundedCornerShape(54.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        text = stringResource(R.string.clear_story),
                        color = buttonTextColor,
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(R.font.ys_display_medium))
                    )
                }
            }
        }
    }
}

@Preview(name = "searchScreen", showSystemUi = false)
@Composable
private fun SearchScreenPreview() {
    val isLoading = false
    val isContented = false
    val isHistoryContented = false
    val isEmpty = false
    val isError = false

    val tracks = listOf(
        Track(
            trackId = 1,
            trackName = "Smells Like Teettttпппппппппппппппппппппtt",
            artistName = "Nirvana",
            trackTimeMillis = "5:01",
            artworkUrl100 = "https://is5-ssl.mzstatic.com/image/thumb/Music125/v4/3d/9d/38/3d9d3811-71f0-3a0e-1ada-3004e56ff852/827969428726.jpg/100x100bb.jpg",
            collectionName = "Nevermind",
            releaseDate = "1991-09-24T12:00:00Z",
            primaryGenreName = "Grunge",
            country = "USA",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/7b/58/c2/7b58c21a-2b51-2bb2-e59a-9bb9b96ad8c3/mzaf_1234567890.m4a",
            isFavorite = false
        ),
        Track(
            trackId = 2,
            trackName = "Billie Jean",
            artistName = "Michael Jackson",
            trackTimeMillis = "4:35",
            artworkUrl100 = "https://is5-ssl.mzstatic.com/image/thumb/Music125/v4/3d/9d/38/3d9d3811-71f0-3a0e-1ada-3004e56ff852/827969428726.jpg/100x100bb.jpg",
            collectionName = "Thriller",
            releaseDate = "1982-11-30T12:00:00Z",
            primaryGenreName = "Pop",
            country = "USA",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/3d/9d/38/3d9d3811-71f0-3a0e-1ada-3004e56ff852/mzaf_1234567891.m4a",
            isFavorite = false
        ),
        Track(
            trackId = 3,
            trackName = "Stayin' Alive",
            artistName = "Bee Gees",
            trackTimeMillis = "4:10",
            artworkUrl100 = "https://is4-ssl.mzstatic.com/image/thumb/Music115/v4/1f/80/1f/1f801fc1-8c0f-ea3e-d3e5-387c6619619e/16UMGIM86640.rgb.jpg/100x100bb.jpg",
            collectionName = "Saturday Night Fever",
            releaseDate = "1977-12-13T12:00:00Z",
            primaryGenreName = "Disco",
            country = "USA",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/1f/80/1f/1f801fc1-8c0f-ea3e-d3e5-387c6619619e/mzaf_1234567892.m4a",
            isFavorite = false
        ),
        Track(
            trackId = 4,
            trackName = "Whole Lotta Love",
            artistName = "Led Zeppelin",
            trackTimeMillis = "5:33",
            artworkUrl100 = "https://is2-ssl.mzstatic.com/image/thumb/Music62/v4/7e/17/e3/7e17e33f-2efa-2a36-e916-7f808576cf6b/mzm.fyigqcbs.jpg/100x100bb.jpg",
            collectionName = "Led Zeppelin II",
            releaseDate = "1969-10-22T12:00:00Z",
            primaryGenreName = "Rock",
            country = "UK",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview62/v4/7e/17/e3/7e17e33f-2efa-2a36-e916-7f808576cf6b/mzaf_1234567893.m4a",
            isFavorite = false
        ),
        Track(
            trackId = 5,
            trackName = "Sweet Child O'Mine",
            artistName = "Guns N' Roses",
            trackTimeMillis = "5:03",
            artworkUrl100 = "https://is5-ssl.mzstatic.com/image/thumb/Music125/v4/a0/4d/c4/a04dc484-03cc-02aa-fa82-5334fcb4bc16/18UMGIM24878.rgb.jpg/100x100bb.jpg",
            collectionName = "Appetite for Destruction",
            releaseDate = "1987-07-21T12:00:00Z",
            primaryGenreName = "Rock",
            country = "USA",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/a0/4d/c4/a04dc484-03cc-02aa-fa82-5334fcb4bc16/mzaf_1234567894.m4a",
            isFavorite = false
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SearchTittle()
        SearchTextField(null, "Запрос")
        if (isLoading) {
            LoadingView()
        }
        if (isContented) {
            TracksListView(tracks.toImmutableList(), null)
        }
        if (isHistoryContented) {
            HistoryContent(tracks.toImmutableList(), onClearHistoryClick = {}, null)
        }
        if (isEmpty) {
            PlaceholderView(
                message = stringResource(R.string.nothing_found),
                imageRes = R.drawable.nothing_found,
                showButton = false,
                onButtonClick = {}
            )
        }
        if (isError) {
            PlaceholderView(
                message = stringResource(R.string.something_went_wrong),
                imageRes = R.drawable.something_went_wrong,
                showButton = true,
                onButtonClick = {}
            )
        }
    }
}
