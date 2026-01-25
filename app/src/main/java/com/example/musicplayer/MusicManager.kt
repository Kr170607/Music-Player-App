package com.example.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

object MusicManager {
    var exoPlayer: ExoPlayer? = null
    var songList: List<Data>? = null
    var currentIndex: Int = 0
    var currentBitmap: Bitmap? = null

    var onSongChanged: (() -> Unit)? = null
    var onPlaybackStatusChanged: ((Boolean) -> Unit)? = null

    @OptIn(UnstableApi::class)
    fun playSong(context: Context, url: String, onPrepared: (ExoPlayer) -> Unit, onCompletion: () -> Unit) {
        if (exoPlayer == null) {
            // HIGH-SPEED OPTIMIZATION: Start playing with almost ZERO buffer
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2500,  // Min buffer (was 15000)
                    10000, // Max buffer (was 50000)
                    250,   // Buffer for playback (Start playing after 0.25s)
                    500    // Buffer for playback after re-buffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()

            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        onPrepared(exoPlayer!!)
                    } else if (state == Player.STATE_ENDED) {
                        onCompletion()
                    }
                }
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlaybackStatusChanged?.invoke(isPlaying)
                    updateNotification(context)
                }
            })
        }

        // Apply media item and start IMMEDIATELY
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.playWhenReady = true 
        
        updateNotification(context)

        // Load bitmap for notification (Background task)
        val song = songList?.getOrNull(currentIndex)
        val imageUrl = song?.artwork?.`150x150` ?: song?.artwork?.`480x480`
        if (imageUrl != null) {
            Picasso.get().load(imageUrl).into(object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    currentBitmap = bitmap
                    updateNotification(context)
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
        }
    }

    fun start() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stopMusic() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        onPlaybackStatusChanged?.invoke(false)
    }

    fun playNext() {
        songList?.let {
            currentIndex = (currentIndex + 1) % it.size
            onSongChanged?.invoke()
        }
    }

    fun playPrevious() {
        songList?.let {
            currentIndex = if (currentIndex > 0) currentIndex - 1 else it.size - 1
            onSongChanged?.invoke()
        }
    }

    fun updateNotification(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = "SHOW_NOTIFICATION"
        }
        ContextCompat.startForegroundService(context, intent)
    }

    val isPlaying: Boolean
        get() = exoPlayer?.isPlaying ?: false
}
