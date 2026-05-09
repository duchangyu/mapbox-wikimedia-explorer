package com.example.awsome_car.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WikimediaResponse(
    val query: Query? = null,
    @SerialName("continue") val continueData: ContinueData? = null
)

@Serializable
data class Query(
    val pages: Map<String, Page> = emptyMap()
)

@Serializable
data class Page(
    val pageid: Int,
    val ns: Int,
    val title: String,
    val coordinates: List<Coordinate>? = null,
    val imageinfo: List<ImageInfo>? = null
)

@Serializable
data class Coordinate(
    val lat: Double,
    val lon: Double
)

@Serializable
data class ImageInfo(
    val thumburl: String? = null,
    val url: String? = null,
    val descriptionurl: String? = null,
    val extmetadata: ExtMetadata? = null
)

@Serializable
data class ExtMetadata(
    @SerialName("ImageDescription") val description: MetadataValue? = null,
    @SerialName("DateTimeOriginal") val dateTime: MetadataValue? = null,
    @SerialName("Artist") val artist: MetadataValue? = null,
    @SerialName("LicenseShortName") val license: MetadataValue? = null,
    @SerialName("GPSLatitude") val gpsLatitude: MetadataValue? = null,
    @SerialName("GPSLongitude") val gpsLongitude: MetadataValue? = null
)

@Serializable
data class MetadataValue(
    val value: String
)

@Serializable
data class ContinueData(
    val gsroffset: Int? = null,
    @SerialName("continue") val continueToken: String? = null
)
