package me.taplika.player.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LibraryRepository(
    private val context: Context,
    private val dao: PlaylistDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val playlists = dao.getPlaylists()

    fun playlistSongs(playlistId: Long) = dao.getSongsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = withContext(ioDispatcher) {
        dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) = withContext(ioDispatcher) {
        dao.updatePlaylist(PlaylistEntity(playlistId = playlistId, name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(ioDispatcher) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addLocalSongsToPlaylist(uris: List<Uri>, playlistId: Long) = withContext(ioDispatcher) {
        uris.forEach { uri ->
            val metadata = extractMetadata(uri)
            val song = SongEntity(
                title = metadata.title ?: uri.lastPathSegment.orEmpty(),
                artist = metadata.artist,
                album = metadata.album,
                duration = metadata.duration,
                sourceType = SongSourceType.LOCAL,
                sourceId = uri.toString(),
                artworkUri = metadata.artworkUri
            )
            dao.addSongToPlaylist(song, playlistId)
        }
    }

    suspend fun addSongToPlaylist(song: SongEntity, playlistId: Long) = withContext(ioDispatcher) {
        dao.addSongToPlaylist(song, playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(ioDispatcher) {
        dao.unlinkSongFromPlaylist(playlistId, songId)
    }

    suspend fun moveSongWithinPlaylist(playlistId: Long, songId: Long, newPosition: Int) = withContext(ioDispatcher) {
        val songs = dao.getSongsForPlaylistOnce(playlistId).mapIndexed { index, song ->
            song.songId to index
        }.toMutableList()
        val currentIndex = songs.indexOfFirst { it.first == songId }
        if (currentIndex == -1) return@withContext
        val item = songs.removeAt(currentIndex)
        songs.add(newPosition.coerceIn(0, songs.size), item)
        songs.forEachIndexed { index, pair ->
            dao.updateSongPosition(playlistId, pair.first, index)
        }
    }

    private suspend fun extractMetadata(uri: Uri): Metadata = withContext(ioDispatcher) {
        val retriever = MediaMetadataRetriever()
        runCatching { retriever.setDataSource(context, uri) }
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val artBytes = retriever.embeddedPicture
        val artworkUri = if (artBytes != null) {
            val file = File.createTempFile("art", ".png", context.cacheDir)
            file.outputStream().use { it.write(artBytes) }
            Uri.fromFile(file).toString()
        } else null
        retriever.release()
        Metadata(title, artist, album, duration, artworkUri)
    }

    data class Metadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val duration: Long?,
        val artworkUri: String?,
    )
}
