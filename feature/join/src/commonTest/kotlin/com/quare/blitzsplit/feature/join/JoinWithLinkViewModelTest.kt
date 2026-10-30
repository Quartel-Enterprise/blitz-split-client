package com.quare.blitzsplit.feature.join

import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.join.presentation.model.CameraAccess
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiAction
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiEvent
import com.quare.blitzsplit.feature.join.presentation.model.JoinWithLinkUiState
import com.quare.blitzsplit.feature.join.presentation.viewmodel.JoinWithLinkViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class JoinWithLinkViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var viewModel: JoinWithLinkViewModel
    private val actions = mutableListOf<JoinWithLinkUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the reader WHEN it opens THEN shows an empty field with no error asking for the camera`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals(
                JoinWithLinkUiState(
                    link = "",
                    isLinkInvalid = false,
                    isQrInvalid = false,
                    cameraAccess = CameraAccess.ASKING,
                    isTorchOn = false,
                ),
                state,
            )
            assertFalse(state.canOpen)
        }

    @Test
    fun `GIVEN the reader asking for the camera WHEN the camera is allowed THEN the camera is granted`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = true))

            // Then
            assertEquals(CameraAccess.GRANTED, viewModel.uiState.value.cameraAccess)
        }

    @Test
    fun `GIVEN the reader asking for the camera WHEN the camera is refused THEN the camera is denied`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = false))

            // Then
            assertEquals(CameraAccess.DENIED, viewModel.uiState.value.cameraAccess)
        }

    @Test
    fun `GIVEN the torch on WHEN the camera access is lost THEN the camera is denied and the torch goes off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = true))
            viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = false))

            // Then
            assertEquals(CameraAccess.DENIED, viewModel.uiState.value.cameraAccess)
            assertFalse(viewModel.uiState.value.isTorchOn)
        }

    @Test
    fun `GIVEN the camera granted WHEN tapping the torch THEN the torch goes on`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = true))

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

        // Then
        assertTrue(viewModel.uiState.value.isTorchOn)
    }

    @Test
    fun `GIVEN the torch on WHEN tapping the torch THEN the torch goes off`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = true))
        viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

        // Then
        assertFalse(viewModel.uiState.value.isTorchOn)
    }

    @Test
    fun `GIVEN the reader still asking for the camera WHEN tapping the torch THEN the torch stays off`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

            // Then
            assertFalse(viewModel.uiState.value.isTorchOn)
        }

    @Test
    fun `GIVEN the camera denied WHEN tapping the torch THEN the torch stays off`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = false))

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnTorchClicked)

        // Then
        assertFalse(viewModel.uiState.value.isTorchOn)
    }

    @Test
    fun `GIVEN the camera denied WHEN tapping open settings THEN opens the camera settings`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnCameraAccessChanged(isGranted = false))

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnOpenSettingsClicked)

            // Then
            assertEquals(listOf<JoinWithLinkUiAction>(JoinWithLinkUiAction.OpenCameraSettings), actions)
        }

    @Test
    fun `GIVEN the reader WHEN reading the QR of a group THEN the invite takes the place of the reader`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "https://blitzsplit.com/g/$CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(listOf(Route.Groups, groupInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN reading the QR of a personal link THEN opens the invite of that member and sharer`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "https://blitzsplit.com/g/$CODE/$MEMBER_CODE?from=$INVITER_CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(listOf(Route.Groups, carlaInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN reading the QR of a personal link with no sharer THEN opens that member`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "https://blitzsplit.com/g/$CODE/$MEMBER_CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(
                listOf(
                    Route.Groups,
                    Route.JoinInvite(
                        CODE,
                        memberCode = MEMBER_CODE,
                        inviterCode = null,
                    ),
                ),
                navigator.backStack.value,
            )
        }

    @Test
    fun `GIVEN the QR of a group just opened the invite WHEN the camera reads the same QR again THEN opens it once`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "https://blitzsplit.com/g/$CODE"
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(listOf(Route.Groups, groupInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a native back already closed the reader WHEN the camera reads an invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            val content = "https://blitzsplit.com/g/$CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader over other screens WHEN reading the QR of a group THEN replaces only the reader`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(under = listOf(Route.Groups, Route.Profile))
            val content = "https://blitzsplit.com/g/$CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertEquals(listOf(Route.Groups, Route.Profile, groupInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN reading the QR of another site THEN warns the QR is not an invite and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "https://example.com/menu"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertTrue(viewModel.uiState.value.isQrInvalid)
            assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN reading a QR of plain text THEN warns the QR is not an invite and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val content = "WIFI:S:Casa;T:WPA;P:12345678;;"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned(content))

            // Then
            assertTrue(viewModel.uiState.value.isQrInvalid)
            assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a QR that is not an invite WHEN less than 3 seconds pass THEN still warns`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("hello"))

        // When
        advanceTimeBy(2999.milliseconds)
        runCurrent()

        // Then
        assertTrue(viewModel.uiState.value.isQrInvalid)
    }

    @Test
    fun `GIVEN a QR that is not an invite WHEN 3 seconds pass THEN the warning goes by itself`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("hello"))

            // When
            advanceTimeBy(3.seconds)
            runCurrent()

            // Then
            assertFalse(viewModel.uiState.value.isQrInvalid)
        }

    @Test
    fun `GIVEN a wrong QR read again 2 seconds later WHEN 3 seconds pass since the first THEN still warns`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("hello"))
            advanceTimeBy(2.seconds)
            runCurrent()
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("hello"))

            // When
            advanceTimeBy(1.seconds)
            runCurrent()

            // Then
            assertTrue(viewModel.uiState.value.isQrInvalid)
        }

    @Test
    fun `GIVEN a wrong QR read again 2 seconds later WHEN 3 seconds pass since the second THEN the warning goes`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("hello"))
            advanceTimeBy(2.seconds)
            runCurrent()
            viewModel.onEvent(JoinWithLinkUiEvent.OnQrScanned("https://example.com"))

            // When
            advanceTimeBy(3.seconds)
            runCurrent()

            // Then
            assertFalse(viewModel.uiState.value.isQrInvalid)
        }

    @Test
    fun `GIVEN the reader WHEN pasting the message shared from Convidar THEN opens the personal invite in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val text = "Entra no grupo Viagem como Carla no Bora Rachar: " +
                "https://blitzsplit.com/g/$CODE/$MEMBER_CODE?from=$INVITER_CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))

            // Then
            assertEquals(listOf(Route.Groups, carlaInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN pasting a link without https THEN opens the group invite in its place`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val text = "blitzsplit.com/g/$CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))

            // Then
            assertEquals(listOf(Route.Groups, groupInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a native back already closed the reader WHEN pasting an invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            navigator.navigateBack()
            val text = "https://blitzsplit.com/g/$CODE"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the reader WHEN pasting text that is not an invite THEN shows it trimmed in the field as invalid`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            val text = "  https://example.com/menu \n"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))

            // Then
            assertEquals("https://example.com/menu", viewModel.uiState.value.link)
            assertTrue(viewModel.uiState.value.isLinkInvalid)
            assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a link typed WHEN pasting blank text THEN the field stays as it was with no error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("blitzsplit.com/g/AB"))
            val text = "   \n"

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted(text))

            // Then
            assertEquals("blitzsplit.com/g/AB", viewModel.uiState.value.link)
            assertFalse(viewModel.uiState.value.isLinkInvalid)
            assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a pasted link that is not an invite WHEN typing THEN sets the link and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted("hello"))

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("blitzsplit.com/g/AB"))

            // Then
            assertEquals("blitzsplit.com/g/AB", viewModel.uiState.value.link)
            assertFalse(viewModel.uiState.value.isLinkInvalid)
        }

    @Test
    fun `GIVEN the reader WHEN typing some text THEN the invite can be opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("blitz"))

        // Then
        assertTrue(viewModel.uiState.value.canOpen)
    }

    @Test
    fun `GIVEN the reader WHEN typing only spaces THEN the invite cannot be opened`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("   "))

        // Then
        assertFalse(viewModel.uiState.value.canOpen)
    }

    @Test
    fun `GIVEN an invite link typed by hand WHEN tapping open THEN the invite takes the place of the reader`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(
                JoinWithLinkUiEvent.OnLinkChanged("blitzsplit.com/g/$CODE/$MEMBER_CODE?from=$INVITER_CODE"),
            )

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnOpenClicked)

            // Then
            assertEquals(listOf(Route.Groups, carlaInvite), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a typed link that is not an invite WHEN tapping open THEN shows the link is invalid and stays`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("blitzsplit.com/g/AB12"))

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnOpenClicked)

            // Then
            assertTrue(viewModel.uiState.value.isLinkInvalid)
            assertEquals("blitzsplit.com/g/AB12", viewModel.uiState.value.link)
            assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a blank field WHEN tapping open THEN nothing happens`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("   "))

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnOpenClicked)

        // Then
        assertFalse(viewModel.uiState.value.isLinkInvalid)
        assertEquals(listOf(Route.Groups, Route.JoinWithLink), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a native back already closed the reader WHEN tapping open on an invite THEN nothing opens`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnLinkChanged("https://blitzsplit.com/g/$CODE"))
            navigator.navigateBack()

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnOpenClicked)

            // Then
            assertEquals(listOf(Route.Groups), navigator.backStack.value)
        }

    @Test
    fun `GIVEN a link shown as invalid WHEN clearing the field THEN empties it and clears the error`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()
            viewModel.onEvent(JoinWithLinkUiEvent.OnPasted("hello"))

            // When
            viewModel.onEvent(JoinWithLinkUiEvent.OnClearClicked)

            // Then
            assertEquals("", viewModel.uiState.value.link)
            assertFalse(viewModel.uiState.value.isLinkInvalid)
            assertFalse(viewModel.uiState.value.canOpen)
        }

    @Test
    fun `GIVEN the reader on top WHEN closing THEN goes back to where it came from`() = runTest(testDispatcher) {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN another screen over the reader WHEN closing THEN the back stack stays`() = runTest(testDispatcher) {
        // Given
        prepareScenario()
        navigator.navigate(Route.Profile)

        // When
        viewModel.onEvent(JoinWithLinkUiEvent.OnCloseClicked)

        // Then
        assertEquals(listOf(Route.Groups, Route.JoinWithLink, Route.Profile), navigator.backStack.value)
    }

    private val groupInvite = Route.JoinInvite(
        CODE,
        memberCode = null,
        inviterCode = null,
    )

    // Why: Carla's personal link, shared by whoever has INVITER_CODE.
    private val carlaInvite = Route.JoinInvite(
        CODE,
        memberCode = MEMBER_CODE,
        inviterCode = INVITER_CODE,
    )

    private fun TestScope.prepareScenario(under: List<Route> = listOf(Route.Groups)) {
        actions.clear()
        navigator = BackStackNavigator(startRoute = under.first()).also { navigator ->
            under.drop(1).forEach(navigator::navigate)
            navigator.navigate(Route.JoinWithLink)
        }
        viewModel = JoinWithLinkViewModel(navigator = navigator)
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val CODE = "AB12cd34"
        const val MEMBER_CODE = "c3Rt9z"
        const val INVITER_CODE = "x1Y2z3"
    }
}
