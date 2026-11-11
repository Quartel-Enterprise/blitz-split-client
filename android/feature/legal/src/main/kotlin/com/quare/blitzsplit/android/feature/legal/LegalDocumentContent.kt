package com.quare.blitzsplit.android.feature.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.EmptyStateColumn
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.legal.domain.model.LegalBlock
import com.quare.blitzsplit.core.legal.domain.model.LegalDate
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.legal.domain.model.LegalSpan
import com.quare.blitzsplit.core.legal.domain.model.LegalTableRow
import com.quare.blitzsplit.core.legal.domain.model.LegalText
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiEvent
import com.quare.blitzsplit.feature.legal.presentation.model.LegalDocumentUiState
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.quare.blitzsplit.android.designsystem.R as DesignR

private const val HEADER_ITEMS = 1
private val textColumnMaxWidth = 640.dp
private val contentPadding = PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = 8.dp,
    bottom = 32.dp,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentContent(
    state: LegalDocumentUiState,
    onEvent: (LegalDocumentUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                // Why: the page's name in the app's language, there before the text; the text's own may be in another.
                title = { Text(stringResource(state.page.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(LegalDocumentUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(DesignR.string.designsystem_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // Why: one column of text at every size, which only centers in a wider window.
        AdaptiveScreen(modifier = Modifier.fillMaxSize().padding(padding)) {
            val document = state.document
            when {
                document != null -> DocumentList(
                    page = state.page,
                    document = document,
                    focusedBlockIndex = state.focusedBlockIndex,
                    onLinkClick = { link -> onEvent(LegalDocumentUiEvent.OnLinkClicked(link)) },
                )

                state.showsLoadFailed -> LoadFailedColumn(onRetry = { onEvent(LegalDocumentUiEvent.OnRetryClicked) })

                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LegalDocumentSkeleton(modifier = Modifier.textColumn())
                }
            }
        }
    }
}

@Composable
private fun DocumentList(
    page: LegalPage,
    document: LegalDocument,
    focusedBlockIndex: Int?,
    onLinkClick: (LegalLink) -> Unit,
) {
    /*
     * Why: the section a link asked for is there from the list's first frame, and only then: the state is kept, so
     * the current text taking the saved one's place, or a rotation, never scrolls again.
     */
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = focusedBlockIndex?.plus(HEADER_ITEMS) ?: 0,
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "updated") {
            Text(
                text = stringResource(page.updatedRes, document.updated.toLongDateText()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.textColumn(),
            )
        }
        items(document.blocks) { block ->
            LegalBlockItem(
                block = block,
                onLinkClick = onLinkClick,
                modifier = Modifier.textColumn(),
            )
        }
    }
}

@Composable
private fun LoadFailedColumn(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        EmptyStateColumn(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.legal_load_failed_title),
            body = stringResource(R.string.legal_load_failed_body),
            action = stringResource(R.string.legal_retry),
            onAction = onRetry,
            modifier = Modifier.textColumn(),
        )
    }
}

// Why: "November 5, 2026" in the app's language; a day the calendar doesn't have (the site's slip) reads as written.
@Composable
private fun LegalDate.toLongDateText(): String {
    val locale = LocalLocale.current.platformLocale
    return remember(this, locale) {
        try {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
            LocalDate.of(year, month, day).format(formatter)
        } catch (_: DateTimeException) {
            "%04d-%02d-%02d".format(locale, year, month, day)
        }
    }
}

private fun Modifier.textColumn(): Modifier = widthIn(max = textColumnMaxWidth).fillMaxWidth()

@get:StringRes
private val LegalPage.titleRes: Int
    get() = when (this) {
        LegalPage.PRIVACY_POLICY -> R.string.legal_privacy_policy
        LegalPage.TERMS_OF_USE -> R.string.legal_terms
    }

// Why: Portuguese, Spanish and Italian say it in the page's gender ("Atualizada em", "Atualizados em"), as the site.
@get:StringRes
private val LegalPage.updatedRes: Int
    get() = when (this) {
        LegalPage.PRIVACY_POLICY -> R.string.legal_updated_privacy_policy
        LegalPage.TERMS_OF_USE -> R.string.legal_updated_terms
    }

private fun textOf(
    text: String,
    isBold: Boolean = false,
    link: LegalLink? = null,
): LegalSpan = LegalSpan(
    text = text,
    isBold = isBold,
    link = link,
)

private fun plainOf(text: String): LegalText = LegalText(listOf(textOf(text)))

private fun createSampleDocument(): LegalDocument = LegalDocument(
    title = "Privacy policy",
    updated = LegalDate(
        year = 2026,
        month = 11,
        day = 5,
    ),
    blocks = listOf(
        LegalBlock.Callout(
            title = plainOf("In short"),
            paragraph = null,
            items = listOf(
                plainOf("You use the app without creating an account. Google and Apple are optional."),
                plainOf("People in a group see what belongs to the group. Nobody sees your email."),
                plainOf("We don't sell data. You delete your account and data from the app itself."),
            ),
        ),
        LegalBlock.Section(
            number = 1,
            anchor = "who",
            title = plainOf("Who we are"),
        ),
        LegalBlock.Paragraph(
            LegalText(
                listOf(
                    textOf("This policy explains how "),
                    textOf(
                        text = "Blitz Split",
                        isBold = true,
                    ),
                    textOf(" handles personal data. By using the app you agree to the "),
                    textOf(
                        text = "Terms of use",
                        link = LegalLink.Page(
                            page = LegalPage.TERMS_OF_USE,
                            anchor = null,
                        ),
                    ),
                    textOf(", which this policy is part of."),
                ),
            ),
        ),
        LegalBlock.Section(
            number = 2,
            anchor = "data",
            title = plainOf("The data we handle"),
        ),
        LegalBlock.Heading(plainOf("What you give us")),
        LegalBlock.Bullets(
            listOf(
                LegalText(
                    listOf(
                        textOf(
                            text = "Profile:",
                            isBold = true,
                        ),
                        textOf(" your name, a photo (optional) and your avatar color."),
                    ),
                ),
                plainOf("Payments you record between members."),
            ),
        ),
        LegalBlock.Callout(
            title = plainOf("Scan receipt"),
            paragraph = plainOf("The photo goes to our server and is read by an AI model. We don't keep it."),
            items = emptyList(),
        ),
        LegalBlock.Section(
            number = 3,
            anchor = "use",
            title = plainOf("What we use it for"),
        ),
        LegalBlock.Table(
            header = listOf(plainOf("Purpose"), plainOf("Data"), plainOf("Legal basis (LGPD)")),
            rows = listOf(
                LegalTableRow(
                    listOf(
                        plainOf("Store, sync and show to the group"),
                        plainOf("Profile, groups, bills, payments, payment keys"),
                        plainOf("Performance of a contract (art. 7, V)"),
                    ),
                ),
                LegalTableRow(
                    listOf(
                        plainOf("Sign in and recover access"),
                        plainOf("Anonymous id, recovery code, Google or Apple account"),
                        plainOf("Performance of a contract"),
                    ),
                ),
            ),
        ),
        LegalBlock.Paragraph(
            LegalText(
                listOf(
                    textOf("Questions? Write to "),
                    textOf(
                        text = "privacy@blitzsplit.com",
                        link = LegalLink.External("mailto:privacy@blitzsplit.com"),
                    ),
                    textOf("."),
                ),
            ),
        ),
    ),
)

@DevicePreviews
@Composable
private fun LegalDocumentPreview() {
    BlitzSplitTheme {
        LegalDocumentContent(
            state = LegalDocumentUiState(
                page = LegalPage.PRIVACY_POLICY,
                document = createSampleDocument(),
                hasLoadFailed = false,
                focusedBlockIndex = null,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun LegalDocumentSkeletonPreview() {
    BlitzSplitTheme {
        LegalDocumentContent(
            state = LegalDocumentUiState(
                page = LegalPage.TERMS_OF_USE,
                document = null,
                hasLoadFailed = false,
                focusedBlockIndex = null,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun LegalDocumentLoadFailedPreview() {
    BlitzSplitTheme {
        LegalDocumentContent(
            state = LegalDocumentUiState(
                page = LegalPage.PRIVACY_POLICY,
                document = null,
                hasLoadFailed = true,
                focusedBlockIndex = null,
            ),
            onEvent = {},
        )
    }
}
