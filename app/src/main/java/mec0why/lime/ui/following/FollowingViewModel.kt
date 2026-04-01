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
                loadFollowedChannels(slugs, showLoading = true)
            }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        val slugs = followingRepository.followedChannels.value
        loadFollowedChannels(slugs, showLoading)
    }

    private fun loadFollowedChannels(slugs: Set<String>, showLoading: Boolean = true) {
        if (slugs.isEmpty()) {
            _followedChannels.value = emptyList()
            return
        }
        viewModelScope.launch {
            if (showLoading) {
                _isLoading.value = true
            }
            _error.value = null
            try {
                val results = slugs.map { slug ->
                    async { kickRepository.getChannel(slug).getOrNull() }
                }.awaitAll().filterNotNull()
                _followedChannels.value = results.sortedWith(
                    compareByDescending<ChannelResponse> { it.livestream != null }
                        .thenByDescending { it.livestream?.viewerCount ?: 0 }
                )
            } catch (_: Exception) {
                _error.value = "Failed to load followed channels"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
