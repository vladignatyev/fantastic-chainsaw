package me.taplika.player.search

import android.content.Context
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.net.CookieManager
import java.net.CookiePolicy

object NewPipeInitializer {
    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val cookieManager = CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }
            val downloader = OkHttpNewPipeDownloader(cookieManager)
            val localization = Localization("en", "US")
            val contentCountry = localization.countryCode
                .takeIf { it.isNotBlank() }
                ?.let { ContentCountry(it) }
                ?: ContentCountry.DEFAULT
            NewPipe.init(downloader, localization, contentCountry)
            initialized = true
        }
    }
}
