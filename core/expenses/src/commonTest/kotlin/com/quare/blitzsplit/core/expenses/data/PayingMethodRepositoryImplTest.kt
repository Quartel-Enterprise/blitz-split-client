package com.quare.blitzsplit.core.expenses.data

import com.quare.blitzsplit.core.expenses.data.datasource.PayingMethodLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.PayingMethodRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.dto.BillPayingMethodDto
import com.quare.blitzsplit.core.expenses.data.dto.PayingMethodDto
import com.quare.blitzsplit.core.expenses.data.mapper.PayingMethodMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.PayingMethodRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.CardColor
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethodType
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PayingMethodRepositoryImplTest {
    private lateinit var local: FakePayingMethodLocalDataSource
    private lateinit var remote: FakePayingMethodRemoteDataSource
    private lateinit var repository: PayingMethodRepositoryImpl

    @Test
    fun `GIVEN live and deleted methods WHEN observing the methods THEN shows the live ones with the default first`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        addedAt = 1L,
                    ),
                    method(
                        id = "gone",
                        addedAt = 2L,
                        isDeleted = true,
                    ),
                    method(
                        id = "cash",
                        type = "cash",
                        addedAt = 3L,
                        isDefault = true,
                    ),
                ),
            )

            // When
            val methods = repository.observeMethods().first()

            // Then
            assertEquals(listOf("cash", "card"), methods.map { it.id })
            assertEquals(listOf(true, false), methods.map { it.isDefault })
        }

    @Test
    fun `GIVEN a bill marked and a mark cleared WHEN observing the marks THEN gives only the marked bill`() = runTest {
        // Given
        prepareScenario(
            storedMarks = listOf(
                mark(
                    expenseId = DINNER,
                    methodId = "card",
                ),
                mark(
                    expenseId = TAXI,
                    methodId = null,
                ),
            ),
        )

        // When
        val marks = repository.observeMarks().first()

        // Then
        assertEquals(mapOf(DINNER to "card"), marks)
    }

    @Test
    fun `GIVEN a new method WHEN saving it THEN stores it pending at the current time with no owner yet`() = runTest {
        // Given
        prepareScenario()
        val card = domainMethod(id = "card")

        // When
        repository.saveMethods(listOf(card))

        // Then
        val stored = local.methods.value.single()
        assertEquals(NOW, stored.updatedAt)
        assertTrue(stored.pendingSync)
        assertNull(stored.ownerUserId)
        assertEquals(listOf(card), repository.observeMethods().first())
    }

    @Test
    fun `GIVEN a method stored ahead of this device clock WHEN editing it THEN stamps it past the stored version`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        updatedAt = NOW + 5_000L,
                    ),
                ),
            )
            val renamed = domainMethod(
                id = "card",
                name = "Inter",
            )

            // When
            repository.saveMethods(listOf(renamed))

            // Then
            val stored = local.methods.value.single()
            assertEquals(NOW + 5_001L, stored.updatedAt)
            assertEquals("Inter", stored.name)
            assertTrue(stored.pendingSync)
        }

    @Test
    fun `GIVEN a method synced to a user WHEN editing it THEN keeps that user as its owner`() = runTest {
        // Given
        prepareScenario(storedMethods = listOf(method(id = "card")))
        val renamed = domainMethod(
            id = "card",
            name = "Inter",
        )

        // When
        repository.saveMethods(listOf(renamed))

        // Then
        assertEquals(
            USER,
            local.methods.value
                .single()
                .ownerUserId,
        )
    }

    @Test
    fun `GIVEN two defaults from devices ahead of this clock WHEN saving a new default THEN it is the default`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "shown-default",
                        isDefault = true,
                        updatedAt = NOW + 5_000L,
                    ),
                    method(
                        id = "older-default",
                        isDefault = true,
                        updatedAt = NOW + 4_000L,
                    ),
                ),
            )
            val formerDefault = repository.observeMethods().first().single { it.isDefault }
            val newDefault = domainMethod(
                id = "pix",
                type = PayingMethodType.PIX,
                isDefault = true,
            )

            // When
            repository.saveMethods(listOf(formerDefault.copy(isDefault = false), newDefault))

            // Then
            assertEquals(
                listOf("pix"),
                repository
                    .observeMethods()
                    .first()
                    .filter { it.isDefault }
                    .map { it.id },
            )
        }

    @Test
    fun `GIVEN a synced default card WHEN deleting it THEN leaves a pending tombstone without name color or default`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        isDefault = true,
                    ),
                ),
            )

            // When
            repository.deleteMethod("card")

            // Then
            val tombstone = local.methods.value.single()
            assertEquals("", tombstone.name)
            assertEquals("", tombstone.icon)
            assertFalse(tombstone.isDefault)
            assertTrue(tombstone.isDeleted)
            assertTrue(tombstone.pendingSync)
            assertEquals(NOW, tombstone.updatedAt)
            assertTrue(repository.observeMethods().first().isEmpty())
        }

    @Test
    fun `GIVEN a method stored ahead of this device clock WHEN deleting it THEN stamps the tombstone past it`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        updatedAt = NOW + 5_000L,
                    ),
                ),
            )

            // When
            repository.deleteMethod("card")

            // Then
            assertEquals(
                NOW + 5_001L,
                local.methods.value
                    .single()
                    .updatedAt,
            )
        }

    @Test
    fun `GIVEN an unknown method WHEN deleting it THEN writes nothing`() = runTest {
        // Given
        prepareScenario(storedMethods = listOf(method(id = "card")))

        // When
        repository.deleteMethod("unknown")

        // Then
        assertEquals(0, local.methodWrites)
        assertEquals(listOf(method(id = "card")), local.methods.value)
    }

    @Test
    fun `GIVEN a method already deleted WHEN deleting it again THEN writes nothing`() = runTest {
        // Given
        val tombstone = method(
            id = "card",
            name = "",
            icon = "",
            isDeleted = true,
            pendingSync = true,
            updatedAt = 3L,
        )
        prepareScenario(storedMethods = listOf(tombstone))

        // When
        repository.deleteMethod("card")

        // Then
        assertEquals(0, local.methodWrites)
        assertEquals(listOf(tombstone), local.methods.value)
    }

    @Test
    fun `GIVEN a bill never marked WHEN marking it THEN stores the mark pending at the current time`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.markBill(DINNER, "card")

        // Then
        assertEquals(
            listOf(
                BillPayingMethodEntity(
                    expenseId = DINNER,
                    methodId = "card",
                    updatedAt = NOW,
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
            local.marks.value,
        )
        assertEquals(mapOf(DINNER to "card"), repository.observeMarks().first())
    }

    @Test
    fun `GIVEN a bill marked with a method WHEN marking it with the same method THEN writes nothing`() = runTest {
        // Given
        prepareScenario(
            storedMarks = listOf(
                mark(
                    expenseId = DINNER,
                    methodId = "card",
                ),
            ),
        )

        // When
        repository.markBill(DINNER, "card")

        // Then
        assertEquals(0, local.markWrites)
    }

    @Test
    fun `GIVEN a bill never marked WHEN clearing its mark THEN writes nothing`() = runTest {
        // Given
        prepareScenario()

        // When
        repository.markBill(DINNER, null)

        // Then
        assertEquals(0, local.markWrites)
        assertTrue(local.marks.value.isEmpty())
    }

    @Test
    fun `GIVEN a marked bill WHEN clearing its mark THEN keeps a pending cleared mark and the bill shows none`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "card",
                    ),
                ),
            )

            // When
            repository.markBill(DINNER, null)

            // Then
            val stored = local.marks.value.single()
            assertNull(stored.methodId)
            assertTrue(stored.pendingSync)
            assertTrue(repository.observeMarks().first().isEmpty())
        }

    @Test
    fun `GIVEN a mark ahead of this device clock WHEN marking another method THEN stamps past it keeping its owner`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "card",
                        updatedAt = NOW + 5_000L,
                    ),
                ),
            )

            // When
            repository.markBill(DINNER, "cash")

            // Then
            assertEquals(
                BillPayingMethodEntity(
                    expenseId = DINNER,
                    methodId = "cash",
                    updatedAt = NOW + 5_001L,
                    pendingSync = true,
                    ownerUserId = USER,
                ),
                local.marks.value.single(),
            )
        }

    @Test
    fun `GIVEN methods and marks of the previous user on the device WHEN syncing THEN drops them and uploads none`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "their-card",
                        pendingSync = true,
                        ownerUserId = PREVIOUS_USER,
                    ),
                ),
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "their-card",
                        pendingSync = true,
                        ownerUserId = PREVIOUS_USER,
                    ),
                ),
            )

            // When
            val result = repository.sync(USER, withMarks = true)

            // Then
            assertTrue(result.isSuccess)
            assertTrue(remote.upsertedMethods.isEmpty())
            assertTrue(remote.upsertedMarks.isEmpty())
            assertTrue(local.methods.value.isEmpty())
            assertTrue(local.marks.value.isEmpty())
        }

    @Test
    fun `GIVEN a method added before any account WHEN syncing THEN uploads it as the user who now owns it`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    pendingSync = true,
                    ownerUserId = null,
                ),
            ),
        )

        // When
        repository.sync(USER, withMarks = false)

        // Then
        assertEquals(listOf(USER), remote.upsertedMethods.flatten().map { it.userId })
        assertEquals(
            USER,
            local.methods.value
                .single()
                .ownerUserId,
        )
    }

    @Test
    fun `GIVEN a newer version on the server WHEN syncing THEN the device takes it synced`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    name = "Nubank",
                    updatedAt = 5L,
                ),
            ),
            serverMethods = listOf(
                methodDto(
                    id = "card",
                    name = "Inter",
                    updatedAt = 9L,
                ),
            ),
        )

        // When
        repository.sync(USER, withMarks = false)

        // Then
        val stored = local.methods.value.single()
        assertEquals("Inter", stored.name)
        assertEquals(9L, stored.updatedAt)
        assertFalse(stored.pendingSync)
    }

    @Test
    fun `GIVEN a newer pending edit on the device WHEN syncing THEN keeps it and uploads it`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    name = "Nubank",
                    updatedAt = 9L,
                    pendingSync = true,
                ),
            ),
            serverMethods = listOf(
                methodDto(
                    id = "card",
                    name = "Inter",
                    updatedAt = 5L,
                ),
            ),
        )

        // When
        repository.sync(USER, withMarks = false)

        // Then
        assertEquals(listOf("Nubank"), remote.upsertedMethods.flatten().map { it.name })
        assertEquals(
            "Nubank",
            local.methods.value
                .single()
                .name,
        )
        assertFalse(
            local.methods.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN a newer pending edit and an older deletion on the server WHEN syncing THEN the deletion wins`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        name = "Edited",
                        updatedAt = 9L,
                        pendingSync = true,
                    ),
                ),
                serverMethods = listOf(
                    methodDto(
                        id = "card",
                        name = "",
                        icon = "",
                        isDeleted = true,
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = false)

            // Then
            assertTrue(repository.observeMethods().first().isEmpty())
            assertTrue(remote.upsertedMethods.flatten().none { it.id == "card" && !it.isDeleted })
            assertTrue(local.methods.value.isEmpty())
        }

    @Test
    fun `GIVEN a method deleted here and a newer edit on the server WHEN syncing THEN it stays deleted everywhere`() =
        runTest {
            // Given
            prepareScenario(
                storedMethods = listOf(
                    method(
                        id = "card",
                        name = "",
                        icon = "",
                        isDeleted = true,
                        updatedAt = 5L,
                        pendingSync = true,
                    ),
                ),
                serverMethods = listOf(
                    methodDto(
                        id = "card",
                        name = "Revived",
                        updatedAt = 9L,
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = false)

            // Then
            assertTrue(repository.observeMethods().first().isEmpty())
            assertEquals(listOf(true), remote.upsertedMethods.flatten().map { it.isDeleted })
            assertTrue(local.methods.value.isEmpty())
        }

    @Test
    fun `GIVEN a deletion on the server of a method the device never had WHEN syncing THEN stores nothing`() = runTest {
        // Given
        prepareScenario(
            serverMethods = listOf(
                methodDto(
                    id = "card",
                    name = "",
                    icon = "",
                    isDeleted = true,
                ),
            ),
        )

        // When
        repository.sync(USER, withMarks = false)

        // Then
        assertEquals(0, local.methodWrites)
        assertTrue(local.methods.value.isEmpty())
    }

    @Test
    fun `GIVEN a method added on another device WHEN syncing THEN the device shows it synced as the user`() = runTest {
        // Given
        prepareScenario(serverMethods = listOf(methodDto(id = "card")))

        // When
        repository.sync(USER, withMarks = false)

        // Then
        assertEquals(listOf("card"), repository.observeMethods().first().map { it.id })
        val stored = local.methods.value.single()
        assertFalse(stored.pendingSync)
        assertEquals(USER, stored.ownerUserId)
    }

    @Test
    fun `GIVEN pending methods WHEN syncing THEN uploads them as the user and nothing stays pending`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    pendingSync = true,
                ),
                method(
                    id = "cash",
                    type = "cash",
                    icon = "",
                    pendingSync = true,
                ),
                method(id = "synced"),
            ),
        )

        // When
        val result = repository.sync(USER, withMarks = false)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            setOf("card", "cash"),
            remote.upsertedMethods
                .flatten()
                .map { it.id }
                .toSet(),
        )
        assertTrue(remote.upsertedMethods.flatten().all { it.userId == USER })
        assertTrue(local.methods.value.none { it.pendingSync })
        assertFalse(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN nothing pending WHEN syncing THEN uploads no methods`() = runTest {
        // Given
        prepareScenario(storedMethods = listOf(method(id = "card")))

        // When
        repository.sync(USER, withMarks = true)

        // Then
        assertTrue(remote.upsertedMethods.isEmpty())
        assertTrue(remote.upsertedMarks.isEmpty())
    }

    @Test
    fun `GIVEN marks on both sides WHEN syncing without the marks THEN neither downloads nor uploads marks`() =
        runTest {
            // Given
            val pendingMark = mark(
                expenseId = DINNER,
                methodId = "card",
                pendingSync = true,
            )
            prepareScenario(
                storedMarks = listOf(pendingMark),
                serverMarks = listOf(
                    markDto(
                        expenseId = TAXI,
                        methodId = "card",
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = false)

            // Then
            assertTrue(remote.fetchedMarkUserIds.isEmpty())
            assertTrue(remote.upsertedMarks.isEmpty())
            assertEquals(listOf(pendingMark), local.marks.value)
        }

    @Test
    fun `GIVEN a newer mark on the server WHEN syncing with the marks THEN the bill takes it`() = runTest {
        // Given
        prepareScenario(
            storedMarks = listOf(
                mark(
                    expenseId = DINNER,
                    methodId = "card",
                    updatedAt = 5L,
                ),
            ),
            serverMarks = listOf(
                markDto(
                    expenseId = DINNER,
                    methodId = "cash",
                    updatedAt = 9L,
                ),
            ),
        )

        // When
        repository.sync(USER, withMarks = true)

        // Then
        assertEquals(mapOf(DINNER to "cash"), repository.observeMarks().first())
        assertFalse(
            local.marks.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN a mark cleared on another device later WHEN syncing with the marks THEN the bill shows none`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "card",
                        updatedAt = 5L,
                    ),
                ),
                serverMarks = listOf(
                    markDto(
                        expenseId = DINNER,
                        methodId = null,
                        updatedAt = 9L,
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = true)

            // Then
            assertTrue(repository.observeMarks().first().isEmpty())
        }

    @Test
    fun `GIVEN a newer pending mark on the device WHEN syncing with the marks THEN keeps it and uploads it`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "card",
                        updatedAt = 9L,
                        pendingSync = true,
                        ownerUserId = null,
                    ),
                ),
                serverMarks = listOf(
                    markDto(
                        expenseId = DINNER,
                        methodId = "cash",
                        updatedAt = 5L,
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = true)

            // Then
            assertEquals(
                listOf(
                    BillPayingMethodDto(
                        userId = USER,
                        expenseId = DINNER,
                        methodId = "card",
                        updatedAt = 9L,
                    ),
                ),
                remote.upsertedMarks.flatten(),
            )
            assertEquals(mapOf(DINNER to "card"), repository.observeMarks().first())
            assertFalse(
                local.marks.value
                    .single()
                    .pendingSync,
            )
        }

    @Test
    fun `GIVEN marks of bills of a group the user left WHEN syncing with marks THEN drops them without upload`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = LEFT_GROUP_BILL,
                        methodId = "card",
                        pendingSync = true,
                    ),
                ),
                serverMarks = listOf(
                    markDto(
                        expenseId = OTHER_LEFT_GROUP_BILL,
                        methodId = "card",
                    ),
                ),
            )

            // When
            repository.sync(USER, withMarks = true)

            // Then
            assertTrue(local.marks.value.isEmpty())
            assertTrue(remote.upsertedMarks.isEmpty())
        }

    @Test
    fun `GIVEN marks of a synced bill and of a bill still waiting WHEN syncing THEN uploads only the synced one`() =
        runTest {
            // Given
            prepareScenario(
                storedMarks = listOf(
                    mark(
                        expenseId = DINNER,
                        methodId = "card",
                        pendingSync = true,
                    ),
                    mark(
                        expenseId = TAXI,
                        methodId = "card",
                        pendingSync = true,
                    ),
                ),
                syncedBills = setOf(DINNER),
            )

            // When
            repository.sync(USER, withMarks = true)

            // Then
            assertEquals(listOf(DINNER), remote.upsertedMarks.flatten().map { it.expenseId })
            assertTrue(
                local.marks.value
                    .single { it.expenseId == TAXI }
                    .pendingSync,
            )
        }

    @Test
    fun `GIVEN the server fails WHEN syncing THEN fails and the methods stay pending`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    pendingSync = true,
                ),
            ),
            fetchError = IllegalStateException("server down"),
        )

        // When
        val result = repository.sync(USER, withMarks = true)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(remote.upsertedMethods.isEmpty())
        assertTrue(
            local.methods.value
                .single()
                .pendingSync,
        )
    }

    @Test
    fun `GIVEN the server refuses the upload WHEN syncing THEN fails and the methods stay pending`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(
                method(
                    id = "card",
                    pendingSync = true,
                ),
            ),
            upsertError = IllegalStateException("server down"),
        )

        // When
        val result = repository.sync(USER, withMarks = true)

        // Then
        assertIs<IllegalStateException>(result.exceptionOrNull())
        assertTrue(
            local.methods.value
                .single()
                .pendingSync,
        )
        assertTrue(repository.observeHasPendingChanges().first())
    }

    @Test
    fun `GIVEN methods and marks WHEN clearing THEN the device keeps none`() = runTest {
        // Given
        prepareScenario(
            storedMethods = listOf(method(id = "card")),
            storedMarks = listOf(
                mark(
                    expenseId = DINNER,
                    methodId = "card",
                ),
            ),
        )

        // When
        repository.clear()

        // Then
        assertTrue(repository.observeMethods().first().isEmpty())
        assertTrue(repository.observeMarks().first().isEmpty())
    }

    private fun method(
        id: String,
        type: String = "credit_card",
        name: String = "Nubank",
        icon: String = "purple",
        isDefault: Boolean = false,
        isDeleted: Boolean = false,
        addedAt: Long = 1L,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = USER,
    ): PayingMethodEntity = PayingMethodEntity(
        id = id,
        type = type,
        name = name,
        icon = icon,
        isDefault = isDefault,
        isDeleted = isDeleted,
        addedAt = addedAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun domainMethod(
        id: String,
        type: PayingMethodType = PayingMethodType.CREDIT_CARD,
        name: String = "Nubank",
        isDefault: Boolean = false,
    ): PayingMethod = PayingMethod(
        id = id,
        type = type,
        name = name,
        cardColor = CardColor.PURPLE.takeIf { type.isCard },
        isDefault = isDefault,
        addedAt = 1L,
    )

    private fun methodDto(
        id: String,
        name: String = "Nubank",
        icon: String = "purple",
        isDeleted: Boolean = false,
        updatedAt: Long = 9L,
    ): PayingMethodDto = PayingMethodDto(
        id = id,
        userId = USER,
        type = "credit_card",
        name = name,
        icon = icon,
        isDefault = false,
        isDeleted = isDeleted,
        addedAt = 1L,
        updatedAt = updatedAt,
    )

    private fun mark(
        expenseId: String,
        methodId: String?,
        updatedAt: Long = 1L,
        pendingSync: Boolean = false,
        ownerUserId: String? = USER,
    ): BillPayingMethodEntity = BillPayingMethodEntity(
        expenseId = expenseId,
        methodId = methodId,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
        ownerUserId = ownerUserId,
    )

    private fun markDto(
        expenseId: String,
        methodId: String?,
        updatedAt: Long = 9L,
    ): BillPayingMethodDto = BillPayingMethodDto(
        userId = USER,
        expenseId = expenseId,
        methodId = methodId,
        updatedAt = updatedAt,
    )

    private fun prepareScenario(
        storedMethods: List<PayingMethodEntity> = emptyList(),
        storedMarks: List<BillPayingMethodEntity> = emptyList(),
        syncedBills: Set<String> = setOf(DINNER, TAXI),
        serverMethods: List<PayingMethodDto> = emptyList(),
        serverMarks: List<BillPayingMethodDto> = emptyList(),
        fetchError: Throwable? = null,
        upsertError: Throwable? = null,
    ) {
        local = FakePayingMethodLocalDataSource(
            storedMethods = storedMethods,
            storedMarks = storedMarks,
            billsOnDevice = setOf(DINNER, TAXI),
            syncedBills = syncedBills,
        )
        remote = FakePayingMethodRemoteDataSource(serverMethods, serverMarks, fetchError, upsertError)
        repository = PayingMethodRepositoryImpl(
            local = local,
            remote = remote,
            mapper = PayingMethodMapperImpl(),
            currentTimeMillis = { NOW },
        )
    }

    private companion object {
        const val NOW = 10_000L
        const val USER = "user-1"
        const val PREVIOUS_USER = "user-0"
        const val DINNER = "dinner"
        const val TAXI = "taxi"
        const val LEFT_GROUP_BILL = "bill-of-left-group"
        const val OTHER_LEFT_GROUP_BILL = "other-bill-of-left-group"
    }
}

// Why: an in-memory version of RoomPayingMethodLocalDataSource, with the DAO's rules for owners, versions and bills.
private class FakePayingMethodLocalDataSource(
    private val billsOnDevice: Set<String>,
    private val syncedBills: Set<String>,
    storedMethods: List<PayingMethodEntity>,
    storedMarks: List<BillPayingMethodEntity>,
) : PayingMethodLocalDataSource {
    val methods = MutableStateFlow(storedMethods)
    val marks = MutableStateFlow(storedMarks)
    var methodWrites = 0
    var markWrites = 0

    override fun observeLiveMethods(): Flow<List<PayingMethodEntity>> = methods.map { list ->
        list.filterNot { it.isDeleted }.sortedWith(compareBy({ it.addedAt }, { it.id }))
    }

    override fun observeMarks(): Flow<List<BillPayingMethodEntity>> = marks.map { list ->
        list.filter { it.methodId != null }
    }

    override fun observeHasPending(): Flow<Boolean> = methods.map { list ->
        list.any { it.pendingSync } || marks.value.any { it.pendingSync && it.expenseId in syncedBills }
    }

    override suspend fun getAllMethods(): List<PayingMethodEntity> = methods.value

    override suspend fun getAllMarks(): List<BillPayingMethodEntity> = marks.value

    override suspend fun getMark(expenseId: String): BillPayingMethodEntity? = marks.value.find {
        it.expenseId ==
            expenseId
    }

    override suspend fun getMarksReadyToUpload(): List<BillPayingMethodEntity> = marks.value.filter {
        it.pendingSync && it.expenseId in syncedBills
    }

    override suspend fun upsertMethods(entities: List<PayingMethodEntity>) {
        methodWrites++
        val ids = entities.map { it.id }.toSet()
        methods.update { list -> list.filterNot { it.id in ids } + entities }
    }

    override suspend fun upsertMarks(entities: List<BillPayingMethodEntity>) {
        markWrites++
        val ids = entities.map { it.expenseId }.toSet()
        marks.update { list -> list.filterNot { it.expenseId in ids } + entities }
    }

    override suspend fun markMethodSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        methods.update { list ->
            list.map { if (it.id == id && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it }
        }
    }

    override suspend fun markMarkSynced(
        expenseId: String,
        syncedUpdatedAt: Long,
    ) {
        marks.update { list ->
            list.map {
                if (it.expenseId == expenseId && it.updatedAt == syncedUpdatedAt) it.copy(pendingSync = false) else it
            }
        }
    }

    override suspend fun deleteSyncedTombstones() {
        methods.update { list -> list.filterNot { it.isDeleted && !it.pendingSync } }
    }

    override suspend fun deleteOrphanMarks() {
        marks.update { list -> list.filter { it.expenseId in billsOnDevice } }
    }

    override suspend fun keepOnlyUser(userId: String) {
        methods.update { list ->
            list
                .filter { it.ownerUserId == null || it.ownerUserId == userId }
                .map { it.copy(ownerUserId = userId) }
        }
        marks.update { list ->
            list
                .filter { it.ownerUserId == null || it.ownerUserId == userId }
                .map { it.copy(ownerUserId = userId) }
        }
    }

    override suspend fun clear() {
        methods.value = emptyList()
        marks.value = emptyList()
    }
}

private class FakePayingMethodRemoteDataSource(
    private val methods: List<PayingMethodDto>,
    private val marks: List<BillPayingMethodDto>,
    private val fetchError: Throwable?,
    private val upsertError: Throwable?,
) : PayingMethodRemoteDataSource {
    val upsertedMethods = mutableListOf<List<PayingMethodDto>>()
    val upsertedMarks = mutableListOf<List<BillPayingMethodDto>>()
    val fetchedMarkUserIds = mutableListOf<String>()

    override suspend fun upsertMethods(methods: List<PayingMethodDto>) {
        upsertError?.let { throw it }
        upsertedMethods += methods
    }

    override suspend fun fetchMethods(userId: String): List<PayingMethodDto> {
        fetchError?.let { throw it }
        return methods.filter { it.userId == userId }
    }

    override suspend fun upsertMarks(marks: List<BillPayingMethodDto>) {
        upsertError?.let { throw it }
        upsertedMarks += marks
    }

    override suspend fun fetchMarks(userId: String): List<BillPayingMethodDto> {
        fetchError?.let { throw it }
        fetchedMarkUserIds += userId
        return marks.filter { it.userId == userId }
    }
}
