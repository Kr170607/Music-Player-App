package com.example.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Picasso

class WelcomePg : AppCompatActivity() {

    private lateinit var bottomView: BottomNavigationView
    private lateinit var miniPlayer: LinearLayout
    private lateinit var miniPlayerImage: ImageView
    private lateinit var miniPlayerTitle: TextView
    private lateinit var miniPlayerArtist: TextView
    private lateinit var miniPlayerPlayPause: ImageButton
    private lateinit var miniPlayerNext: ImageButton

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Please enable notifications to control music from lock screen", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_pg)

        checkNotificationPermission()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        bottomView = findViewById(R.id.bottomNavigation)
        setupMiniPlayer()

        if (savedInstanceState == null) {
            replaceWithFragment(Home_frag())
        }

        bottomView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceWithFragment(Home_frag())
                    true
                }
                R.id.search -> {
                    replaceWithFragment(Player_frag())
                    true
                }
                R.id.profile -> {
                    replaceWithFragment(Profile_frag())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMiniPlayer() {
        miniPlayer = findViewById(R.id.miniPlayer)
        miniPlayerImage = findViewById(R.id.miniPlayerImage)
        miniPlayerTitle = findViewById(R.id.miniPlayerTitle)
        miniPlayerArtist = findViewById(R.id.miniPlayerArtist)
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause)
        miniPlayerNext = findViewById(R.id.miniPlayerNext)

        // Update Mini Player when song or status changes
        MusicManager.onSongChanged = {
            runOnUiThread { updateMiniPlayerUI() }
        }
        MusicManager.onPlaybackStatusChanged = {
            runOnUiThread { updateMiniPlayerUI() }
        }

        miniPlayerPlayPause.setOnClickListener {
            if (MusicManager.isPlaying) MusicManager.pause() else MusicManager.start()
            updateMiniPlayerUI()
        }

        miniPlayerNext.setOnClickListener {
            MusicManager.playNext()
        }

        miniPlayer.setOnClickListener {
            replaceWithFragment(Player_frag())
        }
    }

    fun updateMiniPlayerUI() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex)
        if (song != null) {
            miniPlayer.visibility = View.VISIBLE
            miniPlayerTitle.text = song.title
            miniPlayerArtist.text = song.user?.name
            val imageUrl = song.artwork?.`150x150` ?: song.artwork?.`480x480`
            Picasso.get().load(imageUrl).placeholder(R.drawable.img).into(miniPlayerImage)
            
            miniPlayerPlayPause.setImageResource(
                if (MusicManager.isPlaying) android.R.drawable.ic_media_pause 
                else android.R.drawable.ic_media_play
            )
        } else {
            miniPlayer.visibility = View.GONE
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun replaceWithFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.FrameLayout, fragment)
        
        if (fragment !is Home_frag) {
            fragmentTransaction.addToBackStack(null)
        }
        
        fragmentTransaction.commit()
        updateBottomNavigation(fragment)

        // Hide mini player if we are in the main Player Fragment
        if (fragment is Player_frag) {
            miniPlayer.visibility = View.GONE
        } else {
            updateMiniPlayerUI()
        }
    }

    fun updateBottomNavForPlayer() {
        bottomView.menu.findItem(R.id.search).isChecked = true
        miniPlayer.visibility = View.GONE
    }

    private fun updateBottomNavigation(fragment: Fragment) {
        when (fragment) {
            is Home_frag -> bottomView.menu.findItem(R.id.home).isChecked = true
            is Player_frag -> bottomView.menu.findItem(R.id.search).isChecked = true
            is Profile_frag -> bottomView.menu.findItem(R.id.profile).isChecked = true
        }
    }
}
