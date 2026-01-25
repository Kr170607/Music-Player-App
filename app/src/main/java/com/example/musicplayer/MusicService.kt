package com.example.musicplayer

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private val binder = MusicBinder()
    private val CHANNEL_ID = "music_player_channel"
    private val NOTIFICATION_ID = 1
    private var mediaSession: MediaSessionCompat? = null

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            MusicManager.start()
            updateUI()
        }

        override fun onPause() {
            MusicManager.pause()
            updateUI()
        }

        override fun onSkipToNext() {
            MusicManager.playNext()
            // When song changes via notification next button, we need to restart playback with the new URL
            val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex)
            if (song != null) {
                val audioUrl = "https://discoveryprovider.audius.co/v1/tracks/${song.id}/stream"
                MusicManager.playSong(this@MusicService, audioUrl, onPrepared = {}, onCompletion = { MusicManager.playNext() })
            }
            updateUI()
        }

        override fun onSkipToPrevious() {
            MusicManager.playPrevious()
            val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex)
            if (song != null) {
                val audioUrl = "https://discoveryprovider.audius.co/v1/tracks/${song.id}/stream"
                MusicManager.playSong(this@MusicService, audioUrl, onPrepared = {}, onCompletion = { MusicManager.playNext() })
            }
            updateUI()
        }

        override fun onSeekTo(pos: Long) {
            MusicManager.exoPlayer?.seekTo(pos)
            updateUI()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MusicPlayerSession").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "com.example.musicplayer.ACTION_PLAY" -> MusicManager.start()
            "com.example.musicplayer.ACTION_PAUSE" -> MusicManager.pause()
            "com.example.musicplayer.ACTION_NEXT" -> {
                MusicManager.playNext()
                restartPlaybackForCurrentSong()
            }
            "com.example.musicplayer.ACTION_PREVIOUS" -> {
                MusicManager.playPrevious()
                restartPlaybackForCurrentSong()
            }
        }
        updateUI()
        return START_NOT_STICKY
    }

    private fun restartPlaybackForCurrentSong() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex)
        if (song != null) {
            val audioUrl = "https://discoveryprovider.audius.co/v1/tracks/${song.id}/stream"
            // We use the context from the service
            MusicManager.playSong(this, audioUrl, onPrepared = {}, onCompletion = { MusicManager.playNext() })
        }
    }

    private fun updateUI() {
        updateMediaSession()
        showNotification()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        MusicManager.stopMusic()
        stopSelf()
    }

    private fun updateMediaSession() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex) ?: return
        val player = MusicManager.exoPlayer ?: return

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.user?.name)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, MusicManager.currentBitmap)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration)
            .build()
        mediaSession?.setMetadata(metadata)

        val state = if (MusicManager.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or 
                PlaybackStateCompat.ACTION_PAUSE or 
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, player.currentPosition, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for the music player"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex) ?: return
        
        val intent = Intent(this, WelcomePg::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_music_note_2_24)
            .setContentTitle(song.title)
            .setContentText(song.user?.name)
            .setLargeIcon(MusicManager.currentBitmap)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession?.sessionToken))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(MusicManager.isPlaying)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, "Previous", getPendingIntent("com.example.musicplayer.ACTION_PREVIOUS"))
            .addAction(if (MusicManager.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, "Play/Pause", getPendingIntent(if (MusicManager.isPlaying) "com.example.musicplayer.ACTION_PAUSE" else "com.example.musicplayer.ACTION_PLAY"))
            .addAction(android.R.drawable.ic_media_next, "Next", getPendingIntent("com.example.musicplayer.ACTION_NEXT"))
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
