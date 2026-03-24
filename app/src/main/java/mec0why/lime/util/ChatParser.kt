package mec0why.lime.util

import mec0why.lime.data.model.SevenTVEmote

sealed class ChatToken {
    data class Text(val text: String) : ChatToken()
    data class Link(val url: String) : ChatToken()
    data class Mention(val username: String) : ChatToken()
    data class KickEmote(val id: String, val name: String) : ChatToken()
    data class SevenTvEmoteToken(
        val emotes: List<SevenTVEmote>
    ) : ChatToken()
}

object ChatParser {
    
    private val emoteRegex = Regex("\\[emote:(\\d+):([^\\]]+)\\]")
    private val linkRegex = Regex("(https?://\\S+)")
    private val whitespaceRegex = Regex("(\\s+)")
    private val wordRegex = Regex("\\S+")

    fun parseMessage(
        content: String,
        sevenTvMap: Map<String, SevenTVEmote>
    ): List<ChatToken> {
        val tokens = mutableListOf<ChatToken>()
        
        val systemMatches = (emoteRegex.findAll(content).map { it.range.first to it } +
                linkRegex.findAll(content).map { it.range.first to it })
                .sortedBy { it.first }
                .map { it.second }
                .toList()

        var lastIndex = 0
        for (match in systemMatches) {
            if (match.range.first > lastIndex) {
                val textPart = content.substring(lastIndex, match.range.first)
                parse7TvAndText(textPart, sevenTvMap, tokens)
            }
            if (match.value.startsWith("[emote")) {
                tokens.add(ChatToken.KickEmote(match.groupValues[1], match.groupValues[2]))
            } else {
                tokens.add(ChatToken.Link(match.value))
            }
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < content.length) {
            parse7TvAndText(content.substring(lastIndex), sevenTvMap, tokens)
        }
        
        return tokens
    }

    private fun parse7TvAndText(
        text: String,
        sevenTvMap: Map<String, SevenTVEmote>,
        tokens: MutableList<ChatToken>
    ) {
        val parts = text.split(whitespaceRegex)
        var currentIndex = 0
        val words = wordRegex.findAll(text)
        
        for (wordMatch in words) {
            if (wordMatch.range.first > currentIndex) {
                addTextToken(tokens, text.substring(currentIndex, wordMatch.range.first))
            }
            
            val word = wordMatch.value
            val sevenTvEmote = sevenTvMap[word]
            
            if (sevenTvEmote != null) {
                val isZeroWidth = (sevenTvEmote.flags and 256) != 0
                if (isZeroWidth && tokens.isNotEmpty() && tokens.last() is ChatToken.SevenTvEmoteToken) {
                    val prev = tokens.removeLast() as ChatToken.SevenTvEmoteToken
                    tokens.add(ChatToken.SevenTvEmoteToken(prev.emotes + sevenTvEmote))
                } else if (isZeroWidth && tokens.isNotEmpty() && tokens.last() is ChatToken.KickEmote) {
                    tokens.add(ChatToken.SevenTvEmoteToken(listOf(sevenTvEmote)))
                } else {
                    tokens.add(ChatToken.SevenTvEmoteToken(listOf(sevenTvEmote)))
                }
            } else if (word.startsWith("@") && word.length > 1) {
                val cleanWord = word.trimEnd(',', '.', ':', ';', '!')
                val trailing = word.substring(cleanWord.length)
                tokens.add(ChatToken.Mention(cleanWord.removePrefix("@")))
                if (trailing.isNotEmpty()) {
                    addTextToken(tokens, trailing)
                }
            } else {
                addTextToken(tokens, word)
            }
            
            currentIndex = wordMatch.range.last + 1
        }
        if (currentIndex < text.length) {
            addTextToken(tokens, text.substring(currentIndex))
        }
    }
    
    private fun addTextToken(tokens: MutableList<ChatToken>, text: String) {
        if (tokens.isNotEmpty() && tokens.last() is ChatToken.Text) {
            val prev = tokens.removeLast() as ChatToken.Text
            tokens.add(ChatToken.Text(prev.text + text))
        } else {
            tokens.add(ChatToken.Text(text))
        }
    }
}
