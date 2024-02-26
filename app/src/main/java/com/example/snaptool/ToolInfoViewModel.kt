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

    fun fetchSpecificToolInfo(query: String, infoType: String) {
        viewModelScope.launch {
            try {
                //val prompt = "Tell me about the $query of a tool. do not exceed 1 sentence"
                val request = ToolInfoRequest(
                    model = "gpt-3.5-turbo", // Ensure this model is available and correct
                   // prompt = prompt,
                    messages = listOf(
                        Message(role = "system", content = "You are a helpful assistant."),
                        Message(role = "user", content = "Tell me about the $query of a tool. do not exceed 1 sentence")
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