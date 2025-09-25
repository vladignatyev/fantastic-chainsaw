package me.taplika.player.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Index

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
)

@Entity(
    tableName = "songs",
    indices = [Index(value = ["sourceType", "sourceId"], unique = true)]
)
@TypeConverters(SongSourceTypeConverter::class)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val songId: Long = 0,
    val title: String,
    val artist: String?,
    val album: String?,
    val duration: Long?,
    val sourceType: SongSourceType,
    val sourceId: String,
    val artworkUri: String?,
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["playlistId"]), Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

data class SongWithPosition(
    @ColumnInfo(name = "songId") val songId: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "artist") val artist: String?,
    @ColumnInfo(name = "album") val album: String?,
    @ColumnInfo(name = "duration") val duration: Long?,
    @ColumnInfo(name = "sourceType") val sourceType: SongSourceType,
    @ColumnInfo(name = "sourceId") val sourceId: String,
    @ColumnInfo(name = "artworkUri") val artworkUri: String?,
    @ColumnInfo(name = "position") val position: Int,
)

enum class SongSourceType {
    LOCAL,
    YOUTUBE
}

class SongSourceTypeConverter {
    @TypeConverter
    fun fromString(value: String?): SongSourceType? = value?.let { SongSourceType.valueOf(it) }

    @TypeConverter
    fun toString(value: SongSourceType?): String? = value?.name
}
