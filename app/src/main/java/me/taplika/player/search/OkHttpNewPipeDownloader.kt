package me.taplika.player.search

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.JavaNetCookieJar
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.CookieManager
import java.util.concurrent.TimeUnit

class OkHttpNewPipeDownloader(cookieManager: CookieManager) : Downloader() {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cookieJar(JavaNetCookieJar(cookieManager))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())

        val headers = request.headers()
        if (headers.isNotEmpty()) {
            headers.forEach { (key, values) ->
                if (!values.isNullOrEmpty()) {
                    builder.header(key, values.joinToString(","))
                }
            }
        }

        if (!headers.containsKey("User-Agent")) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Android) ModernMP3Player/1.0"
            )
        }

        val method = request.httpMethod().uppercase()
        val body = when (method) {
            "POST", "PUT", "PATCH" -> createBody(request)
            else -> null
        }
        builder.method(method, body)

        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string()
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }

    private fun createBody(request: Request): RequestBody {
        val data = request.dataToSend()
        val contentType = request.headers()["Content-Type"]?.firstOrNull()
        val mediaType = contentType?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
        return (data ?: ByteArray(0)).toRequestBody(mediaType)
    }
}
