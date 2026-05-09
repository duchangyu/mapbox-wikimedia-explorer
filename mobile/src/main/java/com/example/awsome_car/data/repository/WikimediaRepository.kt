package com.example.awsome_car.data.repository

import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.data.remote.model.Coordinate
import com.example.awsome_car.data.remote.model.ExtMetadata
import com.example.awsome_car.data.remote.model.Page
import com.example.awsome_car.domain.model.PagedResult
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikimediaRepository(private val apiService: WikimediaApiService) : ImageRepository {

    override suspend fun searchImages(query: String, offset: Int?): PagedResult = withContext(Dispatchers.IO) {
        val response = apiService.searchImages(query, offset)
        PagedResult(
            images = response.query?.pages?.values.orEmpty().mapNotNull { it.toWikiImage() },
            nextOffset = response.continueData?.gsroffset
        )
    }

    private fun Page.toWikiImage(): WikiImage? {
        val info = imageinfo?.firstOrNull() ?: return null
        val coordinate = coordinate() ?: return null
        val metadata = info.extmetadata

        return WikiImage(
            id = pageid,
            title = title,
            thumbUrl = info.thumburl.orEmpty(),
            fullUrl = info.url.orEmpty(),
            lat = coordinate.lat,
            lon = coordinate.lon,
            description = metadata?.description?.value,
            date = metadata?.dateTime?.value,
            artist = metadata?.artist?.value,
            license = metadata?.license?.value
        )
    }

    private fun Page.coordinate(): Coordinate? =
        coordinates?.firstOrNull() ?: imageinfo?.firstOrNull()?.extmetadata?.coordinate()

    private fun ExtMetadata.coordinate(): Coordinate? {
        val lat = gpsLatitude?.value?.toDoubleOrNull()
        val lon = gpsLongitude?.value?.toDoubleOrNull()
        return if (lat != null && lon != null) Coordinate(lat, lon) else null
    }
}
