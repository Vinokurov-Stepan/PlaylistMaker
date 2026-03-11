package com.practicum.playlistmaker.media.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.practicum.playlistmaker.R
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.media.presentation.view_model.PlaylistsViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

class PlaylistsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PlaylistsScreen(
                    viewModel = koinViewModel(),
                    onNavigateToPlaylistMaker = {
                        findNavController().navigate(
                            R.id.action_mediaFragment_to_playlistMakerFragment
                        )
                    },
                    onNavigateToPlaylistInfo = { playlist ->
                        val bundle = Bundle().apply {
                            putInt("playlistId", playlist.playlistId)
                        }
                        findNavController().navigate(
                            R.id.action_mediaFragment_to_playlistInfoFragment,
                            bundle
                        )
                    },
                    clickDebounceDelay = CLICK_DEBOUNCE_DELAY
                )
            }
        }
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
        fun newInstance() = PlaylistsFragment()
    }
}

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onNavigateToPlaylistMaker: () -> Unit,
    onNavigateToPlaylistInfo: (Playlist) -> Unit,
    clickDebounceDelay: Long
) {

    viewModel.fillData()
    val playlistState = viewModel.playlistState.collectAsState().value

    var clickTimer by remember { mutableLongStateOf(0L) }

    fun tryPerformAction(action: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - clickTimer >= clickDebounceDelay) {
            clickTimer = currentTime
            action()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CreatePlaylistButton(
            onCreatePlaylistClick = {
                tryPerformAction(onNavigateToPlaylistMaker)
            }
        )
        when (playlistState) {
            is PlaylistsState.Content -> {
                PlaylistsView(
                    playlists = playlistState.playlists.toImmutableList(),
                    onPlaylistClick = { playlist ->
                        tryPerformAction { onNavigateToPlaylistInfo(playlist) }
                    }
                )
            }

            is PlaylistsState.Empty -> {
                PlaceholderView()
            }
        }
    }
}

@Composable
private fun CreatePlaylistButton(
    onCreatePlaylistClick: () -> Unit
) {
    val buttonTextColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.black) else colorResource(R.color.white)
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    Button(
        onClick = onCreatePlaylistClick,
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        shape = RoundedCornerShape(54.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 14.dp),
            text = stringResource(R.string.create_playlist),
            color = buttonTextColor,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium))
        )
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
                .padding(top = 46.dp)
                .size(120.dp)
        )
        Text(
            text = stringResource(R.string.playlists_is_empty),
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
private fun PlaylistsView(
    playlists: ImmutableList<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(playlists) { playlist ->
            PlaylistItem(
                playlist = playlist,
                onPlaylistClick = { onPlaylistClick(playlist) }
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onPlaylistClick: () -> Unit
) {
    val context = LocalContext.current
    val tracksText = remember(playlist.numberOfTracks) {
        context.resources.getQuantityString(
            R.plurals.tracks_count,
            playlist.numberOfTracks,
            playlist.numberOfTracks
        )
    }
    val textColor by animateColorAsState(
        targetValue = if (isSystemInDarkTheme()) colorResource(R.color.white) else colorResource(R.color.black)
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .combinedClickable(
                onClick = onPlaylistClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(playlist.pathToPlaylistIcon)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    error = painterResource(R.drawable.track_icon_placeholder),
                    placeholder = painterResource(R.drawable.track_icon_placeholder),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = playlist.playlistName ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(top = 4.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
            Text(
                text = tracksText,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Preview(name = "playlistsScreen", showSystemUi = false)
@Composable
private fun SearchScreenPreview() {
    val isContented = true
    val isEmpty = false

    val playlists = listOf(
        Playlist(
            playlistId = 1,
            playlistName = "Лучшие хиты 2024 года для вечеринки",
            playlistDescription = "Самые популярные треки этого года",
            pathToPlaylistIcon = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/0c/82/48/0c8248a8-4a5e-8e2a-7a0c-7a9f6a9b3e1c/196589873069.jpg/600x600bb.jpg",
            tracks = "[1,2,3]",
            numberOfTracks = 25
        ),
        Playlist(
            playlistId = 2,
            playlistName = "Медитативная музыка",
            playlistDescription = "Для релаксации и концентрации",
            pathToPlaylistIcon = null,
            tracks = "[4,5]",
            numberOfTracks = 15
        ),
        Playlist(
            playlistId = 3,
            playlistName = "Утренняя зарядка",
            playlistDescription = "Энергичная музыка для пробуждения",
            pathToPlaylistIcon = "",
            tracks = "[6,7,8]",
            numberOfTracks = 12
        ),
        Playlist(
            playlistId = 4,
            playlistName = "Супер длинное название плейлиста которое должно обрезаться тремя точками в конце строки",
            playlistDescription = "Тестовый плейлист",
            pathToPlaylistIcon = "https://is2-ssl.mzstatic.com/image/thumb/Music125/v4/3d/9d/38/3d9d3811-71f0-3a0e-1ada-3004e56ff852/827969428726.jpg/600x600bb.jpg",
            tracks = "[9]",
            numberOfTracks = 1
        ),
        Playlist(
            playlistId = 5,
            playlistName = "Любимый трек",
            playlistDescription = "Мой самый любимый трек",
            pathToPlaylistIcon = "https://is5-ssl.mzstatic.com/image/thumb/Music125/v4/a0/4d/c4/a04dc484-03cc-02aa-fa82-5334fcb4bc16/18UMGIM24878.rgb.jpg/600x600bb.jpg",
            tracks = "[10]",
            numberOfTracks = 1
        ),
        Playlist(
            playlistId = 6,
            playlistName = "Дуэты",
            playlistDescription = "Лучшие дуэты",
            pathToPlaylistIcon = "https://is4-ssl.mzstatic.com/image/thumb/Music115/v4/1f/80/1f/1f801fc1-8c0f-ea3e-d3e5-387c6619619e/16UMGIM86640.rgb.jpg/600x600bb.jpg",
            tracks = "[11,12]",
            numberOfTracks = 2
        ),
        Playlist(
            playlistId = 7,
            playlistName = "Пятница",
            playlistDescription = "Для пятничного настроения",
            pathToPlaylistIcon = "https://is2-ssl.mzstatic.com/image/thumb/Music62/v4/7e/17/e3/7e17e33f-2efa-2a36-e916-7f808576cf6b/mzm.fyigqcbs.jpg/600x600bb.jpg",
            tracks = "[13,14,15,16,17]",
            numberOfTracks = 5
        ),
        Playlist(
            playlistId = 8,
            playlistName = "21 век",
            playlistDescription = "Музыка 21 века",
            pathToPlaylistIcon = "https://is5-ssl.mzstatic.com/image/thumb/Music125/v4/3d/9d/38/3d9d3811-71f0-3a0e-1ada-3004e56ff852/827969428726.jpg/600x600bb.jpg",
            tracks = "[18,19,20]",
            numberOfTracks = 21
        ),
        Playlist(
            playlistId = 9,
            playlistName = "Новый плейлист",
            playlistDescription = "Еще нет треков",
            pathToPlaylistIcon = null,
            tracks = null,
            numberOfTracks = 0
        ),
        Playlist(
            playlistId = 10,
            playlistName = "Вся моя музыка",
            playlistDescription = "Все треки в библиотеке",
            pathToPlaylistIcon = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/b3/7f/03/b37f03ee-db15-6eb4-394f-38f7f8a2c9b6/196589873069.jpg/600x600bb.jpg",
            tracks = "[21,22,23,24,25,26,27,28,29,30]",
            numberOfTracks = 152
        ),
        Playlist(
            playlistId = 11,
            playlistName = "Тест с ошибкой",
            playlistDescription = "Для проверки обработки ошибок",
            pathToPlaylistIcon = "https://невалидный-урл.com/image.jpg",
            tracks = "[31]",
            numberOfTracks = 3
        ),
        Playlist(
            playlistId = 12,
            playlistName = "Тест",
            playlistDescription = null,
            pathToPlaylistIcon = null,
            tracks = null,
            numberOfTracks = 7
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        CreatePlaylistButton {}
        if (isContented) {
            PlaylistsView(
                playlists = playlists.toImmutableList(),
                onPlaylistClick = {}
            )
        }
        if (isEmpty) {
            PlaceholderView()
        }
    }
}
