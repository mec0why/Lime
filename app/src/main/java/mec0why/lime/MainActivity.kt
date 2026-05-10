package mec0why.lime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mec0why.lime.player.PlayerService
import mec0why.lime.player.PlayerState
import mec0why.lime.ui.channel.ChannelScreen
import mec0why.lime.ui.channel.ChannelViewModel
import mec0why.lime.ui.components.MiniPlayerOverlay
import mec0why.lime.ui.following.FollowingScreen
import mec0why.lime.ui.home.HomeScreen
import mec0why.lime.ui.search.SearchScreen
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.DarkSurface
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.LimeTheme
import mec0why.lime.ui.theme.TextSecondary
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        lifecycleScope.launch {
            delay(1200)
            keepSplash = false
        }

        setContent {
            LimeTheme {
                LimeNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "https" && data.host == "mec0why.github.io" && data.path?.startsWith("/lime/callback") == true) {
            val code = data.getQueryParameter("code") ?: return
            val state = data.getQueryParameter("state") ?: return
            val authManager = (applicationContext as LimeApp).authManager
            lifecycleScope.launch {
                authManager.exchangeCode(code, state)
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun LimeNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val playerManager = remember { (context.applicationContext as LimeApp).playerManager }
    val activeSlug by playerManager.currentSlug.collectAsState()
    val playerState by playerManager.playerState.collectAsState()

    val channelViewModel: ChannelViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    val channel by channelViewModel.channel.collectAsState()
    val isStreamOffline = channel != null && channel?.livestream == null

    val bottomItems = listOf(
        BottomNavItem("Streams", Screen.Home.route, Icons.Filled.ArrowUpward, Icons.Outlined.ArrowUpward),
        BottomNavItem("Following", Screen.Following.route, Icons.Filled.Favorite, Icons.Outlined.Favorite),
        BottomNavItem("Search", Screen.Search.route, Icons.Filled.Search, Icons.Outlined.Search)
    )

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeightPx = constraints.maxHeight.toFloat()
        var bottomPaddingPx by remember { mutableFloatStateOf(0f) }

        val density = LocalDensity.current
        var bottomPaddingDp by remember { mutableStateOf(0.dp) }

        val scope = rememberCoroutineScope()
        val miniPlayerHeightPx = with(density) { 72.dp.toPx() }
        val maxOffset = (fullHeightPx - miniPlayerHeightPx - bottomPaddingPx).coerceAtLeast(0f)
        
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val currentTarget = when (playerState) {
            PlayerState.EXPANDED -> 0f
            PlayerState.MINIMIZED -> maxOffset
            else -> fullHeightPx
        }
        
        val animatedOffset by animateFloatAsState(
            targetValue = (currentTarget + dragOffset).coerceIn(0f, fullHeightPx),
            label = "player_offset"
        )
        
        val progress = if (maxOffset > 0f) (animatedOffset / maxOffset).coerceIn(0f, 1f) else 0f

        val currentPlayerHeightPx = ((1f - progress) * fullHeightPx + progress * miniPlayerHeightPx)
        val currentPlayerHeightDp = with(density) { currentPlayerHeightPx.toDp() }
        val fullHeightDp = with(density) { fullHeightPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPaddingDp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        ),
                        onStreamClick = { slug ->
                            playerManager.currentSlug.value = slug
                            channelViewModel.setSlug(slug)
                            playerManager.playerState.value = PlayerState.EXPANDED
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        ),
                        onChannelClick = { slug ->
                            playerManager.currentSlug.value = slug
                            channelViewModel.setSlug(slug)
                            playerManager.playerState.value = PlayerState.EXPANDED
                        }
                    )
                }

                composable(Screen.Following.route) {
                    FollowingScreen(
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        ),
                        onChannelClick = { slug ->
                            playerManager.currentSlug.value = slug
                            channelViewModel.setSlug(slug)
                            playerManager.playerState.value = PlayerState.EXPANDED
                        }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = playerState != PlayerState.HIDDEN && activeSlug != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.zIndex(if (playerState == PlayerState.EXPANDED) 10f else 1f)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, animatedOffset.roundToInt()) }
                    .fillMaxWidth()
                    .height(currentPlayerHeightDp)
                    .clipToBounds()
                    .background(DarkBackground.copy(alpha = 1f - progress))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val totalOffset = currentTarget + dragOffset
                                val closeThreshold = 100f
                                
                                val shouldCloseEntirely = isStreamOffline || (playerManager.playerState.value == PlayerState.MINIMIZED && dragOffset > closeThreshold)
                                
                                if (shouldCloseEntirely) {
                                    playerManager.playerState.value = PlayerState.HIDDEN
                                    scope.launch {
                                        kotlinx.coroutines.delay(300)
                                        playerManager.stop()
                                        context.stopService(android.content.Intent(context, PlayerService::class.java))
                                        dragOffset = 0f
                                    }
                                } else {
                                    if (totalOffset > maxOffset * 0.3f) {
                                        playerManager.playerState.value = PlayerState.MINIMIZED
                                    } else {
                                        playerManager.playerState.value = PlayerState.EXPANDED
                                    }
                                    dragOffset = 0f
                                }
                            },
                            onDragCancel = { dragOffset = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(fullHeightDp)
                        .graphicsLayer { alpha = 1f - progress }
                ) {
                    ChannelScreen(
                        viewModel = channelViewModel,
                        isSurfaceOwner = progress <= 0.5f,
                        onBack = {
                            if (isStreamOffline) {
                                playerManager.playerState.value = PlayerState.HIDDEN
                                scope.launch {
                                    kotlinx.coroutines.delay(300)
                                    playerManager.stop()
                                }
                            } else {
                                playerManager.playerState.value = PlayerState.MINIMIZED
                            }
                        },
                        onClose = {
                            playerManager.playerState.value = PlayerState.HIDDEN
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                playerManager.stop()
                                context.stopService(android.content.Intent(context, PlayerService::class.java))
                            }
                        }
                    )
                }
                
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = progress }
                    ) {
                        MiniPlayerOverlay(
                            modifier = Modifier.fillMaxWidth(),
                            isSurfaceOwner = progress > 0.5f,
                            onNavigateToChannel = { 
                                playerManager.playerState.value = PlayerState.EXPANDED
                            }
                        )
                    }
                }
            }
        }

        if (showBottomBar) {
            val navBarOffset = (1f - progress) * bottomPaddingPx
            
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .zIndex(5f)
                    .graphicsLayer { translationY = navBarOffset }
                    .onGloballyPositioned { 
                        bottomPaddingPx = it.size.height.toFloat()
                        bottomPaddingDp = with(density) { it.size.height.toDp() }
                    }
            ) {
                NavigationBar(
                    containerColor = DarkSurface,
                ) {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LimeGreen,
                                selectedTextColor = LimeGreen,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = LimeGreen.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    }
}