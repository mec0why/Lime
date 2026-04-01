package mec0why.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.amazonaws.ivs.player.Player
import mec0why.lime.LimeApp
import mec0why.lime.ui.theme.DarkSurface
import mec0why.lime.ui.theme.LimeGreen

@Composable
fun MiniPlayerOverlay(
    modifier: Modifier = Modifier,
    isSurfaceOwner: Boolean = true,
    onNavigateToChannel: (String) -> Unit
) {
    val context = LocalContext.current
    val playerManager = remember { (context.applicationContext as LimeApp).playerManager }
    val currentSlug by playerManager.currentSlug.collectAsState()
    val username by playerManager.currentUsername.collectAsState()
    val isVerified by playerManager.isVerified.collectAsState()

    if (currentSlug == null) return

    val ivsPlayer = playerManager.player
    var isPlaying by remember { mutableStateOf(ivsPlayer.state == Player.State.PLAYING) }
    var isBuffering by remember { mutableStateOf(ivsPlayer.state == Player.State.BUFFERING) }
    
    var miniSurface by remember { mutableStateOf<android.view.Surface?>(null) }
    LaunchedEffect(isSurfaceOwner, miniSurface) {
        if (isSurfaceOwner) {
            miniSurface?.let { playerManager.setSurface(it) }
        }
    }

    DisposableEffect(ivsPlayer) {
        val listener = object : Player.Listener() {
            override fun onStateChanged(state: Player.State) {
                isPlaying = state == Player.State.PLAYING
                isBuffering = state == Player.State.BUFFERING
            }
            override fun onCue(cue: com.amazonaws.ivs.player.Cue) {}
            override fun onDurationChanged(duration: Long) {}
            override fun onError(exception: com.amazonaws.ivs.player.PlayerException) {}
            override fun onRebuffering() {}
            override fun onSeekCompleted(position: Long) {}
            override fun onVideoSizeChanged(width: Int, height: Int) {}
            override fun onQualityChanged(quality: com.amazonaws.ivs.player.Quality) {}
        }
        ivsPlayer.addListener(listener)
        onDispose {
            ivsPlayer.removeListener(listener)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(DarkSurface)
            .clickable {
                currentSlug?.let { onNavigateToChannel(it) }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        android.view.TextureView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                    miniSurface = android.view.Surface(st)
                                }
                                override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, width: Int, height: Int) {}
                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    miniSurface?.let {
                                        playerManager.clearSurface(it)
                                        it.release()
                                        miniSurface = null
                                    }
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = username ?: currentSlug ?: "",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (isVerified) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(LimeGreen, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = Color.Black
                        )
                    }
                }
            }

            if (isBuffering) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    color = LimeGreen,
                    strokeWidth = 3.dp
                )
            } else {
                IconButton(onClick = { if (isPlaying) playerManager.userPause() else playerManager.userPlay() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
            }

            IconButton(onClick = { 
                playerManager.stop() 
                context.stopService(android.content.Intent(context, mec0why.lime.player.PlayerService::class.java))
            }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
