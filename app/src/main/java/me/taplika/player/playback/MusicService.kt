package me.taplika.player.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import me.taplika.player.R
import me.taplika.player.ui.MainActivity
import android.net.Uri

private const val NOTIFICATION_CHANNEL_ID = "playback_channel"
private const val NOTIFICATION_ID = 1001

class MusicService : android.app.Service() {
    private val binder = MusicBinder()
    private lateinit var player: ExoPlayer
    private lateinit var notificationManager: PlayerNotificationManager
    private val playerController = PlayerController()
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        isForeground = false
                    }
                }
            })
        }
        createNotificationChannel()
        notificationManager = PlayerNotificationManager.Builder(this, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
            .setMediaDescriptionAdapter(DescriptionAdapter())
            .setNotificationListener(NotificationListener())
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .build().apply {
                setPlayer(player)
                setUseNextAction(true)
                setUsePreviousAction(true)
                setUseFastForwardAction(false)
                setUseRewindAction(false)
                setUseChronometer(true)
                setSmallIcon(R.drawable.ic_stat_music_note)
            }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.setPlayer(null)
        player.release()
    }

    inner class MusicBinder : Binder() {
        val controller: PlayerController
            get() = playerController
    }

    inner class PlayerController internal constructor() {
        fun getPlayer(): ExoPlayer = player

        fun playQueue(items: List<PlayableMedia>, startIndex: Int, repeatMode: RepeatMode) {
            val mediaItems = items.map { media ->
                MediaItem.Builder()
                    .setUri(media.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(media.title)
                            .setArtist(media.artist)
                            .setArtworkUri(media.artworkUri?.let { Uri.parse(it) })
                            .build()
                    )
                    .build()
            }
            player.setMediaItems(mediaItems, startIndex, 0L)
            player.prepare()
            player.playWhenReady = true
            player.repeatMode = when (repeatMode) {
                RepeatMode.NORMAL -> Player.REPEAT_MODE_OFF
                RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            }
        }

        fun setRepeatMode(repeatMode: RepeatMode) {
            player.repeatMode = when (repeatMode) {
                RepeatMode.NORMAL -> Player.REPEAT_MODE_OFF
                RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            }
        }

        fun pause() = player.pause()
        fun play() = player.play()
        fun next() = player.seekToNextMediaItem()
        fun previous() = player.seekToPreviousMediaItem()
    }

    private inner class NotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
            if (ongoing && !isForeground) {
                startForeground(notificationId, notification)
                isForeground = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
            if (!player.playWhenReady) {
                stopSelf()
            }
        }
    }

    private inner class DescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return player.mediaMetadata.title ?: getString(R.string.app_name)
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val intent = Intent(this@MusicService, MainActivity::class.java)
            return PendingIntent.getActivity(
                this@MusicService,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return player.mediaMetadata.artist
        }

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): android.graphics.Bitmap? {
            return null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }
}
