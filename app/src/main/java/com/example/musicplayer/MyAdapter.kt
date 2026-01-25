package com.example.musicplayer

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class MyAdapter(val context: Activity, var dataList: List<Data>, private val onItemClick: (Data, ImageView) -> Unit) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    private val sharedPreferences = context.getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE)
    private val userId = sharedPreferences.getString("userId", "") ?: ""
    private val database = FirebaseDatabase.getInstance().getReference("Users")

    fun setFilteredList(filteredList: List<Data>) {
        this.dataList = filteredList
        notifyDataSetChanged()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.songImage)
        val title: TextView = itemView.findViewById(R.id.songTitle)
        val singer: TextView = itemView.findViewById(R.id.singerName)
        val time: TextView = itemView.findViewById(R.id.songTime)
        val btnLike: ImageButton = itemView.findViewById(R.id.btnLike)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.each_song, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = dataList[position]
        val songId = currentItem.id.toString()

        holder.title.text = currentItem.title ?: "Unknown Title"
        holder.singer.text = currentItem.user?.name ?: "Unknown Artist"
        
        val duration = currentItem.duration ?: 0
        val minutes = duration / 60
        val seconds = duration % 60
        holder.time.text = String.format("%02d:%02d", minutes, seconds)

        val imageUrl = currentItem.artwork?.`150x150`
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.img).into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.img)
        }

        // Check if song is liked
        if (userId.isNotEmpty()) {
            database.child(userId).child("LikedSongs").child(songId).get().addOnSuccessListener {
                if (it.exists()) {
                    holder.btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                    holder.btnLike.setColorFilter(Color.parseColor("#FFD700")) // Explicitly set Gold/Yellow
                } else {
                    holder.btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                    holder.btnLike.setColorFilter(Color.WHITE) // White when not liked
                }
            }
        }

        holder.btnLike.setOnClickListener {
            if (userId.isEmpty()) return@setOnClickListener

            database.child(userId).child("LikedSongs").child(songId).get().addOnSuccessListener {
                if (it.exists()) {
                    // Unlike
                    database.child(userId).child("LikedSongs").child(songId).removeValue()
                    holder.btnLike.setImageResource(android.R.drawable.btn_star_big_off)
                    holder.btnLike.setColorFilter(Color.WHITE)
                } else {
                    // Like
                    database.child(userId).child("LikedSongs").child(songId).setValue(currentItem)
                    holder.btnLike.setImageResource(android.R.drawable.btn_star_big_on)
                    holder.btnLike.setColorFilter(Color.parseColor("#FFD700")) // Explicitly set Gold/Yellow
                }
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(currentItem, holder.image)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}
