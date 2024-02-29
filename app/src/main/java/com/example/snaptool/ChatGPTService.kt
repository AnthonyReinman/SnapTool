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
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int,
    val temperature: Double
)

data class Message(
    val role: String,
    val content: String
)
data class ToolInfoResponse(
    val choices: List<Choice>
)

    data class Choice(
val message: Message
)



