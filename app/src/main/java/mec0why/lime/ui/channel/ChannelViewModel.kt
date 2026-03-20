package mec0why.lime.ui.channel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.data.model.ChannelResponse

class ChannelViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = (application as LimeApp).repository
    private val slug: String = savedStateHandle.get<String>("slug") ?: ""

    val slugFlow: StateFlow<String> = MutableStateFlow(slug)

    private val _channel = MutableStateFlow<ChannelResponse?>(null)
    val channel: StateFlow<ChannelResponse?> = _channel

    private val _playbackUrl = MutableStateFlow<String?>(null)
    val playbackUrl: StateFlow<String?> = _playbackUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadChannel()
        startViewersPolling()
    }

    private fun startViewersPolling() {
        if (slug.isBlank()) return
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10000)
                repository.getChannel(slug).onSuccess { response ->
                    _channel.value = response
                }
            }
        }
    }

    fun loadChannel() {
        if (slug.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getChannel(slug)
                .onSuccess { response ->
                    _channel.value = response
                    _playbackUrl.value = response.playbackUrl
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }
}
