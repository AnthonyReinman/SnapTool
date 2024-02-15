package com.example.snaptool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ToolInfoViewModel : ViewModel() {
    private val _toolHistory = MutableStateFlow("")
    val toolHistory: StateFlow<String> = _toolHistory

    private val _toolUsage = MutableStateFlow("")
    val toolUsage: StateFlow<String> = _toolUsage

    private val _toolMaintenance = MutableStateFlow("")
    val toolMaintenance: StateFlow<String> = _toolMaintenance

    fun handleFetchFailure() {
        _toolHistory.value = "Fetch failed"
        _toolUsage.value = "Could not retrieve usage information."
        _toolMaintenance.value = "Could not retrieve maintenance information."
    }


    fun fetchToolInfo(toolName: String) {
        viewModelScope.launch {
            try {
                // Construct the prompt
                val prompt = "Explain how to use a $toolName."
                val request = ToolInfoRequest(
                    model = "text-davinci-003",
                    prompt = prompt,
                    max_tokens = 100,
                    temperature = 0.5
                )
                val response = RetrofitInstance.api.createCompletion(request)
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    _toolHistory.value = responseBody.choices.firstOrNull()?.text ?: "No information available"
                } else {
                    // Handle API error
                    _toolHistory.value = "Failed to fetch information"
                }
            } catch (e: HttpException) {
                // Handle HTTP error
                _toolHistory.value = "Error: ${e.message}"
            } catch (e: Exception) {
                // Handle other errors
                _toolHistory.value = "Error: ${e.message}"
            }
        }
    }
}