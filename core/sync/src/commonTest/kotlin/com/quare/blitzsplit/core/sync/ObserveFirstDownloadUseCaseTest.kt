package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.testing.FakeAccountRepository
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.impl.ObserveFirstDownloadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObserveFirstDownloadUseCaseTest {
    private val firstDownload = FirstDownload()
    private val account = FakeAccountRepository()
    private val isOnline = MutableStateFlow(true)
    private val useCase = ObserveFirstDownloadUseCase(
        firstDownload = firstDownload,
        accountRepository = account,
        connectivityObserver = NetworkConnectivityObserver { isOnline },
    )

    @Test
    fun `GIVEN a first download started WHEN observing THEN no group came down yet`() = runTest {
        // Given
        firstDownload.start()

        // When
        val progress = useCase().first()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 0,
                groupCount = null,
                isWaitingForInternet = false,
            ),
            progress,
        )
    }

    @Test
    fun `GIVEN a first download started and ended WHEN observing THEN it is not pending`() = runTest {
        // Given
        firstDownload.start()
        firstDownload.end()

        // When
        val progress = useCase().first()

        // Then
        assertNull(progress)
    }

    @Test
    fun `GIVEN nothing restored WHEN observing THEN no first download is pending`() = runTest {
        // Given
        account.awaitsFullSync = false

        // When
        val progress = useCase().first()

        // Then
        assertNull(progress)
    }

    @Test
    fun `GIVEN an account awaiting its full sync after a relaunch WHEN observing THEN a download of unknown count`() =
        runTest {
            // Given
            account.awaitsFullSync = true

            // When
            val progress = useCase().first()

            // Then
            assertEquals(
                FirstDownloadProgress(
                    arrivedGroupCount = 0,
                    groupCount = null,
                    isWaitingForInternet = false,
                ),
                progress,
            )
        }

    @Test
    fun `GIVEN an account awaiting its full sync WHEN offline THEN it waits for the internet`() = runTest {
        // Given
        account.awaitsFullSync = true
        isOnline.value = false

        // When
        val progress = useCase().first()

        // Then
        assertEquals(true, progress?.isWaitingForInternet)
    }

    @Test
    fun `GIVEN a first download counting groups WHEN observing THEN its count wins over the account flag`() = runTest {
        // Given
        account.awaitsFullSync = true
        firstDownload.start()
        firstDownload.expect(7)
        firstDownload.groupArrived()

        // When
        val progress = useCase().first()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 1,
                groupCount = 7,
                isWaitingForInternet = false,
            ),
            progress,
        )
    }

    @Test
    fun `GIVEN the copy awaited WHEN a sync of this launch failed online THEN the wait ends`() = runTest {
        // Given
        account.awaitsFullSync = true
        firstDownload.syncFailed()

        // When
        val progress = useCase().first()

        // Then
        assertNull(progress)
    }

    @Test
    fun `GIVEN the copy awaited and a sync that failed WHEN offline THEN it waits for the internet`() = runTest {
        // Given
        account.awaitsFullSync = true
        firstDownload.syncFailed()
        isOnline.value = false

        // When
        val progress = useCase().first()

        // Then
        assertEquals(
            FirstDownloadProgress(
                arrivedGroupCount = 0,
                groupCount = null,
                isWaitingForInternet = true,
            ),
            progress,
        )
    }
}
