package mec0why.lime.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mec0why.lime.LimeApp
import mec0why.lime.data.model.SearchChannel

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as LimeApp).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _channels = MutableStateFlow<List<SearchChannel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        
        if (query.length < 3) {
            _channels.value = emptyList()
            _error.value = null
            _isLoading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            searchChannels(query)
        }
    }

    private suspend fun searchChannels(query: String) {
        _isLoading.value = true
        _error.value = null

        repository.searchChannels(query).fold(
            onSuccess = { channels ->
                _channels.value = channels
                _isLoading.value = false
            },
            onFailure = { exception ->
                if (exception !is CancellationException) {
                    _error.value = exception.message
                }
                _isLoading.value = false
            }
        )
    }
}
