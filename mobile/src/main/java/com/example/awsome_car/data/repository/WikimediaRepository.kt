package com.example.awsome_car.data.repository

import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.data.remote.model.Coordinate
import com.example.awsome_car.domain.model.PagedResult
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikimediaRepository(private val apiService: WikimediaApiService) : ImageRepository {

    override suspend fun searchImages(query: String, offset: Int?): PagedResult = withContext(Dispatchers.IO) {
        val response = apiService.searchImages(query, offset)

        val images = response.query?.pages?.values?.mapNotNull { page ->
            // Try page-level coordinates first, then fall back to extmetadata GPS
            val pageCoord = page.coordinates?.firstOrNull()
            val info = page.imageinfo?.firstOrNull()
            val extMeta = info?.extmetadata

            val coord = if (pageCoord != null) {
                pageCoord
            } else {
                // Try to parse GPS coordinates from extmetadata
                val lat = extMeta?.gpsLatitude?.value?.toDoubleOrNull()
                val lon = extMeta?.gpsLongitude?.value?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    Coordinate(lat = lat, lon = lon)
                } else null
            }

            if (coord != null && info != null) {
                WikiImage(
                    id = page.pageid,
                    title = page.title,
                    thumbUrl = info.thumburl ?: "",
                    fullUrl = info.url ?: "",
                    lat = coord.lat,
                    lon = coord.lon,
                    description = extMeta?.description?.value,
                    date = extMeta?.dateTime?.value,
                    artist = extMeta?.artist?.value,
                    license = extMeta?.license?.value
                )
            } else null
        } ?: emptyList()

        val nextOffset = response.continueData?.gsroffset
        PagedResult(images = images, nextOffset = nextOffset)
    }
}
