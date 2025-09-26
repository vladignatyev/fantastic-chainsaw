package me.taplika.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.taplika.player.data.PlaylistEntity

@Composable
fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String, (Long) -> Unit) -> Unit,
    onPlaylistSelected: (Long) -> Unit
) {
    val (newPlaylistName, setNewPlaylistName) = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Select playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose an existing playlist or create a new one.")
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlaylistSelected(playlist.playlistId)
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = setNewPlaylistName,
                    label = { Text("New playlist name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val name = newPlaylistName.trim()
                        if (name.isNotEmpty()) {
                            onCreatePlaylist(name) { id ->
                                onPlaylistSelected(id)
                            }
                            setNewPlaylistName("")
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create and add")
                }
            }
        }
    )
}
