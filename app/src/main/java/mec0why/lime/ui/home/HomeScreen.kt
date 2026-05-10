package mec0why.lime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import mec0why.lime.ui.components.StreamCard
import mec0why.lime.ui.theme.LimeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStreamClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val livestreams by viewModel.livestreams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val authManager =
        androidx.compose.runtime.remember { (context.applicationContext as mec0why.lime.LimeApp).authManager }
    val isLoggedIn by authManager.isLoggedIn.collectAsState()
    val currentUserProfile by authManager.currentUser.collectAsState()

    var showLogoutDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lime",
                color = LimeGreen,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            if (isLoggedIn) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Gray, CircleShape)
                            .clip(CircleShape)
                            .clickable { showLogoutDialog = true }
                    ) {
                        if (currentUserProfile?.profilePicture != null) {
                            AsyncImage(
                                model = currentUserProfile?.profilePicture,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.align(Alignment.Center).padding(8.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                androidx.compose.material3.TextButton(onClick = {
                    val url = authManager.buildAuthUrl()
                    androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                        .launchUrl(context, android.net.Uri.parse(url))
                }) {
                    androidx.compose.material3.Text(
                        text = "Log in",
                        color = LimeGreen
                    )
                }
            }
        }

        if (showLogoutDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log out", color = Color.White) },
                text = { Text("Are you sure you want to log out?", color = Color.LightGray) },
                containerColor = mec0why.lime.ui.theme.DarkSurface,
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showLogoutDialog = false
                        authManager.logout()
                    }) {
                        Text("Log out", color = LimeGreen)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        PullToRefreshBox(
            isRefreshing = isLoading && livestreams.isNotEmpty(),
            onRefresh = viewModel::loadLivestreams,
            modifier = Modifier.weight(1f)
        ) {
            when {
                error != null && livestreams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "An error occurred",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                isLoading && livestreams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = livestreams,
                            key = { "${it.slug}_${it.channelId}" }
                        ) { livestream ->
                            StreamCard(
                                livestream = livestream,
                                onClick = { onStreamClick(livestream.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}
