package com.example.snaptool.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.MaterialTheme

@Composable
fun HowToPage(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "How to Use the Tool", style = MaterialTheme.typography.h5)

        Button(onClick = onNavigateBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun ToolHistoryPage(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "History of the Tool", style = MaterialTheme.typography.h5)

        Button(onClick = onNavigateBack) {
            Text("Back to Home")
        }
    }
}

@Composable
fun CleanToolPage(onNavigateBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "How to Clean the Tool", style = MaterialTheme.typography.h5)

        Button(onClick = onNavigateBack) {
            Text("Back to Home")
        }
    }
}
