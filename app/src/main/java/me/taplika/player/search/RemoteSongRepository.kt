package me.taplika.player.search

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.taplika.player.data.SongEntity
import me.taplika.player.data.SongSourceType
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.IOException

class RemoteSongRepository(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    init {
        NewPipeInitializer.init(context)
    }

    suspend fun search(query: String): List<RemoteSong> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val service = ServiceList.YouTube
            val extractor = service.getSearchExtractor(query)
            val info = SearchInfo.getInfo(extractor)
            info.relatedItems.filterIsInstance<StreamInfoItem>()
                .filter { item ->
                    item.streamType == StreamType.AUDIO_STREAM ||
                        item.streamType == StreamType.AUDIO_LIVE_STREAM ||
                        item.streamType == StreamType.VIDEO_STREAM
                }
                .mapNotNull { item ->
                    val videoId = try {
                        service.streamLHFactory.fromUrl(item.url).id
                    } catch (e: ParsingException) {
                        null
                    }

                    videoId?.let {
                        RemoteSong(
                            title = item.name,
                            artist = item.uploaderName,
                            duration = item.duration.takeIf { it > 0 }?.times(1000) ?: 0L,
                            url = item.url,
                            videoId = it,
                            thumbnailUrl = item.thumbnails.firstOrNull()?.url
                        )
                    }
                }
        } catch (e: ExtractionException) {
            emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    suspend fun resolve(song: RemoteSong): StreamResolution? = resolveUrl(song.url)

    suspend fun resolveYoutubeId(videoId: String): StreamResolution? {
        val url = "https://www.youtube.com/watch?v=$videoId"
        return resolveUrl(url)
    }

    private suspend fun resolveUrl(url: String): StreamResolution? = withContext(ioDispatcher) {
        try {
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            val audioStream = selectBestAudio(info.audioStreams)
            audioStream
                ?.takeIf { it.isUrl }
                ?.let {
                    val mimeType = it.format?.mimeType ?: MediaFormat.getMimeById(it.formatId)
                    StreamResolution(
                        streamUrl = it.content,
                        mimeType = mimeType
                    )
                }
        } catch (e: ExtractionException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    private fun selectBestAudio(streams: List<AudioStream>): AudioStream? {
        return streams.maxByOrNull { it.averageBitrate ?: 0 }
    }

    data class RemoteSong(
        val title: String,
        val artist: String?,
        val duration: Long,
        val url: String,
        val videoId: String,
        val thumbnailUrl: String?
    ) {
        fun toSongEntity(): SongEntity = SongEntity(
            title = title,
            artist = artist,
            album = null,
            duration = duration.takeIf { it > 0 },
            sourceType = SongSourceType.YOUTUBE,
            sourceId = videoId,
            artworkUri = thumbnailUrl
        )
    }

    data class StreamResolution(
        val streamUrl: String,
        val mimeType: String?
    )
}
