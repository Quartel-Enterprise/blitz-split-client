package com.quare.blitzsplit.feature.categorymap

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import com.quare.blitzsplit.core.expenses.customcolor.CustomColorEvent
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.GroupCategory
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.expenses.domain.model.tint
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveMapCategoriesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RemoveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RestoreCategoryMapUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveCategoryTermUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SetAppCategoryColorUseCase
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryRepository
import com.quare.blitzsplit.core.expenses.testing.FakeCategoryTermRepository
import com.quare.blitzsplit.core.expenses.testing.createFakeCustomColorUseCases
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" from the map (9.4): always a group's category.
class CategoryMapCategoriesTest {
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private lateinit var categories: FakeCategoryRepository
    private lateinit var viewModel: CategoryMapViewModel
    private val actions = mutableListOf<CategoryMapUiAction>()

    // Why: completed by nobody, so a save or delete started while it holds stays in progress.
    private val writeGate = CompletableDeferred<Unit>()

    // Why: the groups on the device, which a sync may change while the map is open.
    private val groupsOnDevice = MutableStateFlow<List<Group>>(emptyList())

    // Why: null holds every group's bills back, as a read Room hasn't answered yet.
    private val billsByGroup = MutableStateFlow<Map<String, List<Expense>>?>(emptyMap())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the map not loaded yet WHEN Nova categoria THEN nothing opens`() = runTest {
        // Given
        prepareScenario(isWhole = false)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canCreateCategory)
        assertNull(state.categoryForm)
    }

    @Test
    fun `GIVEN the map not loaded yet WHEN editing a group category THEN nothing opens`() = runTest {
        // Given
        prepareScenario(isWhole = false)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // Then
        assertNull(viewModel.uiState.value.categoryForm)
    }

    @Test
    fun `GIVEN no group on the device WHEN Nova categoria THEN nothing opens`() = runTest {
        // Given
        prepareScenario(
            groups = emptyList(),
            groupCategories = emptyList(),
        )

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.canCreateCategory)
        assertNull(state.categoryForm)
    }

    @Test
    fun `GIVEN a group on the device WHEN the map opens THEN a category can be created`() = runTest {
        // Given
        prepareScenario(groups = listOf(trip))

        // When
        val state = viewModel.uiState.value

        // Then
        assertTrue(state.canCreateCategory)
    }

    @Test
    fun `GIVEN a main category of the second group WHEN Nova subcategoria in it THEN it goes in that group under it`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PET_ID))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(HOUSE_ID, form.groupId)
            assertEquals(PET_ID, form.parentId)
            assertNull(form.categoryId)
            assertEquals("", form.name)
        }

    @Test
    fun `GIVEN no section picked WHEN Nova categoria THEN a main category in the group with the latest activity`() =
        runTest {
            // Given
            prepareScenario(groups = listOf(house, trip))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(HOUSE_ID, form.groupId)
            assertNull(form.parentId)
        }

    @Test
    fun `GIVEN the app Transporte WHEN Nova subcategoria in it THEN it goes in the latest group under Transporte`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT.key))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(TRIP_ID, form.groupId)
            assertEquals(AppCategory.TRANSPORT.key, form.parentId)
            assertEquals(AppCategory.TRANSPORT.icon, form.icon)
        }

    @Test
    fun `GIVEN the second group category picked WHEN Nova categoria THEN it goes in the latest group`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(PET_ID))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertEquals(TRIP_ID, form.groupId)
        assertNull(form.parentId)
    }

    @Test
    fun `GIVEN a second group category picked WHEN Nova subcategoria in Transporte THEN it goes in the latest group`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(PET_ID))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT.key))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(TRIP_ID, form.groupId)
            assertEquals(AppCategory.TRANSPORT.key, form.parentId)
        }

    @Test
    fun `GIVEN a group category picked while searching WHEN Nova categoria THEN it goes in the latest group`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(PET_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("uber"))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

            // Then
            assertEquals(
                TRIP_ID,
                viewModel.uiState.value.categoryForm
                    ?.groupId,
            )
        }

    @Test
    fun `GIVEN an app category picked beside the list WHEN Nova categoria THEN it goes in the latest group`() =
        runTest {
            // Given
            prepareScenario(groups = listOf(house, trip))
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(AppCategory.TRANSPORT.key))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(HOUSE_ID, form.groupId)
            assertNull(form.parentId)
        }

    @Test
    fun `GIVEN Novo termo open WHEN Nova categoria THEN the term form closes`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.TRANSPORT.key, "pizza"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.termForm)
        assertNotNull(state.categoryForm)
    }

    @Test
    fun `GIVEN an app subcategory WHEN Nova categoria from it THEN starts a main category`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT_RIDE.key))

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertNull(form.parentId)
        assertEquals(TRIP_ID, form.groupId)
    }

    @Test
    fun `GIVEN Outros WHEN Nova categoria from it THEN starts a main category`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.OTHER.key))

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertNull(form.parentId)
        assertEquals(TRIP_ID, form.groupId)
    }

    @Test
    fun `GIVEN an unknown category WHEN Nova categoria from it THEN starts a main category in the latest group`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = "unknown-category"))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertNull(form.parentId)
            assertEquals(TRIP_ID, form.groupId)
        }

    @Test
    fun `GIVEN two groups WHEN a new category opens THEN its group can be picked`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        assertTrue(viewModel.uiState.value.canPickCategoryGroup)
    }

    @Test
    fun `GIVEN a single group WHEN a new category opens THEN its group can not be picked`() = runTest {
        // Given
        prepareScenario(groups = listOf(trip))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        assertFalse(viewModel.uiState.value.canPickCategoryGroup)
    }

    @Test
    fun `GIVEN two groups WHEN editing a group category THEN its group can not be picked`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))

        // Then
        assertFalse(viewModel.uiState.value.canPickCategoryGroup)
    }

    @Test
    fun `GIVEN no form open WHEN the map shows THEN no group can be picked`() = runTest {
        // Given
        prepareScenario()

        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.canPickCategoryGroup)
        assertNull(state.categoryEditor)
    }

    @Test
    fun `GIVEN a new subcategory in Transporte WHEN picking the other group THEN it moves there keeping Transporte`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT.key))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

            // Then
            val state = viewModel.uiState.value
            assertEquals(HOUSE_ID, state.categoryForm?.groupId)
            assertEquals(AppCategory.TRANSPORT.key, state.categoryForm?.parentId)
            assertEquals("Apê 302", state.categoryEditor?.groupName)
        }

    @Test
    fun `GIVEN a subcategory of a group own category WHEN picking the other group THEN it becomes a main one`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PRAIA_ID))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(HOUSE_ID, form.groupId)
            assertNull(form.parentId)
        }

    @Test
    fun `GIVEN a group category being edited WHEN picking the other group THEN it stays in its group`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertEquals(TRIP_ID, form.groupId)
        assertEquals(AppCategory.FOOD.key, form.parentId)
    }

    @Test
    fun `GIVEN a new category WHEN picking a group not on the device THEN it stays in its group`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PRAIA_ID))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked("unknown-group"))

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertEquals(TRIP_ID, form.groupId)
        assertEquals(PRAIA_ID, form.parentId)
    }

    @Test
    fun `GIVEN a new category in the other group WHEN typed a name the first group has THEN it can be saved`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.FOOD.key))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Churrasco"))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.categoryEditor)
            assertFalse(editor.isNameTaken)
            assertTrue(editor.canSave)
        }

    @Test
    fun `GIVEN a new subcategory of Alimentacao in the same group WHEN typed a name it already has THEN it is taken`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.FOOD.key))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("churrasco"))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.categoryEditor)
            assertEquals(TRIP_ID, editor.form.groupId)
            assertTrue(editor.isNameTaken)
            assertFalse(editor.canSave)
        }

    @Test
    fun `GIVEN a new subcategory of Alimentacao WHEN typed the name of an app subcategory THEN it is taken`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.FOOD.key))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mercado"))

            // Then
            val editor = assertNotNull(viewModel.uiState.value.categoryEditor)
            assertTrue(editor.isNameTaken)
            assertFalse(editor.canSave)
        }

    @Test
    fun `GIVEN a new category in the other group WHEN listing Dentro de THEN only that group own ones show`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PET_ID))

            // When
            val editor = assertNotNull(viewModel.uiState.value.categoryEditor)

            // Then
            val parentIds = editor.parents.map { it.id }
            assertTrue(PET_ID in parentIds)
            assertFalse(PRAIA_ID in parentIds)
            assertTrue(AppCategory.TRANSPORT.key in parentIds)
        }

    @Test
    fun `GIVEN a category Ana created WHEN editing it THEN says its group and who created it`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))

        // Then
        val editor = assertNotNull(viewModel.uiState.value.categoryEditor)
        assertEquals("Viagem Floripa", editor.groupName)
        assertEquals("Ana", editor.creatorName)
    }

    @Test
    fun `GIVEN bills in Praia and in Surf WHEN editing Praia THEN counts its group bills with it or Surf`() = runTest {
        // Given
        prepareScenario(
            expenses = mapOf(
                TRIP_ID to listOf(
                    bill("trip-1", TRIP_ID, PRAIA_ID),
                    bill("trip-2", TRIP_ID, SURF_ID),
                    bill("trip-3", TRIP_ID, AppCategory.FOOD_MARKET.key),
                ),
                HOUSE_ID to listOf(bill("house-1", HOUSE_ID, PRAIA_ID)),
            ),
        )

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // Then
        assertEquals(
            2,
            viewModel.uiState.value.categoryEditor
                ?.usage,
        )
    }

    @Test
    fun `GIVEN an app category WHEN editing it THEN nothing opens`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(AppCategory.TRANSPORT.key))

        // Then
        assertNull(viewModel.uiState.value.categoryForm)
    }

    @Test
    fun `GIVEN a group main category WHEN editing it THEN the form holds its group name parent icon and creator`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(PRAIA_ID, form.categoryId)
            assertEquals(TRIP_ID, form.groupId)
            assertEquals("Praia", form.name)
            assertNull(form.parentId)
            assertEquals(CategoryIcon.SUN, form.icon)
            assertEquals(PIERRE_IN_TRIP, form.createdByMemberId)
        }

    @Test
    fun `GIVEN Novo termo open WHEN editing a group category THEN the term form closes`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(AppCategory.TRANSPORT.key, "pizza"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.termForm)
        assertNotNull(state.categoryForm)
    }

    @Test
    fun `GIVEN a new category WHEN typing its name THEN the form holds it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))

        // Then
        assertEquals(
            "Mergulho",
            viewModel.uiState.value.categoryForm
                ?.name,
        )
    }

    @Test
    fun `GIVEN a new main category WHEN picking an icon THEN the form holds it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryIconPicked(CategoryIcon.GIFT))

        // Then
        assertEquals(
            CategoryIcon.GIFT,
            viewModel.uiState.value.categoryForm
                ?.icon,
        )
    }

    @Test
    fun `GIVEN a new main category WHEN picking a color THEN the form holds it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryColorPicked(CategoryColor.BROWN.tint))

        // Then
        assertEquals(
            CategoryColor.BROWN.tint,
            viewModel.uiState.value.categoryForm
                ?.color,
        )
    }

    @Test
    fun `GIVEN New color opened for Food WHEN adding a hex THEN Food wears it and the palette has it`() = runTest {
        // Given
        prepareScenario()
        val food = AppCategory.FOOD
        val current = viewModel.uiState.value.categories
            .findOrNull(food.key)!!
            .color
        viewModel.onEvent(CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnAddClicked(current, food.key)))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnHexChanged("#3A7BD5")))
        viewModel.onEvent(CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnSaveClicked))

        // Then
        val custom = CustomColor(0x3A7BD5)
        val state = viewModel.uiState.value
        assertEquals(custom.tint, state.categories.findOrNull(food.key)?.color)
        assertEquals(listOf(custom), state.customColors.palette)
        assertNull(state.customColors.draft)
    }

    @Test
    fun `GIVEN a new main category WHEN adding a color from its form THEN only the form holds it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        val current = viewModel.uiState.value.categoryForm!!
            .color
        viewModel.onEvent(
            CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnAddClicked(current, categoryKey = null)),
        )

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnHexChanged("3a7bd5")))
        viewModel.onEvent(CategoryMapUiEvent.OnCustomColor(CustomColorEvent.OnSaveClicked))

        // Then
        assertEquals(
            CustomColor(0x3A7BD5).tint,
            viewModel.uiState.value.categoryForm
                ?.color,
        )
        assertEquals(emptyMap(), categories.appColors.value)
    }

    @Test
    fun `GIVEN a new subcategory WHEN picking a color THEN it keeps its parent color`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT.key))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryColorPicked(CategoryColor.BROWN.tint))

        // Then
        assertEquals(
            AppCategory.TRANSPORT.defaultColor.tint,
            viewModel.uiState.value.categoryForm
                ?.color,
        )
    }

    @Test
    fun `GIVEN an app main category WHEN picking its color THEN the tree wears it until restored`() = runTest {
        // Given
        prepareScenario()
        val food = AppCategory.FOOD

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnAppCategoryColorPicked(food.key, CategoryColor.RED.tint))
        val picked = viewModel.uiState.value.categories
            .findOrNull(food.key)
            ?.color
        viewModel.onEvent(CategoryMapUiEvent.OnAppCategoryColorPicked(food.key, null))
        val restored = viewModel.uiState.value.categories
            .findOrNull(food.key)
            ?.color

        // Then
        assertEquals(CategoryColor.RED.tint, picked)
        assertEquals(food.defaultColor.tint, restored)
    }

    @Test
    fun `GIVEN a new subcategory WHEN picking an icon THEN it keeps its parent icon`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.TRANSPORT.key))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryIconPicked(CategoryIcon.GIFT))

        // Then
        assertEquals(
            AppCategory.TRANSPORT.icon,
            viewModel.uiState.value.categoryForm
                ?.icon,
        )
    }

    @Test
    fun `GIVEN a new main category WHEN picking Transporte as its parent THEN it becomes a subcategory of it`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryParentPicked(AppCategory.TRANSPORT.key))

            // Then
            val form = assertNotNull(viewModel.uiState.value.categoryForm)
            assertEquals(AppCategory.TRANSPORT.key, form.parentId)
            assertEquals(AppCategory.TRANSPORT.icon, form.icon)
        }

    @Test
    fun `GIVEN a new category WHEN picking the other group own category as parent THEN it stays a main one`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryParentPicked(PET_ID))

            // Then
            assertNull(
                viewModel.uiState.value.categoryForm
                    ?.parentId,
            )
        }

    @Test
    fun `GIVEN a group main category with a subcategory being edited WHEN picking a parent THEN it stays a main one`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryParentPicked(AppCategory.LEISURE.key))

            // Then
            val state = viewModel.uiState.value
            assertNull(state.categoryForm?.parentId)
            assertEquals(true, state.categoryEditor?.isParentLocked)
        }

    @Test
    fun `GIVEN a new main category in the other group WHEN saving THEN I create it there and Novo termo opens`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryIconPicked(CategoryIcon.GIFT))
            viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged("uber"))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

            // Then
            val created = categories.groupCategories.value.single { it.name == "Mergulho" }
            assertEquals(HOUSE_ID, created.groupId)
            assertNull(created.parentId)
            assertEquals(CategoryIcon.GIFT, created.icon)
            assertEquals(PIERRE_IN_HOUSE, created.createdByMemberId)
            assertTrue(created.isPending)
            val state = viewModel.uiState.value
            assertNull(state.categoryForm)
            assertEquals("", state.query)
            assertEquals(created.id, state.selectedCategoryId)
            val termForm = assertNotNull(state.termForm)
            assertNull(termForm.original)
            assertEquals("", termForm.text)
            assertEquals(created.id, termForm.categoryId)
            assertEquals("Mergulho", state.formCategory?.name)
        }

    @Test
    fun `GIVEN a new subcategory of Praia WHEN saving THEN Praia is picked and Novo termo opens in the new one`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PRAIA_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

            // Then
            val created = categories.groupCategories.value.single { it.name == "Mergulho" }
            assertEquals(TRIP_ID, created.groupId)
            assertEquals(PRAIA_ID, created.parentId)
            assertEquals(PIERRE_IN_TRIP, created.createdByMemberId)
            val state = viewModel.uiState.value
            assertNull(state.categoryForm)
            assertEquals(PRAIA_ID, state.selectedCategoryId)
            assertEquals(created.id, state.termForm?.categoryId)
        }

    @Test
    fun `GIVEN a group category renamed WHEN saving THEN it is updated and the form just closes`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(AppCategory.TRANSPORT.key))
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Churras"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        val saved = categories.groupCategories.value.single { it.id == CHURRASCO_ID }
        assertEquals("Churras", saved.name)
        assertEquals(ANA_IN_TRIP, saved.createdByMemberId)
        assertEquals(4, categories.groupCategories.value.size)
        val state = viewModel.uiState.value
        assertNull(state.categoryForm)
        assertNull(state.termForm)
        assertEquals(AppCategory.TRANSPORT.key, state.selectedCategoryId)
    }

    @Test
    fun `GIVEN the device fails to save WHEN saving a new category THEN the form stays and says it failed`() = runTest {
        // Given
        prepareScenario(writeError = IllegalStateException("disk full"))
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertFalse(form.isSaving)
        assertEquals("Mergulho", form.name)
        assertNull(viewModel.uiState.value.termForm)
        assertEquals(listOf<CategoryMapUiAction>(CategoryMapUiAction.ShowCategorySaveError), actions)
    }

    @Test
    fun `GIVEN a new category with no name WHEN saving THEN nothing is saved`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(4, categories.groupCategories.value.size)
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertFalse(form.isSaving)
    }

    @Test
    fun `GIVEN a new category with a name the group has WHEN saving THEN nothing is saved`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = AppCategory.FOOD.key))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Churrasco"))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(1, categories.groupCategories.value.count { it.name == "Churrasco" })
        assertNotNull(viewModel.uiState.value.categoryForm)
    }

    @Test
    fun `GIVEN a new category being saved WHEN dismissing the form THEN it stays open`() = runTest {
        // Given
        prepareScenario(holdsWrites = true)
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormDismissed)

        // Then
        val form = assertNotNull(viewModel.uiState.value.categoryForm)
        assertTrue(form.isSaving)
    }

    @Test
    fun `GIVEN a new category open WHEN dismissing the form THEN it closes`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormDismissed)

        // Then
        assertNull(viewModel.uiState.value.categoryForm)
    }

    @Test
    fun `GIVEN Surf with a bill being edited WHEN Apagar THEN asks saying its bill goes to Praia`() = runTest {
        // Given
        prepareScenario(expenses = mapOf(TRIP_ID to listOf(bill("trip-1", TRIP_ID, SURF_ID))))
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(SURF_ID))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // Then
        val deletion = assertNotNull(viewModel.uiState.value.categoryDeletion)
        assertEquals(SURF_ID, deletion.category.id)
        assertEquals(1, deletion.billCount)
        assertEquals(PRAIA_ID, deletion.movesTo.id)
        assertFalse(deletion.isDeleting)
    }

    @Test
    fun `GIVEN Praia with bills in it and in Surf being edited WHEN Apagar THEN asks saying they go to Outros`() =
        runTest {
            // Given
            prepareScenario(
                expenses = mapOf(
                    TRIP_ID to listOf(
                        bill("trip-1", TRIP_ID, PRAIA_ID),
                        bill("trip-2", TRIP_ID, SURF_ID),
                    ),
                ),
            )
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

            // Then
            val deletion = assertNotNull(viewModel.uiState.value.categoryDeletion)
            assertEquals(PRAIA_ID, deletion.category.id)
            assertEquals(2, deletion.billCount)
            assertEquals(AppCategory.OTHER.key, deletion.movesTo.id)
        }

    @Test
    fun `GIVEN the bills of Praia not read yet WHEN Apagar THEN nothing asks with a guessed zero`() = runTest {
        // Given
        prepareScenario(holdsBills = true)
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // Then
        val editor = assertNotNull(viewModel.uiState.value.categoryEditor)
        assertNull(editor.usage)
        assertFalse(editor.canDelete)
        assertNull(viewModel.uiState.value.categoryDeletion)
    }

    @Test
    fun `GIVEN the bills of Praia not read yet WHEN they arrive THEN its usage counts them`() = runTest {
        // Given
        prepareScenario(holdsBills = true)
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // When
        billsByGroup.value =
            mapOf(TRIP_ID to listOf(bill("trip-1", TRIP_ID, PRAIA_ID), bill("trip-2", TRIP_ID, SURF_ID)))

        // Then
        assertEquals(
            2,
            viewModel.uiState.value.categoryEditor
                ?.usage,
        )
    }

    @Test
    fun `GIVEN Editar Praia with a bill in Surf WHEN a sync deletes Surf THEN Praia still counts that bill`() =
        runTest {
            // Given
            prepareScenario(expenses = mapOf(TRIP_ID to listOf(bill("trip-1", TRIP_ID, SURF_ID))))
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

            // When
            categories.groupCategories.value = categories.groupCategories.value.map {
                if (it.id == SURF_ID) it.copy(isDeleted = true) else it
            }

            // Then
            assertEquals(
                1,
                viewModel.uiState.value.categoryEditor
                    ?.usage,
            )
        }

    @Test
    fun `GIVEN Nova categoria in Ape 302 WHEN a sync takes the group off the device THEN the form closes`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

        // When
        groupsOnDevice.value = listOf(trip)

        // Then
        assertNull(viewModel.uiState.value.categoryForm)
        assertNull(viewModel.uiState.value.categoryEditor)
    }

    @Test
    fun `GIVEN Nova categoria in the trip WHEN a sync takes the other group off THEN the form stays`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // When
        groupsOnDevice.value = listOf(trip)

        // Then
        assertEquals(
            TRIP_ID,
            viewModel.uiState.value.categoryForm
                ?.groupId,
        )
    }

    @Test
    fun `GIVEN Praia picked beside the list and asked to delete WHEN confirming THEN it is a tombstone and unpicked`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(PRAIA_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

            // Then
            val deleted = categories.groupCategories.value.single { it.id == PRAIA_ID }
            assertTrue(deleted.isDeleted)
            val state = viewModel.uiState.value
            assertNull(state.categoryDeletion)
            assertNull(state.categoryForm)
            assertNull(state.selectedCategoryId)
            assertFalse(state.sections.any { it.category.id == PRAIA_ID })
        }

    @Test
    fun `GIVEN another category picked beside the list WHEN deleting Churrasco THEN the pick stays`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertTrue(
            categories.groupCategories.value
                .single { it.id == CHURRASCO_ID }
                .isDeleted,
        )
        assertEquals(PRAIA_ID, viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun `GIVEN the device fails to delete WHEN confirming THEN the question closes and says it failed`() = runTest {
        // Given
        prepareScenario(writeError = IllegalStateException("disk full"))
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertNull(viewModel.uiState.value.categoryDeletion)
        assertFalse(
            categories.groupCategories.value
                .single { it.id == PRAIA_ID }
                .isDeleted,
        )
        assertEquals(listOf<CategoryMapUiAction>(CategoryMapUiAction.ShowCategoryDeleteError), actions)
    }

    @Test
    fun `GIVEN a category being deleted WHEN dismissing the question THEN it stays`() = runTest {
        // Given
        prepareScenario(holdsWrites = true)
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed)

        // Then
        val deletion = assertNotNull(viewModel.uiState.value.categoryDeletion)
        assertTrue(deletion.isDeleting)
    }

    @Test
    fun `GIVEN the delete question open WHEN dismissing it THEN it closes and the form stays`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.categoryDeletion)
        assertNotNull(state.categoryForm)
        assertFalse(
            categories.groupCategories.value
                .single { it.id == PRAIA_ID }
                .isDeleted,
        )
    }

    @Test
    fun `GIVEN group subcategories with no terms WHEN the map opens THEN they show as rows to add the first one`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val sections = viewModel.uiState.value.sections

            // Then
            val praia = sections.single { it.category.id == PRAIA_ID }
            val surf = praia.rows.single { it.category.id == SURF_ID }
            assertTrue(surf.terms.isEmpty())
            assertEquals("Viagem Floripa", surf.groupName)
            val food = sections.single { it.category.id == AppCategory.FOOD.key }
            assertTrue(food.rows.any { it.category.id == CHURRASCO_ID && it.terms.isEmpty() })
        }

    @Test
    fun `GIVEN an app subcategory with no terms WHEN the map opens THEN it shows no row`() = runTest {
        // Given
        prepareScenario()

        // When
        val sections = viewModel.uiState.value.sections

        // Then
        val education = sections.single { it.category.id == AppCategory.EDUCATION.key }
        assertFalse(education.rows.any { it.category.id == AppCategory.EDUCATION_SUPPLIES.key })
        assertTrue(sections.flatMap { it.rows }.none { !it.category.isGroupCategory && it.terms.isEmpty() })
    }

    @Test
    fun `GIVEN categories of two groups WHEN naming their groups THEN each says its own and the app ones none`() =
        runTest {
            // Given
            prepareScenario()

            // When
            val state = viewModel.uiState.value

            // Then
            val pet = state.sections.single { it.category.id == PET_ID }
            assertEquals("Apê 302", pet.groupName)
            assertEquals("Apê 302", state.groupNameOf(pet.category))
            val praia = state.sections.single { it.category.id == PRAIA_ID }
            assertEquals("Viagem Floripa", state.groupNameOf(praia.category))
            val transport = state.sections.single { it.category.id == AppCategory.TRANSPORT.key }
            assertNull(state.groupNameOf(transport.category))
        }

    @Test
    fun `GIVEN a group on the device WHEN Nova categoria THEN sends the create clicked as a main category`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))

        // Then
        assertEquals(
            listOf(
                "category_create_clicked" to mapOf<String, Any>("is_subcategory" to false, "source" to "category_map"),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Praia in the trip WHEN Nova subcategoria in it THEN sends the create clicked as a subcategory`() =
        runTest {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PRAIA_ID))

            // Then
            assertEquals(
                listOf(
                    "category_create_clicked" to mapOf<String, Any>(
                        "is_subcategory" to true,
                        "source" to "category_map",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a group category WHEN tapping its pencil THEN sends the edit clicked without it`() = runTest {
        // Given
        prepareScenario()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))

        // Then
        assertEquals(
            listOf("category_edit_clicked" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new category in the trip WHEN picking Ape 302 THEN sends the group picked without it`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryGroupPicked(HOUSE_ID))

        // Then
        assertEquals(
            listOf("category_group_picked" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new category WHEN picking Transporte in Dentro de THEN sends the parent picked without it`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryParentPicked(AppCategory.TRANSPORT.key))

            // Then
            assertEquals(
                listOf("category_parent_picked" to mapOf<String, Any>("source" to "category_map")),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a new main category WHEN picking the gift icon THEN sends the icon picked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryIconPicked(CategoryIcon.GIFT))

        // Then
        assertEquals(
            listOf(
                "category_icon_picked" to mapOf<String, Any>("icon" to CategoryIcon.GIFT, "source" to "category_map"),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new main category named WHEN saving THEN sends the save clicked then the new main category saved`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

            // Then
            assertEquals(
                listOf(
                    "category_save_clicked" to mapOf<String, Any>("source" to "category_map"),
                    "category_saved" to mapOf<String, Any>(
                        "is_new" to true,
                        "is_subcategory" to false,
                        "source" to "category_map",
                    ),
                ),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN a new subcategory of Praia named WHEN saving THEN sends the new subcategory saved`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_save_clicked" to mapOf<String, Any>("source" to "category_map"),
                "category_saved" to mapOf<String, Any>(
                    "is_new" to true,
                    "is_subcategory" to true,
                    "source" to "category_map",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Nova categoria inside Transporte WHEN saving THEN sends the new category saved as a subcategory`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Balsa"))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryParentPicked(AppCategory.TRANSPORT.key))
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

            // Then
            assertEquals(
                "category_saved" to mapOf<String, Any>(
                    "is_new" to true,
                    "is_subcategory" to true,
                    "source" to "category_map",
                ),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN Churrasco under Alimentacao renamed WHEN saving THEN sends the category saved as not new`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(CHURRASCO_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Churras"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf(
                "category_save_clicked" to mapOf<String, Any>("source" to "category_map"),
                "category_saved" to mapOf<String, Any>(
                    "is_new" to false,
                    "is_subcategory" to true,
                    "source" to "category_map",
                ),
            ),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the main category Praia renamed WHEN saving THEN sends it saved as not new nor a subcategory`() =
        runTest {
            // Given
            prepareScenario()
            viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Litoral"))
            trackedEvents.clear()

            // When
            viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

            // Then
            assertEquals(
                "category_saved" to mapOf<String, Any>(
                    "is_new" to false,
                    "is_subcategory" to false,
                    "source" to "category_map",
                ),
                trackedEvents.last(),
            )
        }

    @Test
    fun `GIVEN the device fails to save WHEN saving a new category THEN sends only the save clicked`() = runTest {
        // Given
        prepareScenario(writeError = IllegalStateException("disk full"))
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf("category_save_clicked" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a new category with no name WHEN saving THEN sends only the save clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormSaveClicked)

        // Then
        assertEquals(
            listOf("category_save_clicked" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN Praia being edited WHEN Apagar THEN sends the delete clicked`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)

        // Then
        assertEquals(
            listOf("category_delete_clicked" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN the delete question open WHEN confirming THEN sends the delete confirmed`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertEquals(
            listOf("category_delete_confirmed" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a category being deleted WHEN confirming again THEN sends nothing`() = runTest {
        // Given
        prepareScenario(holdsWrites = true)
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no delete question open WHEN confirming THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN the delete question open WHEN dismissing it THEN sends the delete cancelled`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed)

        // Then
        assertEquals(
            listOf("category_delete_cancelled" to mapOf<String, Any>("source" to "category_map")),
            trackedEvents,
        )
    }

    @Test
    fun `GIVEN a category being deleted WHEN dismissing the question THEN sends nothing`() = runTest {
        // Given
        prepareScenario(holdsWrites = true)
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteClicked)
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteConfirmed)
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN no delete question open WHEN dismissing THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnEditCategoryClicked(PRAIA_ID))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryDeleteDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new category open WHEN typing its name THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryNameChanged("Mergulho"))

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    @Test
    fun `GIVEN a new category open WHEN closing the form THEN sends nothing`() = runTest {
        // Given
        prepareScenario()
        viewModel.onEvent(CategoryMapUiEvent.OnNewCategoryClicked(parentId = null))
        trackedEvents.clear()

        // When
        viewModel.onEvent(CategoryMapUiEvent.OnCategoryFormDismissed)

        // Then
        assertTrue(trackedEvents.isEmpty())
    }

    private val trip = group(
        id = TRIP_ID,
        name = "Viagem Floripa",
        members = listOf(member(PIERRE_IN_TRIP, "Pierre", isMe = true), member(ANA_IN_TRIP, "Ana", isMe = false)),
    )

    private val house = group(
        id = HOUSE_ID,
        name = "Apê 302",
        members = listOf(member(BRUNO_IN_HOUSE, "Bruno", isMe = false), member(PIERRE_IN_HOUSE, "Pierre", isMe = true)),
    )

    // Why: Churrasco under the app's Alimentação and Praia with Surf in the trip, Pet in the house.
    private val defaultGroupCategories = listOf(
        groupCategory(CHURRASCO_ID, TRIP_ID, "Churrasco", AppCategory.FOOD.key, createdBy = ANA_IN_TRIP),
        groupCategory(PRAIA_ID, TRIP_ID, "Praia", null, CategoryIcon.SUN, createdBy = PIERRE_IN_TRIP),
        groupCategory(SURF_ID, TRIP_ID, "Surf", PRAIA_ID, createdBy = PIERRE_IN_TRIP),
        groupCategory(PET_ID, HOUSE_ID, "Pet", null, CategoryIcon.PAW, createdBy = BRUNO_IN_HOUSE),
    )

    private fun group(
        id: String,
        name: String,
        members: List<GroupMember>,
    ): Group = Group(
        id = id,
        name = name,
        type = GroupType.TRIP,
        color = AvatarColor.BLUE,
        shape = GroupShape.SQUARE,
        photo = null,
        currency = GroupCurrency.BRL,
        members = members,
        isPending = false,
        inviteCode = null,
        isCreatedByMe = true,
        defaultSplit = null,
        updatedAt = 0L,
        simplifiesDebts = false,
        isSimplifyDebtsPending = false,
        simplifyDebtsChangedBy = null,
        simplifyDebtsConflictBy = null,
    )

    private fun member(
        id: String,
        name: String,
        isMe: Boolean,
    ): GroupMember = GroupMember(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isMe = isMe,
        hasJoined = true,
        userId = null,
        inviteCode = null,
        photoUrl = null,
    )

    private fun groupCategory(
        id: String,
        groupId: String,
        name: String,
        parentId: String?,
        icon: CategoryIcon? = null,
        createdBy: String? = null,
    ): GroupCategory = GroupCategory(
        id = id,
        groupId = groupId,
        name = name,
        parentId = parentId,
        icon = icon,
        color = null,
        createdByMemberId = createdBy,
        isDeleted = false,
        isPending = false,
    )

    private fun bill(
        id: String,
        groupId: String,
        categoryId: String?,
    ): Expense = Expense(
        id = id,
        groupId = groupId,
        description = "Conta",
        currency = GroupCurrency.BRL,
        amountMinor = 1_000,
        splitMode = SplitMode.EQUAL,
        payers = emptyList(),
        shares = emptyList(),
        items = null,
        categoryId = categoryId,
        spentAt = NOW,
        isPending = false,
        updatedAt = NOW,
        updatedByMemberId = null,
    )

    private fun TestScope.prepareScenario(
        groups: List<Group> = listOf(trip, house),
        groupCategories: List<GroupCategory> = defaultGroupCategories,
        expenses: Map<String, List<Expense>> = emptyMap(),
        isWhole: Boolean = true,
        writeError: Throwable? = null,
        holdsWrites: Boolean = false,
        holdsBills: Boolean = false,
    ) {
        groupsOnDevice.value = groups
        billsByGroup.value = if (holdsBills) null else expenses
        val terms = FakeCategoryTermRepository()
        categories = FakeCategoryRepository(initialGroupCategories = groupCategories).also {
            it.saveError = writeError
        }
        val saveGroupCategory = SaveGroupCategoryUseCase(categories) { NOW }
        val deleteGroupCategory = DeleteGroupCategoryUseCase(categories) { NOW }
        val navigator = BackStackNavigator(startRoute = Route.Groups).also { it.navigate(Route.CategoryMap) }
        viewModel = CategoryMapViewModel(
            trackEvent = { name, params -> trackedEvents += name to params },
            navigator = navigator,
            saveCategoryTerm = SaveCategoryTermUseCase(terms) { NOW },
            removeCategoryTerm = RemoveCategoryTermUseCase(terms) { NOW },
            restoreCategoryMap = RestoreCategoryMapUseCase(terms) { NOW },
            setAppCategoryColor = SetAppCategoryColorUseCase(categories),
            saveGroupCategory = SaveGroupCategory {
                categoryId,
                groupId,
                name,
                parentId,
                icon,
                color,
                createdByMemberId,
                ->
                if (holdsWrites) writeGate.await()
                saveGroupCategory(categoryId, groupId, name, parentId, icon, color, createdByMemberId)
            },
            customColorUseCases = createFakeCustomColorUseCases(),
            deleteGroupCategory = { categoryId ->
                if (holdsWrites) writeGate.await()
                deleteGroupCategory(categoryId)
            },
            observeCategoryMap = ObserveCategoryMapUseCase(terms),
            observeMapCategories = ObserveMapCategoriesUseCase(categories),
            observeGroupsByActivity = { groupsOnDevice },
            observeExpenses = { groupId -> billsByGroup.filterNotNull().map { it[groupId].orEmpty() } },
            observeAccount = {
                flowOf(
                    AccountSnapshot(
                        status = AccountStatus.LocalOnly,
                        recoveryCode = null,
                        recoveryCodePrompt = RecoveryCodePrompt.NOT_DUE,
                        email = null,
                        signOutWhenSynced = false,
                        awaitsFullSync = false,
                    ),
                )
            },
            observeFirstDownload = ObserveFirstDownload {
                MutableStateFlow(
                    if (isWhole) {
                        null
                    } else {
                        FirstDownloadProgress(
                            arrivedGroupCount = 0,
                            groupCount = null,
                            isWaitingForInternet = false,
                        )
                    },
                )
            },
            connectivityObserver = NetworkConnectivityObserver { MutableStateFlow(true) },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiAction.collect(actions::add) }
    }

    private companion object {
        const val NOW = 1_000L
        const val TRIP_ID = "group-trip"
        const val HOUSE_ID = "group-house"
        const val PIERRE_IN_TRIP = "member-pierre-trip"
        const val ANA_IN_TRIP = "member-ana-trip"
        const val PIERRE_IN_HOUSE = "member-pierre-house"
        const val BRUNO_IN_HOUSE = "member-bruno-house"
        const val CHURRASCO_ID = "11111111-1111-1111-1111-111111111111"
        const val PRAIA_ID = "22222222-2222-2222-2222-222222222222"
        const val SURF_ID = "33333333-3333-3333-3333-333333333333"
        const val PET_ID = "44444444-4444-4444-4444-444444444444"
    }
}
