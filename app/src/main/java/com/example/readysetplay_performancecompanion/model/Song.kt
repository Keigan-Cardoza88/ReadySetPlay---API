package com.example.readysetplay_performancecompanion.model

data class Song(
    val id: Int,
    val artist: String,
    val featured_artist: String?,
    val song_name: String,
    val artist_page: String?,
    val feat_artist_page: String?,
    val tabs_page: String?,
    val chords_lyrics: String?
)

