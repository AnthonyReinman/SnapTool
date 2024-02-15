package com.example.snaptool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.viewModels

class ToolInfoActivity : ComponentActivity() {
    // Instantiate the ViewModel
    private val toolInfoViewModel by viewModels<ToolInfoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Pass the ViewModel instance
            ToolInfoScreen(viewModel = toolInfoViewModel)
        }
    }
}

@Composable
fun ToolInfoScreen(viewModel: ToolInfoViewModel) {
    // Collect state from the ViewModel
    val toolHistory by viewModel.toolHistory.collectAsState()
    val toolUsage by viewModel.toolUsage.collectAsState()
    val toolMaintenance by viewModel.toolMaintenance.collectAsState()


    Column {
        Text(text = toolHistory)
        Text(text = toolUsage)
        Text(text = toolMaintenance)
    }
}