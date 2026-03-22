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
import mec0why.lime.ui.theme.TextPrimary

@Composable
fun StreamDetailsSection(
    channel: ChannelResponse?,
    showControls: Boolean,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channel?.user?.username ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )

                        if (channel?.verified == true) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp)
                                    .background(LimeGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkBackground
                                )
                            }
                        }
                    }

                    channel?.livestream?.let { live ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "● LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = LimeGreen
                            )
                            if (live.categories.isNotEmpty()) {
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
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                androidx.compose.material3.TextButton(
                    onClick = onFollowToggle,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (isFollowing) TextPrimary else DarkBackground,
                        containerColor = if (isFollowing) DarkBackground else LimeGreen
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (isFollowing) "Unfollow" else "Follow")
                }
            }

            channel?.livestream?.let { live ->
                Text(
                    text = live.sessionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
