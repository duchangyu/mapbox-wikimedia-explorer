package com.example.awsome_car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.awsome_car.di.ServiceLocator
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.presentation.MapUiState
import com.example.awsome_car.presentation.MapViewModel
import com.google.gson.JsonObject
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MapViewModel(ServiceLocator.repository) as T
            }
        }
    }

    private lateinit var mapView: MapView
    private var annotationManager: PointAnnotationManager? = null
    private val annotationMap = mutableMapOf<Int, PointAnnotation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ServiceLocator.init(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)
        annotationManager = mapView.annotations.createPointAnnotationManager().apply {
            addClickListener(OnPointAnnotationClickListener { annotation ->
                val imageId = annotation.getData()?.asJsonObject?.get("imageId")?.asInt ?: -1
                if (imageId != -1) {
                    val image = viewModel.uiState.value.images.find { it.id == imageId }
                    if (image != null) {
                        viewModel.onImageSelected(image)
                        zoomToImage(image)
                    }
                }
                true
            })
        }

        findViewById<ComposeView>(R.id.composeView).setContent {
            val uiState by viewModel.uiState.collectAsState()
            MainScreen(
                uiState = uiState,
                onSearch = viewModel::search,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onToggleList = viewModel::toggleList,
                onImageSelected = { image ->
                    viewModel.onImageSelected(image)
                    zoomToImage(image)
                },
                onZoomToFit = { zoomToFit() },
                onLoadMore = { viewModel.loadMore() }
            )

            // Sync annotations with UI state
            SyncAnnotations(uiState.images)

            if (uiState.errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = { Text("Search Result") },
                    text = { Text(uiState.errorMessage ?: "Unknown error") },
                    confirmButton = {
                        Button(onClick = { viewModel.dismissError() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun SyncAnnotations(images: List<WikiImage>) {
        annotationManager?.let { manager ->
            val currentIds = annotationMap.keys.toSet()
            val newIds = images.map { it.id }.toSet()

            // Remove annotations for images no longer present
            val toRemove = currentIds - newIds
            toRemove.forEach { id ->
                annotationMap[id]?.let { manager.delete(it) }
                annotationMap.remove(id)
            }

            // Add annotations for new images only
            val markerBitmap = bitmapFromDrawableRes(R.drawable.ic_launcher_foreground)
            images.forEach { image ->
                if (!annotationMap.containsKey(image.id)) {
                    val options = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(image.lon, image.lat))
                        .withData(JsonObject().apply {
                            addProperty("imageId", image.id)
                        })

                    if (markerBitmap != null) {
                        options.withIconImage(markerBitmap)
                    }
                    val annotation = manager.create(options)
                    annotationMap[image.id] = annotation
                }
            }
        }
    }

    private fun bitmapFromDrawableRes(resourceId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(this, resourceId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun zoomToImage(image: WikiImage) {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(image.lon, image.lat))
                .zoom(14.0)
                .build()
        )
    }

    private fun zoomToFit() {
        val uiState = viewModel.uiState.value
        if (uiState.images.isEmpty()) return

        val points = uiState.images.map { Point.fromLngLat(it.lon, it.lat) }
        val camera = mapView.mapboxMap.cameraForCoordinates(points, EdgeInsets(100.0, 100.0, 100.0, 100.0))
        mapView.mapboxMap.setCamera(camera)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MapUiState,
    onSearch: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleList: () -> Unit,
    onImageSelected: (WikiImage) -> Unit,
    onZoomToFit: () -> Unit,
    onLoadMore: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Search Bar Overlay
        SearchBar(
            query = uiState.searchQuery,
            onQueryChanged = onQueryChanged,
            onSearch = onSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(onClick = onZoomToFit, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.Place, contentDescription = "Zoom to Fit")
            }
            FloatingActionButton(onClick = onToggleList) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Open List")
            }
        }

        // Selected Image Popup
        uiState.selectedImage?.let { image ->
            ImagePopup(
                image = image,
                onClose = { /* Could be handled by VM */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            )
        }

        // Image List Overlay
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search Wikimedia Commons...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun ImagePopup(image: WikiImage, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = image.thumbUrl,
                    contentDescription = image.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { println("Image load error: ${it.result.throwable}") },
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = image.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(text = image.description ?: "No description", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text(text = "Lat: ${image.lat}, Lon: ${image.lon}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ImageListOverlay(
    images: List<WikiImage>,
    isLoadingMore: Boolean,
    hasMoreResults: Boolean,
    onImageSelected: (WikiImage) -> Unit,
    onClose: () -> Unit,
    onLoadMore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Results (${images.size})", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onClose) { Text("Close") }
            }
            LazyColumn {
                items(images) { image ->
                    ListItem(
                        headlineContent = { Text(image.title) },
                        supportingContent = { Text(image.description ?: "") },
                        leadingContent = {
                            AsyncImage(
                                model = image.thumbUrl,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        modifier = Modifier.clickable { onImageSelected(image) }
                    )
                }
                if (hasMoreResults || isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator()
                            } else {
                                Button(onClick = onLoadMore) {
                                    Text("Load More")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
