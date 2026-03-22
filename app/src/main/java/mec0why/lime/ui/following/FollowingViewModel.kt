package mec0why.lime.ui.following

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.data.model.ChannelResponse

class FollowingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LimeApp
    private val kickRepository = app.repository
    private val followingRepository = app.followingRepository

    private val _followedChannels = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val followedChannels: StateFlow<List<ChannelResponse>> = _followedChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            followingRepository.followedChannels.collect { slugs ->
                loadFollowedChannels(slugs)
            }
        }
    }

    fun refresh() {
        val slugs = followingRepository.followedChannels.value
        loadFollowedChannels(slugs)
    }

    private fun loadFollowedChannels(slugs: Set<String>) {
        if (slugs.isEmpty()) {
            _followedChannels.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val results = slugs.map { slug ->
                    async { kickRepository.getChannel(slug).getOrNull() }
                }.awaitAll().filterNotNull()
                _followedChannels.value = results.sortedByDescending { it.livestream != null }
            } catch (_: Exception) {
                _error.value = "Failed to load followed channels"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
