package com.example.snaptool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers

class ToolInfoViewModel : ViewModel() {

    private val chatGPTService = RetrofitInstance.api

    private val _toolHistory = MutableStateFlow("")
    val toolHistory: StateFlow<String> = _toolHistory

    private val _toolUsage = MutableStateFlow("")
    val toolUsage: StateFlow<String> = _toolUsage

    private val _toolMaintenance = MutableStateFlow("")
    val toolMaintenance: StateFlow<String> = _toolMaintenance

    fun fetchToolInfo(toolName: String) {
        fetchSpecificToolInfo("$toolName history", "history")
        fetchSpecificToolInfo("$toolName usage", "usage")
        fetchSpecificToolInfo("$toolName maintenance", "maintenance")
    }

    fun fetchSpecificToolInfo(toolName: String, infoType: String) {
        viewModelScope.launch {
            try {
                val query = when (infoType) {
                    "history" -> "Tell me the history of the $toolName. Act as a funny history professor that loves talking about the history of tool. No more than 2 sentences "
                    "usage" -> "How do you use a $toolName? Act as a professional handyman and explain in no more than 2 sentences."
                    "maintenance" -> "How do you maintain a $toolName? Act as a professional handyman and explain in no more than 2 sentences."
                    else -> "Tell me about the $toolName."
                }

                val request = ToolInfoRequest(
                    model = "gpt-3.5-turbo",
                    messages = listOf(
                        Message(role = "system", content = "You are a helpful assistant."),
                        Message(role = "user", content = query)
                    ),
                    max_tokens = 500,
                    temperature = 0.5

                )
                val response = chatGPTService.createCompletion(request)
                if (response.isSuccessful && response.body() != null) {
                    val text = response.body()!!.choices.firstOrNull()?.text ?: "Information not available."
                    when (infoType) {
                        "history" -> _toolHistory.value = text
                        "usage" -> _toolUsage.value = text
                        "maintenance" -> _toolMaintenance.value = text
                    }
                } else {
                    updateFailureState(infoType)
                }
            } catch (e: Exception) {
                updateFailureState(infoType)
            }
        }
    }

    private fun updateFailureState(infoType: String) {
        val errorMessage = "Failed to fetch information."
        when (infoType) {
            "history" -> _toolHistory.value = errorMessage
            "usage" -> _toolUsage.value = errorMessage
            "maintenance" -> _toolMaintenance.value = errorMessage
        }
    }
}