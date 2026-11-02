package com.quare.blitzsplit.core.profile.data

import com.quare.blitzsplit.core.profile.data.datasource.PushDeviceRemoteDataSource
import com.quare.blitzsplit.core.profile.data.dto.RegisterPushDeviceParamsDto
import com.quare.blitzsplit.core.profile.data.mapper.PushDeviceMapperImpl
import com.quare.blitzsplit.core.profile.data.repository.PushDeviceRepositoryImpl
import com.quare.blitzsplit.core.profile.domain.model.ApnsEnvironment
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.model.PushPlatform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushDeviceRepositoryImplTest {
    private lateinit var remote: FakePushDeviceRemoteDataSource
    private lateinit var repository: PushDeviceRepositoryImpl

    @Test
    fun `GIVEN no token yet WHEN observing the device THEN there is none`() = runTest {
        // Given
        prepareScenario(device = null)

        // When
        val observed = repository.observeDevice().first()

        // Then
        assertNull(observed)
    }

    @Test
    fun `GIVEN no token yet WHEN the platform hands one over THEN observing gives that device`() = runTest {
        // Given
        prepareScenario(device = null)
        val android = device(token = "fcm-1")

        // When
        repository.setDevice(android)

        // Then
        assertEquals(android, repository.observeDevice().first())
    }

    @Test
    fun `GIVEN a token WHEN the platform hands over a new one THEN observing gives the latest`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))
        val renewed = device(token = "fcm-2")

        // When
        repository.setDevice(renewed)

        // Then
        assertEquals(renewed, repository.observeDevice().first())
    }

    @Test
    fun `GIVEN no token yet WHEN registering THEN succeeds without asking the server`() = runTest {
        // Given
        prepareScenario(device = null)

        // When
        val result = repository.register(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(remote.registered.isEmpty())
    }

    @Test
    fun `GIVEN an Android token WHEN registering THEN sends the device to the server`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))

        // When
        val result = repository.register(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf(
                RegisterPushDeviceParamsDto(
                    token = "fcm-1",
                    platform = "android",
                    apnsEnvironment = null,
                    webP256dh = null,
                    webAuth = null,
                ),
            ),
            remote.registered,
        )
    }

    @Test
    fun `GIVEN the device registered for a user WHEN registering it again for the same user THEN sends nothing`() =
        runTest {
            // Given
            prepareScenario(device = device(token = "fcm-1"))
            repository.register(userId = "user-1")

            // When
            val result = repository.register(userId = "user-1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, remote.registered.size)
        }

    @Test
    fun `GIVEN the device registered WHEN the token changes and registering again THEN sends the new token`() =
        runTest {
            // Given
            prepareScenario(device = device(token = "fcm-1"))
            repository.register(userId = "user-1")
            repository.setDevice(device(token = "fcm-2"))

            // When
            repository.register(userId = "user-1")

            // Then
            assertEquals(listOf("fcm-1", "fcm-2"), remote.registered.map { it.token })
        }

    @Test
    fun `GIVEN a browser registered WHEN its keys change and registering again THEN sends the new keys`() = runTest {
        // Given
        prepareScenario(
            device = webDevice(
                p256dh = "key-1",
                auth = "auth-1",
            ),
        )
        repository.register(userId = "user-1")
        repository.setDevice(
            webDevice(
                p256dh = "key-2",
                auth = "auth-2",
            ),
        )

        // When
        repository.register(userId = "user-1")

        // Then
        assertEquals(
            listOf("key-1" to "auth-1", "key-2" to "auth-2"),
            remote.registered.map { it.webP256dh to it.webAuth },
        )
    }

    @Test
    fun `GIVEN the device registered for a user WHEN registering it for another user THEN sends it again`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))
        repository.register(userId = "user-1")

        // When
        val result = repository.register(userId = "user-2")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, remote.registered.size)
    }

    @Test
    fun `GIVEN the device moved to another user WHEN the first user registers it again THEN sends it again`() =
        runTest {
            // Given
            prepareScenario(device = device(token = "fcm-1"))
            repository.register(userId = "user-1")
            repository.register(userId = "user-2")

            // When
            repository.register(userId = "user-1")

            // Then
            assertEquals(3, remote.registered.size)
        }

    @Test
    fun `GIVEN the server fails WHEN registering THEN returns the failure`() = runTest {
        // Given
        prepareScenario(
            device = device(token = "fcm-1"),
            registerError = IllegalStateException("server down"),
        )

        // When
        val result = repository.register(userId = "user-1")

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a failed register WHEN registering again THEN tries the server again`() = runTest {
        // Given
        prepareScenario(
            device = device(token = "fcm-1"),
            registerError = IllegalStateException("server down"),
        )
        repository.register(userId = "user-1")
        remote.registerError = null

        // When
        val result = repository.register(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, remote.registerCalls)
        assertEquals(1, remote.registered.size)
    }

    @Test
    fun `GIVEN the register is cancelled WHEN registering THEN the cancellation goes through`() = runTest {
        // Given
        prepareScenario(
            device = device(token = "fcm-1"),
            registerError = CancellationException("signed out"),
        )

        // When
        val error = assertFailsWith<CancellationException> { repository.register(userId = "user-1") }

        // Then
        assertEquals("signed out", error.message)
    }

    @Test
    fun `GIVEN no token yet WHEN unregistering THEN succeeds without asking the server`() = runTest {
        // Given
        prepareScenario(device = null)

        // When
        val result = repository.unregister()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, remote.unregisterCalls)
    }

    @Test
    fun `GIVEN a token this launch never registered WHEN unregistering THEN asks the server to forget it`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))

        // When
        val result = repository.unregister()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("fcm-1"), remote.unregisteredTokens)
    }

    @Test
    fun `GIVEN the device registered WHEN unregistering THEN asks the server to forget its current token`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))
        repository.register(userId = "user-1")
        repository.setDevice(device(token = "fcm-2"))

        // When
        repository.unregister()

        // Then
        assertEquals(listOf("fcm-2"), remote.unregisteredTokens)
    }

    @Test
    fun `GIVEN the server fails WHEN unregistering THEN returns the failure`() = runTest {
        // Given
        prepareScenario(
            device = device(token = "fcm-1"),
            unregisterError = IllegalStateException("server down"),
        )

        // When
        val result = repository.unregister()

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }

    @Test
    fun `GIVEN the unregister is cancelled WHEN unregistering THEN the cancellation goes through`() = runTest {
        // Given
        prepareScenario(
            device = device(token = "fcm-1"),
            unregisterError = CancellationException("left"),
        )

        // When
        val error = assertFailsWith<CancellationException> { repository.unregister() }

        // Then
        assertEquals("left", error.message)
    }

    @Test
    fun `GIVEN a sign out after registering WHEN the same user signs in again THEN sends the device again`() = runTest {
        // Given
        prepareScenario(device = device(token = "fcm-1"))
        repository.register(userId = "user-1")
        repository.unregister()

        // When
        val result = repository.register(userId = "user-1")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, remote.registered.size)
    }

    private fun device(
        token: String,
        platform: PushPlatform = PushPlatform.ANDROID,
        apnsEnvironment: ApnsEnvironment? = null,
    ): PushDevice = PushDevice(
        token = token,
        platform = platform,
        apnsEnvironment = apnsEnvironment,
        webP256dh = null,
        webAuth = null,
    )

    private fun webDevice(
        p256dh: String,
        auth: String,
    ): PushDevice = PushDevice(
        token = "https://push.example/endpoint",
        platform = PushPlatform.WEB,
        apnsEnvironment = null,
        webP256dh = p256dh,
        webAuth = auth,
    )

    private fun prepareScenario(
        device: PushDevice?,
        registerError: Throwable? = null,
        unregisterError: Throwable? = null,
    ) {
        remote = FakePushDeviceRemoteDataSource().also {
            it.registerError = registerError
            it.unregisterError = unregisterError
        }
        repository = PushDeviceRepositoryImpl(
            remoteDataSource = remote,
            mapper = PushDeviceMapperImpl(),
        )
        device?.let(repository::setDevice)
    }
}

private class FakePushDeviceRemoteDataSource : PushDeviceRemoteDataSource {
    val registered = mutableListOf<RegisterPushDeviceParamsDto>()
    val unregisteredTokens = mutableListOf<String>()
    var registerCalls = 0
    var unregisterCalls = 0
    var registerError: Throwable? = null
    var unregisterError: Throwable? = null

    override suspend fun register(params: RegisterPushDeviceParamsDto) {
        registerCalls++
        registerError?.let { throw it }
        registered += params
    }

    override suspend fun unregister(token: String) {
        unregisterCalls++
        unregisterError?.let { throw it }
        unregisteredTokens += token
    }
}
