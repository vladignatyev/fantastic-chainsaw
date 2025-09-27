package me.taplika.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.taplika.player.data.PlaylistEntity
import me.taplika.player.playback.RepeatMode
import me.taplika.player.playback.MusicServiceConnection.PlayerUiState
import me.taplika.player.search.RemoteSongRepository.RemoteSong
import me.taplika.player.ui.widgets.PlayerBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playlists: List<PlaylistEntity>,
    searchResults: List<RemoteSong>,
    isSearching: Boolean,
    repeatMode: RepeatMode,
    playerState: PlayerUiState,
    onPlaylistSelected: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onSearch: (String) -> Unit,
    onPlayRemoteSong: (RemoteSong) -> Unit,
    onAddRemoteSong: (RemoteSong) -> Unit,
    onToggleRepeat: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onImportLocal: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var createDialogVisible by remember { mutableStateOf(false) }
    var renameDialogVisible by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<PlaylistEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Modern MP3 Player") },
                actions = {
                    IconButton(onClick = onImportLocal) {
                        Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Import local songs")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { createDialogVisible = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add playlist")
            }
        },
        bottomBar = {
            PlayerBar(
                playerState = playerState,
                repeatMode = repeatMode,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleRepeat = onToggleRepeat
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SearchSection(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { onSearch(query) }
                )
            }

            if (isSearching) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (searchResults.isNotEmpty()) {
                item {
                    Text(text = "Search results", style = MaterialTheme.typography.titleMedium)
                }
                items(searchResults) { song ->
                    SearchResultItem(
                        song = song,
                        onPlay = { onPlayRemoteSong(song) },
                        onAdd = { onAddRemoteSong(song) }
                    )
                }
                item { Divider() }
            }

            item {
                Text(text = "Playlists", style = MaterialTheme.typography.titleMedium)
            }

            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistSelected(playlist.playlistId) },
                    onRename = {
                        renameTarget = playlist
                        renameDialogVisible = true
                    },
                    onDelete = { onDeletePlaylist(playlist.playlistId) }
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "Create a playlist to start organizing your music.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (createDialogVisible) {
        PlaylistNameDialog(
            title = "New playlist",
            initial = "",
            onDismiss = { createDialogVisible = false },
            onConfirm = {
                onCreatePlaylist(it)
                createDialogVisible = false
            }
        )
    }

    if (renameDialogVisible) {
        val target = renameTarget
        if (target != null) {
            PlaylistNameDialog(
                title = "Rename playlist",
                initial = target.name,
                onDismiss = {
                    renameDialogVisible = false
                    renameTarget = null
                },
                onConfirm = { name ->
                    onRenamePlaylist(target.playlistId, name)
                    renameDialogVisible = false
                    renameTarget = null
                }
            )
        }
    }
}

@Composable
private fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search YouTube") },
            trailingIcon = {
                IconButton(onClick = onSearch) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Text(text = "Search songs or composers using NewPipe.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SearchResultItem(
    song: RemoteSong,
    onPlay: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = song.title,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!song.artist.isNullOrEmpty()) {
                    Text(text = song.artist ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onPlay) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = onAdd) {
                Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "Add to playlist")
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = playlist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "Tap to manage songs", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRename) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun PlaylistNameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Playlist name") }
            )
        }
    )
}
