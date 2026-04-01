package mec0why.lime.ui.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import mec0why.lime.data.model.ChatMessageEvent
import mec0why.lime.data.model.SevenTVEmote
import mec0why.lime.ui.theme.LimeGreen
import mec0why.lime.ui.theme.TextPrimary
import mec0why.lime.ui.theme.TextSecondary
import mec0why.lime.util.ChatParser
import mec0why.lime.util.ChatToken
import java.util.UUID

@Composable
fun ChatSection(
    chatroomId: Int,
    modifier: Modifier = Modifier,
    kickUserId: Int? = null,
    subscriberBadges: List<mec0why.lime.data.model.SubscriberBadge> = emptyList(),
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val sevenTvEmotes by viewModel.sevenTvEmotes.collectAsState()
    val userColors by viewModel.userColors.collectAsState()

    LaunchedEffect(chatroomId, kickUserId) {
        viewModel.connect(chatroomId, kickUserId)
    }

    val listState = rememberLazyListState()
    var userPausedScroll by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { 
            Triple(
                listState.firstVisibleItemIndex, 
                listState.firstVisibleItemScrollOffset, 
                listState.isScrollInProgress
            ) 
        }.collect { (index, offset, isScrolling) ->
            val isScrolledUp = index > 0 || offset > 15
            
            if (isScrolling && isScrolledUp) {
                userPausedScroll = true
            } else if (!isScrolledUp) {
                userPausedScroll = false
                unreadCount = 0
            }
        }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            if (!userPausedScroll) {
                listState.scrollToItem(0)
                unreadCount = 0
            } else {
                unreadCount++
            }
        }
    }

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    sevenTvEmotes = sevenTvEmotes,
                    userColors = userColors,
                    subscriberBadges = subscriberBadges
                )
            }
        }

        AnimatedVisibility(
            visible = userPausedScroll,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        scope.launch {
                            userPausedScroll = false
                            unreadCount = 0
                            listState.scrollToItem(0)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    AnimatedVisibility(
                        visible = unreadCount > 0,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEvent,
    modifier: Modifier = Modifier,
    sevenTvEmotes: Map<String, SevenTVEmote> = emptyMap(),
    userColors: Map<String, String> = emptyMap(),
    subscriberBadges: List<mec0why.lime.data.model.SubscriberBadge> = emptyList()
) {
    val senderColorStr = message.sender?.identity?.color
    val nameColor = try {
        if (!senderColorStr.isNullOrBlank()) {
            Color(senderColorStr.toColorInt())
        } else {
            Color.White
        }
    } catch (_: Exception) {
        Color.White
    }

    val content = message.content
    val tokens = remember(content, sevenTvEmotes) {
        ChatParser.parseMessage(content, sevenTvEmotes)
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        if (message.type == "reply" && message.metadata?.originalSender != null) {
            Row(
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Reply",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Replying to @${message.metadata.originalSender.username}: ${message.metadata.originalMessage?.content ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        
        Row {
            val inlineContentMap = mutableMapOf<String, InlineTextContent>()

        Text(
            text = buildAnnotatedString {
                message.sender?.identity?.badges?.forEach { badge ->
                    val badgeId = "badge_${badge.type}_${UUID.randomUUID()}"
                    appendInlineContent(badgeId, "[${badge.type}]")
                    inlineContentMap[badgeId] = InlineTextContent(
                        Placeholder(
                            width = 1.3.em,
                            height = 1.3.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                        )
                    ) {
                        val imageUrl = when (badge.type) {
                            "subscriber" -> {
                                val sortedBadges = subscriberBadges.sortedByDescending { it.months }
                                val bestBadge = sortedBadges.firstOrNull { badge.count >= it.months }
                                bestBadge?.badgeImage?.src ?: "https://cdn.kicktalk.app/Badges/subscriber.svg"
                            }
                            "sub_gifter" -> {
                                "https://cdn.kicktalk.app/Badges/subgifter1.svg"
                            }
                            else -> {
                                "https://cdn.kicktalk.app/Badges/${badge.type}.svg"
                            }
                        }
                        
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .build(),
                            contentDescription = badge.text,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    append(" ")
                }

                withStyle(style = SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                    append(message.sender?.username ?: "Unknown")
                }
                append(": ")
                withStyle(style = SpanStyle(color = TextPrimary)) {
                    for (token in tokens) {
                        when (token) {
                            is ChatToken.Text -> {
                                append(token.text)
                            }
                            is ChatToken.Mention -> {
                                val mentionColorStr = userColors[token.username.lowercase()] ?: userColors[token.username.replace("@", "").lowercase()]
                                val mentionColor = try {
                                    if (!mentionColorStr.isNullOrBlank()) {
                                        Color(mentionColorStr.toColorInt())
                                    } else {
                                        LimeGreen
                                    }
                                } catch (_: Exception) {
                                    LimeGreen
                                }
                                withStyle(style = SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)) {
                                    append("@${token.username.replace("@", "")}")
                                }
                            }
                            is ChatToken.Link -> {
                                pushLink(
                                    androidx.compose.ui.text.LinkAnnotation.Url(
                                        url = token.url,
                                        styles = androidx.compose.ui.text.TextLinkStyles(
                                            style = SpanStyle(color = TextPrimary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                        )
                                    )
                                )
                                append(token.url)
                                pop()
                            }
                            is ChatToken.KickEmote -> {
                                val inlineId = "kick_${token.id}"
                                appendInlineContent(inlineId, "[${token.name}]")
                                inlineContentMap[inlineId] = InlineTextContent(
                                    Placeholder(
                                        width = 2.2.em,
                                        height = 2.2.em,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )
                                ) {
                                    val imageUrl = "https://files.kick.com/emotes/${token.id}/fullsize"
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageUrl)
                                            .build(),
                                        contentDescription = token.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            is ChatToken.SevenTvEmoteToken -> {
                                val baseEmote = token.emotes.first()
                                val file = baseEmote.data?.host?.files?.firstOrNull()
                                val aspectRatio = if (file != null && file.height > 0) file.width.toFloat() / file.height.toFloat() else 1.0f
                                val widthEmStr = (2.2f * aspectRatio).em

                                val inlineId = "7tv_${baseEmote.id}_${UUID.randomUUID()}"
                                appendInlineContent(inlineId, "[${baseEmote.name}]")
                                inlineContentMap[inlineId] = InlineTextContent(
                                    Placeholder(
                                        width = widthEmStr,
                                        height = 2.2.em,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        for (emote in token.emotes) {
                                            val webpFiles = emote.data?.host?.files?.filter { it.format.equals("WEBP", ignoreCase = true) }
                                            val bestFile = if (!webpFiles.isNullOrEmpty()) webpFiles.maxByOrNull { it.width } else emote.data?.host?.files?.maxByOrNull { it.width }
                                            val fileUrlName = bestFile?.name ?: "4x.webp"
                                            val imageUrl = "https://cdn.7tv.app/emote/${emote.id}/$fileUrlName"
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(imageUrl)
                                                    .build(),
                                                contentDescription = emote.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            inlineContent = inlineContentMap,
            style = MaterialTheme.typography.bodyMedium
        )
        }
    }
}
