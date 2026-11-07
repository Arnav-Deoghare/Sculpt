package com.arnav.sculpt.camera

import com.arnav.sculpt.appContainer
import com.arnav.sculpt.data.SettingsStore
import com.arnav.sculpt.data.db.PoseEntity
import com.arnav.sculpt.ui.WheelOption
import com.arnav.sculpt.ui.WheelPill
import com.arnav.sculpt.util.ImageUtils
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import com.arnav.sculpt.data.db.PhotoEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SculptCamera"
private const val HANDS_FREE_SECONDS = 3
private val TIMER_OPTIONS = listOf(0, 3, 5, 10)

@Composable
fun CameraScreen(onDone: () -> Unit = {}) {
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
        CameraContent(onDone)
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
private fun CameraContent(onDone: () -> Unit) {
    val container = LocalContext.current.appContainer
    val poses by container.poses.enabledPoses.collectAsState(initial = emptyList())

    if (poses.isEmpty()) {
        // Only visible for an instant while the poses load.
        Box(Modifier.fillMaxSize().background(Color.Black))
    } else {
        CameraBody(poses, onDone)
    }
}

@Composable
private fun CameraBody(poses: List<PoseEntity>, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // The selected pose is tracked by id, so it survives poses being added or hidden in Settings.
    var selectedId by remember { mutableStateOf(SettingsStore.selectedPoseId(context)) }
    val poseIndex = poses.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val currentPose = poses[poseIndex]
    val currentPoseNow by rememberUpdatedState(currentPose)

    val photos by container.photos.photos.collectAsState(initial = emptyList())
    val counts by container.photos.countsByPose.collectAsState(initial = emptyMap())
    val lastPhoto: File? = photos.firstOrNull()?.let { container.photos.file(it) }

    // Poses that already have a photo from today (local date), and whether the set just finished.
    val doneToday: Set<String> = remember(photos) { posesDoneToday(photos) }
    val doneTodayNow by rememberUpdatedState(doneToday)
    var finished by remember { mutableStateOf(false) }
    val onDoneNow by rememberUpdatedState(onDone)

    // Hands-free: one tap starts a run that shoots each remaining pose every few seconds.
    val handsFree = remember { SettingsStore.handsFree(context) }
    var autoRun by remember { mutableStateOf(false) }

    // "Photo captured" flourish: the shot freezes full-screen, then shrinks into its progress segment.
    var flying by remember { mutableStateOf<FlyingShot?>(null) }
    // Window position of each pose's progress segment, so the frozen shot can shrink into it.
    val segmentCenters = remember { HashMap<String, Offset>() }
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    val flight = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    var ghostOn by remember { mutableStateOf(SettingsStore.ghostOn(context)) }
    var ghostAlpha by remember { mutableFloatStateOf(SettingsStore.ghostAlpha(context)) }
    var timerSeconds by remember { mutableIntStateOf(SettingsStore.timerSeconds(context)) }
    var countdown by remember { mutableIntStateOf(0) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    var flashMode by remember { mutableIntStateOf(SettingsStore.flashMode(context)) }
    // Most front cameras have no flash unit, so the flash button is dimmed and ignored there.
    var hasFlash by remember { mutableStateOf(false) }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Apply the flash mode to the capture use case (off whenever the camera has no flash).
    LaunchedEffect(flashMode, hasFlash) {
        imageCapture.flashMode = if (hasFlash) flashMode else ImageCapture.FLASH_MODE_OFF
    }

    // Remember the selected pose between visits.
    LaunchedEffect(currentPose.id) {
        SettingsStore.setSelectedPoseId(context, currentPose.id)
    }

    // Stop any running countdown when leaving the camera tab.
    DisposableEffect(Unit) {
        onDispose { countdownJob?.cancel() }
    }

    // Ghost overlay: the most recent photo of the currently selected pose.
    val ghostFile: File? = remember(currentPose.id, photos) {
        photos.firstOrNull { it.poseId == currentPose.id }?.let { container.photos.file(it) }
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
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                hasFlash = camera.cameraInfo.hasFlashUnit()
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    fun capture(pose: PoseEntity) {
        val file = container.photos.newFile()
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(TAG, "Saved ${file.absolutePath}")
                    // Record the photo in the database; appScope so it finishes even if the
                    // screen is left right after the shot.
                    container.appScope.launch { container.photos.add(file, pose.id) }
                    val done = doneTodayNow + pose.id
                    val allDone = poses.all { it.id in done }
                    // Otherwise move on to the next pose that has no photo yet today.
                    var nextId: String? = null
                    if (!allDone) {
                        val from = poses.indexOfFirst { it.id == pose.id }
                        nextId = (1 until poses.size)
                            .map { poses[(from + it) % poses.size] }
                            .firstOrNull { it.id !in done }?.id
                    }
                    // The pill changes (or the day finishes) once the thumbnail lands in it.
                    scope.launch {
                        val bmp = withContext(Dispatchers.IO) {
                            ImageUtils.decode(file, 300)?.asImageBitmap()
                        }
                        if (bmp != null) {
                            flying = FlyingShot(bmp, pose.id, nextId, allDone)
                        } else {
                            nextId?.let { selectedId = it }
                            autoRun = false
                            if (allDone) finished = true
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exception)
                }
            }
        )
    }

    fun startCountdown(seconds: Int) {
        countdownJob = scope.launch {
            try {
                for (i in seconds downTo 1) {
                    countdown = i
                    delay(1000)
                }
                countdown = 0
                capture(currentPoseNow)
            } finally {
                countdown = 0
                countdownJob = null
            }
        }
    }

    LaunchedEffect(flying) {
        val shot = flying ?: return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        flight.snapTo(0f)
        delay(260) // hold the freeze-frame so you can check the shot
        flight.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        shot.nextId?.let { selectedId = it }
        flying = null
        if (shot.finishes) {
            autoRun = false
            finished = true
        } else if (autoRun) {
            // Hands-free: line up the next pose automatically.
            startCountdown(HANDS_FREE_SECONDS)
        }
    }

    fun onShutter() {
        // Tapping the shutter during a countdown or a hands-free run stops it.
        if (countdownJob != null || autoRun) {
            autoRun = false
            countdownJob?.cancel()
            return
        }
        // Ignore taps while the previous shot is still animating.
        if (flying != null) return
        if (handsFree) {
            autoRun = true
            startCountdown(HANDS_FREE_SECONDS)
            return
        }
        if (timerSeconds == 0) {
            capture(currentPose)
            return
        }
        startCountdown(timerSeconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { originInWindow = it.positionInWindow() }
    ) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Pill(text = "Ghost: ${if (ghostOn) "On" else "Off"}") {
                        ghostOn = !ghostOn
                        SettingsStore.setGhostOn(context, ghostOn)
                    }
                }

                // Timer: tap cycles, long-press and drag opens the wheel (strip opens below).
                WheelPill(
                    options = TIMER_OPTIONS.map { WheelOption(if (it == 0) "Off" else "${it}s") },
                    selectedIndex = TIMER_OPTIONS.indexOf(timerSeconds).coerceAtLeast(0),
                    pillText = if (timerSeconds == 0) "Timer: Off" else "Timer: ${timerSeconds}s",
                    openAbove = false,
                    onSelect = { index ->
                        timerSeconds = TIMER_OPTIONS[index]
                        SettingsStore.setTimerSeconds(context, timerSeconds)
                    },
                    chipWidth = 64.dp
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    IconPillButton(enabled = hasFlash, onClick = {
                        // Off -> Auto -> On -> Off
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                        SettingsStore.setFlashMode(context, flashMode)
                    }) {
                        FlashIcon(
                            mode = if (hasFlash) flashMode else ImageCapture.FLASH_MODE_OFF,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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
                        text = "No earlier ${currentPose.name} photo yet",
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

        // The captured photo: frozen full-screen, then shrinking into its progress segment.
        flying?.let { shot ->
            Image(
                bitmap = shot.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val t = flight.value
                        val target = (segmentCenters[shot.poseId] ?: Offset.Zero) - originInWindow
                        val s = 1f - 0.93f * t
                        scaleX = s
                        scaleY = s
                        translationX = (target.x - size.width / 2f) * t
                        translationY = (target.y - size.height / 2f) * t
                        shape = RoundedCornerShape((t * 160f).dp)
                        clip = true
                        alpha = if (t < 0.85f) 1f else (1f - t) / 0.15f
                    }
            )
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

        // Hands-free: say which pose is coming up.
        if (autoRun && countdown > 0) {
            Text(
                text = currentPose.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 150.dp)
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
            TodayProgress(
                poses = poses,
                doneToday = doneToday,
                currentId = currentPose.id,
                onSegmentCenter = { id, c -> segmentCenters[id] = c }
            )

            Spacer(Modifier.height(10.dp))

            PoseSelector(
                poses = poses,
                selectedIndex = poseIndex,
                counts = counts,
                doneToday = doneToday,
                onSelect = { selectedId = poses[it].id }
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
                        .background(if (countdownJob != null || autoRun) Color.Red else Color.White)
                        .clickable { onShutter() }
                )

                IconPillButton(
                    size = 52.dp,
                    onClick = {
                        lensFacing =
                            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                    }
                ) {
                    FlipCameraIcon(modifier = Modifier.size(30.dp))
                }
            }
        }

        // "Done for today" overlay: fades to black, holds a moment, then returns home.
        AnimatedVisibility(
            visible = finished,
            enter = fadeIn(tween(700)),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DONE FOR TODAY",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 6.sp
                )
            }
        }
        LaunchedEffect(finished) {
            if (finished) {
                delay(2200)
                onDoneNow()
            }
        }
    }
}

/** A just-captured photo on its way into its progress segment. */
private class FlyingShot(
    val bitmap: ImageBitmap,
    val poseId: String,
    val nextId: String?,
    val finishes: Boolean
)

/** Ids of poses that have at least one photo taken today (local time). */
private fun posesDoneToday(photos: List<PhotoEntity>): Set<String> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    return photos.asSequence()
        .filter { Instant.ofEpochMilli(it.takenAt).atZone(zone).toLocalDate() == today }
        .mapNotNull { it.poseId }
        .toSet()
}

/** One segment per pose: filled when done today, outlined for the pose currently selected. */
@Composable
private fun TodayProgress(
    poses: List<PoseEntity>,
    doneToday: Set<String>,
    currentId: String,
    onSegmentCenter: (String, Offset) -> Unit
) {
    val doneCount = poses.count { it.id in doneToday }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$doneCount of ${poses.size} today",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x99000000))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            poses.forEach { pose ->
                val done = pose.id in doneToday
                val color by animateColorAsState(
                    if (done) Color.White else Color(0x66FFFFFF),
                    label = "seg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { onSegmentCenter(pose.id, it.boundsInWindow().center) }
                        .height(if (pose.id == currentId) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(color)
                )
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

/** Round translucent button that hosts an icon, matching the look of the pills. */
@Composable
private fun IconPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Color(0x99000000))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
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