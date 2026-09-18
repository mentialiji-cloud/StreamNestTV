package com.shqiptv.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: ChannelAdapter
    private var allChannels: List<Channel> = emptyList()
    private var selectedCategory = "All"
    private var selectedChannel: Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SourceStore.get(this) == null) {
            startActivity(Intent(this, SourceActivity::class.java))
            finish(); return
        }
        setContentView(R.layout.activity_main)

        val grid = findViewById<RecyclerView>(R.id.channelGrid)
        adapter = ChannelAdapter(::showDetails, ::openPlayer)
        grid.layoutManager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
        grid.adapter = adapter
        grid.setHasFixedSize(true)
        grid.itemAnimator = null
        grid.setItemViewCacheSize(24)
        grid.recycledViewPool.setMaxRecycledViews(0, 40)

        findViewById<Button>(R.id.fullscreenButton).setOnClickListener { selectedChannel?.let(::openPlayer) }
        findViewById<Button>(R.id.navSettings).setOnClickListener { startActivity(Intent(this, SourceActivity::class.java)) }
        findViewById<Button>(R.id.navLive).setOnClickListener { selectedCategory = "All"; applyFilter() }
        findViewById<Button>(R.id.navMovies).setOnClickListener { toast("Movies view is ready for the next build") }
        findViewById<Button>(R.id.navSeries).setOnClickListener { toast("Series view is ready for the next build") }
        findViewById<Button>(R.id.navFavorites).setOnClickListener { toast("Favorites will be added in the next build") }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilter()
            override fun afterTextChanged(s: Editable?) {}
        })

        loadChannels()
    }

    private fun loadChannels() {
        findViewById<TextView>(R.id.listTitle).text = "Loading channels…"
        lifecycleScope.launch {
            try {
                val source = SourceStore.get(this@MainActivity) ?: return@launch
                allChannels = ChannelRepository.load(this@MainActivity, source)
                buildCategories()
                applyFilter()
                findViewById<RecyclerView>(R.id.channelGrid).post {
                    findViewById<RecyclerView>(R.id.channelGrid).findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }
            } catch (e: Exception) {
                findViewById<TextView>(R.id.listTitle).text = e.message ?: "Could not load channels"
            }
        }
    }

    private fun buildCategories() {
        val row = findViewById<LinearLayout>(R.id.categoryRow)
        row.removeAllViews()
        val groups = listOf("All") + allChannels.map { it.group }.filter { it.isNotBlank() }.distinct().take(40)
        for (group in groups) {
            val b = Button(this).apply {
                text = group
                isAllCaps = false
                isFocusable = true
                textSize = 15f
                setTextColor(getColor(R.color.text))
                setBackgroundResource(R.drawable.bg_nav)
                minWidth = 110
                setPadding(22, 0, 22, 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 48.dp()).apply { marginEnd = 8.dp() }
                setOnClickListener { selectedCategory = group; applyFilter() }
            }
            row.addView(b)
        }
    }

    private fun applyFilter() {
        if (!::adapter.isInitialized) return
        val q = findViewById<EditText>(R.id.searchInput).text?.toString()?.trim()?.lowercase().orEmpty()
        val filtered = allChannels.asSequence()
            .filter { selectedCategory == "All" || it.group == selectedCategory }
            .filter { q.isBlank() || it.name.lowercase().contains(q) || it.group.lowercase().contains(q) }
            .toList()
        adapter.submit(filtered)
        findViewById<TextView>(R.id.listTitle).text = "${if (selectedCategory == "All") "All Channels" else selectedCategory}  •  ${filtered.size}"
    }

    private fun showDetails(channel: Channel) {
        selectedChannel = channel
        findViewById<TextView>(R.id.detailName).text = channel.name
        findViewById<TextView>(R.id.detailCategory).text = channel.group
        findViewById<TextView>(R.id.detailNow).text = "Live now"
        val image = findViewById<ImageView>(R.id.detailImage)
        val fallback = findViewById<TextView>(R.id.detailFallback)
        if (channel.logoUrl.isNotBlank()) {
            fallback.visibility = View.GONE
            image.visibility = View.VISIBLE
            image.load(channel.logoUrl) { crossfade(false) }
        } else {
            image.setImageDrawable(null)
            image.visibility = View.GONE
            fallback.visibility = View.VISIBLE
        }
    }

    private fun openPlayer(channel: Channel) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_NAME, channel.name)
            putExtra(PlayerActivity.EXTRA_URL, channel.streamUrl)
        })
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
