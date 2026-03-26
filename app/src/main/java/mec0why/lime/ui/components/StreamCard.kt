package mec0why.lime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import mec0why.lime.LimeApp
import mec0why.lime.data.model.Livestream
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.TextPrimary
import mec0why.lime.ui.theme.TextSecondary

@Composable
fun StreamCard(
    livestream: Livestream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as LimeApp
    val hydrator = app.channelHydrator
    
    var state by remember(livestream.slug) {
        val (cachedName, cachedVerified, hasData) = hydrator.getImmediate(livestream.slug)
        val initialState = if (hasData) {
            livestream.copy(username = cachedName, verified = cachedVerified)
        } else {
            livestream
        }
        mutableStateOf(initialState)
    }

    if (state.streamTitle != livestream.streamTitle || 
        state.viewerCount != livestream.viewerCount || 
        state.thumbnail != livestream.thumbnail) {
        state = state.copy(
            streamTitle = livestream.streamTitle,
            viewerCount = livestream.viewerCount,
            thumbnail = livestream.thumbnail
        )
    }

    LaunchedEffect(livestream.slug) {
        hydrator.hydrate(livestream.slug)?.let { channel ->
            state = state.copy(
                username = channel.user?.username,
                verified = channel.verified
            )
        }
    }

    val cacheBuster = System.currentTimeMillis() / (1000 * 60 * 5)
    
    val thumbnailRequest = remember(state.thumbnail, cacheBuster) {
        coil.request.ImageRequest.Builder(context)
            .data(state.thumbnail.ifEmpty { null })
            .diskCacheKey("${state.thumbnail}_$cacheBuster")
            .memoryCacheKey("${state.thumbnail}_$cacheBuster")
            .crossfade(true)
            .build()
    }

    val profileRequest = remember(state.profilePicture, cacheBuster) {
        coil.request.ImageRequest.Builder(context)
            .data(state.profilePicture.ifEmpty { null })
            .diskCacheKey("${state.profilePicture}_$cacheBuster")
            .memoryCacheKey("${state.profilePicture}_$cacheBuster")
            .crossfade(true)
            .build()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box {
                SubcomposeAsyncImage(
                    model = thumbnailRequest,
                    contentDescription = state.streamTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    loading = {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).shimmerEffect())
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.DarkGray))
                    }
                )

                Badge(
                    text = "LIVE",
                    backgroundColor = LimeGreen,
                    textColor = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Badge(
                        text = "${formatViewerCount(state.viewerCount)} viewers",
                        backgroundColor = Color.Black,
                        textColor = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = profileRequest,
                    contentDescription = state.slug,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    loading = {
                        Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                    },
                    error = {
                        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
                    }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.streamTitle.ifEmpty { state.slug },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.username ?: state.slug,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.verified) {
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
                        state.category?.let {
                            Text(
                                text = it.name,
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
                }
            }
        }
    }
}

@Composable
private fun Badge(
    text: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

private fun formatViewerCount(count: Int): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}
