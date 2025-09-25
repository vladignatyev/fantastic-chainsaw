package me.taplika.player.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query(
        "SELECT songs.songId, songs.title, songs.artist, songs.album, songs.duration, songs.sourceType, songs.sourceId, songs.artworkUri, playlist_songs.position " +
            "FROM songs INNER JOIN playlist_songs ON songs.songId = playlist_songs.songId " +
            "WHERE playlist_songs.playlistId = :playlistId ORDER BY playlist_songs.position"
    )
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongWithPosition>>

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getLastPosition(playlistId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("SELECT * FROM songs WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun findSongBySource(sourceType: SongSourceType, sourceId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun linkSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun unlinkSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_songs SET position = :position WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun addSongToPlaylist(song: SongEntity, playlistId: Long): Long {
        val existing = findSongBySource(song.sourceType, song.sourceId)
        val songId = existing?.songId ?: insertSong(song)
        val currentPosition = getLastPosition(playlistId) ?: -1
        linkSongToPlaylist(PlaylistSongCrossRef(playlistId, songId, currentPosition + 1))
        return songId
    }

    @Query("SELECT songs.* FROM songs INNER JOIN playlist_songs ON songs.songId = playlist_songs.songId WHERE playlist_songs.playlistId = :playlistId")
    suspend fun getSongsForPlaylistOnce(playlistId: Long): List<SongEntity>
}
