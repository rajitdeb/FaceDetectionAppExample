package com.rajit.facedetectionapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.rajit.facedetectionapp.databinding.ActivityMainBinding
import kotlin.math.roundToLong

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityMainBinding

    private val imageCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val extras = it.data?.extras
                val bitmap = extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    startFaceDetection(bitmap)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Bitmap capture unsuccessful!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(
                        applicationContext,
                        "Camera permission not granted. Cannot detect face!!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        _binding.apply {

            detectBtn.setOnClickListener {

                // Check if the app has permission to open Camera
                if (
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {

                    // Detect Face
                    detectFace()

                } else {

                    // Ask for Camera Permission
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)

                }
            }

        }

    }

    private fun detectFace() {

        // Capture Image
        captureImageAsBitmap()

    }

    private fun captureImageAsBitmap() {

        // Assuming the app has CAMERA permission, we first click a picture through Phone's Camera App
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            imageCaptureLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "detectFace: No Camera app found!!")
            Toast.makeText(applicationContext, "No camera app found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.d(TAG, "detectFace: Unknown error occurred!! Error: $e")
            Toast.makeText(applicationContext, "Error: $e", Toast.LENGTH_SHORT).show()
        }

    }

    private fun startFaceDetection(bitmap: Bitmap) {
        // Configure Face Detection Options
        val options = configureFaceDetectionOptions()

        // Prepare the image for analysis
        val image = InputImage.fromBitmap(bitmap, 0)

        // Get instance of Face Detector and pass FaceDetectorOptions to it
        val detector = FaceDetection.getClient(options)

        // Process the image
        processImage(detector, image)
    }

    private fun processImage(detector: FaceDetector, image: InputImage) {
        detector
            .process(image)
            .addOnSuccessListener { faces ->
                // Task Completed Successfully
                Log.d(TAG, "processImage: Processing Completed. Analysis Successful :)")
                Toast.makeText(applicationContext, "Processing Completed", Toast.LENGTH_SHORT)
                    .show()

                // Show the bitmap image
                _binding.analysedImage.setImageBitmap(image.bitmapInternal)

                // Show the results of the Faces detected
                displayFaceResults(faces)

            }
            .addOnFailureListener {
                // Task Failed
                Log.d(TAG, "processImage: Processing Failed :( !!!")
                Toast.makeText(applicationContext, "Processing Failed!!!", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun displayFaceResults(faces: List<Face>) {
        // Check if the faces list is empty
        if (faces.isNotEmpty()) {

            // Only show the results for the first Face
            val currentFace = faces[0]

            _binding.apply {
                val smileProbabilityPercentage = currentFace.smilingProbability?.times(100)
                if (smileProbabilityPercentage != null) {
                    smileTv.text = "${smileProbabilityPercentage}%"
                }

                if (currentFace.rightEyeOpenProbability != null && currentFace.leftEyeOpenProbability != null) {
                    val leftEyeOpenProbPercent = currentFace.leftEyeOpenProbability?.times(100)
                    leftEyeOpenTv.text = "${leftEyeOpenProbPercent}%"

                    val rightEyeOpenProbPercent = currentFace.rightEyeOpenProbability?.times(100)
                    rightEyeOpenTv.text = "${rightEyeOpenProbPercent}%"

                }
            }

            showResultsView()

        } else {
            // No faces detected in the captured image
            Log.d(TAG, "displayFaceResults: No faces detected :( !!!")
            Toast.makeText(applicationContext, "No faces detected :( !!!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showResultsView() {
        _binding.apply {
            imageAnalysisResultsLayout.visibility = View.VISIBLE
        }
    }


    private fun configureFaceDetectionOptions(): FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .build()
    }

}