package mec0why.lime.ui.channel

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.LiveRed
import mec0why.lime.ui.theme.TextPrimary
import mec0why.lime.ui.theme.TextSecondary
import mec0why.lime.ui.theme.TextTertiary

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
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
    val activity = LocalContext.current as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showChatOverlay by remember { mutableStateOf(true) }
    var isClosing by remember { mutableStateOf(false) }
    var chatWidthFraction by remember { mutableStateOf(0.35f) }
    var isResizing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    val handleBack = {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            isClosing = true
            exoPlayer.stop()
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
            exoPlayer.release()
            val window = activity?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingParam: Boolean) {
                isPlaying = isPlayingParam
            }
        }
        exoPlayer.addListener(listener)
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(playbackUrl) {
        playbackUrl?.let { url ->
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
        }
    }

    val isLive = !playbackUrl.isNullOrBlank() && channel?.livestream != null

    if (isFullscreen && !isClosing) {
        val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val screenWidth = configuration.screenWidthDp.dp

            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .weight(if (showChatOverlay) 1f - chatWidthFraction else 1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { showControls = !showControls }
                    }
                ) {
                    if (isLive) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                            IconButton(
                                onClick = {
                                    isClosing = true
                                    exoPlayer.stop()
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

                            if (isLive) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isLive) {
                                    IconButton(
                                        onClick = {
                                            playbackUrl?.let { url ->
                                                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                                exoPlayer.prepare()
                                                exoPlayer.play()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Refresh",
                                            tint = Color.White
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showChatOverlay = !showChatOverlay }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Toggle Chat",
                                        tint = if (showChatOverlay) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                }

                                IconButton(
                                    onClick = { isFullscreen = false }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FullscreenExit,
                                        contentDescription = "Exit fullscreen",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (showChatOverlay) {
                    channel?.chatroom?.id?.let { chatroomId ->
                        Box(
                            modifier = Modifier
                                .weight(chatWidthFraction)
                                .fillMaxHeight()
                                .background(DarkBackground)
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
            }

            if (showChatOverlay) {
                val boundaryX = screenWidth * (1f - chatWidthFraction)

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
                                    chatWidthFraction = (chatWidthFraction - dragFraction).coerceIn(0.2f, 0.7f)
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
            }
        }
        return
    }

    Scaffold(
        containerColor = DarkBackground
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            error != null && channel == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        when {
                            isClosing -> {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                            }
                            isLive && !isClosing -> {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { showControls = !showControls }
                                    }
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                            }
                                        },
                                        update = { view ->
                                            view.player = exoPlayer
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showControls,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                                            IconButton(
                                                onClick = handleBack,
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

                                            IconButton(
                                                onClick = {
                                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
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

                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        playbackUrl?.let { url ->
                                                            exoPlayer.setMediaItem(MediaItem.fromUri(url))
                                                            exoPlayer.prepare()
                                                            exoPlayer.play()
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = "Refresh",
                                                        tint = Color.White
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { isFullscreen = true }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Fullscreen,
                                                        contentDescription = "Fullscreen",
                                                        tint = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = handleBack,
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

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls || channel?.livestream == null,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = channel?.user?.profilePic,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel?.user?.username ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )

                                channel?.livestream?.let { live ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "● LIVE",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LiveRed
                                        )
                                        Text(
                                            text = "${formatViewers(live.viewerCount)} viewers",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }

                            if (channel?.verified == true) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            LimeGreen,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DarkBackground
                                    )
                                }
                            }
                        }

                        channel?.livestream?.let { live ->
                            Text(
                                text = live.sessionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )

                            if (live.categories.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    live.categories.forEach { cat ->
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LimeGreen,
                                            modifier = Modifier
                                                .background(
                                                    LimeGreen.copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        channel?.user?.bio?.let { bio ->
                            if (bio.isNotBlank()) {
                                Text(
                                    text = bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        }
                    }

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

private fun formatViewers(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}
