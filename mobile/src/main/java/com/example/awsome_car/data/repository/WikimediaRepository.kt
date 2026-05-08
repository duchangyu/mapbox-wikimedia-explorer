package com.example.awsome_car.data.repository

import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.domain.model.WikiImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikimediaRepository(private val apiService: WikimediaApiService) {

    suspend fun searchImages(query: String, offset: Int? = null): List<WikiImage> = withContext(Dispatchers.IO) {
        val response = apiService.searchImages(query, offset)
        response.query?.pages?.values?.mapNotNull { page ->
            val coord = page.coordinates?.firstOrNull()
            val info = page.imageinfo?.firstOrNull()
            
            if (coord != null && info != null) {
                WikiImage(
                    id = page.pageid,
                    title = page.title,
                    thumbUrl = info.thumburl,
                    fullUrl = info.url,
                    lat = coord.lat,
                    lon = coord.lon,
                    description = info.extmetadata?.description?.value,
                    date = info.extmetadata?.dateTime?.value,
                    artist = info.extmetadata?.artist?.value,
                    license = info.extmetadata?.license?.value
                )
            } else null
        } ?: emptyList()
    }
}
