package mec0why.lime.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowingRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lime_following", Context.MODE_PRIVATE)
    
    private val _followedChannels = MutableStateFlow<Set<String>>(emptySet())
    val followedChannels: StateFlow<Set<String>> = _followedChannels.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _followedChannels.value = prefs.getStringSet("channels", emptySet())?.toSet() ?: emptySet()
        }
    }

    fun isFollowing(slug: String): Boolean {
        return _followedChannels.value.contains(slug)
    }

    fun followChannel(slug: String) {
        val current = _followedChannels.value.toMutableSet()
        current.add(slug)
        saveChannels(current)
    }

    fun unfollowChannel(slug: String) {
        val current = _followedChannels.value.toMutableSet()
        current.remove(slug)
        saveChannels(current)
    }

    fun toggleFollow(slug: String) {
        if (isFollowing(slug)) {
            unfollowChannel(slug)
        } else {
            followChannel(slug)
        }
    }

    private fun saveChannels(channels: Set<String>) {
        _followedChannels.value = channels
        CoroutineScope(Dispatchers.IO).launch {
            prefs.edit { putStringSet("channels", channels) }
        }
    }
}
