package com.example.translatorsundanese

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TranslateApi {
    @GET("translate")
    suspend fun translate(
        @Query("engine") engine: String = "google",
        @Query("text") text: String,
        @Query("to") to: String
    ): Response<TranslateResponse>  // pakai model dari TranslateModels.kt
}