package mec0why.lime.ui.channel

import android.graphics.Color.parseColor
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import mec0why.lime.data.model.ChatMessageEvent
import mec0why.lime.ui.theme.TextPrimary

@Composable
fun ChatSection(
    chatroomId: Int,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(chatroomId) {
        viewModel.connect(chatroomId)
    }

    val listState = rememberLazyListState()
    val isBottomVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 5
        }
    }
    
    var unreadCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            if (isBottomVisible) {
                listState.animateScrollToItem(0)
                unreadCount = 0
            } else {
                unreadCount++
            }
        }
    }

    LaunchedEffect(isBottomVisible) {
        if (isBottomVisible) {
            unreadCount = 0
        }
    }

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message)
            }
        }

        AnimatedVisibility(
            visible = !isBottomVisible,
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
                            listState.animateScrollToItem(0)
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
fun ChatMessageItem(message: ChatMessageEvent) {
    val senderColorStr = message.sender?.identity?.color
    val nameColor = try {
        if (!senderColorStr.isNullOrBlank()) {
            Color(parseColor(senderColorStr))
        } else {
            Color.White
        }
    } catch (e: Exception) {
        Color.White
    }

    val content = message.content
    val emoteRegex = Regex("\\[emote:(\\d+):([^\\]]+)\\]")
    val matches = emoteRegex.findAll(content).toList()

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                    append(message.sender?.username ?: "Unknown")
                }
                append(": ")
                withStyle(style = SpanStyle(color = TextPrimary)) {
                    if (matches.isEmpty()) {
                        append(content)
                    } else {
                        var lastIndex = 0
                        for (match in matches) {
                            if (match.range.first > lastIndex) {
                                append(content.substring(lastIndex, match.range.first))
                            }
                            val emoteId = match.groupValues[1]
                            val emoteName = match.groupValues[2]
                            appendInlineContent("emote_$emoteId", "[$emoteName]")
                            lastIndex = match.range.last + 1
                        }
                        if (lastIndex < content.length) {
                            append(content.substring(lastIndex))
                        }
                    }
                }
            },
            inlineContent = matches.associate { match ->
                val emoteId = match.groupValues[1]
                val emoteName = match.groupValues[2]
                "emote_$emoteId" to InlineTextContent(
                    Placeholder(
                        width = 2.em,
                        height = 2.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    val imageUrl = "https://files.kick.com/emotes/$emoteId/fullsize"
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .build(),
                        contentDescription = emoteName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
