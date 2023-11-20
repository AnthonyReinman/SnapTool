package com.example.snaptool

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.DetectLabelsRequest
import com.example.snaptool.ui.theme.SnapToolTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapToolTheme {
                var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                var toolName by remember { mutableStateOf("") }
                var showResultScreen by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showResultScreen && imageBitmap != null) {
                        ResultScreen(toolName, imageBitmap!!) {
                            // When the 'Home' button is clicked in ResultScreen, reset states Might change this
                            showResultScreen = false
                        }
                    } else {
                        // Camera and Analysis UI
                        CameraFeature { capturedBitmap ->
                            imageBitmap = capturedBitmap
                            analyzeImageWithRekognition(capturedBitmap) { name ->
                                toolName = name
                                showResultScreen = true
                            }
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun ResultScreen(toolName: String, imageBitmap: Bitmap, onHomeClicked: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tool Identified: $toolName", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Image(bitmap = imageBitmap.asImageBitmap(), contentDescription = "Captured Image")
            }

            // Home button in the upper right corner
            Button(
                onClick = onHomeClicked,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 100.dp, height = 40.dp)
            ) {
                Text("Home")
            }
        }
    }

private fun analyzeImageWithRekognition(bitmap: Bitmap, onResult: (String) -> Unit) {
    //LOG
    Log.d("Rekognition", "Starting image analysis")
    val base64Image = convertToBase64(bitmap)

    //LOG
    Log.d("Rekognition", "Base64 Image length: ${base64Image.length}")
    // Setup AWS credentials
    val awsCredentials = BasicAWSCredentials("AKIASIQPUJRXEZFILVVG", "c8T78iNoyrbH4EHi7lD2cIzqw+N6dXI7SwhBCXsi")
    val rekognitionClient = AmazonRekognitionClient(awsCredentials)

    // Prepare the request
    val request = DetectLabelsRequest()
        .withImage(com.amazonaws.services.rekognition.model.Image()
            .withBytes(ByteBuffer.wrap(Base64.decode(base64Image, Base64.DEFAULT))))
        .withMaxLabels(10)
        .withMinConfidence(75F)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Send the request and handle the response
            val response = rekognitionClient.detectLabels(request)
            Log.d("Rekognition", "Response received: ${response.labels}")

            val labelName = response.labels.maxByOrNull { it.confidence }?.name ?: "Unknown"
            Log.d("Rekognition", "Most confident label: $labelName")

            // Switch to the Main dispatcher to update the UI
            withContext(Dispatchers.Main) {
                onResult(labelName)
                //LOGS
                Log.d("UIUpdate", "Tool name updated: $labelName")
            }
        } catch (e: Exception) {
            Log.e("Rekognition", "Error: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                onResult("Error: ${e.localizedMessage}")
                //LOGS
                Log.e("UIUpdate", "Error updating tool name: ${e.localizedMessage}")
            }
        }
    }
}

    // Send the request and handle the response
   /* val response = rekognitionClient.detectLabels(request)
    response.labels.forEach { label ->
        // Process and use label data
        println("${label.name}: ${label.confidence}")
    }
}*/

private fun convertToBase64(bitmap: Bitmap): String {
    ByteArrayOutputStream().apply {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
        return Base64.encodeToString(toByteArray(), Base64.DEFAULT)
    }
}
}

@Composable
fun CameraFeature(onImageCaptured: (Bitmap) -> Unit) {
    val context = LocalContext.current

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                Log.d("CameraFeature", "Image captured successfully")
                onImageCaptured(bitmap) // Ensure the callback is invoked here
            } else {
                Log.d("CameraFeature", "Failed to capture image")
            }
        }
    )

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take Picture")
        }

        Spacer(modifier = Modifier.height(16.dp))


    }
}

/*@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SnapToolTheme {
        CameraFeature(imageBitmap = null, onImageCaptured = {})
    }
}*/