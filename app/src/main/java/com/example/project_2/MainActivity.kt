package com.example.project_2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.project_2.ui.theme.Project_2Theme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermission()) {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSION, 0)
        }
        setContent {
            Project_2Theme {
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
                        )
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // Preview Camera is by default front cam
                    }
                }

                var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
                var selectedOption by remember { mutableStateOf("None") }
                var faceCount by remember { mutableStateOf<Int?>(null) }
                var faces by remember { mutableStateOf<List<android.graphics.Rect>?>(null) }

                val options = listOf(
                    "None",
                    "Face detection",
                    "Contour detection",
                    "Mesh detection",
                    "Selfie segmentation"
                )

                Column(
                    modifier = Modifier
                        .padding(PaddingValues())
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedImage != null) {
                            Image(
                                bitmap = capturedImage!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp)),
                                contentScale = ContentScale.Crop
                            )

                            if (faces != null) {
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    faces?.forEach { face ->
                                        drawRect(
                                            topLeft = androidx.compose.ui.geometry.Offset(
                                                face.left.toFloat(),
                                                face.top.toFloat()
                                            ),
                                            size = androidx.compose.ui.geometry.Size(
                                                face.width().toFloat(),
                                                face.height().toFloat()
                                            ),
                                            color = Color.Red,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            CameraPreview(
                                controller = controller,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .height(500.dp),
                                selectedOption = selectedOption,
                                capturedImage = capturedImage,
                                onFacesDetected = { detectedFaces ->
                                    faces = detectedFaces
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (capturedImage != null) {
                        Button(
                            onClick = {
                                capturedImage = null
                                faceCount = null
                                faces = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(1.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Try Another", fontSize = 16.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                takePhoto(
                                    controller = controller,
                                    selectedOption = selectedOption,
                                    onPhotoTaken = { bitmap, count ->
                                        capturedImage = bitmap
                                        faceCount = count
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(1.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Take a Picture", fontSize = 16.sp)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = (option == selectedOption),
                                        onClick = { selectedOption = option }
                                    )
                                    Text(
                                        text = option,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
// If the selected option is Face Detection and option is Face detection then only will this statement be printed
                                if (selectedOption == "Face detection" && option == "Face detection" && faceCount != null && faceCount!! > 0) {
                                    Text(
                                        text = "$faceCount face${if (faceCount == 1) "" else "s"} detected",
                                        fontSize = 14.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto(
        controller: LifecycleCameraController,
        selectedOption: String,
        onPhotoTaken: (Bitmap, Int?) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val rotationDegrees = image.imageInfo.rotationDegrees
                    val originalBitmap = image.toBitmap()

                    val matrix = android.graphics.Matrix().apply {
                        postRotate(rotationDegrees.toFloat())
                    }

                    val rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap,
                        0,
                        0,
                        originalBitmap.width,
                        originalBitmap.height,
                        matrix,
                        true
                    )

                    image.close()

                    when (selectedOption) {
                        "Face detection" -> detectFaces(rotatedBitmap, onPhotoTaken)
                        "Contour detection" -> detectFaceContours(rotatedBitmap, onPhotoTaken)
                        "Selfie segmentation" -> performSelfieSegmentation(rotatedBitmap) { segmentedBitmap ->
                            onPhotoTaken(segmentedBitmap ?: rotatedBitmap, null)
                        }
                        else -> onPhotoTaken(rotatedBitmap, null)
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera:", "Couldn't take photo", exception)
                }
            }
        )
    }

    private fun detectFaces(bitmap: Bitmap, onResult: (Bitmap, Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val faceCount = faces.size
                val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(processedBitmap)
                val paint = Paint().apply {
                    color = android.graphics.Color.RED
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1f
                }

                for (face in faces) {
                    val bounds = face.boundingBox
                    canvas.drawRect(bounds, paint)
                }

                onResult(processedBitmap, faceCount)
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Face detection failed", e)
                onResult(bitmap, 0)
            }



    }
// Contour Detection
    private fun detectFaceContours(bitmap: Bitmap, onResult: (Bitmap, Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .enableTracking()
            .build()
        val detector = FaceDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val faceCount = faces.size
                val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = android.graphics.Canvas(processedBitmap)
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 3f
                }

                for (face in faces) {
                    // Draw bounding box
                    val bounds = face.boundingBox
                    canvas.drawRect(bounds, paint)

                    // Draw face contours
                    val contourPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2f
                    }

                    for (contour in face.allContours) {
                        val points = contour.points
                        for (i in 1 until points.size) {
                            val start = points[i - 1]
                            val end = points[i]
                            canvas.drawLine(start.x, start.y, end.x, end.y, contourPaint)
                        }
                    }
                }

                onResult(processedBitmap, faceCount)
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Face detection failed", e)
                onResult(bitmap, 0)
            }
    }


    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier,
        selectedOption: String,
        capturedImage: Bitmap?,
        onFacesDetected: (List<android.graphics.Rect>) -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var faces by remember { mutableStateOf<List<android.graphics.Rect>>(emptyList()) }
        var previewWidth by remember { mutableStateOf(0) }
        var previewHeight by remember { mutableStateOf(0) }

        Box(
            modifier = modifier
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        this.controller = controller
                        controller.bindToLifecycle(lifecycleOwner)

                        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                            previewWidth = right - left
                            previewHeight = bottom - top
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if ((selectedOption == "Face detection" || selectedOption == "Contour detection") && capturedImage == null && faces.isNotEmpty()) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    faces.forEach { face ->
                        drawRect(
                            topLeft = androidx.compose.ui.geometry.Offset(
                                face.left.toFloat(),
                                face.top.toFloat()
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                face.width().toFloat(),
                                face.height().toFloat()
                            ),
                            color = Color.Red,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )
                    }
                }
            }
        }

        if ((selectedOption == "Face detection" || selectedOption == "Contour detection") && capturedImage == null) {
            LaunchedEffect(selectedOption) {
                var lastDetectionTime = System.currentTimeMillis()

                controller.setImageAnalysisAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastDetectionTime >= 200) {
                        lastDetectionTime = currentTime

                        val bitmap = image.toBitmap()
                        val rotationDegrees = image.imageInfo.rotationDegrees

                        detectFaces(bitmap) { _, faceCount ->
                            val facesList = mutableListOf<android.graphics.Rect>()
                            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
                            val detector = FaceDetection.getClient()
                            detector.process(inputImage)
                                .addOnSuccessListener { detectedFaces ->
                                    for (face in detectedFaces) {
                                        val mappedRect = mapBoundingBoxToPreview(
                                            face.boundingBox,
                                            rotationDegrees,
                                            bitmap.width,
                                            bitmap.height,
                                            previewWidth,
                                            previewHeight
                                        )
                                        facesList.add(mappedRect)
                                    }
                                    faces = facesList
                                    onFacesDetected(facesList)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MLKit", "Face detection failed", e)
                                }
                        }
                    }
                    image.close()
                }
            }
        }
    }

    private fun mapBoundingBoxToPreview(
        boundingBox: android.graphics.Rect,
        rotationDegrees: Int,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Int,
        previewHeight: Int
    ): android.graphics.Rect {
        val scaleX = previewWidth.toFloat() / imageWidth.toFloat()
        val scaleY = previewHeight.toFloat() / imageHeight.toFloat()
        val scale = max(scaleX, scaleY)
        val offsetX = (previewWidth - imageWidth * scale) / 2
        val offsetY = (previewHeight - imageHeight * scale) / 2

        return android.graphics.Rect(
            (boundingBox.left * scale + offsetX + previewWidth * 0.16f).toInt(),
            (boundingBox.top * scale + offsetY - previewHeight * 0.16f).toInt(),
            (boundingBox.right * scale + offsetX + previewWidth * 0.16f).toInt(),
            (boundingBox.bottom * scale + offsetY - previewHeight * 0.16f).toInt()
        )
    }

    private fun hasRequiredPermission(): Boolean {
        return CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }
}