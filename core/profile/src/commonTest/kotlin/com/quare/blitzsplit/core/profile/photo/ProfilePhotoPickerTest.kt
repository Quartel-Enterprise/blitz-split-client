package com.quare.blitzsplit.core.profile.photo

import com.quare.blitzsplit.core.model.CropPhoto
import com.quare.blitzsplit.core.model.PhotoCrop
import com.quare.blitzsplit.core.model.PhotoCropEditor
import com.quare.blitzsplit.core.model.PhotoCropEvent
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.provider.image.UnsupportedImageException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProfilePhotoPickerTest {
    private lateinit var statuses: MutableList<Pair<Boolean, Boolean>>
    private lateinit var editors: MutableList<PhotoCropEditor?>
    private lateinit var prepared: MutableList<ProfilePhoto>
    private lateinit var loadedSources: MutableList<ByteArray>
    private lateinit var preparations: MutableList<Pair<CropPhoto, PhotoCrop>>
    private lateinit var heldLoads: MutableList<CompletableDeferred<Result<CropPhoto>>>
    private lateinit var heldPreparations: MutableList<CompletableDeferred<Result<ProfilePhoto.Pending>>>
    private lateinit var picker: ProfilePhotoPicker

    @Test
    fun `GIVEN a readable photo WHEN preparing it THEN opens the editor with the photo centered`() = runTest {
        // Given
        prepareScenario()

        // When
        picker.prepare(firstSource)
        runCurrent()

        // Then
        assertContentEquals(firstSource, loadedSources.single())
        assertEquals(listOf(spinner, idle), statuses)
        assertEquals(listOf<PhotoCropEditor?>(editorOf(landscape)), editors)
        assertTrue(prepared.isEmpty())
        assertTrue(preparations.isEmpty())
    }

    @Test
    fun `GIVEN the screen busy saving WHEN a photo is picked THEN it is not read`() = runTest {
        // Given
        prepareScenario(isBusy = { true })

        // When
        picker.prepare(firstSource)
        runCurrent()

        // Then
        assertTrue(loadedSources.isEmpty())
        assertTrue(statuses.isEmpty())
        assertTrue(editors.isEmpty())
    }

    @Test
    fun `GIVEN a photo in the editor and the screen turned busy WHEN confirming THEN nothing is prepared`() = runTest {
        // Given
        var isBusy = false
        prepareScenario(isBusy = { isBusy })
        picker.prepare(firstSource)
        runCurrent()
        isBusy = true

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertTrue(preparations.isEmpty())
        assertEquals(listOf<PhotoCropEditor?>(editorOf(landscape)), editors)
    }

    @Test
    fun `GIVEN a photo in the editor WHEN pinching around a point THEN the crop zooms around it`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(
            PhotoCropEvent.OnPinched(
                zoomChange = 2f,
                focusX = 0.1f,
                focusY = 0.1f,
            ),
        )

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toZoomedBy(2f, 0.1f, 0.1f)), editors.last())
    }

    @Test
    fun `GIVEN an unreadable photo WHEN preparing it THEN shows the error and opens no editor`() = runTest {
        // Given
        prepareScenario(loadResult = Result.failure(UnsupportedImageException()))

        // When
        picker.prepare(firstSource)
        runCurrent()

        // Then
        assertEquals(listOf(spinner, error), statuses)
        assertTrue(editors.isEmpty())
        assertTrue(prepared.isEmpty())
    }

    @Test
    fun `GIVEN a photo still loading WHEN another photo is prepared THEN only the newer one opens in the editor`() =
        runTest {
            // Given
            prepareScenario(holdsLoads = true)
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.prepare(secondSource)
            runCurrent()
            heldLoads[0].complete(Result.success(landscape))
            heldLoads[1].complete(Result.success(portrait))
            runCurrent()

            // Then
            assertEquals(listOf<PhotoCropEditor?>(editorOf(portrait)), editors)
            assertEquals(listOf(spinner, spinner, idle), statuses)
        }

    @Test
    fun `GIVEN a photo still loading WHEN another photo is prepared and the older fails THEN its error never shows`() =
        runTest {
            // Given
            prepareScenario(holdsLoads = true)
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.prepare(secondSource)
            runCurrent()
            heldLoads[0].complete(Result.failure(UnsupportedImageException()))
            runCurrent()

            // Then
            assertEquals(listOf(spinner, spinner), statuses)
            assertTrue(editors.isEmpty())
            assertNotNull(picker.pickInProgress)
        }

    @Test
    fun `GIVEN a photo still loading WHEN reading the pick in progress THEN is the active job`() = runTest {
        // Given
        prepareScenario(holdsLoads = true)
        picker.prepare(firstSource)
        runCurrent()

        // When
        val pick = picker.pickInProgress

        // Then
        assertNotNull(pick)
        assertTrue(pick.isActive)
    }

    @Test
    fun `GIVEN the editor opened WHEN reading the pick in progress THEN there is none`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        val pick = picker.pickInProgress

        // Then
        assertNull(pick)
    }

    @Test
    fun `GIVEN nothing picked WHEN reading the pick in progress THEN there is none`() = runTest {
        // Given
        prepareScenario()

        // When
        val pick = picker.pickInProgress

        // Then
        assertNull(pick)
    }

    @Test
    fun `GIVEN a confirmed photo still being encoded WHEN reading the pick in progress THEN is the active job`() =
        runTest {
            // Given
            prepareScenario(holdsPreparations = true)
            picker.prepare(firstSource)
            runCurrent()
            picker.onEvent(PhotoCropEvent.OnConfirmClicked)
            runCurrent()

            // When
            val pick = picker.pickInProgress

            // Then
            assertNotNull(pick)
            assertTrue(pick.isActive)
        }

    @Test
    fun `GIVEN the editor open WHEN dragging THEN the editor shows the photo moved`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(
            PhotoCropEvent.OnDragged(
                dx = 0.1f,
                dy = 0f,
            ),
        )

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toDragged(0.1f, 0f)), editors.last())
        assertEquals(0.1f, editors.last()?.crop?.offsetX)
    }

    @Test
    fun `GIVEN the editor open WHEN pinching THEN the editor shows the photo zoomed by the pinch`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(
            PhotoCropEvent.OnPinched(
                zoomChange = 1.5f,
                focusX = 0f,
                focusY = 0f,
            ),
        )

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toZoomedBy(1.5f)), editors.last())
        assertEquals(1.5f, editors.last()?.crop?.zoom)
    }

    @Test
    fun `GIVEN a zoomed photo WHEN pinching again THEN the zoom multiplies`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(
            PhotoCropEvent.OnPinched(
                zoomChange = 1.5f,
                focusX = 0f,
                focusY = 0f,
            ),
        )

        // When
        picker.onEvent(
            PhotoCropEvent.OnPinched(
                zoomChange = 1.5f,
                focusX = 0f,
                focusY = 0f,
            ),
        )

        // Then
        assertEquals(2.25f, editors.last()?.crop?.zoom)
    }

    @Test
    fun `GIVEN the editor open WHEN the zoom slider changes THEN the editor shows that zoom`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnZoomChanged(zoom = 2f))

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toZoomed(2f)), editors.last())
        assertEquals(2f, editors.last()?.crop?.zoom)
    }

    @Test
    fun `GIVEN the editor open WHEN turning THEN the editor shows the photo a quarter turn clockwise`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnTurnClicked)

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toTurned()), editors.last())
        assertEquals(1, editors.last()?.crop?.quarterTurns)
    }

    @Test
    fun `GIVEN the editor open WHEN mirroring horizontally THEN the editor shows the photo mirrored`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnMirrorHorizontallyClicked)

        // Then
        assertEquals(editorOf(landscape, centered(landscape).toMirroredHorizontally()), editors.last())
        assertEquals(true, editors.last()?.crop?.isMirrored)
    }

    @Test
    fun `GIVEN the editor open WHEN mirroring vertically THEN the editor shows the photo upside down mirrored`() =
        runTest {
            // Given
            prepareScenario()
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.onEvent(PhotoCropEvent.OnMirrorVerticallyClicked)

            // Then
            assertEquals(editorOf(landscape, centered(landscape).toMirroredVertically()), editors.last())
            assertEquals(true, editors.last()?.crop?.isMirrored)
            assertEquals(2, editors.last()?.crop?.quarterTurns)
        }

    @Test
    fun `GIVEN no editor open WHEN a crop gesture arrives THEN nothing changes`() = runTest {
        // Given
        prepareScenario()

        // When
        picker.onEvent(
            PhotoCropEvent.OnDragged(
                dx = 0.1f,
                dy = 0.1f,
            ),
        )
        picker.onEvent(
            PhotoCropEvent.OnPinched(
                zoomChange = 2f,
                focusX = 0f,
                focusY = 0f,
            ),
        )
        picker.onEvent(PhotoCropEvent.OnZoomChanged(zoom = 2f))
        picker.onEvent(PhotoCropEvent.OnTurnClicked)
        picker.onEvent(PhotoCropEvent.OnMirrorHorizontallyClicked)
        picker.onEvent(PhotoCropEvent.OnMirrorVerticallyClicked)
        runCurrent()

        // Then
        assertTrue(editors.isEmpty())
        assertTrue(statuses.isEmpty())
    }

    @Test
    fun `GIVEN the editor dismissed WHEN a crop gesture arrives THEN the editor stays closed`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(PhotoCropEvent.OnDismissed)

        // When
        picker.onEvent(PhotoCropEvent.OnTurnClicked)

        // Then
        assertEquals(listOf(editorOf(landscape), null), editors)
    }

    @Test
    fun `GIVEN the editor open WHEN dismissed THEN it closes leaving the photo as it was`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnDismissed)
        runCurrent()

        // Then
        assertNull(editors.last())
        assertEquals(listOf(spinner, idle), statuses)
        assertTrue(preparations.isEmpty())
        assertTrue(prepared.isEmpty())
    }

    @Test
    fun `GIVEN a framed photo WHEN confirmed THEN the editor closes and the square framed is prepared`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(PhotoCropEvent.OnTurnClicked)
        picker.onEvent(PhotoCropEvent.OnZoomChanged(zoom = 2f))
        val framed = centered(landscape).toTurned().toZoomed(2f)

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertNull(editors.last())
        val (photo, crop) = preparations.single()
        assertSame(landscape, photo)
        assertEquals(framed, crop)
        assertEquals(listOf<ProfilePhoto>(pending), prepared)
        assertEquals(listOf(spinner, idle, spinner), statuses)
    }

    @Test
    fun `GIVEN a photo being encoded WHEN looking at the screen THEN the editor is closed and the spinner shows`() =
        runTest {
            // Given
            prepareScenario(holdsPreparations = true)
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.onEvent(PhotoCropEvent.OnConfirmClicked)
            runCurrent()

            // Then
            assertNull(editors.last())
            assertEquals(spinner, statuses.last())
            assertTrue(prepared.isEmpty())
        }

    @Test
    fun `GIVEN encoding the framed photo fails WHEN confirmed THEN shows the error and prepares nothing`() = runTest {
        // Given
        prepareScenario(prepareResult = Result.failure(UnsupportedImageException()))
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertEquals(error, statuses.last())
        assertTrue(prepared.isEmpty())
        assertNull(editors.last())
    }

    @Test
    fun `GIVEN no editor open WHEN confirmed THEN nothing is prepared`() = runTest {
        // Given
        prepareScenario()

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertTrue(preparations.isEmpty())
        assertTrue(prepared.isEmpty())
        assertTrue(statuses.isEmpty())
        assertTrue(editors.isEmpty())
    }

    @Test
    fun `GIVEN a photo already confirmed WHEN confirmed again THEN it is prepared only once`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertEquals(1, preparations.size)
        assertEquals(listOf<ProfilePhoto>(pending), prepared)
    }

    @Test
    fun `GIVEN the editor open WHEN another photo is prepared THEN the editor closes and opens on the newer photo`() =
        runTest {
            // Given
            prepareScenario(loadResults = listOf(landscape, portrait))
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.prepare(secondSource)
            runCurrent()

            // Then
            assertEquals(listOf(editorOf(landscape), null, editorOf(portrait)), editors)
            assertTrue(preparations.isEmpty())
        }

    @Test
    fun `GIVEN a photo being encoded WHEN another photo is prepared THEN the older is never prepared`() = runTest {
        // Given
        prepareScenario(
            loadResults = listOf(landscape, portrait),
            holdsPreparations = true,
        )
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // When
        picker.prepare(secondSource)
        runCurrent()
        heldPreparations.single().complete(Result.success(pending))
        runCurrent()

        // Then
        assertTrue(prepared.isEmpty())
        assertEquals(editorOf(portrait), editors.last())
    }

    @Test
    fun `GIVEN a photo being encoded WHEN another is prepared and the older fails THEN its error never shows`() =
        runTest {
            // Given
            prepareScenario(
                loadResults = listOf(landscape, portrait),
                holdsPreparations = true,
            )
            picker.prepare(firstSource)
            runCurrent()
            picker.onEvent(PhotoCropEvent.OnConfirmClicked)
            runCurrent()

            // When
            picker.prepare(secondSource)
            runCurrent()
            heldPreparations.single().complete(Result.failure(UnsupportedImageException()))
            runCurrent()

            // Then
            assertTrue(error !in statuses)
            assertEquals(editorOf(portrait), editors.last())
        }

    @Test
    fun `GIVEN a photo still loading WHEN the picker fails THEN the older photo never opens and the error shows`() =
        runTest {
            // Given
            prepareScenario(holdsLoads = true)
            picker.prepare(firstSource)
            runCurrent()

            // When
            picker.fail()
            heldLoads.single().complete(Result.success(landscape))
            runCurrent()

            // Then
            assertTrue(editors.isEmpty())
            assertEquals(listOf(spinner, error), statuses)
            assertNull(picker.pickInProgress)
        }

    @Test
    fun `GIVEN the editor open WHEN the picker fails THEN the editor closes and the error shows`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.fail()

        // Then
        assertNull(editors.last())
        assertEquals(error, statuses.last())
    }

    @Test
    fun `GIVEN nothing picked WHEN the picker fails THEN the error shows`() = runTest {
        // Given
        prepareScenario()

        // When
        picker.fail()

        // Then
        assertEquals(listOf(error), statuses)
        assertTrue(editors.isEmpty())
    }

    @Test
    fun `GIVEN a photo still loading WHEN cancelled THEN the photo never opens in the editor`() = runTest {
        // Given
        prepareScenario(holdsLoads = true)
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.cancel()
        heldLoads.single().complete(Result.success(landscape))
        runCurrent()

        // Then
        assertTrue(editors.isEmpty())
        assertEquals(listOf(spinner), statuses)
        assertNull(picker.pickInProgress)
    }

    @Test
    fun `GIVEN the editor open WHEN cancelled THEN the editor closes`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()

        // When
        picker.cancel()

        // Then
        assertEquals(listOf(editorOf(landscape), null), editors)
        assertTrue(preparations.isEmpty())
    }

    @Test
    fun `GIVEN the editor cancelled WHEN confirmed THEN nothing is prepared`() = runTest {
        // Given
        prepareScenario()
        picker.prepare(firstSource)
        runCurrent()
        picker.cancel()

        // When
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // Then
        assertTrue(preparations.isEmpty())
        assertTrue(prepared.isEmpty())
    }

    @Test
    fun `GIVEN a confirmed photo still being encoded WHEN cancelled THEN it is never prepared`() = runTest {
        // Given
        prepareScenario(holdsPreparations = true)
        picker.prepare(firstSource)
        runCurrent()
        picker.onEvent(PhotoCropEvent.OnConfirmClicked)
        runCurrent()

        // When
        picker.cancel()
        heldPreparations.single().complete(Result.success(pending))
        runCurrent()

        // Then
        assertTrue(prepared.isEmpty())
        assertNull(picker.pickInProgress)
    }

    @Test
    fun `GIVEN nothing picked WHEN cancelled THEN nothing is shown`() = runTest {
        // Given
        prepareScenario()

        // When
        picker.cancel()

        // Then
        assertTrue(editors.isEmpty())
        assertTrue(statuses.isEmpty())
    }

    private val spinner = true to false
    private val idle = false to false
    private val error = false to true
    private val firstSource = byteArrayOf(1)
    private val secondSource = byteArrayOf(2)
    private val landscape = CropPhoto(
        jpeg = byteArrayOf(10),
        width = 1200,
        height = 600,
    )
    private val portrait = CropPhoto(
        jpeg = byteArrayOf(20),
        width = 600,
        height = 1200,
    )
    private val pending = ProfilePhoto.Pending(byteArrayOf(9))

    private fun centered(photo: CropPhoto): PhotoCrop = PhotoCrop.of(photo.width, photo.height)

    private fun editorOf(
        photo: CropPhoto,
        crop: PhotoCrop = centered(photo),
    ): PhotoCropEditor = PhotoCropEditor(
        photo = photo,
        crop = crop,
    )

    private fun TestScope.prepareScenario(
        loadResult: Result<CropPhoto> = Result.success(landscape),
        loadResults: List<CropPhoto>? = null,
        prepareResult: Result<ProfilePhoto.Pending> = Result.success(pending),
        holdsLoads: Boolean = false,
        holdsPreparations: Boolean = false,
        isBusy: () -> Boolean = { false },
    ) {
        statuses = mutableListOf()
        editors = mutableListOf()
        prepared = mutableListOf()
        loadedSources = mutableListOf()
        preparations = mutableListOf()
        heldLoads = mutableListOf()
        heldPreparations = mutableListOf()
        picker = ProfilePhotoPicker(
            loadCropPhoto = { source ->
                loadedSources += source
                when {
                    // Why: ignores cancellation, as an encoder that can't stop midway would.
                    holdsLoads -> {
                        val load = CompletableDeferred<Result<CropPhoto>>().also(heldLoads::add)
                        withContext(NonCancellable) { load.await() }
                    }

                    loadResults != null -> Result.success(loadResults[loadedSources.lastIndex])

                    else -> loadResult
                }
            },
            prepareProfilePhoto = { photo, crop ->
                preparations += photo to crop
                if (holdsPreparations) {
                    val preparation = CompletableDeferred<Result<ProfilePhoto.Pending>>().also(heldPreparations::add)
                    withContext(NonCancellable) { preparation.await() }
                } else {
                    prepareResult
                }
            },
            scope = backgroundScope,
            showStatus = { isPreparing, hasFailed -> statuses += isPreparing to hasFailed },
            showEditor = editors::add,
            onPrepared = prepared::add,
            isBusy = isBusy,
        )
    }
}
