package me.taplika.player.playback

data class PlayableMedia(
    val uri: String,
    val title: String,
    val artist: String?,
    val artworkUri: String?
)

enum class RepeatMode {
    NORMAL,
    REPEAT_ALL
}
