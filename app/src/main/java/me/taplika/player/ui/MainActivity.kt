package me.taplika.player.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.taplika.player.search.RemoteSongRepository.RemoteSong
import me.taplika.player.ui.screens.HomeScreen
import me.taplika.player.ui.screens.PlaylistScreen
import me.taplika.player.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicPlayerTheme {
                MusicPlayerRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MusicPlayerRoot(viewModel: MainViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val navController = rememberNavController()
    val playerState by viewModel.playerState.collectAsState()

    var pendingUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingRemoteSong by remember { mutableStateOf<RemoteSong?>(null) }
    var playlistPickerVisible by remember { mutableStateOf(false) }
    var playlistPickerSource by remember { mutableStateOf<PickerSource?>(null) }

    val context = LocalContext.current
    val pickAudioLauncher = rememberLauncherForAudio(context) { uris ->
        if (uris.isNotEmpty()) {
            pendingUris = uris
            playlistPickerSource = PickerSource.Local
            playlistPickerVisible = true
        }
    }

    val onPlayPauseAction = {
        if (playerState.isConnected && playerState.isPlaying) {
            viewModel.pause()
        } else {
            viewModel.play()
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                playlists = playlists,
                searchResults = searchResults,
                isSearching = isSearching,
                repeatMode = repeatMode,
                playerState = playerState,
                onPlaylistSelected = {
                    viewModel.selectPlaylist(it)
                    navController.navigate("playlist/$it")
                },
                onCreatePlaylist = { name ->
                    viewModel.createPlaylist(name)
                },
                onRenamePlaylist = { playlistId, name ->
                    viewModel.renamePlaylist(playlistId, name)
                },
                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                onSearch = { query -> viewModel.search(query) },
                onPlayRemoteSong = { song -> viewModel.playRemoteSong(song) },
                onAddRemoteSong = { song ->
                    pendingRemoteSong = song
                    playlistPickerSource = PickerSource.Remote
                    playlistPickerVisible = true
                },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onPlayPause = onPlayPauseAction,
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onImportLocal = { pickAudioLauncher.launch(arrayOf("audio/*")) }
            )
        }
        composable("playlist/{id}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            LaunchedEffect(playlistId) {
                playlistId?.let { viewModel.selectPlaylist(it) }
            }
            val songs by viewModel.playlistSongs.collectAsState()
            PlaylistScreen(
                playlist = playlists.firstOrNull { it.playlistId == playlistId },
                songs = songs,
                playlists = playlists,
                repeatMode = repeatMode,
                playerState = playerState,
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onPlayPause = onPlayPauseAction,
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onBack = { navController.popBackStack() },
                onPlaySong = { songId -> playlistId?.let { viewModel.playSongFromPlaylist(it, songId) } },
                onRemoveSong = { songId -> playlistId?.let { viewModel.removeSongFromPlaylist(it, songId) } },
                onMoveSongToPlaylist = { song, target ->
                    playlistId?.let { currentId ->
                        viewModel.moveSongToPlaylist(song, target, currentId)
                    }
                }
            )
        }
    }

    if (playlistPickerVisible) {
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = {
                playlistPickerVisible = false
                pendingRemoteSong = null
                pendingUris = emptyList()
                playlistPickerSource = null
            },
            onCreatePlaylist = { name, onCreated ->
                viewModel.createPlaylist(name) { id ->
                    onCreated(id)
                }
            },
            onPlaylistSelected = { playlistId ->
                when (playlistPickerSource) {
                    PickerSource.Remote -> pendingRemoteSong?.let { viewModel.addRemoteSongToPlaylist(it, playlistId) }
                    PickerSource.Local -> if (pendingUris.isNotEmpty()) {
                        viewModel.addLocalSongsToPlaylist(pendingUris, playlistId)
                    }
                    else -> {}
                }
                playlistPickerVisible = false
                pendingRemoteSong = null
                pendingUris = emptyList()
                playlistPickerSource = null
            }
        )
    }
}

private enum class PickerSource { Remote, Local }

@Composable
private fun rememberLauncherForAudio(context: Context, onResult: (List<Uri>) -> Unit) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    val filtered = uris?.filterNotNull() ?: emptyList()
    filtered.forEach { uri ->
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (ex: SecurityException) {
            // ignore if permission already granted
        }
    }
    onResult(filtered)
}
