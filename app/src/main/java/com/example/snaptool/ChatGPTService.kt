package com.example.snaptool

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface ChatGPTService {
    @Headers("Authorization: Bearer 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")
    @POST("/v1/completions")
    suspend fun createCompletion(@Body request: ToolInfoRequest): Response<ToolInfoResponse>
}

data class ToolInfoRequest(
    val model: String,
    val prompt: String,
    val max_tokens: Int,
    val temperature: Double
)

data class ToolInfoResponse(val choices: List<Choice>)
data class Choice(val text: String)

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ChatGPTService by lazy {
        retrofit.create(ChatGPTService::class.java)
    }
}
