package com.example.awsome_car.data.repository

import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.domain.model.PagedResult
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikimediaRepository(private val apiService: WikimediaApiService) : ImageRepository {

    override suspend fun searchImages(query: String, offset: Int?): PagedResult = withContext(Dispatchers.IO) {
        val response = apiService.searchImages(query, offset)

        val images = response.query?.pages?.values?.mapNotNull { page ->
            val coord = page.coordinates?.firstOrNull()
            val info = page.imageinfo?.firstOrNull()

            if (coord != null && info != null) {
                WikiImage(
                    id = page.pageid,
                    title = page.title,
                    thumbUrl = info.thumburl ?: "",
                    fullUrl = info.url ?: "",
                    lat = coord.lat,
                    lon = coord.lon,
                    description = info.extmetadata?.description?.value,
                    date = info.extmetadata?.dateTime?.value,
                    artist = info.extmetadata?.artist?.value,
                    license = info.extmetadata?.license?.value
                )
            } else null
        } ?: emptyList()

        val nextOffset = response.continueData?.gsroffset
        PagedResult(images = images, nextOffset = nextOffset)
    }
}
