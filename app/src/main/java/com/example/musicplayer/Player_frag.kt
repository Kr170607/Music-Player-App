package com.example.musicplayer

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionInflater
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.Serializable
import kotlin.math.abs

class Player_frag : Fragment() {

    private lateinit var rotateAnimation: ObjectAnimator
    private lateinit var playPauseBtn: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var ivSongImageReal: ImageView
    private lateinit var btnLike: ImageButton
    private lateinit var rootView: View
    
    private lateinit var bar1: View
    private lateinit var bar2: View
    private lateinit var bar3: View
    private lateinit var bar4: View
    private lateinit var bar5: View
    private val barAnimators = mutableListOf<ObjectAnimator>()

    private val database = FirebaseDatabase.getInstance().getReference("Users")
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = transition

        val sharedPreferences = requireContext().getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)
        userId = sharedPreferences.getString("userId", "") ?: ""
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_frag, container, false)
        rootView = view

        val newSongList = arguments?.getSerializable("songList") as? List<Data>
        val newIndex = arguments?.getInt("currentIndex") ?: 0

        if (newSongList != null) {
            MusicManager.songList = newSongList
            MusicManager.currentIndex = newIndex
        }

        tvTitle = view.findViewById(R.id.playerSongTitle)
        tvArtist = view.findViewById(R.id.playerArtistName)
        ivSongImageReal = view.findViewById(R.id.playerSongImage)
        val discContainer = view.findViewById<View>(R.id.discContainer)
        btnLike = view.findViewById(R.id.btnPlayerLike)
        
        bar1 = view.findViewById(R.id.bar1)
        bar2 = view.findViewById(R.id.bar2)
        bar3 = view.findViewById(R.id.bar3)
        bar4 = view.findViewById(R.id.bar4)
        bar5 = view.findViewById(R.id.bar5)
        
        playPauseBtn = view.findViewById(R.id.btnPlayPause)
        val btnPrevious = view.findViewById<ImageButton>(R.id.btnPrevious)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNext)
        
        seekBar = view.findViewById(R.id.seekBar)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)

        rotateAnimation = ObjectAnimator.ofFloat(discContainer, "rotation", 0f, 360f).apply {
            duration = 3000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        
        setupBarAnimations()
        setupSwipeGestures(discContainer)

        updateUI()

        MusicManager.onSongChanged = {
            activity?.runOnUiThread {
                updateUI()
                startNewSong()
            }
        }

        MusicManager.onPlaybackStatusChanged = { isPlaying ->
            activity?.runOnUiThread {
                if (isPlaying) {
                    if (rotateAnimation.isPaused) rotateAnimation.resume() else if (!rotateAnimation.isStarted) rotateAnimation.start()
                    startVisualizer()
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                    updateSeekBar()
                } else {
                    rotateAnimation.pause()
                    stopVisualizer()
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        }

        playPauseBtn.setOnClickListener {
            if (MusicManager.isPlaying) {
                MusicManager.pause()
            } else {
                MusicManager.start()
            }
        }

        btnPrevious.setOnClickListener {
            MusicManager.playPrevious()
        }

        btnNext.setOnClickListener {
            MusicManager.playNext()
        }

        btnLike.setOnClickListener {
            toggleLike()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MusicManager.exoPlayer?.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        if (newSongList != null) {
            startNewSong()
        } else {
            syncWithExistingPlayback()
        }

        return view
    }

    private fun setupBarAnimations() {
        val bars = listOf(bar1, bar2, bar3, bar4, bar5)
        val durations = listOf(400L, 700L, 500L, 800L, 600L)
        
        bars.forEachIndexed { i, bar ->
            val anim = ObjectAnimator.ofFloat(bar, "scaleY", 0.3f, 1.0f).apply {
                duration = durations[i]
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
            barAnimators.add(anim)
        }
    }

    private fun startVisualizer() {
        barAnimators.forEach { if (it.isPaused) it.resume() else if (!it.isStarted) it.start() }
    }

    private fun stopVisualizer() {
        barAnimators.forEach { it.pause() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeGestures(view: View) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            MusicManager.playPrevious()
                        } else {
                            MusicManager.playNext()
                        }
                        return true
                    }
                }
                return false
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun updateUI() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex) ?: return
        
        tvTitle.text = song.title
        tvArtist.text = song.user?.name
        val imageUrl = song.artwork?.`480x480` ?: song.artwork?.`150x150`
        
        Picasso.get().load(imageUrl).placeholder(R.drawable.img).into(ivSongImageReal, object : Callback {
            override fun onSuccess() {
                val bitmap = (ivSongImageReal.drawable as BitmapDrawable).bitmap
                applyDynamicBackground(bitmap)
            }
            override fun onError(e: Exception?) {}
        })
        
        MusicManager.exoPlayer?.let {
            tvTotalTime.text = formatTime(it.duration.toInt())
            seekBar.max = it.duration.toInt()
            seekBar.progress = it.currentPosition.toInt()
            tvCurrentTime.text = formatTime(it.currentPosition.toInt())
        }

        checkIfLiked(song.id.toString())
    }

    private fun applyDynamicBackground(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val dominantColor = palette?.getDominantColor(Color.BLACK) ?: Color.BLACK
            val darkerColor = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.7f)
            
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(dominantColor, darkerColor, Color.BLACK)
            )
            rootView.background = gradient
        }
    }

    private fun checkIfLiked(songId: String) {
        if (userId.isEmpty()) return
        database.child(userId).child("LikedSongs").child(songId).get().addOnSuccessListener {
            if (it.exists()) {
                btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                btnLike.setColorFilter(Color.parseColor("#FFD700"))
            } else {
                btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                btnLike.setColorFilter(Color.WHITE)
            }
        }
    }

    private fun toggleLike() {
        if (userId.isEmpty()) return
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex) ?: return
        val songId = song.id.toString()

        database.child(userId).child("LikedSongs").child(songId).get().addOnSuccessListener {
            if (it.exists()) {
                database.child(userId).child("LikedSongs").child(songId).removeValue()
                btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                btnLike.setColorFilter(Color.WHITE)
            } else {
                database.child(userId).child("LikedSongs").child(songId).setValue(song)
                btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                btnLike.setColorFilter(Color.parseColor("#FFD700"))
            }
        }
    }

    private fun startNewSong() {
        val song = MusicManager.songList?.getOrNull(MusicManager.currentIndex) ?: return
        val audioUrl = "https://discoveryprovider.audius.co/v1/tracks/${song.id}/stream"
        
        MusicManager.playSong(requireContext(), audioUrl, onPrepared = {
            tvTotalTime.text = formatTime(it.duration.toInt())
            seekBar.max = it.duration.toInt()
            if (!rotateAnimation.isStarted) rotateAnimation.start() else rotateAnimation.resume()
            startVisualizer()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            updateSeekBar()
        }, onCompletion = {
            MusicManager.playNext()
        })
    }

    private fun syncWithExistingPlayback() {
        if (MusicManager.isPlaying) {
            rotateAnimation.start()
            startVisualizer()
            playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
            updateSeekBar()
        } else {
            playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
            stopVisualizer()
        }
    }

    private fun updateSeekBar() {
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() {
                MusicManager.exoPlayer?.let {
                    if (it.isPlaying) {
                        seekBar.progress = it.currentPosition.toInt()
                        tvCurrentTime.text = formatTime(it.currentPosition.toInt())
                        handler.postDelayed(this, 50)
                    }
                }
            }
        })
    }

    private fun formatTime(milliseconds: Int): String {
        val secondsTotal = milliseconds / 1000
        val minutes = secondsTotal / 60
        val seconds = secondsTotal % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        barAnimators.forEach { it.cancel() }
    }
}
