package mec0why.lime.ui.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mec0why.lime.data.model.ChannelResponse
import mec0why.lime.ui.theme.DarkBackground
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.LiveRed
import mec0why.lime.ui.theme.TextPrimary
import mec0why.lime.ui.theme.TextSecondary
import mec0why.lime.ui.theme.TextTertiary

@Composable
fun StreamDetailsSection(
    channel: ChannelResponse?,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showControls || channel?.livestream == null,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
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
}

private fun formatViewers(count: Int): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}
