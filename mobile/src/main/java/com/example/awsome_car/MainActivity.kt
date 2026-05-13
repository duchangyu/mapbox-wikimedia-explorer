package com.example.awsome_car

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import com.example.awsome_car.di.ServiceLocator
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.map.ImageAnnotationController
import com.example.awsome_car.presentation.MapViewModel
import com.example.awsome_car.presentation.MapViewModelFactory
import com.example.awsome_car.presentation.components.MainScreen
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(ServiceLocator.repository)
    }

    private lateinit var mapView: MapView
    private lateinit var imageAnnotationController: ImageAnnotationController

    override fun onCreate(savedInstanceState: Bundle?) {
        ServiceLocator.init(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS)
        imageAnnotationController = ImageAnnotationController(this, mapView) { image ->
            selectImage(image)
            zoomToImage(image)
        }

        findViewById<ComposeView>(R.id.composeView).setContent {
            val uiState by viewModel.uiState.collectAsState()

            MainScreen(
                uiState = uiState,
                onSearch = viewModel::search,
                onQueryChanged = viewModel::onSearchQueryChanged,
                onToggleList = viewModel::toggleList,
                onImageSelected = { image ->
                    selectImage(image)
                    zoomToImage(image)
                },
                onDismissSelectedImage = { selectImage(null) },
                onZoomToFit = ::zoomToFit,
                onLoadMore = viewModel::loadMore,
                onDismissError = viewModel::dismissError
            )

            LaunchedEffect(uiState.images) {
                imageAnnotationController.sync(uiState.images)
            }

            LaunchedEffect(uiState.fitBoundsRequestId) {
                if (uiState.fitBoundsRequestId > 0) {
                    zoomToFit()
                }
            }
        }
    }

    override fun onDestroy() {
        if (::imageAnnotationController.isInitialized) {
            imageAnnotationController.clear()
        }
        super.onDestroy()
    }

    private fun selectImage(image: WikiImage?) {
        viewModel.onImageSelected(image)
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
        val images = viewModel.uiState.value.images
        if (images.isEmpty()) return

        val points = images.map { Point.fromLngLat(it.lon, it.lat) }
        mapView.mapboxMap.cameraForCoordinates(
            points,
            camera = cameraOptions {  },
            EdgeInsets(100.0, 100.0, 100.0, 100.0),
            maxZoom = null,
            offset = null
        ){
            cameraPosition ->
            mapView.mapboxMap.setCamera(cameraPosition)
        }

    }
}
