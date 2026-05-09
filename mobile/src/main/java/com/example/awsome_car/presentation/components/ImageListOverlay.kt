package com.example.awsome_car.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.awsome_car.domain.model.WikiImage

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
                items(images, key = WikiImage::id) { image ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = image.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text("Lat: ${image.lat}, Lon: ${image.lon}")
                        },
                        leadingContent = {
                            WikimediaThumbnail(
                                image = image,
                                contentDescription = image.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(4.dp))
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
