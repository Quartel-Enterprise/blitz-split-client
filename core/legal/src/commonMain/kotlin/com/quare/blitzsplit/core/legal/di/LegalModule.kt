package com.quare.blitzsplit.core.legal.di

import com.quare.blitzsplit.core.legal.data.datasource.LegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.datasource.RoomLegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.SiteLegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.parser.LegalDocumentParser
import com.quare.blitzsplit.core.legal.data.parser.LegalDocumentParserImpl
import com.quare.blitzsplit.core.legal.data.repository.LegalRepositoryImpl
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.legal.domain.usecase.ObserveLegalDocument
import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.legal.domain.usecase.RefreshLegalDocument
import com.quare.blitzsplit.core.legal.domain.usecase.impl.GetLegalLanguageUseCase
import com.quare.blitzsplit.core.legal.domain.usecase.impl.ObserveLegalDocumentUseCase
import com.quare.blitzsplit.core.legal.domain.usecase.impl.PrefetchLegalDocumentsUseCase
import com.quare.blitzsplit.core.legal.domain.usecase.impl.RefreshLegalDocumentUseCase
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

private const val SITE_TIMEOUT_MILLIS = 20_000L
private val siteClient = named("legalSiteClient")

val legalModule = module {
    // Why: a plain client of its own for the public site, on each platform's engine; Supabase's carries the session.
    single(siteClient) {
        HttpClient {
            // Why: a connection that stalls ends in "Couldn't load", with "Try again", not in an endless skeleton.
            install(HttpTimeout) { requestTimeoutMillis = SITE_TIMEOUT_MILLIS }
        }
    }
    single<LegalRemoteDataSource> { SiteLegalRemoteDataSource(httpClient = get(siteClient)) }
    singleOf(::RoomLegalLocalDataSource).bind<LegalLocalDataSource>()
    singleOf(::LegalDocumentParserImpl).bind<LegalDocumentParser>()
    singleOf(::LegalRepositoryImpl).bind<LegalRepository>()
    factoryOf(::GetLegalLanguageUseCase).bind<GetLegalLanguage>()
    factoryOf(::ObserveLegalDocumentUseCase).bind<ObserveLegalDocument>()
    factoryOf(::RefreshLegalDocumentUseCase).bind<RefreshLegalDocument>()
    factory<PrefetchLegalDocuments> {
        PrefetchLegalDocumentsUseCase(
            drawsTextsInApp = drawsLegalTextsInApp,
            getLegalLanguage = get(),
            repository = get(),
        )
    }
}
