package com.quare.blitzsplit.core.expenses.di

import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.GroupCategoryRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomGroupCategoryLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomSettlementLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SettlementLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SettlementRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SupabaseExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SupabaseGroupCategoryRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SupabaseSettlementRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapper
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapperImpl
import com.quare.blitzsplit.core.expenses.data.mapper.GroupCategoryMapper
import com.quare.blitzsplit.core.expenses.data.mapper.GroupCategoryMapperImpl
import com.quare.blitzsplit.core.expenses.data.mapper.SettlementMapper
import com.quare.blitzsplit.core.expenses.data.mapper.SettlementMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.CategoryRepositoryImpl
import com.quare.blitzsplit.core.expenses.data.repository.ExpenseRepositoryImpl
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryDeletedBillRepository
import com.quare.blitzsplit.core.expenses.data.repository.InMemoryRecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.data.repository.SettlementRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.DeletedBillRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.RecordedPaymentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeDeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.usecase.ConsumeRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveDeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecordedPayment
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettledUp
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.expenses.domain.usecase.RecordSettlement
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.SettleUp
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ConsumeRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.NoticeBillGoneUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveCategoryTreeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveDeletedBillNoticeUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsByActivityUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveGroupsMoneyUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObservePeopleBalancesUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveRecordedPaymentUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettledUpUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveSettlementsUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.RecordSettlementUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SaveGroupCategoryUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.SettleUpUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.UpdateExpenseUseCase
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val expenseModule = module {
    singleOf(::RoomExpenseLocalDataSource).bind<ExpenseLocalDataSource>()
    singleOf(::SupabaseExpenseRemoteDataSource).bind<ExpenseRemoteDataSource>()
    singleOf(::ExpenseMapperImpl).bind<ExpenseMapper>()
    singleOf(::ExpenseRepositoryImpl).bind<ExpenseRepository>()
    // Why: read on every call, so a device that changes time zone counts days in the new one.
    single<CurrentTimeZone> { CurrentTimeZone { TimeZone.currentSystemDefault() } }
    factoryOf(::ObserveExpensesUseCase).bind<ObserveExpenses>()
    factoryOf(::CreateExpenseUseCase).bind<CreateExpense>()
    factoryOf(::ObserveExpenseUseCase).bind<ObserveExpense>()
    factoryOf(::UpdateExpenseUseCase).bind<UpdateExpense>()
    factoryOf(::DeleteExpenseUseCase).bind<DeleteExpense>()
    // Why: one for the whole app, since the screen that sets what it knows is not the one that reads it.
    singleOf(::InMemoryDeletedBillRepository).bind<DeletedBillRepository>()
    factoryOf(::NoticeBillGoneUseCase).bind<NoticeBillGone>()
    factoryOf(::ObserveDeletedBillNoticeUseCase).bind<ObserveDeletedBillNotice>()
    factoryOf(::ConsumeDeletedBillNoticeUseCase).bind<ConsumeDeletedBillNotice>()
    singleOf(::RoomSettlementLocalDataSource).bind<SettlementLocalDataSource>()
    singleOf(::SupabaseSettlementRemoteDataSource).bind<SettlementRemoteDataSource>()
    singleOf(::SettlementMapperImpl).bind<SettlementMapper>()
    singleOf(::SettlementRepositoryImpl).bind<SettlementRepository>()
    // Why: one for the whole app, since Registrar pagamento posts the notice that the screen under it shows.
    singleOf(::InMemoryRecordedPaymentRepository).bind<RecordedPaymentRepository>()
    factoryOf(::ObserveSettlementsUseCase).bind<ObserveSettlements>()
    factoryOf(::ObserveGroupBalancesUseCase).bind<ObserveGroupBalances>()
    factoryOf(::ObserveGroupsMoneyUseCase).bind<ObserveGroupsMoney>()
    factoryOf(::ObserveGroupsByActivityUseCase).bind<ObserveGroupsByActivity>()
    factoryOf(::RecordSettlementUseCase).bind<RecordSettlement>()
    factoryOf(::DeleteSettlementUseCase).bind<DeleteSettlement>()
    factoryOf(::ObserveRecordedPaymentUseCase).bind<ObserveRecordedPayment>()
    factoryOf(::ConsumeRecordedPaymentUseCase).bind<ConsumeRecordedPayment>()
    factoryOf(::ObservePeopleBalancesUseCase).bind<ObservePeopleBalances>()
    factoryOf(::SettleUpUseCase).bind<SettleUp>()
    factoryOf(::ObserveSettledUpUseCase).bind<ObserveSettledUp>()
    singleOf(::RoomGroupCategoryLocalDataSource).bind<GroupCategoryLocalDataSource>()
    singleOf(::SupabaseGroupCategoryRemoteDataSource).bind<GroupCategoryRemoteDataSource>()
    singleOf(::GroupCategoryMapperImpl).bind<GroupCategoryMapper>()
    singleOf(::CategoryRepositoryImpl).bind<CategoryRepository>()
    factoryOf(::ObserveCategoryTreeUseCase).bind<ObserveCategoryTree>()
    factoryOf(::SaveGroupCategoryUseCase).bind<SaveGroupCategory>()
    factoryOf(::DeleteGroupCategoryUseCase).bind<DeleteGroupCategory>()
}
