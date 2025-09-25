package me.taplika.player.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.taplika.player.data.LibraryRepository
import me.taplika.player.data.MusicDatabase
import me.taplika.player.data.PlaylistEntity
import me.taplika.player.data.SongEntity
import me.taplika.player.data.SongSourceType
import me.taplika.player.data.SongWithPosition
import me.taplika.player.playback.MusicServiceConnection
import me.taplika.player.playback.PlayableMedia
import me.taplika.player.playback.RepeatMode
import me.taplika.player.search.RemoteSongRepository
import me.taplika.player.search.RemoteSongRepository.RemoteSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MusicDatabase.getInstance(application)
    private val libraryRepository = LibraryRepository(application, database.playlistDao())
    private val remoteRepository = RemoteSongRepository(application)
    private val musicConnection = MusicServiceConnection(application)

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = libraryRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlistSongs: StateFlow<List<SongWithPosition>> = selectedPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else libraryRepository.playlistSongs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchResults = MutableStateFlow<List<RemoteSong>>(emptyList())
    val searchResults: StateFlow<List<RemoteSong>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NORMAL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    val playerState = musicConnection.playerState

    init {
        musicConnection.bind()
    }

    override fun onCleared() {
        super.onCleared()
        musicConnection.unbind()
    }

    fun selectPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = libraryRepository.createPlaylist(name)
            onCreated(id)
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch { libraryRepository.renamePlaylist(playlistId, name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { libraryRepository.deletePlaylist(playlistId) }
    }

    fun addLocalSongsToPlaylist(uris: List<Uri>, playlistId: Long) {
        viewModelScope.launch { libraryRepository.addLocalSongsToPlaylist(uris, playlistId) }
    }

    fun addRemoteSongToPlaylist(song: RemoteSong, playlistId: Long) {
        viewModelScope.launch { libraryRepository.addSongToPlaylist(song.toSongEntity(), playlistId) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { libraryRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun moveSongToPlaylist(song: SongWithPosition, targetPlaylistId: Long, fromPlaylistId: Long) {
        viewModelScope.launch {
            val entity = song.toSongEntity()
            libraryRepository.addSongToPlaylist(entity, targetPlaylistId)
            libraryRepository.removeSongFromPlaylist(fromPlaylistId, song.songId)
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            try {
                val results = remoteRepository.search(query)
                _searchResults.value = results
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playRemoteSong(song: RemoteSong) {
        viewModelScope.launch {
            val resolution = remoteRepository.resolve(song) ?: return@launch
            musicConnection.playQueue(
                listOf(
                    PlayableMedia(
                        uri = resolution.streamUrl,
                        title = song.title,
                        artist = song.artist,
                        artworkUri = song.thumbnailUrl
                    )
                ),
                startIndex = 0,
                repeatMode = repeatMode.value
            )
        }
    }

    fun playSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val songs = libraryRepository.playlistSongs(playlistId).first()
            if (songs.isEmpty()) return@launch
            val playablePairs = withContext(Dispatchers.IO) {
                songs.mapNotNull { song ->
                    val playable = when (song.sourceType) {
                        SongSourceType.LOCAL -> PlayableMedia(
                            uri = song.sourceId,
                            title = song.title,
                            artist = song.artist,
                            artworkUri = song.artworkUri
                        )
                        SongSourceType.YOUTUBE -> {
                            val resolution = remoteRepository.resolveYoutubeId(song.sourceId)
                            resolution?.let {
                                PlayableMedia(
                                    uri = it.streamUrl,
                                    title = song.title,
                                    artist = song.artist,
                                    artworkUri = song.artworkUri
                                )
                            }
                        }
                    }
                    playable?.let { song.songId to it }
                }
            }
            if (playablePairs.isNotEmpty()) {
                val startIndex = playablePairs.indexOfFirst { it.first == songId }.coerceAtLeast(0)
                musicConnection.playQueue(
                    playablePairs.map { it.second },
                    startIndex = startIndex.coerceAtLeast(0),
                    repeatMode = repeatMode.value
                )
            }
        }
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            RepeatMode.NORMAL -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.NORMAL
        }
        _repeatMode.value = next
        musicConnection.setRepeatMode(next)
    }

    fun pause() = musicConnection.pause()
    fun play() = musicConnection.play()
    fun next() = musicConnection.next()
    fun previous() = musicConnection.previous()

    private fun SongWithPosition.toSongEntity(): SongEntity = SongEntity(
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        sourceType = sourceType,
        sourceId = sourceId,
        artworkUri = artworkUri
    )
}
