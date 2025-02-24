package com.example.project_2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import java.util.concurrent.Executors
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Bitmap
import android.graphics.Color

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

            val options = listOf(
                "None",
                "Mesh detection"
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedImage != null) {
                        Image(
                            bitmap = capturedImage!!.asImageBitmap(),
                            contentDescription = "Captured Photo",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CameraPreview(controller, Modifier.fillMaxSize())
                    }
                }

                Button(
                    onClick = {
                        if (capturedImage != null) {
                            capturedImage = null
                            faceCount = null
                        } else {
                            takePhoto(controller, selectedOption) { bitmap, count ->
                                capturedImage = bitmap
                                faceCount = count
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(1.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Blue)
                ) {
                    Text(if (capturedImage != null) "Try Another" else "Take a Picture", fontSize = 16.sp)
                }

                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = (option == selectedOption), onClick = { selectedOption = option })
                            Text(text = option, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
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

                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(
                        originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                    )

                    image.close()

                    if (selectedOption == "Mesh detection") {
                        detectMesh(rotatedBitmap, onPhotoTaken)
                    } else {
                        onPhotoTaken(rotatedBitmap, null)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera:", "Couldn't take photo", exception)
                }
            }
        )
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
                    color = Color.GREEN
                    style = Paint.Style.FILL
                    strokeWidth = 4f
                }

                val linePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }

                faces.forEach { face ->
                    val points = face.allPoints

                    // 获取图像尺寸
                    val imageWidth = bitmap.width
                    val imageHeight = bitmap.height
                    Log.d("FaceMesh", "Image size: width=$imageWidth, height=$imageHeight")

                    // 获取人脸边界框
                    val boundingBox = face.boundingBox
                    Log.d("FaceMesh", "Bounding box: $boundingBox")

                    // 过滤边界框内的点
                    val filteredPoints = points.filter { point ->
                        point.position.x >= boundingBox.left &&
                                point.position.x <= boundingBox.right &&
                                point.position.y >= boundingBox.top &&
                                point.position.y <= boundingBox.bottom
                    }

                    Log.d("FaceMesh", "Filtered points: ${filteredPoints.size}")

                    // 打印过滤后的点坐标
                    filteredPoints.forEachIndexed { index, point ->
                        Log.d("FaceMesh", "Filtered Point $index: (${point.position.x}, ${point.position.y})")
                    }

                    // 计算每个点与最近的 6 个点的连接
                    val nearestNeighbors = mutableMapOf<Int, List<Int>>()
                    for (i in filteredPoints.indices) {
                        val currentPoint = filteredPoints[i]
                        val distances = filteredPoints.mapIndexed { index, point ->
                            val dx = point.position.x - currentPoint.position.x
                            val dy = point.position.y - currentPoint.position.y
                            val distance = dx * dx + dy * dy // 使用平方距离避免开方计算
                            Pair(index, distance)
                        }

                        // 按距离排序，取最近的 6 个点（排除自身）
                        val nearest = distances
                            .filter { it.first != i } // 排除自身
                            .sortedBy { it.second }   // 按距离排序
                            .take(6)                  // 取最近的 6 个点
                            .map { it.first }         // 只保留索引

                        nearestNeighbors[i] = nearest
                    }

                    // 绘制线条连接每个点与其最近的 6 个点
                    nearestNeighbors.forEach { (index, neighbors) ->
                        val currentPoint = filteredPoints[index]
                        val currentPixel = PointF(currentPoint.position.x, currentPoint.position.y)

                        // 遍历最近的 6 个点
                        for (neighborIndex in neighbors) {
                            val neighborPoint = filteredPoints[neighborIndex]
                            val neighborPixel = PointF(neighborPoint.position.x, neighborPoint.position.y)

                            // 绘制线条连接当前点和邻居点
                            canvas.drawLine(
                                currentPixel.x, currentPixel.y,
                                neighborPixel.x, neighborPixel.y,
                                linePaint
                            )
                        }
                    }

                    // 绘制点
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

    @Composable
    fun CameraPreview(
        controller: LifecycleCameraController,
        modifier: Modifier
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = modifier
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