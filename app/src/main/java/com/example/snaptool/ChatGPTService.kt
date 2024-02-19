package com.example.snaptool

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface ChatGPTService {
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun createCompletion(
        @Body toolInfoRequest: ToolInfoRequest
    ): Response<ToolInfoResponse>
}

data class ToolInfoRequest(
    val model: String = "text-davinci-003",
    val prompt: String,
    val max_tokens: Int,
    val temperature: Double
)

data class ToolInfoResponse(
    val choices: List<Choice>
)

data class Choice(
    val text: String
)
