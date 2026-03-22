package mec0why.lime.ui.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import mec0why.lime.data.model.ChannelResponse
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.DarkSurfaceVariant
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.TextPrimary
import mec0why.lime.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = viewModel()
) {
    val channels by viewModel.followedChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Following",
            color = LimeGreen,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        PullToRefreshBox(
            isRefreshing = isLoading && channels.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f)
        ) {
            when {
                error != null && channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = error ?: "An error occurred", color = MaterialTheme.colorScheme.error)
                    }
                }
                isLoading && channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LimeGreen)
                    }
                }
                channels.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "You are not following any channels yet.", color = TextSecondary)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(items = channels, key = { it.slug }) { channel ->
                            FollowingChannelItem(
                                channel = channel,
                                onClick = { onChannelClick(channel.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowingChannelItem(
    channel: ChannelResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = channel.user?.profilePic,
            contentDescription = channel.slug,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            val isLive = channel.livestream != null && channel.livestream.isLive
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = channel.user?.username ?: channel.slug,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.verified) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(LimeGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = DarkBackground
                        )
                    }
                }
                
                val category = channel.livestream?.categories?.firstOrNull()?.name ?: channel.recentCategories.firstOrNull()?.name
                if (category != null && isLive) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeGreen,
                        modifier = Modifier
                            .background(LimeGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            val title = channel.livestream?.sessionTitle
            if (!title.isNullOrEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isLive) {
                    Text(
                        text = "● LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeGreen
                    )
                    Text(
                        text = "${formatViewerCount(channel.livestream.viewerCount)} viewers",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatViewerCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}
