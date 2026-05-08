package com.example.awsome_car.domain.model

data class WikiImage(
    val id: Int,
    val title: String,
    val thumbUrl: String,
    val fullUrl: String,
    val lat: Double,
    val lon: Double,
    val description: String?,
    val date: String?,
    val artist: String?,
    val license: String?
)
