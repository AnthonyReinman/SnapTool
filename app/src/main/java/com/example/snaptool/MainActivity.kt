package com.example.snaptool

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.viewModels

//ChatGPT
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var cameraLauncher: ActivityResultLauncher<Void?>? = null
    private lateinit var imageBitmap: Bitmap
    private var toolName: String = ""
    private val toolInfoViewModel: ToolInfoViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val toolInfoViewModel: ToolInfoViewModel by viewModels()



        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("MainActivity", "Permission result: $isGranted")
            if (isGranted) {
                Log.d("MainActivity", "Permission granted, launching camera.")
                cameraLauncher?.launch(null)
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                Log.d("MainActivity", "Image captured successfully")
                //imageBitmap = bitmap
                analyzeImageWithRekognition(bitmap)
            } else {
                Log.d("MainActivity", "Failed to capture image")
            }
        }

        setContent {
            SnapToolTheme {
                ToolQueryButtons(viewModel = toolInfoViewModel)
                //TriggerChatGPTButton(viewModel = toolInfoViewModel)
                // App's UI content
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    var toolName by remember { mutableStateOf("") }
                    var showResultScreen by remember { mutableStateOf(false) }

                    if (showResultScreen && imageBitmap != null) {
                        ResultScreen(toolName = toolName, imageBitmap = imageBitmap!!, viewModel = toolInfoViewModel) {
                            showResultScreen = false
                            imageBitmap = null
                            toolName = ""
                        }
                    } else {
                        HomeScreen {
                            if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher?.launch(null)
                            } else {
                                ActivityCompat.requestPermissions(
                                    this@MainActivity,
                                    arrayOf(Manifest.permission.CAMERA),
                                    REQUEST_CAMERA_PERMISSION
                                )
                            }
                        }
                    }
                }
            }





        }
    }

    @Composable
    fun ToolQueryButtons(viewModel: ToolInfoViewModel) {
        Column {
            Button(onClick = { viewModel.fetchSpecificToolInfo("hammer", "history") }) {
                Text("History")
            }
            Button(onClick = { viewModel.fetchSpecificToolInfo("hammer", "usage") }) {
                Text("Usage")
            }
            Button(onClick = { viewModel.fetchSpecificToolInfo("hammer", "maintenance") }) {
                Text("Maintenance")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        SnapToolTheme {
            ToolQueryButtons(viewModel = toolInfoViewModel)
        }
    }

    @Composable
    fun TriggerChatGPTButton(viewModel: ToolInfoViewModel) {
        Button(onClick = { viewModel.fetchSpecificToolInfo("hammer", "history") }) {
            Text("Ask ChatGPT")
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                cameraLauncher?.launch(null)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    @Composable
    fun ResultScreen(toolName: String, imageBitmap: Bitmap,viewModel: ToolInfoViewModel, onHomeClicked: () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF5CB9FF)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tool Identified: $toolName", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                                modifier = Modifier
                                .fillMaxWidth() // Fill the width of the parent
                            .height(300.dp) // Specify the height you want
                    )
                    Button(onClick = { viewModel.fetchSpecificToolInfo(toolName, "usage") }) {
                        Text("HowToUseIt")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchSpecificToolInfo(toolName, "history") }) {
                        Text("HistoryOfIt")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchSpecificToolInfo(toolName, "maintenance") }) {
                        Text("HowToCleanIt")
                    }

                    TriggerChatGPTButton(viewModel = viewModel)
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
    }

    @Composable
    fun HomeScreen(onTakePicture: () -> Unit) {
        val image = painterResource(id = R.drawable.logo)
        val backgroundColor = Color(0xFF5CB9FF)
        Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = image,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                Spacer(modifier = Modifier.height(32.dp)) //move the button up
                Button(onClick = onTakePicture) {
                    Text("Take Picture")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun ToolInfoScreen(viewModel: ToolInfoViewModel, onHomeClick: () -> Unit) {
        val toolHistory by viewModel.toolHistory.collectAsState()
        val toolUsage by viewModel.toolUsage.collectAsState()
        val toolMaintenance by viewModel.toolMaintenance.collectAsState()

        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF5CB9FF)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tool History: $toolHistory",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = "Tool Usage: $toolUsage",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = "Tool Maintenance: $toolMaintenance",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(8.dp)
                )
                Button(
                    onClick = onHomeClick,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Return to Home")
                }

            }
        }
    }


private fun analyzeImageWithRekognition(bitmap: Bitmap) {
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
                toolName = labelName
                imageBitmap = bitmap
                setContent {
                    SnapToolTheme {
                        ResultScreen(toolName = toolName, imageBitmap = imageBitmap!!, viewModel = toolInfoViewModel) {

                        }
                    }
                }

                Log.d("UIUpdate", "Tool name updated: $labelName")
            }
        } catch (e: Exception) {
            Log.e("Rekognition", "Error: ${e.localizedMessage}")
            withContext(Dispatchers.Main) {
                //onResult("Error: ${e.localizedMessage}")
                //LOGS
                Log.e("UIUpdate", "Error updating tool name: ${e.localizedMessage}")
            }
        }
    }
}

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
