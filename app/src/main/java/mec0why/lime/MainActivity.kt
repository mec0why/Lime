package mec0why.lime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mec0why.lime.ui.search.SearchScreen
import mec0why.lime.ui.channel.ChannelScreen
import mec0why.lime.ui.home.HomeScreen
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.DarkSurface
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.LimeTheme
import mec0why.lime.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LimeTheme {
                LimeNavigation()
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

    val bottomItems = listOf(
        BottomNavItem("Streams", Screen.Home.route, Icons.Filled.LiveTv, Icons.Outlined.LiveTv),
        BottomNavItem("Search", Screen.Search.route, Icons.Filled.Search, Icons.Outlined.Search)
    )

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = DarkSurface) {
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
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStreamClick = { slug ->
                        navController.navigate(Screen.Channel.createRoute(slug))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onChannelClick = { slug ->
                        navController.navigate(Screen.Channel.createRoute(slug))
                    }
                )
            }

            composable(
                route = Screen.Channel.route,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) {
                ChannelScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}