package me.taplika.player.playback

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import me.taplika.player.search.RemoteSongRepository

class YoutubeResolvingDataSourceFactory(context: Context) : DataSource.Factory {
    private val repository = RemoteSongRepository(context)
    private val cache = ConcurrentHashMap<String, RemoteSongRepository.StreamResolution>()
    private val baseFactory = DefaultDataSource.Factory(
        context,
        DefaultHttpDataSource.Factory()
    )

    override fun createDataSource(): DataSource {
        val upstream = baseFactory.createDataSource()
        return ResolvingDataSource(upstream, Resolver())
    }

    private inner class Resolver : ResolvingDataSource.Resolver {
        override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
            if (dataSpec.uri.scheme != YOUTUBE_SCHEME) {
                return dataSpec
            }

            val rawId = dataSpec.uri.schemeSpecificPart ?: dataSpec.uri.lastPathSegment
            val videoId = rawId?.removePrefix("//")?.takeIf { it.isNotBlank() }
                ?: throw IOException("Missing YouTube video id")

            val resolution = cache[videoId] ?: runBlocking {
                repository.resolveYoutubeId(videoId)
            }?.also { resolved ->
                cache[videoId] = resolved
            }

            val streamUrl = resolution?.streamUrl
                ?: throw IOException("Unable to resolve stream for video id: $videoId")

            val resolvedUri = Uri.parse(streamUrl)
            return dataSpec.buildUpon().setUri(resolvedUri).build()
        }
    }

    companion object {
        private const val YOUTUBE_SCHEME = "yt"

        fun buildYoutubeUri(videoId: String): String = "$YOUTUBE_SCHEME:$videoId"
    }
}
