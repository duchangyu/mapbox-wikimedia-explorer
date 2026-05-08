package com.example.awsome_car.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awsome_car.data.repository.WikimediaRepository
import com.example.awsome_car.domain.model.WikiImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val images: List<WikiImage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImage: WikiImage? = null,
    val isListVisible: Boolean = false,
    val searchQuery: String = ""
)

class MapViewModel(private val repository: WikimediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, images = emptyList()) }
            try {
                val results = repository.searchImages(query)
                if (results.isEmpty()) {
                    _uiState.update { it.copy(images = results, isLoading = false, errorMessage = "No geotagged images found for '$query'") }
                } else {
                    _uiState.update { it.copy(images = results, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Network Error: ${e.message}") }
            }
        }
    }

    fun onImageSelected(image: WikiImage?) {
        _uiState.update { it.copy(selectedImage = image, isListVisible = false) }
    }

    fun toggleList() {
        _uiState.update { it.copy(isListVisible = !it.isListVisible) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
