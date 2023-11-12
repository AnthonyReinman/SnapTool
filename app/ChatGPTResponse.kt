import com.google.gson.annotations.SerializedName

data class ChatGPTResponse(
    val id: String,
    val object: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>
) {
    data class Choice(
        val index: Int,
        val message: Message,
        val finish_reason: String
    ) {
        data class Message(
            val role: String,
            val content: String
        )
    }
}
