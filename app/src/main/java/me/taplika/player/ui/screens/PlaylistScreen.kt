package me.taplika.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.taplika.player.data.PlaylistEntity
import me.taplika.player.data.SongWithPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlist: PlaylistEntity?,
    songs: List<SongWithPosition>,
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit,
    onPlaySong: (Long) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onMoveSongToPlaylist: (SongWithPosition, Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (playlist == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Playlist not found", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (songs.isEmpty()) {
                item {
                    Text("This playlist is empty. Add songs from search or local storage.")
                }
            } else {
                items(songs, key = { it.songId }) { song ->
                    SongRow(
                        song = song,
                        playlists = playlists.filter { it.playlistId != playlist.playlistId },
                        onPlay = { onPlaySong(song.songId) },
                        onRemove = { onRemoveSong(song.songId) },
                        onMove = { target -> onMoveSongToPlaylist(song, target) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: SongWithPosition,
    playlists: List<PlaylistEntity>,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Long) -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!song.artist.isNullOrEmpty()) {
                    Text(text = song.artist, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onPlay) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = { expanded.value = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            if (playlists.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No other playlists") },
                    onClick = { },
                    enabled = false
                )
            } else {
                playlists.forEach { playlist ->
                    DropdownMenuItem(
                        text = { Text("Move to ${playlist.name}") },
                        onClick = {
                            expanded.value = false
                            onMove(playlist.playlistId)
                        }
                    )
                }
            }
            DropdownMenuItem(
                text = { Text("Remove from playlist") },
                onClick = {
                    expanded.value = false
                    onRemove()
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}
