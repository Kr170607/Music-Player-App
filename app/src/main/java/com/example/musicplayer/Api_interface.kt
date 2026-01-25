package com.example.musicplayer

import retrofit2.Call
import retrofit2.http.GET

interface Api_interface {
    @GET("tracks/trending?limit=100")
    fun getProductData() : Call<AudiusResponse>
}