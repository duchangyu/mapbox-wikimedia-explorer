package com.example.awsome_car.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.presentation.MapUiState

@Composable
fun MainScreen(
    uiState: MapUiState,
    onSearch: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleList: () -> Unit,
    onImageSelected: (WikiImage) -> Unit,
    onDismissSelectedImage: () -> Unit,
    onZoomToFit: () -> Unit,
    onLoadMore: () -> Unit,
    onDismissError: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.searchQuery,
            onQueryChanged = onQueryChanged,
            onSearch = onSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        MapActionButtons(
            onZoomToFit = onZoomToFit,
            onToggleList = onToggleList,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 88.dp, end = 16.dp)
        )

        uiState.selectedImage?.let { image ->
            ImagePopup(
                image = image,
                onClose = onDismissSelectedImage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            )
        }

        if (uiState.isListVisible) {
            ImageListOverlay(
                images = uiState.images,
                isLoadingMore = uiState.isLoadingMore,
                hasMoreResults = uiState.hasMoreResults,
                onImageSelected = onImageSelected,
                onClose = onToggleList,
                onLoadMore = onLoadMore
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Search Result") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = onDismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun MapActionButtons(
    onZoomToFit: () -> Unit,
    onToggleList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FloatingActionButton(
            onClick = onZoomToFit,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(Icons.Default.Place, contentDescription = "Zoom to Fit")
        }
        FloatingActionButton(onClick = onToggleList) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Open List")
        }
    }
}
