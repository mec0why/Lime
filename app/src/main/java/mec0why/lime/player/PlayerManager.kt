package mec0why.lime.player

import android.content.Context
import androidx.core.net.toUri
import com.amazonaws.ivs.player.Player
import kotlinx.coroutines.flow.MutableStateFlow

enum class PlayerState { HIDDEN, MINIMIZED, EXPANDED }

class PlayerManager(context: Context) {
    val player: Player = Player.Factory.create(context.applicationContext).apply {
        setLiveLowLatencyEnabled(true)
    }

    val currentSlug = MutableStateFlow<String?>(null)
    val currentUsername = MutableStateFlow<String?>(null)
    val isVerified = MutableStateFlow(false)
    val playerState = MutableStateFlow(PlayerState.HIDDEN)

    var isUserPaused = false
        private set

    private var currentSurface: android.view.Surface? = null
    private var currentLoadedUrl: String? = null

    fun setSurface(surface: android.view.Surface) {
        if (currentSurface != surface) {
            currentSurface = surface
            player.setSurface(surface)
            if (!isUserPaused && currentLoadedUrl != null &&
                player.state != Player.State.PLAYING &&
                player.state != Player.State.BUFFERING
            ) {
                player.play()
            }
        }
    }

    fun clearSurface(surface: android.view.Surface?) {
        if (currentSurface == surface) {
            currentSurface = null
            player.setSurface(null)
        }
    }

    fun userPlay() {
        isUserPaused = false
        player.play()
    }

    fun userPause() {
        isUserPaused = true
        player.pause()
    }

    fun play(url: String, slug: String, username: String? = null, verified: Boolean = false) {
        isUserPaused = false
        currentSlug.value = slug
        currentUsername.value = username
        isVerified.value = verified

        if (currentLoadedUrl == url) {
            if (player.state != Player.State.PLAYING) {
                player.play()
            }
            return
        }

        currentLoadedUrl = url
        player.load(url.toUri())
        player.play()
    }

    fun stop() {
        isUserPaused = false
        player.pause()
        currentSlug.value = null
        currentUsername.value = null
        isVerified.value = false
        currentLoadedUrl = null
        playerState.value = PlayerState.HIDDEN
        clearSurface(currentSurface)
    }
}
