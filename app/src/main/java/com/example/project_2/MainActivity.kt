package com.example.project_2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.max

class MainActivity : ComponentActivity() {

    private val FACE_MESH_CONNECTIONS = listOf(
        Pair(10, 338), Pair(338, 297), Pair(297, 332), Pair(332, 284), Pair(284, 251), Pair(251, 389), Pair(389, 356), Pair(356, 454), Pair(454, 323), Pair(323, 361), Pair(361, 288), Pair(288, 397), Pair(397, 365), Pair(365, 379), Pair(379, 378), Pair(378, 400), Pair(400, 377), Pair(377, 152), Pair(152, 148), Pair(148, 176), Pair(176, 149), Pair(149, 150), Pair(150, 136), Pair(136, 172), Pair(172, 58), Pair(58, 132), Pair(132, 93), Pair(93, 234), Pair(234, 127), Pair(127, 162), Pair(162, 21), Pair(21, 54), Pair(54, 103), Pair(103, 67), Pair(67, 109), Pair(109, 10)
    )

    private val FACE_MESH_TRIANGLES = listOf(
        Triple(10, 338, 297),
        Triple(297, 332, 284),
        Triple(284, 251, 389),
        Triple(389, 356, 454),
        Triple(454, 323, 361),
        Triple(361, 288, 397),
        Triple(397, 365, 379),
        Triple(379, 378, 400),
        Triple(400, 377, 152),
        Triple(152, 148, 176),
        Triple(176, 149, 150),
        Triple(150, 136, 172),
        Triple(172, 58, 132),
        Triple(132, 93, 234),
        Triple(234, 127, 162),
        Triple(162, 21, 54),
        Triple(54, 103, 67),
        Triple(67, 109, 10)
    )

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
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
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
                        "Mesh detection" -> detectMesh(rotatedBitmap, onPhotoTaken)

                        "Selfie segmentation" -> performSelfieSegmentation(rotatedBitmap) { segmentedBitmap ->
                            onPhotoTaken(segmentedBitmap ?: rotatedBitmap, null)}
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

    private fun detectMesh(bitmap: Bitmap, onResult: (Bitmap, Int) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = FaceMeshDetectorOptions.Builder().build()

        val detector = FaceMeshDetection.getClient(options)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(processedBitmap)

                val pointPaint = Paint().apply {
                    color = Color.Green.toArgb()
                    style = Paint.Style.FILL
                    strokeWidth = 4f
                }

                val linePaint = Paint().apply {
                    color = Color.White.toArgb()
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }

                faces.forEach { face ->
                    val points = face.allPoints

                    val imageWidth = bitmap.width
                    val imageHeight = bitmap.height
                    Log.d("FaceMesh", "Image size: width=$imageWidth, height=$imageHeight")

                    val boundingBox = face.boundingBox
                    Log.d("FaceMesh", "Bounding box: $boundingBox")

                    val filteredPoints = points.filter { point ->
                        point.position.x >= boundingBox.left &&
                                point.position.x <= boundingBox.right &&
                                point.position.y >= boundingBox.top &&
                                point.position.y <= boundingBox.bottom
                    }

                    Log.d("FaceMesh", "Filtered points: ${filteredPoints.size}")

                    filteredPoints.forEachIndexed { index, point ->
                        Log.d("FaceMesh", "Filtered Point $index: (${point.position.x}, ${point.position.y})")
                    }

                    val nearestNeighbors = mutableMapOf<Int, List<Int>>()
                    for (i in filteredPoints.indices) {
                        val currentPoint = filteredPoints[i]
                        val distances = filteredPoints.mapIndexed { index, point ->
                            val dx = point.position.x - currentPoint.position.x
                            val dy = point.position.y - currentPoint.position.y
                            val distance = dx * dx + dy * dy
                            Pair(index, distance)
                        }

                        val nearest = distances
                            .filter { it.first != i }
                            .sortedBy { it.second }
                            .take(6)
                            .map { it.first }

                        nearestNeighbors[i] = nearest
                    }

                    nearestNeighbors.forEach { (index, neighbors) ->
                        val currentPoint = filteredPoints[index]
                        val currentPixel = PointF(currentPoint.position.x, currentPoint.position.y)

                        for (neighborIndex in neighbors) {
                            val neighborPoint = filteredPoints[neighborIndex]
                            val neighborPixel = PointF(neighborPoint.position.x, neighborPoint.position.y)

                            canvas.drawLine(
                                currentPixel.x, currentPixel.y,
                                neighborPixel.x, neighborPixel.y,
                                linePaint
                            )
                        }
                    }

                    filteredPoints.forEach { point ->
                        val pixel = PointF(point.position.x, point.position.y)
                        canvas.drawCircle(pixel.x, pixel.y, 2.5f, pointPaint)
                    }
                }

                onResult(processedBitmap, faces.size)
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Mesh detection failed", e)
                onResult(bitmap, 0)
            }
    }

    private fun performSelfieSegmentation(bitmap: Bitmap, onResult: (Bitmap?) -> Unit) {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()

        val segmenter = Segmentation.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(image)
            .addOnSuccessListener { mask ->
                val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                applySegmentationMask(outputBitmap, mask.buffer, mask.width, mask.height)
                onResult(outputBitmap)
            }
            .addOnFailureListener { e ->
                Log.e("Segmentation", "Segmentation failed: ${e.message}")
                onResult(bitmap) // Return original image on failure
            }
    }

    private fun applySegmentationMask(
        output: Bitmap,
        maskBuffer: ByteBuffer,
        maskWidth: Int,
        maskHeight: Int
    ) {
        maskBuffer.rewind()
        val maskBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)

        val pixels = IntArray(maskWidth * maskHeight)
        for (i in pixels.indices) {
            val alpha = (maskBuffer.float * 255).toInt().coerceIn(0, 255)
            pixels[i] = android.graphics.Color.argb(alpha, 255, 255, 255)
        }
        maskBitmap.setPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)


        val scaledMask = Bitmap.createScaledBitmap(maskBitmap, output.width, output.height, true)


        val overlayPaint = Paint().apply {
            color = android.graphics.Color.rgb(186, 85, 211) // Purple Tint
            alpha = (0.5f * 255).toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }

        val canvas = Canvas(output)
        canvas.drawBitmap(scaledMask, 0f, 0f, overlayPaint)
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