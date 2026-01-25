package com.example.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class MusicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context!!, serviceIntent)
    }
}
