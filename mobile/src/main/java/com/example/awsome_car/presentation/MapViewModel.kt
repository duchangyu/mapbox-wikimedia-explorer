package com.example.awsome_car.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val images: List<WikiImage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val selectedImage: WikiImage? = null,
    val isListVisible: Boolean = false,
    val searchQuery: String = "",
    val hasMoreResults: Boolean = false,
    val nextOffset: Int? = null
)

class MapViewModel(private val repository: ImageRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    images = emptyList(),
                    hasMoreResults = false,
                    nextOffset = null
                )
            }
            try {
                val result = repository.searchImages(query)
                if (result.images.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            images = emptyList(),
                            isLoading = false,
                            errorMessage = "No geotagged images found for '$query'"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            images = result.images,
                            isLoading = false,
                            hasMoreResults = result.nextOffset != null,
                            nextOffset = result.nextOffset
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Network Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMoreResults || currentState.nextOffset == null) return

        val query = currentState.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val result = repository.searchImages(query, currentState.nextOffset)
                _uiState.update {
                    it.copy(
                        images = it.images + result.images,
                        isLoadingMore = false,
                        hasMoreResults = result.nextOffset != null,
                        nextOffset = result.nextOffset
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = "Network Error: ${e.message}"
                    )
                }
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
