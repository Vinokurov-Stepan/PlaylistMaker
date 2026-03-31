package com.practicum.playlistmaker.player.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.core.domain.models.Track
import com.practicum.playlistmaker.core.util.NetworkStateBroadcastReceiver
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.player.presentation.service.MusicService
import com.practicum.playlistmaker.player.presentation.service.PlayerState
import com.practicum.playlistmaker.player.presentation.view_model.AudioPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class AudioFragment : Fragment() {

    private val networkStateBroadcastReceiver = NetworkStateBroadcastReceiver()
    private lateinit var trackAddMessage: String
    private lateinit var trackAddedMessage: String

    private val viewModel: AudioPlayerViewModel by lazy {
        getViewModel(parameters = {
            parametersOf(trackAddMessage, trackAddedMessage)
        })
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            viewModel.setAudioPlayerControl(binder.getService())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.removeAudioPlayerControl()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            bindMusicService()
        } else {
            Toast.makeText(requireContext(), R.string.foreground_service_error, Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        trackAddMessage = getString(R.string.trackAdd)
        trackAddedMessage = getString(R.string.trackAdded)

        return ComposeView(requireContext()).apply {
            setContent {
                AudioScreen(
                    viewModel = viewModel,
                    onBackPressed = { findNavController().navigateUp() },
                    onCreatePlaylistClick = {
                        findNavController().navigate(R.id.action_audioFragment_to_playlistMakerFragment)
                    },
                    clickDebounceDelay = CLICK_DEBOUNCE_DELAY,
                    animationSpecTime = ANIMATION_SPEC_TIME
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            bindMusicService()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAppInForeground()
        ContextCompat.registerReceiver(
            requireContext(),
            networkStateBroadcastReceiver,
            IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"),
            ContextCompat.RECEIVER_NOT_EXPORTED
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

    override fun onDestroy() {
        super.onDestroy()
        unbindMusicService()
    }

    private fun bindMusicService() {
        val stopIntent = Intent(requireContext(), MusicService::class.java)
        requireContext().stopService(stopIntent)
        val intent = Intent(requireContext(), MusicService::class.java)
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindMusicService() {
        requireContext().unbindService(serviceConnection)
    }

    companion object {
        private const val CLICK_DEBOUNCE_DELAY = 1_000L
        private const val ANIMATION_SPEC_TIME = 300
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioScreen(
    viewModel: AudioPlayerViewModel,
    onBackPressed: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    clickDebounceDelay: Long,
    animationSpecTime: Int
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val playlistsState by viewModel.playlistState.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val currentAnimationSpecTime by rememberUpdatedState(animationSpecTime)

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
        confirmValueChange = { true }
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    val scope = rememberCoroutineScope()
    val transitionProgress = remember { Animatable(0f) }

    val closeBottomSheet = {
        if (showBottomSheet) {
            showBottomSheet = false
            scope.launch {
                bottomSheetState.hide()
            }
        }
    }

    val openBottomSheet = {
        if (!showBottomSheet) {
            showBottomSheet = true
            scope.launch {
                bottomSheetState.partialExpand()
            }
        }
    }

    var clickTimer by remember { mutableLongStateOf(0L) }
    val debouncedOnPlaylistClick = remember(clickDebounceDelay) {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - clickTimer >= clickDebounceDelay) {
                scope.launch {
                    clickTimer = currentTime
                    closeBottomSheet()
                    onCreatePlaylistClick()
                }
            }
        }
    }

    LaunchedEffect(playerState.message) {
        playerState.message?.let { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
            viewModel.resetMessage()
        }
    }

    LaunchedEffect(playerState.shouldHideBottomSheet) {
        if (playerState.shouldHideBottomSheet) {
            closeBottomSheet()
            viewModel.resetBottomSheetFlag()
        }
    }

    LaunchedEffect(bottomSheetState.currentValue) {
        if (bottomSheetState.currentValue == SheetValue.Hidden && showBottomSheet) {
            showBottomSheet = false
        }
    }

    LaunchedEffect(bottomSheetState.currentValue, bottomSheetState.targetValue) {
        snapshotFlow { bottomSheetState.currentValue to bottomSheetState.targetValue }
            .collect { (_, target) ->
                val targetProgress = when (target) {
                    SheetValue.Expanded -> 1f
                    SheetValue.Hidden -> 0f
                    SheetValue.PartiallyExpanded -> 0.5f
                }
                transitionProgress.animateTo(
                    targetProgress,
                    animationSpec = tween(currentAnimationSpecTime)
                )
            }
    }

    val dimAlpha = if (showBottomSheet) transitionProgress.value * 0.6f else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.white))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrowback_white),
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colorResource(id = R.color.white),
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(colorResource(id = R.color.white))
            ) {
                if (playerState.track != null) {
                    TrackContent(
                        context = context,
                        playerState = playerState,
                        onPlayPauseClick = viewModel::playbackControl,
                        onFavoriteClick = viewModel::onFavouriteClicked,
                        showBottomSheetClick = {
                            viewModel.fillData()
                            openBottomSheet()
                        }
                    )
                }
            }
        }

        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable(
                        onClick = {
                            closeBottomSheet()
                        }
                    )
            )
        }

        if (showBottomSheet) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetContent = {
                    PlaylistsBottomSheetContent(
                        context = context,
                        playlistsState = playlistsState,
                        onCreatePlaylistClick = debouncedOnPlaylistClick,
                        onAddToPlaylistClick = { playlist ->
                            viewModel.onTrackAddToPlaylist(playlist)
                        }
                    )
                },
                sheetPeekHeight = 505.dp,
                sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                sheetContainerColor = colorResource(id = R.color.white),
                sheetSwipeEnabled = true,
                sheetDragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colorResource(id = R.color.grey))
                                .clickable {
                                    scope.launch {
                                        when (bottomSheetState.currentValue) {
                                            SheetValue.Expanded -> {
                                                openBottomSheet()
                                            }

                                            else -> {
                                                closeBottomSheet()
                                            }
                                        }
                                    }
                                }
                        )
                    }
                },
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun TrackContent(
    context: Context,
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    showBottomSheetClick: () -> Unit
) {
    val track = playerState.track!!

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 30.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(track.getCoverArtwork())
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
                error = painterResource(R.drawable.track_icon_placeholder),
                placeholder = painterResource(R.drawable.track_icon_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = track.trackName,
            fontSize = 22.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            color = colorResource(id = R.color.black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )

        Text(
            text = track.artistName,
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            color = colorResource(id = R.color.black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 30.dp)
        )

        PlayerControls(
            playerState = playerState,
            showBottomSheetClick = showBottomSheetClick,
            onPlayPauseClick = onPlayPauseClick,
            onFavoriteClick = onFavoriteClick
        )

        Text(
            text = playerState.timer ?: "00:00",
            fontSize = 14.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            color = colorResource(id = R.color.black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }

    TrackDetails(
        track = track
    )
}

@Composable
private fun PlayerControls(
    playerState: PlayerState,
    showBottomSheetClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(51.dp)
                .background(
                    color = colorResource(id = R.color.grey),
                    shape = CircleShape
                )
                .clickable(enabled = playerState.isPlayButtonEnabled) { showBottomSheetClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.add_to_playlist),
                tint = colorResource(id = R.color.white),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable(enabled = playerState.isPlayButtonEnabled) { onPlayPauseClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    when {
                        playerState is PlayerState.Playing -> {
                            R.drawable.to_stop_track
                        }

                        else -> {
                            R.drawable.to_play_track
                        }
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(84.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(51.dp)
                .background(
                    color = colorResource(id = R.color.grey),
                    shape = CircleShape
                )
                .clickable(enabled = playerState.isPlayButtonEnabled) { onFavoriteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (playerState.isTrackLicked) {
                        R.drawable.track_licked_icon
                    } else {
                        R.drawable.to_like_track_icon
                    }
                ),
                contentDescription = null,
                tint = if (playerState.isTrackLicked) {
                    colorResource(id = R.color.red)
                } else {
                    colorResource(id = R.color.white)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TrackDetails(
    track: Track
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        DetailRow(
            label = stringResource(R.string.track_time),
            value = track.trackTimeMillis
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (track.collectionName.isNotEmpty()) {
            DetailRow(
                label = stringResource(R.string.track_album),
                value = track.collectionName
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        DetailRow(
            label = stringResource(R.string.track_year),
            value = track.releaseDate.substringBefore("-")
        )
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow(
            label = stringResource(R.string.track_genre),
            value = track.primaryGenreName
        )
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow(
            label = stringResource(R.string.track_country),
            value = track.country
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_regular)),
            color = colorResource(id = R.color.light_grey)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_regular)),
            color = colorResource(id = R.color.black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlaylistsBottomSheetContent(
    context: Context,
    playlistsState: PlaylistsState,
    onCreatePlaylistClick: () -> Unit,
    onAddToPlaylistClick: (Playlist) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.add_to_playlist),
            fontSize = 19.sp,
            fontFamily = FontFamily(Font(R.font.ys_display_medium)),
            color = colorResource(id = R.color.black),
            modifier = Modifier.padding(top = 30.dp)
        )
        Button(
            onClick = onCreatePlaylistClick,
            modifier = Modifier
                .padding(top = 28.dp)
                .height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.black),
            ),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.create_playlist),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.ys_display_medium)),
                color = colorResource(id = R.color.white)
            )
        }
        when (playlistsState) {
            is PlaylistsState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 10.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(playlistsState.playlists) { playlist ->
                        PlaylistItem(
                            context = context,
                            playlist = playlist,
                            onClick = { onAddToPlaylistClick(playlist) }
                        )
                    }
                }
            }

            is PlaylistsState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 100.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.playlist_is_empty),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(R.font.ys_display_medium)),
                        color = colorResource(id = R.color.black),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    context: Context,
    playlist: Playlist,
    onClick: () -> Unit
) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context = context)
                    .data(playlist.pathToPlaylistIcon)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
                error = painterResource(R.drawable.track_icon_placeholder),
                placeholder = painterResource(R.drawable.track_icon_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playlist.playlistName!!,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                    color = colorResource(id = R.color.black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modifier = Modifier
                        .padding(top = 1.dp),
                    text = pluralStringResource(
                        id = R.plurals.tracks_count,
                        count = playlist.numberOfTracks,
                        playlist.numberOfTracks
                    ),
                    fontSize = 11.sp,
                    fontFamily = FontFamily(Font(R.font.ys_display_regular)),
                    color = colorResource(id = R.color.light_grey),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private class TrackStateProvider : PreviewParameterProvider<Track> {
    override val values = sequenceOf(
        createMockTrack(
            trackName = "This Is A Very Long Song Title That Should Definitely Be Truncated With Ellipsis",
            artistName = "The Long Name Artist Band",
            trackTimeMillis = "12:34",
            collectionName = "The Album With An Extremely Long Name That Will Also Be Truncated",
            releaseDate = "2024-03-15",
            primaryGenreName = "Progressive Experimental Post-Rock",
            country = "United States of America"
        ),
        createMockTrack(
            trackName = "Rhapsody",
            artistName = "Queen",
            trackTimeMillis = "05:55",
            collectionName = "Opera",
            releaseDate = "1975",
            primaryGenreName = "Rock",
            country = "UK"
        )
    )
}

private class PlaylistsStateProvider : PreviewParameterProvider<PlaylistsState> {
    override val values = sequenceOf(
        PlaylistsState.Empty,
        PlaylistsState.Content(
            listOf(
                createMockPlaylist(
                    "Очень длинное название плейлиста которое точно не поместится в одну строку",
                    121
                ),
                createMockPlaylist("Pop Hits", 1),
                createMockPlaylist("Jazz Vibes", 0),
                createMockPlaylist("Workout Mix", 2)
            )
        )
    )
}

private class PlayerStateProvider : PreviewParameterProvider<PlayerState> {
    override val values = sequenceOf(
        PlayerState.Paused(
            track = createMockTrack(),
            timer = "00:00",
            isTrackLicked = false
        ),
        PlayerState.Playing(
            track = createMockTrack(),
            timer = "01:23",
            isTrackLicked = true
        )
    )
}

@Preview(
    name = "Audio Player - Various Tracks",
    showBackground = true
)
@Composable
private fun PreviewAudioPlayerVariousTracks(
    @PreviewParameter(TrackStateProvider::class) track: Track
) {
    MaterialTheme {
        AudioScreenPreview(
            playerState = PlayerState.Playing(
                track = track,
                timer = "02:34",
                isTrackLicked = false
            ),
            playlistsState = PlaylistsState.Content(emptyList())
        )
    }
}

@Preview(
    name = "Audio Player - Various Player States",
    showBackground = true
)
@Composable
private fun PreviewAudioPlayerVariousStates(
    @PreviewParameter(PlayerStateProvider::class) playerState: PlayerState
) {
    MaterialTheme {
        AudioScreenPreview(
            playerState = playerState,
            playlistsState = PlaylistsState.Content(emptyList())
        )
    }
}

@Preview(
    name = "Bottom Sheet - Various Playlists",
    showBackground = true
)
@Composable
private fun PreviewBottomSheetVariousPlaylists(
    @PreviewParameter(PlaylistsStateProvider::class) playlistsState: PlaylistsState
) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            PlaylistsBottomSheetContent(
                context = LocalContext.current,
                playlistsState = playlistsState,
                onCreatePlaylistClick = {},
                onAddToPlaylistClick = {}
            )
        }
    }
}

private fun createMockTrack(
    trackId: Int = 123,
    trackName: String = "Bohemian Rhapsody",
    artistName: String = "Queen",
    trackTimeMillis: String = "05:55",
    artworkUrl100: String = "",
    collectionName: String = "A Night at the Opera",
    releaseDate: String = "1975-10-31",
    primaryGenreName: String = "Rock",
    country: String = "UK",
    previewUrl: String = "",
    isFavorite: Boolean = false
): Track {
    return Track(
        trackId = trackId,
        trackName = trackName,
        artistName = artistName,
        trackTimeMillis = trackTimeMillis,
        artworkUrl100 = artworkUrl100,
        collectionName = collectionName,
        releaseDate = releaseDate,
        primaryGenreName = primaryGenreName,
        country = country,
        previewUrl = previewUrl,
        isFavorite = isFavorite
    )
}

private fun createMockPlaylist(
    playlistName: String = "Мой плейлист",
    numberOfTracks: Int = 15,
    pathToPlaylistIcon: String? = null,
    playlistId: Int = 1,
    playlistDescription: String? = null,
    tracks: String? = null
): Playlist {
    return Playlist(
        playlistId = playlistId,
        playlistName = playlistName,
        playlistDescription = playlistDescription,
        pathToPlaylistIcon = pathToPlaylistIcon,
        tracks = tracks,
        numberOfTracks = numberOfTracks
    )
}

@Composable
private fun AudioScreenPreview(
    playerState: PlayerState,
    playlistsState: PlaylistsState
) {
    val viewModel = remember {
        object : AudioPlayerViewModel(
            playlistsInteractor = null,
            trackAddMessage = "Добавлено в плейлист",
            trackAddedMessage = "Трек уже в плейлисте"
        ) {
            override val playerState: StateFlow<PlayerState> =
                MutableStateFlow(playerState).asStateFlow()
            override val playlistState: StateFlow<PlaylistsState> =
                MutableStateFlow(playlistsState).asStateFlow()
        }
    }

    AudioScreen(
        viewModel = viewModel,
        onBackPressed = {},
        onCreatePlaylistClick = {},
        clickDebounceDelay = 1000L,
        animationSpecTime = 300
    )
}
