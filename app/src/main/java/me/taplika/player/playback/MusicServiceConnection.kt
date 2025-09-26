package me.taplika.player.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicServiceConnection(private val context: Context) {
    private val applicationContext = context.applicationContext
    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private var controller: MusicService.PlayerController? = null
    private var isBound = false
    private var playerListener: Player.Listener? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicBinder ?: return
            controller = binder.controller
            val player = controller?.getPlayer() ?: return
            registerListener(player)
            _playerState.value = player.snapshot()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            val player = controller?.getPlayer()
            controller = null
            playerListener?.let { listener ->
                player?.removeListener(listener)
            }
            playerListener = null
            _playerState.value = PlayerUiState()
        }
    }

    fun bind() {
        if (isBound) return
        val intent = Intent(applicationContext, MusicService::class.java)
        applicationContext.startService(intent)
        isBound = applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (!isBound) return
        playerListener?.let { listener ->
            controller?.getPlayer()?.removeListener(listener)
        }
        applicationContext.unbindService(connection)
        isBound = false
        controller = null
        playerListener = null
        _playerState.value = PlayerUiState()
    }

    fun playQueue(items: List<PlayableMedia>, startIndex: Int, repeatMode: RepeatMode) {
        if (!isBound) bind()
        controller?.playQueue(items, startIndex, repeatMode)
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        if (!isBound) bind()
        controller?.setRepeatMode(repeatMode)
    }

    fun pause() {
        if (!isBound) bind()
        controller?.pause()
    }

    fun play() {
        if (!isBound) bind()
        controller?.play()
    }

    fun next() {
        if (!isBound) bind()
        controller?.next()
    }

    fun previous() {
        if (!isBound) bind()
        controller?.previous()
    }

    private fun registerListener(player: ExoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = player.snapshot()
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _playerState.value = player.snapshot()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                _playerState.value = player.snapshot()
            }
        }
        playerListener?.let { player.removeListener(it) }
        player.addListener(listener)
        playerListener = listener
    }

    private fun ExoPlayer.snapshot(): PlayerUiState = PlayerUiState(
        isConnected = true,
        isPlaying = isPlaying,
        title = mediaMetadata.title?.toString(),
        artist = mediaMetadata.artist?.toString(),
        artworkUri = mediaMetadata.artworkUri
    )

    data class PlayerUiState(
        val isConnected: Boolean = false,
        val isPlaying: Boolean = false,
        val title: String? = null,
        val artist: String? = null,
        val artworkUri: Any? = null
    )
}
