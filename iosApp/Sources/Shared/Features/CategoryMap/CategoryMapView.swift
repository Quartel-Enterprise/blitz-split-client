import Observation
import Shared
import SwiftUI

@Observable
final class CategoryMapModel {
    private(set) var state: CategoryMapUiState
    private(set) var saveErrorCount = 0

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createCategoryMapViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! CategoryMapUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            if action is CategoryMapUiActionShowSaveError { self?.saveErrorCount += 1 }
        })
    }

    /// The state as it is now, for bindings read between two observation updates.
    var current: CategoryMapUiState {
        holder.viewModel.uiState.value as! CategoryMapUiState
    }

    func send(_ event: any CategoryMapUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Category map" (9.1), pushed from the profile and from "Category" (9.0): a list of cards on iPhone and iPad
/// upright, and lying down, on an iPad lying down and on an iPhone Duo open the categories beside the terms of the
/// one picked. "New term" and "Edit term" (9.2) open as a sheet (a form sheet with an iPad's room).
struct CategoryMapView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: CategoryMapModel?

    var body: some View {
        Group {
            if let model {
                CategoryMapScreen(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .onAppear {
            if model == nil { model = CategoryMapModel() }
        }
    }
}

struct CategoryMapScreen: View {
    let model: CategoryMapModel
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    @State private var layout = ScreenLayout()

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        Group {
            if layout.usesTwoColumns, !state.isSearching, !state.isEmpty {
                TwoColumnsLayout(firstWidth: 320, spacing: 24) {
                    List { MapListSections(model: model, showsCards: false) }
                } second: {
                    List { MapDetailSections(model: model) }
                }
            } else {
                List { MapListSections(model: model, showsCards: true) }
            }
        }
        .measuringScreenLayout($layout)
        .searchable(text: query, prompt: Text("Search terms"))
        .navigationTitle("Category map")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        // On iPhone the map takes the whole screen, without the app's tabs
        .toolbar(horizontalSizeClass == .compact ? .hidden : .automatic, for: .tabBar)
        #endif
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Add term", systemImage: "plus") {
                    model.send(CategoryMapUiEventOnAddTermClicked(categoryId: nil, text: ""))
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Menu("More options", systemImage: "ellipsis") {
                    Button("Restore default", systemImage: "arrow.counterclockwise") {
                        model.send(CategoryMapUiEventOnRestoreClicked.shared)
                    }
                }
            }
        }
        .sheet(isPresented: termShown) {
            NavigationStack { TermFormView(model: model) }
                .formSheetSizing()
        }
        .restoreMapAlert(model: model)
        .glassToast("Couldn't save the map. Try again.", trigger: model.saveErrorCount)
        .safeAreaInset(edge: .bottom) {
            if let removed = state.removedTerm {
                RemovedTermBanner(term: removed, model: model)
            }
        }
    }

    private var query: Binding<String> {
        Binding(
            get: { model.current.query },
            set: { model.send(CategoryMapUiEventOnQueryChanged(text: $0)) },
        )
    }

    private var termShown: Binding<Bool> {
        Binding(
            get: { model.state.termForm != nil },
            set: { shown in
                if !shown, model.current.termForm != nil { model.send(CategoryMapUiEventOnTermDismissed.shared) }
            },
        )
    }
}

/// The notices, then what the search found, the empty map, or the categories: as sections with their terms, or as
/// rows to pick from beside the terms.
struct MapListSections: View {
    let model: CategoryMapModel
    let showsCards: Bool

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        MapNoticeSections(model: model)
        if state.isSearching {
            MapSearchSection(model: model)
        } else if state.isEmpty {
            EmptyMapSection(model: model)
        } else if showsCards {
            ForEach(state.sections, id: \.category.id) { section in
                MapSectionCard(section: section, model: model)
            }
        } else {
            Section {
                ForEach(state.sections, id: \.category.id) { section in
                    let isSelected = section.category.id == state.selectedSection?.category.id
                    Button {
                        model.send(CategoryMapUiEventOnSectionSelected(categoryId: section.category.id))
                    } label: {
                        CategoryRowLabel(
                            category: section.category,
                            title: CategoryCopy.name(section.category),
                            subtitle: CategoryMapCopy.count(section),
                            isSelected: isSelected,
                        )
                    }
                    .buttonStyle(.plain)
                    .listRowBackground(isSelected ? Palette.accent.opacity(0.14) : nil)
                }
            }
        }
    }
}

/// The terms of the main category picked, beside the list.
struct MapDetailSections: View {
    let model: CategoryMapModel

    var body: some View {
        if let section = model.state.selectedSection {
            Section {
                MapSectionRows(section: section, model: model)
                AddInSectionButton(section: section, model: model)
            } header: {
                HStack(spacing: 12) {
                    CategoryBadge(category: section.category, size: 44)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(verbatim: CategoryCopy.name(section.category))
                            .font(.title3.bold())
                        Text(verbatim: "\(CategoryMapCopy.count(section)) · \(CategoryMapCopy.subcategories(section))")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
                .textCase(nil)
                .padding(.bottom, 4)
            } footer: {
                Text("Tap a term to change its category or remove it. It matches the whole word, ignoring accents and case.")
            }
        }
    }
}

/// A main category's section on iPhone and iPad upright: its terms in lines by subcategory, then "Add term in X".
struct MapSectionCard: View {
    let section: MapSection
    let model: CategoryMapModel

    var body: some View {
        Section {
            MapSectionRows(section: section, model: model)
            AddInSectionButton(section: section, model: model)
        } header: {
            HStack(spacing: 12) {
                CategoryBadge(category: section.category, size: 32)
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: CategoryCopy.name(section.category))
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text(verbatim: CategoryMapCopy.count(section))
                        .font(.caption)
                }
            }
            .textCase(nil)
        }
    }
}

struct MapSectionRows: View {
    let section: MapSection
    let model: CategoryMapModel

    var body: some View {
        if section.rows.isEmpty {
            Text("No terms yet.")
                .foregroundStyle(.secondary)
        }
        ForEach(section.rows, id: \.category.id) { row in
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 4) {
                    Text(verbatim: CategoryMapCopy.rowLabel(row))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    if let group = row.groupName {
                        Text("only in \(group)")
                            .font(.caption)
                            .foregroundStyle(Palette.accent)
                    }
                }
                TermChipsRow(row: row, model: model)
            }
            .padding(.vertical, 2)
        }
    }
}

/// A line's terms as chips that wrap, each with the orange dot while it waits in the queue (9.1g).
struct TermChipsRow: View {
    let row: MapRow
    let model: CategoryMapModel

    var body: some View {
        let path = CategoryMapCopy.path(row.category, in: model.state)
        TermFlowLayout(spacing: 8) {
            ForEach(row.terms, id: \.key) { term in
                Button {
                    model.send(CategoryMapUiEventOnTermClicked(key: term.key))
                } label: {
                    HStack(spacing: 6) {
                        Text(verbatim: term.term)
                            .font(.subheadline.weight(.medium))
                        if term.isPending {
                            Circle()
                                .fill(.orange)
                                .frame(width: 8, height: 8)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .overlay {
                        RoundedRectangle(cornerRadius: 8).stroke(.secondary.opacity(0.4))
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(CategoryMapCopy.termLabel(term, path: path))
            }
        }
    }
}

struct AddInSectionButton: View {
    let section: MapSection
    let model: CategoryMapModel

    var body: some View {
        Button("Add term in \(CategoryCopy.name(section.category))", systemImage: "plus") {
            model.send(CategoryMapUiEventOnAddTermClicked(categoryId: section.category.id, text: ""))
        }
    }
}

/// "Offline · 2 changes queued", "Your map stays on this device only" and the intro, above the map.
struct MapNoticeSections: View {
    let model: CategoryMapModel

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        if state.showsOfflineNotice {
            Section {
                Label(CategoryMapCopy.offlineQueue(Int(state.map.pendingCount)), systemImage: "icloud.slash")
            }
        }
        if state.showsDeviceOnlyNotice, !state.isSearching {
            Section {
                Text("Your map stays on this device only. To take it to your other devices, sign in with Google or Apple in your Profile.")
                Button("Open Profile") {
                    model.send(CategoryMapUiEventOnProfileClicked.shared)
                }
            }
        }
        if !state.isSearching, !state.isEmpty {
            Section {
            } footer: {
                if state.isOnline {
                    Text("When a bill's title has one of these words, its category comes by itself. “Uber home” becomes Transport.")
                } else {
                    Text("The map works offline: changes count right away on this device and reach your others once back online.")
                }
            }
        }
    }
}

/// "2 terms with “uber”", or "No term with “pizzaria”" with "Add “pizzaria”" (9.1b, 9.1c).
struct MapSearchSection: View {
    let model: CategoryMapModel

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        let query = state.query.trimmingCharacters(in: .whitespaces)
        if state.showsNoResult {
            ContentUnavailableView {
                Label("No term with “\(query)”", systemImage: "magnifyingglass")
            } description: {
                Text("Add it and bills with this word in their title arrive with the right category.")
            } actions: {
                Button("Add “\(query)”") {
                    model.send(CategoryMapUiEventOnAddTermClicked(categoryId: nil, text: query))
                }
                .buttonStyle(.borderedProminent)
            }
        } else {
            let results = state.searchResults
            Section {
                ForEach(results, id: \.term.key) { result in
                    Button {
                        model.send(CategoryMapUiEventOnTermClicked(key: result.term.key))
                    } label: {
                        HStack(spacing: 12) {
                            CategoryBadge(category: result.parent ?? result.category, size: 36)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(marked(result))
                                Text(verbatim: CategoryMapCopy.path(result.category, in: state))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                }
            } header: {
                Text(verbatim: CategoryMapCopy.results(results.count, query: query))
            } footer: {
                if results.count > 1 {
                    Text("When two terms match a title, the longest one wins.")
                }
            }
        }
    }

    private func marked(_ result: MapSearchResult) -> AttributedString {
        let term = result.term.term
        guard Int(result.matchEnd) <= term.count, result.matchEnd > result.matchStart else { return AttributedString(term) }
        let start = term.index(term.startIndex, offsetBy: Int(result.matchStart))
        let end = term.index(term.startIndex, offsetBy: Int(result.matchEnd))
        var hit = AttributedString(String(term[start ..< end]))
        hit.font = .body.bold()
        return AttributedString(String(term[..<start])) + hit + AttributedString(String(term[end...]))
    }
}

/// "Your map is empty" (9.1e) with "Add term" and "Restore default".
struct EmptyMapSection: View {
    let model: CategoryMapModel

    var body: some View {
        ContentUnavailableView {
            Label("Your map is empty", systemImage: "tag")
        } description: {
            Text("Without terms, the category doesn't come from the title: you pick it on each bill.")
        } actions: {
            Button("Add term") {
                model.send(CategoryMapUiEventOnAddTermClicked(categoryId: nil, text: ""))
            }
            .buttonStyle(.borderedProminent)
            Button("Restore default") {
                model.send(CategoryMapUiEventOnRestoreClicked.shared)
            }
        }
    }
}

/// "“café” left the map" with Undo (9.1d), until another change or the person dismisses it.
struct RemovedTermBanner: View {
    let term: MapTerm
    let model: CategoryMapModel

    var body: some View {
        HStack(spacing: 12) {
            Text("“\(term.term)” left the map")
                .font(.subheadline)
            Spacer()
            Button("Undo") {
                model.send(CategoryMapUiEventOnUndoRemoveClicked.shared)
            }
            .font(.subheadline.weight(.semibold))
            Button("Dismiss", systemImage: "xmark") {
                model.send(CategoryMapUiEventOnRemovedNoticeDismissed.shared)
            }
            .labelStyle(.iconOnly)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .glassEffect(.regular, in: .capsule)
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .accessibilityElement(children: .contain)
    }
}

/// "New term" or "Edit term" (9.2): the word, the category it sends titles to, an example, and Remove for a term
/// already in the map. A word another category has turns Save into "Move to X" (9.2c).
struct TermFormView: View {
    let model: CategoryMapModel

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        Form {
            if let form = state.termForm {
                Section {
                    TextField("Word or phrase", text: text, prompt: Text("Word or phrase"))
                    #if os(iOS)
                        .textInputAutocapitalization(.never)
                    #else
                        // The header already says it: a Mac form would repeat it as the row's label
                        .labelsHidden()
                    #endif
                        .autocorrectionDisabled()
                } header: {
                    Text("Word or phrase")
                } footer: {
                    Text(verbatim: CategoryMapCopy.termHelp(state, form: form))
                        .foregroundStyle(state.termError != nil || state.duplicateTerm != nil ? Palette.negative : .secondary)
                }
                Section {
                    Picker("Category", selection: category) {
                        ForEach(state.categoryOptions, id: \.id) { option in
                            Text(verbatim: option.parentId == nil
                                ? CategoryMapCopy.option(option, in: state)
                                : "   \(CategoryMapCopy.option(option, in: state))")
                                .tag(option.id)
                        }
                    }
                } footer: {
                    Text(verbatim: CategoryMapCopy.categoryHelp(state, form: form))
                }
                if let example = CategoryMapCopy.example(state, form: form) {
                    Section {
                        Text(verbatim: example)
                            .foregroundStyle(.secondary)
                    }
                }
                if form.isEditing {
                    Section {
                        Button("Remove", role: .destructive) {
                            model.send(CategoryMapUiEventOnTermRemoveClicked.shared)
                        }
                    }
                }
            }
        }
        .formStyle(.grouped)
        .navigationTitle(state.termForm?.isEditing == true ? Text("Edit term") : Text("New term"))
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", systemImage: "xmark", role: .cancel) {
                    model.send(CategoryMapUiEventOnTermDismissed.shared)
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(CategoryMapCopy.saveTitle(state), systemImage: "checkmark") {
                    model.send(CategoryMapUiEventOnTermSaveClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(!state.canSaveTerm)
            }
        }
    }

    private var text: Binding<String> {
        Binding(
            get: { model.current.termForm?.text ?? "" },
            set: { model.send(CategoryMapUiEventOnTermTextChanged(text: $0)) },
        )
    }

    private var category: Binding<String> {
        Binding(
            get: { model.current.termForm?.categoryId ?? "" },
            set: { model.send(CategoryMapUiEventOnTermCategoryPicked(categoryId: $0)) },
        )
    }
}

extension View {
    /// "Restore the default map?" (9.1f), saying which of the person's own terms stay.
    func restoreMapAlert(model: CategoryMapModel) -> some View {
        alert(
            "Restore the default map?",
            isPresented: Binding(
                get: { model.state.isRestoreConfirmOpen },
                set: { shown in
                    if !shown, model.current.isRestoreConfirmOpen { model.send(CategoryMapUiEventOnRestoreDismissed.shared) }
                },
            ),
        ) {
            Button("Restore") {
                model.send(CategoryMapUiEventOnRestoreConfirmed.shared)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(verbatim: CategoryMapCopy.restoreMessage(model.state))
        }
    }
}

/// The chips of a line, wrapping onto the next one when the row is full.
struct TermFlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache _: inout ()) -> CGSize {
        let rows = rowsOf(subviews, width: proposal.width ?? .infinity)
        let height = rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(rows.count - 1, 0))
        let width = rows.map(\.width).max() ?? 0
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal _: ProposedViewSize, subviews: Subviews, cache _: inout ()) {
        var y = bounds.minY
        for row in rowsOf(subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var indices: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func rowsOf(_ subviews: Subviews, width: CGFloat) -> [Row] {
        var rows: [Row] = []
        var current = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            let added = current.indices.isEmpty ? size.width : current.width + spacing + size.width
            if added > width, !current.indices.isEmpty {
                rows.append(current)
                current = Row(indices: [index], width: size.width, height: size.height)
            } else {
                current.indices.append(index)
                current.width = added
                current.height = max(current.height, size.height)
            }
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
    }
}
