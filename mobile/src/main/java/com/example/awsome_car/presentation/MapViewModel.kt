package com.example.awsome_car.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awsome_car.domain.model.PagedResult
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val images: List<WikiImage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val selectedImage: WikiImage? = null,
    val isListVisible: Boolean = false,
    val searchQuery: String = "",
    val hasMoreResults: Boolean = false,
    val nextOffset: Int? = null,
    val fitBoundsRequestId: Long = 0L
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: ImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.prepareForSearch() }
            try {
                updateWithSearchResult(query, repository.searchImages(query))
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.networkErrorMessage()) }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        val nextOffset = currentState.nextOffset
        if (!currentState.canLoadMore(nextOffset)) return

        val query = currentState.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val result = repository.searchImages(query, nextOffset)
                _uiState.update { it.append(result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = e.networkErrorMessage()) }
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

    private fun updateWithSearchResult(query: String, result: PagedResult) {
        _uiState.update { state ->
            if (result.images.isEmpty()) {
                state.copy(
                    images = emptyList(),
                    isLoading = false,
                    errorMessage = "No geotagged images found for '$query'"
                )
            } else {
                state.copy(
                    images = result.images,
                    isLoading = false,
                    hasMoreResults = result.hasMoreResults,
                    nextOffset = result.nextOffset,
                    fitBoundsRequestId = state.fitBoundsRequestId + 1
                )
            }
        }
    }

    private fun MapUiState.prepareForSearch(): MapUiState = copy(
        isLoading = true,
        errorMessage = null,
        images = emptyList(),
        selectedImage = null,
        isListVisible = false,
        hasMoreResults = false,
        nextOffset = null
    )

    private fun MapUiState.append(result: PagedResult): MapUiState = copy(
        images = images + result.images,
        isLoadingMore = false,
        hasMoreResults = result.hasMoreResults,
        nextOffset = result.nextOffset
    )

    private fun MapUiState.canLoadMore(nextOffset: Int?): Boolean =
        !isLoadingMore && hasMoreResults && nextOffset != null

    private val PagedResult.hasMoreResults: Boolean
        get() = nextOffset != null

    private fun Exception.networkErrorMessage(): String = "Network Error: $message"
}
