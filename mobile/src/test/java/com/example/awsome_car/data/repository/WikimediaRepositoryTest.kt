package com.example.awsome_car.data.repository

import com.example.awsome_car.data.remote.WikimediaApiService
import com.example.awsome_car.data.remote.model.ContinueData
import com.example.awsome_car.data.remote.model.Coordinate
import com.example.awsome_car.data.remote.model.ExtMetadata
import com.example.awsome_car.data.remote.model.ImageInfo
import com.example.awsome_car.data.remote.model.MetadataValue
import com.example.awsome_car.data.remote.model.Page
import com.example.awsome_car.data.remote.model.Query
import com.example.awsome_car.data.remote.model.WikimediaResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WikimediaRepositoryTest {

    private lateinit var fakeApi: FakeWikimediaApiService
    private lateinit var repository: WikimediaRepository

    @Before
    fun setup() {
        fakeApi = FakeWikimediaApiService()
        repository = WikimediaRepository(fakeApi)
    }

    @Test
    fun `searchImages maps pages with coordinates and imageinfo to WikiImage`() = runTest {
        val page = Page(
            pageid = 123,
            ns = 6,
            title = "Test Image.jpg",
            coordinates = listOf(Coordinate(lat = 10.5, lon = 20.5)),
            imageinfo = listOf(
                ImageInfo(
                    thumburl = "https://thumb.example.com/123.jpg",
                    url = "https://example.com/123.jpg",
                    descriptionurl = "https://commons.example.com/123",
                    extmetadata = ExtMetadata(
                        description = MetadataValue("A test image"),
                        dateTime = MetadataValue("2024-01-15"),
                        artist = MetadataValue("Test Artist"),
                        license = MetadataValue("CC BY-SA 4.0")
                    )
                )
            )
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("123" to page)),
                continueData = null
            )
        )

        val result = repository.searchImages("test")

        assertEquals(1, result.images.size)
        val image = result.images[0]
        assertEquals(123, image.id)
        assertEquals("Test Image.jpg", image.title)
        assertEquals(10.5, image.lat, 0.001)
        assertEquals(20.5, image.lon, 0.001)
        assertEquals("https://thumb.example.com/123.jpg", image.thumbUrl)
        assertEquals("A test image", image.description)
        assertEquals("2024-01-15", image.date)
        assertEquals("Test Artist", image.artist)
        assertEquals("CC BY-SA 4.0", image.license)
        assertNull(result.nextOffset)
    }

    @Test
    fun `searchImages filters pages without coordinates`() = runTest {
        val pageWithCoord = Page(
            pageid = 1,
            ns = 6,
            title = "Has Coords.jpg",
            coordinates = listOf(Coordinate(lat = 1.0, lon = 2.0)),
            imageinfo = listOf(ImageInfo(thumburl = "url1"))
        )
        val pageWithoutCoord = Page(
            pageid = 2,
            ns = 6,
            title = "No Coords.jpg",
            coordinates = emptyList(),
            imageinfo = listOf(ImageInfo(thumburl = "url2"))
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("1" to pageWithCoord, "2" to pageWithoutCoord)),
                continueData = null
            )
        )

        val result = repository.searchImages("test")

        assertEquals(1, result.images.size)
        assertEquals("Has Coords.jpg", result.images[0].title)
    }

    @Test
    fun `searchImages filters pages without imageinfo`() = runTest {
        val pageWithInfo = Page(
            pageid = 1,
            ns = 6,
            title = "Has Info.jpg",
            coordinates = listOf(Coordinate(lat = 1.0, lon = 2.0)),
            imageinfo = listOf(ImageInfo(thumburl = "url1"))
        )
        val pageWithoutInfo = Page(
            pageid = 2,
            ns = 6,
            title = "No Info.jpg",
            coordinates = listOf(Coordinate(lat = 3.0, lon = 4.0)),
            imageinfo = null
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("1" to pageWithInfo, "2" to pageWithoutInfo)),
                continueData = null
            )
        )

        val result = repository.searchImages("test")

        assertEquals(1, result.images.size)
        assertEquals("Has Info.jpg", result.images[0].title)
    }

    @Test
    fun `searchImages returns empty list when query is null`() = runTest {
        fakeApi.setNextResponse(WikimediaResponse(query = null, continueData = null))

        val result = repository.searchImages("test")

        assertTrue(result.images.isEmpty())
        assertNull(result.nextOffset)
    }

    @Test
    fun `searchImages extracts nextOffset from continue data`() = runTest {
        val page = Page(
            pageid = 1,
            ns = 6,
            title = "Test.jpg",
            coordinates = listOf(Coordinate(lat = 1.0, lon = 2.0)),
            imageinfo = listOf(ImageInfo(thumburl = "url1"))
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("1" to page)),
                continueData = ContinueData(gsroffset = 20, continueToken = "gsroffset||")
            )
        )

        val result = repository.searchImages("test")

        assertEquals(1, result.images.size)
        assertEquals(20, result.nextOffset)
    }

    @Test
    fun `searchImages passes offset to api service`() = runTest {
        val page = Page(
            pageid = 1,
            ns = 6,
            title = "Test.jpg",
            coordinates = listOf(Coordinate(lat = 1.0, lon = 2.0)),
            imageinfo = listOf(ImageInfo(thumburl = "url1"))
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("1" to page)),
                continueData = null
            )
        )

        repository.searchImages("test", offset = 20)

        assertEquals(20, fakeApi.lastOffset)
    }

    @Test
    fun `searchImages propagates exceptions`() = runTest {
        fakeApi.setNextError(RuntimeException("API Error"))

        try {
            repository.searchImages("test")
            assertTrue("Expected exception to be thrown", false)
        } catch (e: RuntimeException) {
            assertEquals("API Error", e.message)
        }
    }

    @Test
    fun `searchImages handles empty metadata fields gracefully`() = runTest {
        val page = Page(
            pageid = 1,
            ns = 6,
            title = "Minimal.jpg",
            coordinates = listOf(Coordinate(lat = 1.0, lon = 2.0)),
            imageinfo = listOf(
                ImageInfo(
                    thumburl = "url1",
                    extmetadata = null
                )
            )
        )
        fakeApi.setNextResponse(
            WikimediaResponse(
                query = Query(pages = mapOf("1" to page)),
                continueData = null
            )
        )

        val result = repository.searchImages("test")

        assertEquals(1, result.images.size)
        val image = result.images[0]
        assertNull(image.description)
        assertNull(image.date)
        assertNull(image.artist)
        assertNull(image.license)
    }

    private class FakeWikimediaApiService : WikimediaApiService {
        private var nextResponse: WikimediaResponse? = null
        private var nextError: Throwable? = null
        var lastOffset: Int? = null
            private set

        fun setNextResponse(response: WikimediaResponse) {
            nextResponse = response
            nextError = null
        }

        fun setNextError(error: Throwable) {
            nextError = error
            nextResponse = null
        }

        override suspend fun searchImages(
            searchTerm: String,
            offset: Int?,
            action: String,
            generator: String,
            namespace: Int,
            limit: Int,
            prop: String,
            iiprop: String,
            thumbWidth: Int,
            format: String,
            origin: String
        ): WikimediaResponse {
            lastOffset = offset
            nextError?.let { throw it }
            return nextResponse ?: throw IllegalStateException("No response configured")
        }
    }
}
