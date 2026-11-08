package dev.jetphotos.android

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class JetApi {
    private val client = OkHttpClient.Builder()
        .callTimeout(35, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun search(rawQuery: String, photoCount: Int = 12): List<JetPhoto> =
        withContext(Dispatchers.IO) {
            val query = rawQuery.trim()
            require(query.isNotEmpty()) { "Enter an aircraft registration or aircraft code." }
            require(!query.any(Char::isWhitespace)) {
                "Use a registration or a single aircraft code without spaces, like N787BK or A350-1000."
            }
            val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
            val url = "https://www.jetapi.dev/api?reg=$encoded&photos=${photoCount.coerceIn(1, 20)}&only_jp=true"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    400 -> error("JetAPI rejected that search. Use a registration or a single aircraft code without spaces.")
                    404 -> error("No photos were returned for that search. Check the registration or aircraft code.")
                    in 200..299 -> {
                        val body = response.body?.string().orEmpty()
                        val decoded = runCatching { gson.fromJson(body, JetApiResponse::class.java) }
                            .getOrNull() ?: error("JetAPI returned photo data the app couldn't read.")
                        decoded.images
                    }
                    else -> error("JetAPI returned error (${response.code}). Try again in a bit.")
                }
            }
        }
}
