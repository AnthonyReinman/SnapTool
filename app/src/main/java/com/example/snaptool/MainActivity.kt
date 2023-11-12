package com.example.snaptool

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.snaptool.ui.theme.SnapToolTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import com.amazonaws.services.rekognition.model.DetectLabelsRequest
import com.amazonaws.services.rekognition.model.Image
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
//import com.example.snaptool.BuildConfig
import androidx.compose.ui.graphics.Color
import android.util.Log


class MainActivity : ComponentActivity() {
    private lateinit var rekognitionClient: AmazonRekognitionClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            "identityPoolId", // Replace with your Identity Pool ID
            Regions.US_WEST_1
        )
        rekognitionClient = AmazonRekognitionClient(credentialsProvider)

        setContent {
            SnapToolTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraFeature(rekognitionClient = rekognitionClient)
                }
            }
        }
    }
    companion object {
        const val TAG = "SnapToolApp"
    }
}

fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return ByteBuffer.wrap(byteArray)
}

fun detectLabels(rekognitionClient: AmazonRekognitionClient, imageByteBuffer: ByteBuffer, onResult: (String) -> Unit, onError: (String) -> Unit)
{
    val request = DetectLabelsRequest()
        .withImage(Image().withBytes(imageByteBuffer))
        .withMaxLabels(10)
        .withMinConfidence(75F)

    // Detection in background thread to avoid blocking the UI
    Thread(Runnable {
        try {
            val result = rekognitionClient.detectLabels(request)
            Log.d(MainActivity.TAG, "Rekognition result: ${result.labels.joinToString { it.name }}")
            val toolLabel = result.labels.firstOrNull { it.name.equals("Tool", ignoreCase = true) }
            val toolName = toolLabel?.name ?: "No tool found"
            onResult(toolName) // Call the lambda with the tool name
        } catch (e: Exception) {
            Log.e(MainActivity.TAG, "Rekognition error: ${e.message}", e)
            e.printStackTrace() // handle the exception
            onError("Error: ${e.message}") // Pass error message to the lambda
        }
    }).start()
}

@Composable
fun CameraFeature(rekognitionClient: AmazonRekognitionClient) {
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var toolDescription by remember { mutableStateOf<String?>(null) } // To store the ChatGPT description
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            Log.d(MainActivity.TAG, "Received camera result")
            if (bitmap != null) {
                Log.d(MainActivity.TAG, "Bitmap is not null")
                imageBitmap = bitmap
            } else {
                Log.d(MainActivity.TAG, "Bitmap is null")
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

        Spacer(modifier = Modifier.height(16.dp)) // Add space between the button and the image

        imageBitmap?.let { bitmap ->
            val imageByteBuffer = convertBitmapToByteBuffer(bitmap)
            detectLabels(rekognitionClient, imageByteBuffer, { toolName ->
                // Handle the successful case
                if (toolName != "No tool found") {
                    queryChatGPTApi(toolName) { description ->
                        toolDescription = description
                        errorMessage = null
                    }
                } else {
                    toolDescription = null
                    errorMessage = "No tool recognized in the image."
                }
            }, { error ->
                // Handle the error case
                errorMessage = error
                toolDescription = null
            })


            // Display the image with some padding and a fixed height
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            )
        }
        // Display tool description from ChatGPT
        toolDescription?.let { description ->
            Log.d(MainActivity.TAG, "Updating UI with tool description")
            Text(text = description)
        }
        errorMessage?.let { error ->
            Log.d(MainActivity.TAG, "Updating UI with error message")
            Text(text = error, color = Color.Red)
        }
    }
}


fun queryChatGPTApi(toolName: String, onResponse: (String) -> Unit) {
    val jsonRequestBody = """
    {
      "model": "gpt-3.5-turbo",
      "messages": [
        {
          "role": "system",
          "content": "You are a helpful assistant."
        },
        {
          "role": "user",
          "content": "What is a $toolName used for?"
        }
      ]
    }
    """.trimIndent()

    val requestBody = jsonRequestBody.toRequestBody("application/json".toMediaType())
    val apiKey = "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"
    val request = Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .post(requestBody)
        .header("Authorization", "Bearer $apiKey ")
        .build()

    // This should be executed on a background thread
    val response = OkHttpClient().newCall(request).execute()
    val responseString = response.body?.string()
    Log.d(MainActivity.TAG, "ChatGPT response: $responseString")

    // Parse the response to extract the assistant's message
    // Update this with proper JSON parsing
    val message = parseAssistantMessageFromResponse(responseString)

    onResponse(message)
}

// Dummy function to represent JSON parsing, replace with actual parsing
fun parseAssistantMessageFromResponse(responseString: String?): String {
    // You would parse the JSON and extract the assistant's message here
    // This is where you would use a JSON library like Gson or kotlinx.serialization
    return "Parsed message from the response"
}

/*@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SnapToolTheme {
        CameraFeature()
    }
}*/