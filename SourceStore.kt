package com.shqiptv.app

import android.content.Context

object SourceStore {
    private const val PREFS = "shqiptv_source"
    private const val TYPE = "type"
    private const val M3U = "m3u"
    private const val SERVER = "server"
    private const val USER = "user"
    private const val PASS = "pass"

    data class Source(
        val type: String,
        val m3uUrl: String = "",
        val server: String = "",
        val username: String = "",
        val password: String = ""
    )

    fun get(context: Context): Source? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val type = p.getString(TYPE, null) ?: return null
        return Source(
            type = type,
            m3uUrl = p.getString(M3U, "") ?: "",
            server = p.getString(SERVER, "") ?: "",
            username = p.getString(USER, "") ?: "",
            password = p.getString(PASS, "") ?: ""
        )
    }

    fun saveM3u(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TYPE, "m3u")
            .putString(M3U, url.trim())
            .remove(SERVER).remove(USER).remove(PASS)
            .apply()
    }

    fun saveXtream(context: Context, server: String, username: String, password: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(TYPE, "xtream")
            .putString(SERVER, server.trim().trimEnd('/'))
            .putString(USER, username.trim())
            .putString(PASS, password)
            .remove(M3U)
            .apply()
    }
}
