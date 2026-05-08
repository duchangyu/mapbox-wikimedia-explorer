package com.example.awsome_car.domain.repository

import com.example.awsome_car.domain.model.PagedResult

interface ImageRepository {
    suspend fun searchImages(query: String, offset: Int? = null): PagedResult
}
