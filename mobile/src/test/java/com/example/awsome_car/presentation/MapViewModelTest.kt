package com.example.awsome_car.presentation

import com.example.awsome_car.domain.model.PagedResult
import com.example.awsome_car.domain.model.WikiImage
import com.example.awsome_car.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeImageRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeImageRepository()
        viewModel = MapViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search with results updates images and hasMoreResults`() = runTest {
        val images = listOf(
            createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0),
            createWikiImage(id = 2, title = "Image 2", lat = 3.0, lon = 4.0)
        )
        fakeRepository.setNextResult(PagedResult(images = images, nextOffset = 20))

        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(images, state.images)
        assertFalse(state.isLoading)
        assertTrue(state.hasMoreResults)
        assertEquals(20, state.nextOffset)
        assertNull(state.errorMessage)
    }

    @Test
    fun `search with empty results shows error message`() = runTest {
        fakeRepository.setNextResult(PagedResult(images = emptyList(), nextOffset = null))

        viewModel.onSearchQueryChanged("xyz_no_results")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.images.isEmpty())
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("No geotagged images"))
    }

    @Test
    fun `search failure shows network error`() = runTest {
        fakeRepository.setNextError(RuntimeException("Network timeout"))

        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Network Error"))
    }

    @Test
    fun `loadMore appends images and updates pagination state`() = runTest {
        val firstPage = listOf(createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0))
        val secondPage = listOf(createWikiImage(id = 2, title = "Image 2", lat = 3.0, lon = 4.0))
        fakeRepository.setNextResult(PagedResult(images = firstPage, nextOffset = 20))

        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        fakeRepository.setNextResult(PagedResult(images = secondPage, nextOffset = null))
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.images.size)
        assertEquals(firstPage[0], state.images[0])
        assertEquals(secondPage[0], state.images[1])
        assertFalse(state.hasMoreResults)
        assertNull(state.nextOffset)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadMore failure shows error but preserves existing images`() = runTest {
        val firstPage = listOf(createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0))
        fakeRepository.setNextResult(PagedResult(images = firstPage, nextOffset = 20))

        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        fakeRepository.setNextError(RuntimeException("Connection lost"))
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.images.size)
        assertFalse(state.isLoadingMore)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun `onImageSelected updates selectedImage and closes list`() = runTest {
        val image = createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0)
        viewModel.onImageSelected(image)

        val state = viewModel.uiState.value
        assertEquals(image, state.selectedImage)
        assertFalse(state.isListVisible)
    }

    @Test
    fun `onImageSelected with null clears selectedImage`() = runTest {
        val image = createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0)
        viewModel.onImageSelected(image)

        viewModel.onImageSelected(null)

        assertNull(viewModel.uiState.value.selectedImage)
    }

    @Test
    fun `toggleList toggles visibility`() {
        assertFalse(viewModel.uiState.value.isListVisible)

        viewModel.toggleList()
        assertTrue(viewModel.uiState.value.isListVisible)

        viewModel.toggleList()
        assertFalse(viewModel.uiState.value.isListVisible)
    }

    @Test
    fun `dismissError clears error message`() = runTest {
        fakeRepository.setNextResult(PagedResult(images = emptyList(), nextOffset = null))
        viewModel.onSearchQueryChanged("xyz")
        viewModel.search()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `search resets images and pagination state`() = runTest {
        val firstPage = listOf(createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0))
        fakeRepository.setNextResult(PagedResult(images = firstPage, nextOffset = 20))
        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        viewModel.onImageSelected(firstPage[0])
        viewModel.toggleList()

        val newImages = listOf(createWikiImage(id = 3, title = "Image 3", lat = 5.0, lon = 6.0))
        fakeRepository.setNextResult(PagedResult(images = newImages, nextOffset = null))
        viewModel.onSearchQueryChanged("tree")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(newImages, state.images)
        assertFalse(state.hasMoreResults)
        assertNull(state.nextOffset)
        assertNull(state.selectedImage)
        assertFalse(state.isListVisible)
    }

    @Test
    fun `blank search query does nothing`() = runTest {
        viewModel.onSearchQueryChanged("   ")
        viewModel.search()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.images.isEmpty())
    }

    @Test
    fun `loadMore without nextOffset does nothing`() = runTest {
        val images = listOf(createWikiImage(id = 1, title = "Image 1", lat = 1.0, lon = 2.0))
        fakeRepository.setNextResult(PagedResult(images = images, nextOffset = null))
        viewModel.onSearchQueryChanged("park")
        viewModel.search()
        advanceUntilIdle()

        // Reset tracking to verify loadMore doesn't call repository
        fakeRepository.clearCalls()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(0, fakeRepository.callCount)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    private fun createWikiImage(
        id: Int,
        title: String,
        lat: Double,
        lon: Double
    ): WikiImage = WikiImage(
        id = id,
        title = title,
        thumbUrl = "https://example.com/$id.jpg",
        fullUrl = "https://example.com/$id-full.jpg",
        lat = lat,
        lon = lon,
        description = "Description for $title",
        date = "2024-01-01",
        artist = "Test Artist",
        license = "CC BY-SA"
    )

    private class FakeImageRepository : ImageRepository {
        private var nextResult: PagedResult? = null
        private var nextError: Throwable? = null
        var callCount = 0
            private set
        private val _calls = mutableListOf<Pair<String, Int?>>()

        fun setNextResult(result: PagedResult) {
            nextResult = result
            nextError = null
        }

        fun setNextError(error: Throwable) {
            nextError = error
            nextResult = null
        }

        fun clearCalls() {
            callCount = 0
            _calls.clear()
        }

        override suspend fun searchImages(query: String, offset: Int?): PagedResult {
            callCount++
            _calls.add(query to offset)
            nextError?.let { throw it }
            return nextResult ?: PagedResult(emptyList(), null)
        }
    }
}
