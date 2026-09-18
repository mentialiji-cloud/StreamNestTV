package com.shqiptv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SourceActivity : AppCompatActivity() {
    private var mode = "m3u"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_source)

        val m3uFields = findViewById<LinearLayout>(R.id.m3uFields)
        val xtreamFields = findViewById<LinearLayout>(R.id.xtreamFields)
        val status = findViewById<TextView>(R.id.status)
        val loading = findViewById<ProgressBar>(R.id.loading)
        val existing = SourceStore.get(this)

        existing?.let {
            mode = it.type
            findViewById<EditText>(R.id.m3uUrl).setText(it.m3uUrl)
            findViewById<EditText>(R.id.serverUrl).setText(it.server)
            findViewById<EditText>(R.id.username).setText(it.username)
            findViewById<EditText>(R.id.password).setText(it.password)
        }

        fun updateMode() {
            m3uFields.visibility = if (mode == "m3u") View.VISIBLE else View.GONE
            xtreamFields.visibility = if (mode == "xtream") View.VISIBLE else View.GONE
        }
        updateMode()

        findViewById<Button>(R.id.modeM3u).setOnClickListener { mode = "m3u"; updateMode() }
        findViewById<Button>(R.id.modeXtream).setOnClickListener { mode = "xtream"; updateMode() }

        findViewById<Button>(R.id.saveSource).setOnClickListener {
            status.text = ""
            try {
                if (mode == "m3u") {
                    val url = findViewById<EditText>(R.id.m3uUrl).text.toString().trim()
                    require(url.isNotBlank()) { "Enter your M3U URL" }
                    SourceStore.saveM3u(this, url)
                } else {
                    val server = findViewById<EditText>(R.id.serverUrl).text.toString().trim()
                    val user = findViewById<EditText>(R.id.username).text.toString().trim()
                    val pass = findViewById<EditText>(R.id.password).text.toString()
                    require(server.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) { "Enter server, username and password" }
                    SourceStore.saveXtream(this, server, user, pass)
                }
            } catch (e: Exception) {
                status.text = e.message
                return@setOnClickListener
            }

            loading.visibility = View.VISIBLE
            status.text = "Testing source and loading channels…"
            lifecycleScope.launch {
                try {
                    val channels = ChannelRepository.load(this@SourceActivity, SourceStore.get(this@SourceActivity)!!)
                    status.text = "Loaded ${channels.size} channels"
                    startActivity(Intent(this@SourceActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    finish()
                } catch (e: Exception) {
                    loading.visibility = View.GONE
                    status.text = e.message ?: "Could not load this source"
                }
            }
        }
    }
}
