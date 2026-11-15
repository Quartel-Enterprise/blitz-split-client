package com.quare.blitzsplit.core.releasenotes.di

import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesLocalDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.ReleaseNotesRemoteDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.RoomReleaseNotesLocalDataSource
import com.quare.blitzsplit.core.releasenotes.data.datasource.SiteReleaseNotesRemoteDataSource
import com.quare.blitzsplit.core.releasenotes.data.parser.ReleaseNotesFeedParser
import com.quare.blitzsplit.core.releasenotes.data.parser.ReleaseNotesFeedParserImpl
import com.quare.blitzsplit.core.releasenotes.data.repository.ReleaseNotesRepositoryImpl
import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetInstalledVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetReleaseNotesLanguage
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.ObserveShowsBetaReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.RefreshReleaseNotes
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.GetInstalledVersionUseCase
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.GetReleaseNotesLanguageUseCase
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.ObserveReleaseNotesUseCase
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.ObserveShowsBetaReleaseNotesUseCase
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.PrefetchReleaseNotesUseCase
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.RefreshReleaseNotesUseCase
import com.quare.blitzsplit.core.releasenotes.generated.ReleaseNotesConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

private const val SITE_TIMEOUT_MILLIS = 20_000L
private val siteClient = named("releaseNotesSiteClient")

val releaseNotesModule = module {
    // Why: a plain client of its own for the public site, on each platform's engine; Supabase's carries the session.
    single(siteClient) {
        HttpClient {
            // Why: a connection that stalls ends in "Couldn't load", with "Try again", not in an endless skeleton.
            install(HttpTimeout) { requestTimeoutMillis = SITE_TIMEOUT_MILLIS }
        }
    }
    single<ReleaseNotesRemoteDataSource> { SiteReleaseNotesRemoteDataSource(httpClient = get(siteClient)) }
    singleOf(::RoomReleaseNotesLocalDataSource).bind<ReleaseNotesLocalDataSource>()
    singleOf(::ReleaseNotesFeedParserImpl).bind<ReleaseNotesFeedParser>()
    singleOf(::ReleaseNotesRepositoryImpl).bind<ReleaseNotesRepository>()
    factory<GetInstalledVersion> { GetInstalledVersionUseCase(versionName = ReleaseNotesConfig.APP_VERSION) }
    factoryOf(::GetReleaseNotesLanguageUseCase).bind<GetReleaseNotesLanguage>()
    factoryOf(::ObserveReleaseNotesUseCase).bind<ObserveReleaseNotes>()
    factoryOf(::RefreshReleaseNotesUseCase).bind<RefreshReleaseNotes>()
    factoryOf(::PrefetchReleaseNotesUseCase).bind<PrefetchReleaseNotes>()
    factoryOf(::ObserveShowsBetaReleaseNotesUseCase).bind<ObserveShowsBetaReleaseNotes>()
}
