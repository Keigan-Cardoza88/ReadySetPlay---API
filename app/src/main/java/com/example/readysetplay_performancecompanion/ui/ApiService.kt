package com.example.readysetplay_performancecompanion.ui

import com.example.readysetplay_performancecompanion.model.SongResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Define an interface that will have the endpoints of your API
interface ApiService {
    @GET("search/")
    fun searchSongs(
        @Query("song_name") songName: String
    ): Call<SongResponse>
}
