package com.example.snaptool

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ChatGPTService by lazy {
        retrofit.create(ChatGPTService::class.java)
    }
}