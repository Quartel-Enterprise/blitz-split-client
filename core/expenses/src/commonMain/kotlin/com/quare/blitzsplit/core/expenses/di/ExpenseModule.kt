package com.quare.blitzsplit.core.expenses.di

import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.RoomExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SupabaseExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapper
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapperImpl
import com.quare.blitzsplit.core.expenses.data.repository.ExpenseRepositoryImpl
import com.quare.blitzsplit.core.expenses.domain.model.CurrentTimeZone
import com.quare.blitzsplit.core.expenses.domain.model.DeletedBillNotice
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.CreateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.UpdateExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.CreateExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.DeleteExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpenseUseCase
import com.quare.blitzsplit.core.expenses.domain.usecase.impl.ObserveExpensesUseCase
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
    singleOf(::DeletedBillNotice)
}
