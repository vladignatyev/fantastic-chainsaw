package me.taplika.player.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.taplika.player.playback.RepeatMode
import me.taplika.player.playback.MusicServiceConnection.PlayerUiState

@Composable
fun PlayerBar(
    playerState: PlayerUiState,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    if (!playerState.isConnected) return
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = playerState.artworkUri ?: me.taplika.player.R.drawable.ic_stat_music_note,
                contentDescription = playerState.title,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop
            )
            ColumnInfo(title = playerState.title, artist = playerState.artist)
            IconButton(onClick = onPrevious) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = onPlayPause) {
                val icon = if (playerState.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow
                Icon(imageVector = icon, contentDescription = "Play or pause")
            }
            IconButton(onClick = onNext) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = onToggleRepeat) {
                val icon = if (repeatMode == RepeatMode.REPEAT_ALL) Icons.Outlined.RepeatOn else Icons.Outlined.Repeat
                Icon(imageVector = icon, contentDescription = "Toggle repeat")
            }
        }
    }
}

@Composable
private fun ColumnInfo(title: String?, artist: String?) {
    Column(modifier = Modifier.weight(1f)) {
        Text(text = title ?: "", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        if (!artist.isNullOrBlank()) {
            Text(text = artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}
