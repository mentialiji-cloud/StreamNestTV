package com.shqiptv.app

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.playerView)
        findViewById<TextView>(R.id.channelOverlay).text = intent.getStringExtra(EXTRA_NAME) ?: "Live TV"
    }

    override fun onStart() {
        super.onStart()
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.playWhenReady = true
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerView.showController()
                }
            })
        }
    }

    override fun onStop() {
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (playerView.isControllerFullyVisible) playerView.hideController() else playerView.showController()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    companion object {
        const val EXTRA_NAME = "channel_name"
        const val EXTRA_URL = "stream_url"
    }
}
