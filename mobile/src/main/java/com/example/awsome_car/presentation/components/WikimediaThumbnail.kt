package com.example.awsome_car.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.awsome_car.R
import com.example.awsome_car.data.remote.WikimediaHttpConfig
import com.example.awsome_car.domain.model.WikiImage

@Composable
fun WikimediaThumbnail(
    image: WikiImage,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageUrl = image.thumbUrl.ifBlank { image.fullUrl }
    val request = remember(context, imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl.ifBlank { null })
            .crossfade(true)
            .setHeader("User-Agent", WikimediaHttpConfig.USER_AGENT)
            .setHeader("Accept", "image/*,*/*;q=0.8")
            .build()
    }
    val fallbackPainter = painterResource(R.drawable.ic_launcher_foreground)

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
