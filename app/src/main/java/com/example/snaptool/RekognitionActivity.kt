package com.example.snaptool

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.activity.viewModels
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.amazonaws.services.rekognition.model.DetectLabelsRequest
import com.amazonaws.services.rekognition.model.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class RekognitionActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap
    private val toolInfoViewModel: ToolInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekognition)

        analyzeImageWithRekognition(bitmap)
    }

    private fun analyzeImageWithRekognition(bitmap: Bitmap) {
        val credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            "us-east-2:2ba3dbfc-d836-4c08-a227-c4216126a5f4", // Identity pool ID
            Regions.US_EAST_2 // Region
        )
        val rekognitionClient = AmazonRekognitionClient(credentialsProvider)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes = ByteBuffer.wrap(byteArrayOutputStream.toByteArray())

        val request = DetectLabelsRequest()
            .withImage(Image().withBytes(imageBytes))
            .withMaxLabels(10)
            .withMinConfidence(70f)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = rekognitionClient.detectLabels(request)
                val labels = response.labels


                val toolName = labels.firstOrNull()?.name ?: return@launch

                Log.d("RekognitionActivity", "Detected tool: $toolName")


                toolInfoViewModel.fetchToolInfo(toolName)
            } catch (e: Exception) {
                Log.e("RekognitionActivity", "Rekognition error: ${e.localizedMessage}")
            }
        }
    }
}