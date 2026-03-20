package mec0why.lime.ui.channel

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.TextSecondary

data class VideoTrackInfo(
    val name: String,
    val trackIndex: Int
)

private fun formatViewersCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = viewModel()
) {
    val channel by viewModel.channel.collectAsState()
    val playbackUrl by viewModel.playbackUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val slug by viewModel.slugFlow.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val activity = androidx.activity.compose.LocalActivity.current
    val windowInfo = LocalWindowInfo.current

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showChatOverlay by remember { mutableStateOf(true) }
    var isClosing by remember { mutableStateOf(false) }
    var chatWidthFraction by remember { mutableFloatStateOf(0.30f) }
    var isResizing by remember { mutableStateOf(false) }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var availableTracks by remember { mutableStateOf(emptyList<VideoTrackInfo>()) }
    var selectedTrackName by remember { mutableStateOf("Auto") }
    var userExplicitTrackName by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val context = LocalContext.current
    
    val ivsPlayer = remember {
        Player.Factory.create(context).apply {
            setLiveLowLatencyEnabled(true)
        }
    }

    val handleBack = {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            isClosing = true
            ivsPlayer.pause()
            onBack()
        }
    }

    BackHandler(onBack = handleBack)

    LaunchedEffect(isLandscape) {
        if (isLandscape) isFullscreen = true
    }

    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ivsPlayer.release()
            val window = activity?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var isCatchingUp by remember { mutableStateOf(false) }
    var streamUptime by remember { mutableStateOf("00:00") }
    var streamDelay by remember { mutableStateOf("0.0s") }

    LaunchedEffect(isPlaying, channel, isCatchingUp) {
        val createdAtIso = channel?.livestream?.createdAt?.replace(" ", "T")
        val startMillis = if (!createdAtIso.isNullOrBlank()) {
            try {
                val isoString = if (!createdAtIso.endsWith("Z")) "${createdAtIso}Z" else createdAtIso
                java.time.Instant.parse(isoString).toEpochMilli()
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        } else 0L

        while (isActive) {
            if (startMillis > 0) {
                val diff = System.currentTimeMillis() - startMillis
                val seconds = (diff / 1000) % 60
                val minutes = (diff / (1000 * 60)) % 60
                val hours = diff / (1000 * 60 * 60)
                streamUptime = if (hours > 0) {
                    String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
                }
            } else {
                streamUptime = "00:00"
            }
            
            val offsetMs = ivsPlayer.liveLatency
            if (offsetMs > 0) {
                streamDelay = String.format(java.util.Locale.US, "%.1fs", offsetMs / 1000f)
                
                if (isCatchingUp && offsetMs < 1000L) {
                    isCatchingUp = false
                    ivsPlayer.setPlaybackRate(1.0f)
                }
            } else {
                streamDelay = "0.0s"
            }
            
            kotlinx.coroutines.delay(if (isCatchingUp) 200L else 1000L)
        }
    }

    var videoWidth by remember { mutableStateOf(16) }
    var videoHeight by remember { mutableStateOf(9) }

    LaunchedEffect(ivsPlayer) {
        val listener = object : Player.Listener() {
            override fun onStateChanged(state: Player.State) {
                isPlaying = state == Player.State.PLAYING
                isBuffering = state == Player.State.BUFFERING
                
                if (state == Player.State.BUFFERING && isCatchingUp) {
                    isCatchingUp = false
                    ivsPlayer.setPlaybackRate(1.0f)
                }
                
                if (state == Player.State.PLAYING || state == Player.State.READY) {
                    val qualities = ivsPlayer.qualities
                    val newTracks = mutableListOf<VideoTrackInfo>()
                    qualities.forEachIndexed { index, quality ->
                        newTracks.add(VideoTrackInfo(quality.name, index))
                    }
                    availableTracks = newTracks.distinctBy { it.name }
                        .sortedByDescending { it.name.substringBefore("p").toIntOrNull() ?: 0 }
                        
                    if (ivsPlayer.isAutoQualityMode) {
                        if (ivsPlayer.quality != null) {
                            selectedTrackName = "Auto: ${ivsPlayer.quality.name}"
                        } else {
                            selectedTrackName = "Auto"
                        }
                    } else {
                        if (ivsPlayer.quality != null) {
                            selectedTrackName = ivsPlayer.quality.name
                        }
                    }
                }
            }

            override fun onVideoSizeChanged(width: Int, height: Int) {
                if (width > 0 && height > 0) {
                    videoWidth = width
                    videoHeight = height
                }
            }
            override fun onQualityChanged(quality: com.amazonaws.ivs.player.Quality) {
                if (ivsPlayer.isAutoQualityMode) {
                    selectedTrackName = "Auto: ${quality.name}"
                } else {
                    selectedTrackName = quality.name
                }
            }
            override fun onDurationChanged(duration: Long) {}
            override fun onError(exception: PlayerException) {}
            override fun onCue(cue: Cue) {}
            override fun onRebuffering() {}
            override fun onSeekCompleted(position: Long) {}
        }
        ivsPlayer.addListener(listener)
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(playbackUrl) {
        playbackUrl?.let { url ->
            ivsPlayer.load(Uri.parse(url))
            ivsPlayer.play()
        }
    }

    val isLive = !playbackUrl.isNullOrBlank() && channel?.livestream != null

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenWidth = maxWidth
        
        if (isFullscreen && !isClosing) {
            if (showChatOverlay) {
                val boundaryX = maxWidth * (1f - chatWidthFraction)
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .offset(x = boundaryX - 24.dp)
                        .zIndex(2f)
                        .pointerInput(screenWidthPx) {
                            detectHorizontalDragGestures(
                                onDragStart = { 
                                    isResizing = true
                                    showControls = true 
                                },
                                onDragEnd = { isResizing = false },
                                onDragCancel = { isResizing = false },
                                onHorizontalDrag = { change, dragAmount ->
                                    showControls = true
                                    change.consume()
                                    val dragFraction = dragAmount / screenWidthPx
                                    chatWidthFraction = (chatWidthFraction - dragFraction).coerceIn(0.2f, 0.6f)
                                }
                            )
                        }
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls || isResizing,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = 48.dp)
                                .background(LimeGreen, RoundedCornerShape(2.dp))
                        )
                    }
                }

                channel?.chatroom?.id?.let { chatroomId ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(chatWidthFraction)
                            .fillMaxHeight()
                            .background(DarkBackground)
                            .zIndex(1f)
                    ) {
                        ChatSection(
                            chatroomId = chatroomId,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            Scaffold(containerColor = DarkBackground) { padding ->
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    error != null && channel == null -> {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            Text(text = error ?: "An error occurred", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            StreamDetailsSection(
                                channel = channel,
                                showControls = showControls
                            )

                            channel?.chatroom?.id?.let { chatroomId ->
                                ChatSection(
                                    chatroomId = chatroomId,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        val videoLayerModifier = if (isFullscreen && !isClosing) {
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(if (showChatOverlay) 1f - chatWidthFraction else 1f)
                .background(Color.Black)
                .zIndex(0f)
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .zIndex(3f)
        }

        Box(
            modifier = videoLayerModifier
                .pointerInput(Unit) {
                    detectTapGestures { showControls = !showControls }
                }
        ) {
            if (isClosing) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            } else if (isLive) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val vidRatio = videoWidth.toFloat() / videoHeight.toFloat()
                    val boxRatio = if (maxHeight.value > 0) maxWidth.value / maxHeight.value else 1f
                    val isVideoWider = vidRatio > boxRatio
                    
                    AndroidView(
                        factory = { ctx ->
                            android.view.SurfaceView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                        ivsPlayer.setSurface(holder.surface)
                                    }
                                    override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, w: Int, h: Int) {}
                                    override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                        ivsPlayer.setSurface(null)
                                    }
                                })
                            }
                        },
                        modifier = Modifier
                            .aspectRatio(vidRatio, matchHeightConstraintsFirst = !isVideoWider)
                    )
                    
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center).size(64.dp),
                            color = LimeGreen,
                            strokeWidth = 6.dp
                        )
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isFullscreen) isFullscreen = false else {
                                        isClosing = true
                                        ivsPlayer.pause()
                                        onBack()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }

                        if (!isBuffering) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) ivsPlayer.pause() else ivsPlayer.play()
                                },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isCatchingUp = !isCatchingUp
                                    ivsPlayer.setPlaybackRate(if (isCatchingUp) 1.1f else 1.0f)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FastForward,
                                    contentDescription = "Catch up latency",
                                    tint = if (isCatchingUp) LimeGreen else Color.White
                                )
                            }

                            IconButton(
                                onClick = { showSettingsSheet = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(if (isFullscreen) Alignment.BottomStart else Alignment.BottomStart)
                                .padding(8.dp)
                                .padding(start = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Filled.Timer, contentDescription = "Uptime", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(text = streamUptime, color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Filled.Speed, contentDescription = "Delay", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(text = streamDelay, color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            channel?.livestream?.viewerCount?.let { count ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Viewers", tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(text = formatViewersCount(count), color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isFullscreen) {
                                IconButton(
                                    onClick = { showChatOverlay = !showChatOverlay }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Toggle Chat",
                                        tint = if (showChatOverlay) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isFullscreen = !isFullscreen }
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = "Toggle fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            isClosing = true
                            ivsPlayer.pause()
                            onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Stream is currently offline",
                            color = TextSecondary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = DarkBackground
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Quality", 
                    style = MaterialTheme.typography.titleLarge, 
                    color = Color.White, 
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val options = listOf("Auto") + availableTracks.map { it.name }
                
                options.forEach { option ->
                    val isSelected = if (option == "Auto") selectedTrackName.startsWith("Auto") else option == selectedTrackName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                userExplicitTrackName = option
                                if (option == "Auto") {
                                    ivsPlayer.isAutoQualityMode = true
                                    if (ivsPlayer.quality != null) {
                                        selectedTrackName = "Auto: ${ivsPlayer.quality.name}"
                                    } else {
                                        selectedTrackName = "Auto"
                                    }
                                } else {
                                    val quality = ivsPlayer.qualities.find { it.name == option }
                                    if (quality != null) {
                                        ivsPlayer.setQuality(quality)
                                        selectedTrackName = option
                                    }
                                }
                                showSettingsSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) LimeGreen else Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.Check, contentDescription = null, tint = LimeGreen)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
