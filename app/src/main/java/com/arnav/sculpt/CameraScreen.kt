package com.arnav.sculpt

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SculptCamera"
private val TIMER_OPTIONS = listOf(0, 3, 5, 10)

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        CameraContent()
    } else {
        PermissionNeeded(onRequest = { launcher.launch(Manifest.permission.CAMERA) })
    }
}

@Composable
private fun PermissionNeeded(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sculpt needs camera access to take progress photos.",
            color = Color.White
        )
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant camera permission")
        }
    }
}

@Composable
private fun CameraContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Poses chosen in Settings. Read once per visit to the camera tab.
    val poses = remember { SettingsStore.enabledPoses(context) }
    val customName = remember { SettingsStore.customPoseName(context) }
    var poseIndex by remember {
        mutableIntStateOf(
            poses.indexOfFirst { it.id == SettingsStore.selectedPoseId(context) }.coerceAtLeast(0)
        )
    }
    val currentPose = poses[poseIndex.coerceIn(0, poses.lastIndex)]

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var lastPhoto by remember { mutableStateOf<File?>(PhotoStore.latest(context)) }

    var ghostOn by remember { mutableStateOf(SettingsStore.ghostOn(context)) }
    var ghostAlpha by remember { mutableFloatStateOf(SettingsStore.ghostAlpha(context)) }
    var timerSeconds by remember { mutableIntStateOf(SettingsStore.timerSeconds(context)) }
    var countdown by remember { mutableIntStateOf(0) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Remember the selected pose between visits.
    LaunchedEffect(poseIndex) {
        SettingsStore.setSelectedPoseId(context, currentPose.id)
    }

    // Photo count per pose, shown in the pose picker. Refreshed after every capture.
    val counts = remember(lastPhoto) {
        PhotoStore.all(context)
            .mapNotNull { it.poseId }
            .groupingBy { it }
            .eachCount()
    }

    // Stop any running countdown when leaving the camera tab.
    DisposableEffect(Unit) {
        onDispose { countdownJob?.cancel() }
    }

    // Ghost overlay: the most recent photo of the currently selected pose.
    val ghostFile = remember(currentPose, lastPhoto) {
        PhotoStore.latestFor(context, currentPose.id)
    }
    val ghostBitmap by produceState<ImageBitmap?>(initialValue = null, ghostFile) {
        value = ghostFile?.let {
            withContext(Dispatchers.IO) { ImageUtils.decode(it, 1280)?.asImageBitmap() }
        }
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    fun capture(pose: Pose) {
        val file = PhotoStore.newFile(context, pose.id)
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "Saved ${file.absolutePath}")
                    lastPhoto = file
                    // Move on to the next pose after a photo is taken.
                    if (poses.size > 1) {
                        poseIndex = (poses.indexOf(pose) + 1) % poses.size
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                }
            }
        )
    }

    fun onShutter() {
        // Tapping the shutter during a countdown cancels it.
        if (countdownJob != null) {
            countdownJob?.cancel()
            return
        }
        if (timerSeconds == 0) {
            capture(currentPose)
            return
        }
        countdownJob = scope.launch {
            try {
                for (i in timerSeconds downTo 1) {
                    countdown = i
                    delay(1000)
                }
                countdown = 0
                capture(poses[poseIndex.coerceIn(0, poses.lastIndex)])
            } finally {
                countdown = 0
                countdownJob = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Ghost overlay. The front camera preview is mirrored, so mirror the ghost to match.
        if (ghostOn) {
            ghostBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = ghostAlpha,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = if (lensFacing == CameraSelector.LENS_FACING_FRONT) -1f else 1f
                        }
                )
            }
        }

        // Top controls: ghost toggle, pose selector, timer.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(text = "Ghost: ${if (ghostOn) "On" else "Off"}") {
                    ghostOn = !ghostOn
                    SettingsStore.setGhostOn(context, ghostOn)
                }
                Pill(text = if (timerSeconds == 0) "Timer: Off" else "Timer: ${timerSeconds}s") {
                    val next = TIMER_OPTIONS[(TIMER_OPTIONS.indexOf(timerSeconds) + 1) % TIMER_OPTIONS.size]
                    timerSeconds = next
                    SettingsStore.setTimerSeconds(context, next)
                }
            }

            if (ghostOn) {
                if (ghostFile != null) {
                    Slider(
                        value = ghostAlpha,
                        onValueChange = { ghostAlpha = it },
                        onValueChangeFinished = { SettingsStore.setGhostAlpha(context, ghostAlpha) },
                        valueRange = 0.1f..0.8f,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text(
                        text = "No earlier ${currentPose.label(customName)} photo yet",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Countdown number.
        if (countdown > 0) {
            Text(
                text = countdown.toString(),
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Bottom controls: pose pill sits right above the shutter row.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PoseSelector(
                poses = poses,
                customName = customName,
                selectedIndex = poseIndex,
                counts = counts,
                onSelect = { poseIndex = it }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Thumbnail(lastPhoto)

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(if (countdownJob != null) Color.Red else Color.White)
                        .clickable { onShutter() }
                )

                TextButton(onClick = {
                    lensFacing =
                        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                }) {
                    Text("Flip", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x99000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun Thumbnail(file: File?) {
    val bitmap = remember(file) {
        file?.let { ImageUtils.decode(it, 200) }
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.DarkGray)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Last photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
