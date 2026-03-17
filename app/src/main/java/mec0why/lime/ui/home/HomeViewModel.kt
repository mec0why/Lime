package mec0why.lime.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.data.model.Livestream

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as LimeApp).repository

    private val _livestreams = MutableStateFlow<List<Livestream>>(emptyList())
    val livestreams: StateFlow<List<Livestream>> = _livestreams

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadLivestreams()
    }

    fun loadLivestreams() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getLivestreams()
                .onSuccess { _livestreams.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
