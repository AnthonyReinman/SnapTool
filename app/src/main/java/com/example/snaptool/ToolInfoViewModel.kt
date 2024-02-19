package com.example.snaptool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ToolInfoViewModel : ViewModel() {
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

    private fun fetchSpecificToolInfo(query: String, infoType: String) {
        viewModelScope.launch {
            try {
                val prompt = "Tell me about the $query of a tool."
                val request = ToolInfoRequest(
                    model = "text-davinci-003", // Adjust model as necessary
                    prompt = prompt,
                    max_tokens = 150,
                    temperature = 0.5
                )
                val response = RetrofitInstance.api.createCompletion(request)
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