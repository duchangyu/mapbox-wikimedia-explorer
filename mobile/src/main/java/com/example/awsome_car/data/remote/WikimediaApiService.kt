package com.example.awsome_car.data.remote

import com.example.awsome_car.data.remote.model.WikimediaResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WikimediaApiService {
    @GET("w/api.php")
    suspend fun searchImages(
        @Query("gsrsearch") searchTerm: String,
        @Query("gsroffset") offset: Int? = null,
        @Query("action") action: String = "query",
        @Query("generator") generator: String = "search",
        @Query("gsrnamespace") namespace: Int = 6,
        @Query("gsrlimit") limit: Int = 20,
        @Query("prop") prop: String = "coordinates|imageinfo",
        @Query("iiprop") iiprop: String = "url|extmetadata",
        @Query("iiurlwidth") thumbWidth: Int = 300,
        @Query("format") format: String = "json",
        @Query("origin") origin: String = "*"
    ): WikimediaResponse
}
