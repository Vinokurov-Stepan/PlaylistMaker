package com.practicum.playlistmaker.media.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.ui.root.RootActivity
import com.practicum.playlistmaker.media.presentation.FavouritesState
import com.practicum.playlistmaker.media.presentation.view_model.FavouritesViewModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class FavouritesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FavouritesScreen(
                    viewModel = koinViewModel(),
                    onNavigateToAudio = { track ->
                        (activity as RootActivity).animateBottomNavigationView()
                        findNavController().navigate(
                            R.id.action_mediaFragment_to_audioFragment
                        )
                    },
                    clickDebounceDelay = CLICK_DEBOUNCE_DELAY
                )
            }
        }
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
        fun newInstance() = FavouritesFragment()
    }
}

@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    onNavigateToAudio: (Track) -> Unit,
    clickDebounceDelay: Long
) {
    viewModel.fillData()
    val state = viewModel.state.collectAsState().value

    var clickTimer by remember { mutableLongStateOf(0L) }
    val debouncedOnTrackClick = remember(clickDebounceDelay) {
        { track: Track ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - clickTimer >= clickDebounceDelay) {
                clickTimer = currentTime
                viewModel.onTrackClicked(track)
                onNavigateToAudio(track)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        FavouritesContent(
            state = state,
            onTrackClick = debouncedOnTrackClick
        )
    }
}

@Composable
private fun FavouritesContent(
    state: FavouritesState,
    onTrackClick: (Track) -> Unit
) {
    when (state) {
        is FavouritesState.Content -> {
            TracksListView(
                tracks = state.tracks,
                onTrackClick = onTrackClick
            )
        }

        is FavouritesState.Empty -> {
            PlaceholderView()
        }
    }
}

@Composable
private fun PlaceholderView() {
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.nothing_found),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 106.dp)
                .size(120.dp)
        )
        Text(
            text = stringResource(R.string.favorites_is_empty),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            fontSize = 19.sp,
            color = textColor,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TracksListView(
    tracks: ImmutableList<Track>,
    onTrackClick: (Track) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(tracks) { track ->
            TrackItem(
                track = track,
                onClick = { onTrackClick(track) }
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

@Preview(name = "favouritesScreen", showSystemUi = false)
@Composable
private fun FavouritesScreenPreview() {
    val isContented = false
    val isEmpty = false

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
        if (isContented) {
            TracksListView(tracks.toImmutableList(), onTrackClick = {})
        }
        if (isEmpty) {
            PlaceholderView()
        }
    }
}
