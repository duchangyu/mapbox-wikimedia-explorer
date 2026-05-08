package com.example.awsome_car.domain.model

data class PagedResult(
    val images: List<WikiImage>,
    val nextOffset: Int?
)
