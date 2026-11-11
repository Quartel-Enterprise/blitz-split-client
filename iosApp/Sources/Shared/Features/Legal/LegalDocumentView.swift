import Observation
import Shared
import SwiftUI

@Observable
final class LegalDocumentModel {
    private(set) var state: LegalDocumentUiState
    /// Whether the text already opened scrolled to the section a link asked for, so coming back to it stays put.
    @ObservationIgnored var didFocus = false
    /// A link to the section this very screen opened at: the text scrolls back to it, each tap a new request.
    private(set) var sectionScroll: LegalSectionScroll?

    @ObservationIgnored private let holder: ViewModelHolder<LegalDocumentViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteLegalDocument) {
        holder = BlitzSplitApple.shared.createLegalDocumentViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! LegalDocumentUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if let scroll = action as? LegalDocumentUiActionScrollToSection {
                self?.sectionScroll = LegalSectionScroll(blockIndex: scroll.blockIndex?.intValue)
                return
            }
            guard let open = action as? LegalDocumentUiActionOpenUrl, let url = URL(string: open.url) else { return }
            #if os(iOS)
            UIApplication.shared.open(url)
            #else
            NSWorkspace.shared.open(url)
            #endif
        })
    }

    func send(_ event: any LegalDocumentUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// One request to scroll the text to a section's block, or to its top.
struct LegalSectionScroll: Equatable {
    let id = UUID()
    let blockIndex: Int?
}

/// "Privacy Policy" and "Terms of Service", drawn from the blocks the shared code reads the site's Markdown into:
/// pushed over the profile on iPhone and iPad, the page's name in the bar, and a sheet on the Mac's Settings with
/// Done. One column, 640 pt at most, centred in a wider window. A link to the other text opens it over this one
/// (on the Mac, in this sheet's place, Done coming back to this one); a web or mail address opens outside the app.
struct LegalDocumentView: View {
    let route: RouteLegalDocument

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: LegalDocumentModel?

    private var title: LocalizedStringKey {
        route.page == .privacyPolicy ? "Privacy Policy" : "Terms of Service"
    }

    var body: some View {
        Group {
            if let model {
                LegalDocumentScreen(model: model, title: title)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        #if os(iOS)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        #else
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") { model?.send(LegalDocumentUiEventOnBackClicked.shared) }
            }
        }
        .onExitCommand { model?.send(LegalDocumentUiEventOnBackClicked.shared) }
        .frame(minWidth: 480, idealWidth: 700, minHeight: 400, idealHeight: 620)
        #endif
        .onAppear {
            if model == nil { model = LegalDocumentModel(route: route) }
        }
    }
}

private struct LegalDocumentScreen: View {
    let model: LegalDocumentModel
    let title: LocalizedStringKey

    private var state: LegalDocumentUiState {
        model.state
    }

    var body: some View {
        Group {
            if let document = state.document {
                LegalDocumentText(model: model, document: document, title: title)
            } else if state.showsLoadFailed {
                ContentUnavailableView {
                    Label("Couldn't load this page", systemImage: "wifi.exclamationmark")
                } description: {
                    Text("Check your connection and try again.")
                } actions: {
                    Button("Try again") {
                        model.send(LegalDocumentUiEventOnRetryClicked.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .prominentLabel()
                }
            } else {
                // A device that never downloaded this text: its lines shimmer until it arrives or fails
                ScrollView {
                    LegalDocumentPlaceholder()
                        .shimmering()
                        .legalColumn()
                }
                .scrollDisabled(true)
            }
        }
        .background(Palette.groupedBackground.ignoresSafeArea())
    }
}

private struct LegalDocumentText: View {
    let model: LegalDocumentModel
    let document: LegalDocument
    let title: LocalizedStringKey

    /// The first line's id, for a scroll back to the top; the blocks' ids are their indexes.
    static let topId = "top"

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    #if os(macOS)
                    // A sheet has no bar to name the page in
                    Text(title)
                        .font(.title2.bold())
                        .accessibilityAddTraits(.isHeader)
                    #endif
                    Text(isPrivacyPolicy ? "Updated \(updated) (privacy policy)" : "Updated \(updated) (terms)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .id(Self.topId)
                    ForEach(Array(document.blocks.enumerated()), id: \.offset) { index, block in
                        LegalBlockView(block: block)
                            .id(index)
                    }
                }
                .textSelection(.enabled)
                .legalColumn()
            }
            // The section a link of the other text asked for, once: the current text taking the saved one's place
            // and coming back to this screen both stay where the reader is
            .onChange(of: model.state.focusedBlockIndex, initial: true) { _, index in
                guard let index, !model.didFocus else { return }
                model.didFocus = true
                proxy.scrollTo(Int(truncating: index), anchor: .top)
            }
            .onChange(of: model.sectionScroll) { _, scroll in
                guard let scroll else { return }
                withAnimation {
                    if let blockIndex = scroll.blockIndex {
                        proxy.scrollTo(blockIndex, anchor: .top)
                    } else {
                        proxy.scrollTo(LegalDocumentText.topId, anchor: .top)
                    }
                }
            }
        }
        .environment(\.openURL, OpenURLAction { url in
            guard let link = LegalLinkAddress.link(of: url) else { return .systemAction }
            model.send(LegalDocumentUiEventOnLinkClicked(link: link))
            return .handled
        })
    }

    /// Which text it is: "Updated" agrees with it where the language marks gender (Portuguese, Spanish, Italian).
    private var isPrivacyPolicy: Bool {
        model.state.page == .privacyPolicy
    }

    /// The day the text last changed, long ("November 5, 2026") in the device's own format.
    private var updated: String {
        let day = document.updated
        let components = DateComponents(year: Int(day.year), month: Int(day.month), day: Int(day.day))
        guard let date = Calendar.current.date(from: components) else { return "" }
        return date.formatted(date: .long, time: .omitted)
    }
}

private struct LegalBlockView: View {
    let block: any LegalBlock

    var body: some View {
        switch block {
        case let section as LegalBlockSection:
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(verbatim: String(section.number))
                    .foregroundStyle(Palette.accent)
                    .monospacedDigit()
                Text(section.title.attributed)
            }
            .font(.title3.bold())
            .padding(.top, 14)
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
        case let heading as LegalBlockHeading:
            Text(heading.text.attributed)
                .font(.headline)
                .padding(.top, 4)
                .accessibilityAddTraits(.isHeader)
        case let paragraph as LegalBlockParagraph:
            Text(paragraph.text.attributed)
        case let bullets as LegalBlockBullets:
            LegalBulletList(items: bullets.items)
        case let callout as LegalBlockCallout:
            // The summary that opens the text, or a note inside a section
            VStack(alignment: .leading, spacing: 10) {
                Text(callout.title.attributed)
                    .font(.headline)
                if let paragraph = callout.paragraph {
                    Text(paragraph.attributed)
                }
                if !callout.items.isEmpty {
                    LegalBulletList(items: callout.items)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 16))
        case let table as LegalBlockTable:
            LegalTableCards(table: table)
        default:
            EmptyView()
        }
    }
}

private struct LegalBulletList: View {
    let items: [LegalText]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    Text(verbatim: "•")
                        .foregroundStyle(.secondary)
                        .accessibilityHidden(true)
                    Text(item.attributed)
                }
            }
        }
    }
}

/// A table as cards, one per row, as the site's phone layout: the row's first cell its title, each other cell under
/// its column's name.
private struct LegalTableCards: View {
    let table: LegalBlockTable

    var body: some View {
        VStack(spacing: 10) {
            ForEach(Array(table.rows.enumerated()), id: \.offset) { _, row in
                VStack(alignment: .leading, spacing: 10) {
                    if let title = row.cells.first {
                        Text(title.attributed)
                            .font(.headline)
                    }
                    // The row's other cells, the empty ones left out by the shared code
                    ForEach(Array(table.detailsOf(row: row).enumerated()), id: \.offset) { _, detail in
                        VStack(alignment: .leading, spacing: 2) {
                            if let label = detail.label {
                                Text(label.attributed)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Text(detail.text.attributed)
                                .font(.subheadline)
                        }
                    }
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 16))
            }
        }
    }
}

/// The text still coming: the date, the summary's card and a section's title over its lines.
private struct LegalDocumentPlaceholder: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ShimmerLine(width: 150, height: 10)
            ShimmerBlock(height: 150, cornerRadius: 16)
            ForEach(0..<3, id: \.self) { _ in
                ShimmerLine(width: 200, height: 18)
                    .padding(.top, 10)
                ShimmerLine()
                ShimmerLine()
                ShimmerLine(width: 220)
            }
        }
    }
}

private extension View {
    /// The text's one column: 640 pt at most, centred in a wider window.
    func legalColumn() -> some View {
        padding(.horizontal, 20)
            .padding(.vertical, 16)
            .frame(maxWidth: 640, alignment: .leading)
            .frame(maxWidth: .infinity)
    }
}

private extension LegalText {
    /// The line with its bold runs and its links, which carry an address of the app's own (`LegalLinkAddress`) so a
    /// tap reaches the ViewModel rather than the system.
    var attributed: AttributedString {
        spans.reduce(into: AttributedString()) { text, span in
            var run = AttributedString(span.text)
            if span.isBold { run.inlinePresentationIntent = .stronglyEmphasized }
            if let link = span.link, let url = LegalLinkAddress.url(of: link) {
                run.link = url
                run.foregroundColor = Palette.accent
                run.underlineStyle = .single
            }
            text.append(run)
        }
    }
}

/// A link of the text as the address its run carries, and back: `Text` only hands a tap its URL.
private enum LegalLinkAddress {
    private static let scheme = "blitzsplit-legal"
    private static let externalHost = "external"
    private static let pageHost = "page"

    static func url(of link: any LegalLink) -> URL? {
        var components = URLComponents()
        components.scheme = scheme
        switch link {
        case let external as LegalLinkExternal:
            components.host = externalHost
            components.queryItems = [URLQueryItem(name: "url", value: external.url)]
        case let page as LegalLinkPage:
            components.host = pageHost
            components.queryItems = [URLQueryItem(name: "slug", value: page.page.slug)]
                + (page.anchor.map { [URLQueryItem(name: "anchor", value: $0)] } ?? [])
        default:
            return nil
        }
        return components.url
    }

    static func link(of url: URL) -> (any LegalLink)? {
        guard url.scheme == scheme, let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }
        let items = components.queryItems ?? []
        func value(_ name: String) -> String? {
            items.first { $0.name == name }?.value
        }
        switch components.host {
        case externalHost:
            return value("url").map { LegalLinkExternal(url: $0) }
        case pageHost:
            guard let page = value("slug").flatMap({ LegalPage.companion.fromSlug(slug: $0) }) else { return nil }
            return LegalLinkPage(page: page, anchor: value("anchor"))
        default:
            return nil
        }
    }
}
