package mec0why.lime.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.MainActivity

class PlayerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 10101
        const val CHANNEL_ID = "lime_media_playback"
        const val ACTION_PLAY = "lime.intent.action.PLAY"
        const val ACTION_PAUSE = "lime.intent.action.PAUSE"
        const val ACTION_STOP = "lime.intent.action.STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_AVATAR_URL = "extra_avatar_url"
    }

    private lateinit var mediaSession: MediaSession
    private lateinit var playerManager: PlayerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var title: String = "Live"
    private var artist: String = "Lime Stream"
    private var artBitmap: Bitmap? = null
    private var isPlaying = false
    private var isBuffering = false

    private val playerListener = object : Player.Listener() {
        override fun onStateChanged(state: Player.State) {
            isPlaying = state == Player.State.PLAYING
            isBuffering = state == Player.State.BUFFERING
            updatePlaybackState()
            updateNotification()
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {}
        override fun onQualityChanged(quality: com.amazonaws.ivs.player.Quality) {}
        override fun onDurationChanged(duration: Long) {}
        override fun onError(exception: PlayerException) {}
        override fun onCue(cue: Cue) {}
        override fun onRebuffering() {}
        override fun onSeekCompleted(position: Long) {}
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = (application as LimeApp).playerManager

        createNotificationChannel()

        mediaSession = MediaSession(this, "LimePlayerSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { playerManager.userPlay() }
                override fun onPause() { playerManager.userPause() }
                override fun onStop() {
                    playerManager.stop()
                    stopSelf()
                }
            })
            isActive = true
        }

        acquireWakeLocks()
        playerManager.player.addListener(playerListener)

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playerManager.userPlay()
            ACTION_PAUSE -> playerManager.userPause()
            ACTION_STOP -> {
                playerManager.stop()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                title = intent?.getStringExtra(EXTRA_TITLE) ?: "Live"
                artist = intent?.getStringExtra(EXTRA_ARTIST) ?: "Lime Stream"
                val avatarUrl = intent?.getStringExtra(EXTRA_AVATAR_URL)
                if (avatarUrl != null) loadArtwork(avatarUrl)
                updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        playerManager.player.removeListener(playerListener)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)

        mediaSession.isActive = false
        mediaSession.setMetadata(null)
        mediaSession.release()
        
        releaseWakeLocks()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun loadArtwork(url: String) {
        scope.launch {
            val request = ImageRequest.Builder(this@PlayerService)
                .data(url)
                .allowHardware(false)
                .build()
            val result = Coil.imageLoader(this@PlayerService).execute(request)
            if (result is SuccessResult) {
                artBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                updateMetadata()
                updateNotification()
            }
        }
    }

    private fun updatePlaybackState() {
        val state = when {
            isPlaying -> PlaybackState.STATE_PLAYING
            isBuffering -> PlaybackState.STATE_BUFFERING
            else -> PlaybackState.STATE_PAUSED
        }
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
    }

    private fun updateMetadata() {
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
        artBitmap?.let {
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
        }
        mediaSession.setMetadata(builder.build())
    }

    private fun updateNotification() {
        updateMetadata()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlayerService::class.java).setAction(ACTION_PLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlayerService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PlayerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val action = if (isPlaying) {
            Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                "Pause", pauseIntent
            ).build()
        } else {
            Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_play),
                "Play", playIntent
            ).build()
        }
        val stopAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Stop", stopIntent
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setLargeIcon(artBitmap)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(action)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun acquireWakeLocks() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "lime::PlayerWakeLock").apply {
            acquire()
        }
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "lime::PlayerWifiLock").apply {
            acquire()
        }
    }

    private fun releaseWakeLocks() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        runCatching { if (wifiLock?.isHeld == true) wifiLock?.release() }
    }
}
