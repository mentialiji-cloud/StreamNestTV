package com.shqiptv.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

object ChannelRepository {
    private const val CACHE_FILE = "channels_cache.json"

    suspend fun load(context: Context, source: SourceStore.Source): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val channels = when (source.type) {
                "m3u" -> loadM3u(source.m3uUrl)
                "xtream" -> loadXtream(source.server, source.username, source.password)
                else -> emptyList()
            }
            if (channels.isNotEmpty()) saveCache(context, channels)
            channels
        } catch (e: Exception) {
            val cached = loadCache(context)
            if (cached.isNotEmpty()) cached else throw e
        }
    }

    private fun getText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "ShqipTV/1.0 AndroidTV")
        return try {
            val code = conn.responseCode
            if (code !in 200..299) error("Server returned HTTP $code")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun loadM3u(playlistUrl: String): List<Channel> {
        require(playlistUrl.startsWith("http://") || playlistUrl.startsWith("https://")) {
            "M3U URL must start with http:// or https://"
        }
        val text = getText(playlistUrl)
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val out = ArrayList<Channel>()
        var name = "Channel"
        var logo = ""
        var group = "Other"
        var tvgId = ""
        val attr = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")
        for (line in lines) {
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                val attrs = attr.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                name = line.substringAfter(',', attrs["tvg-name"] ?: "Channel").trim().ifBlank { "Channel" }
                logo = attrs["tvg-logo"] ?: ""
                group = attrs["group-title"] ?: "Other"
                tvgId = attrs["tvg-id"] ?: ""
            } else if (!line.startsWith("#")) {
                val resolved = try { URI(playlistUrl).resolve(line).toString() } catch (_: Exception) { line }
                out += Channel(name, resolved, logo, group.ifBlank { "Other" }, tvgId)
                name = "Channel"; logo = ""; group = "Other"; tvgId = ""
            }
        }
        if (out.isEmpty()) error("No playable channels found in this M3U playlist")
        return out.distinctBy { it.streamUrl }
    }

    private fun loadXtream(server: String, username: String, password: String): List<Channel> {
        require(server.startsWith("http://") || server.startsWith("https://")) {
            "Server URL must start with http:// or https://"
        }
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val base = server.trimEnd('/')
        val auth = "username=${enc(username)}&password=${enc(password)}"

        val categoriesJson = getText("$base/player_api.php?$auth&action=get_live_categories")
        val categoryMap = mutableMapOf<String, String>()
        val categories = JSONArray(categoriesJson)
        for (i in 0 until categories.length()) {
            val c = categories.optJSONObject(i) ?: continue
            categoryMap[c.optString("category_id")] = c.optString("category_name", "Other")
        }

        val streamsJson = getText("$base/player_api.php?$auth&action=get_live_streams")
        val streams = JSONArray(streamsJson)
        val out = ArrayList<Channel>(streams.length())
        for (i in 0 until streams.length()) {
            val s = streams.optJSONObject(i) ?: continue
            val id = s.optString("stream_id")
            if (id.isBlank()) continue
            val name = s.optString("name", "Channel")
            val logo = s.optString("stream_icon", "")
            val group = categoryMap[s.optString("category_id")] ?: "Other"
            val streamUrl = "$base/live/${enc(username)}/${enc(password)}/$id.ts"
            out += Channel(name, streamUrl, logo, group, s.optString("epg_channel_id", ""))
        }
        if (out.isEmpty()) error("No live channels returned by the Xtream server")
        return out
    }

    private fun saveCache(context: Context, channels: List<Channel>) {
        val arr = JSONArray()
        channels.forEach { c ->
            arr.put(JSONObject().apply {
                put("name", c.name); put("url", c.streamUrl); put("logo", c.logoUrl)
                put("group", c.group); put("tvgId", c.tvgId)
            })
        }
        File(context.filesDir, CACHE_FILE).writeText(arr.toString())
    }

    private fun loadCache(context: Context): List<Channel> {
        val f = File(context.filesDir, CACHE_FILE)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(Channel(o.optString("name"), o.optString("url"), o.optString("logo"), o.optString("group", "Other"), o.optString("tvgId")))
                }
            }
        } catch (_: Exception) { emptyList() }
    }
}
