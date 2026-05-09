package com.example.awsome_car.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.content.res.AppCompatResources
import com.example.awsome_car.R
import com.example.awsome_car.domain.model.WikiImage
import com.google.gson.JsonObject
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class ImageAnnotationController(
    private val context: Context,
    mapView: MapView,
    private val onImageClicked: (WikiImage) -> Unit
) {
    private val annotationsByImageId = mutableMapOf<Int, PointAnnotation>()
    private var imagesById = emptyMap<Int, WikiImage>()
    private val markerBitmap by lazy { bitmapFromDrawableRes(R.drawable.ic_launcher_foreground) }

    private val annotationManager: PointAnnotationManager =
        mapView.annotations.createPointAnnotationManager().apply {
            addClickListener(OnPointAnnotationClickListener { annotation ->
                annotation.imageId()?.let(imagesById::get)?.let(onImageClicked)
                true
            })
        }

    fun sync(images: List<WikiImage>) {
        imagesById = images.associateBy(WikiImage::id)

        val newImageIds = imagesById.keys
        val removedImageIds = annotationsByImageId.keys - newImageIds
        removedImageIds.forEach { imageId ->
            annotationsByImageId.remove(imageId)?.let(annotationManager::delete)
        }

        images.forEach { image ->
            annotationsByImageId.getOrPut(image.id) { createAnnotation(image) }
        }
    }

    fun clear() {
        annotationsByImageId.values.forEach(annotationManager::delete)
        annotationsByImageId.clear()
        imagesById = emptyMap()
    }

    private fun createAnnotation(image: WikiImage): PointAnnotation {
        val options = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(image.lon, image.lat))
            .withData(JsonObject().apply { addProperty(IMAGE_ID_KEY, image.id) })

        markerBitmap?.let(options::withIconImage)
        return annotationManager.create(options)
    }

    private fun PointAnnotation.imageId(): Int? = runCatching {
        getData()?.asJsonObject?.get(IMAGE_ID_KEY)?.asInt
    }.getOrNull()

    private fun bitmapFromDrawableRes(resourceId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resourceId) ?: return null
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_MARKER_SIZE_PX
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_MARKER_SIZE_PX
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private companion object {
        const val IMAGE_ID_KEY = "imageId"
        const val DEFAULT_MARKER_SIZE_PX = 48
    }
}
