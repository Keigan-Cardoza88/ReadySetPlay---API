package com.example.readysetplay_performancecompanion.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.readysetplay_performancecompanion.R
import com.example.readysetplay_performancecompanion.model.Song

class SongAdapter(private val songs: List<Song>, private val onItemClick: (Song) -> Unit) :
    RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songName: TextView = view.findViewById(R.id.songName)
        val artist: TextView = view.findViewById(R.id.artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.songName.text = song.song_name
        holder.artist.text = song.artist

        holder.itemView.setOnClickListener { onItemClick(song) } // Corrected click listener
    }

    override fun getItemCount() = songs.size
}
