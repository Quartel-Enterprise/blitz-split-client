#if os(macOS)
import Shared
import SwiftUI

/// Ajustes › Categories (⌘,): the person's category map (9.1), as the canvas draws it, beside the profile's sections
/// in the sidebar (`SettingsView`). Makes the map's model once the section shows, as the screens do.
struct MacCategoryMapPane: View {
    @State private var model: CategoryMapModel?

    var body: some View {
        Group {
            if let model {
                MacCategoryMapSettings(model: model)
            } else {
                Color.clear
            }
        }
        .onAppear {
            if model == nil { model = CategoryMapModel() }
        }
    }
}

/// Ajustes › Categories: the categories on the left, a table of the picked one's terms on the right with + and −
/// under it, the search above and "Restore Default". "New Term" and "Edit Term" open as a sheet on the window.
struct MacCategoryMapSettings: View {
    let model: CategoryMapModel
    @State private var selectedTerm: String?

    private var state: CategoryMapUiState {
        model.state
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 12) {
                TextField("Search terms", text: query, prompt: Text("Search terms"))
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: 260)
                Spacer()
                Button("Restore Default…") {
                    model.send(CategoryMapUiEventOnRestoreClicked.shared)
                }
            }
            .padding(12)
            MapNoticeBar(model: model)
            if state.isSearching || state.isEmpty {
                List { MapListSections(model: model, showsCards: true) }
            } else {
                HSplitView {
                    List(selection: section) {
                        ForEach(state.sections, id: \.category.id) { section in
                            CategoryRowLabel(
                                category: section.category,
                                title: CategoryCopy.name(section.category),
                                subtitle: CategoryMapCopy.count(section),
                                isSelected: false,
                            )
                            .tag(section.category.id)
                        }
                    }
                    .frame(minWidth: 220, maxWidth: 280)
                    termsTable
                }
            }
        }
        .sheet(isPresented: termShown) {
            NavigationStack { TermFormView(model: model) }
                .frame(width: 460, height: 420)
        }
        .restoreMapAlert(model: model)
        .glassToast("Couldn't save the map. Try again.", trigger: model.saveErrorCount)
        .safeAreaInset(edge: .bottom) {
            if let removed = state.removedTerm {
                RemovedTermBanner(term: removed, model: model)
            }
        }
    }

    private var termsTable: some View {
        let rows = (state.selectedSection?.rows ?? []).flatMap { row in
            row.terms.map { MacTermRow(id: $0.key, row: row, term: $0) }
        }
        return VStack(spacing: 0) {
            Table(rows, selection: $selectedTerm) {
                TableColumn("Term") { item in
                    HStack(spacing: 6) {
                        Text(verbatim: item.term.term)
                        if item.term.isPending {
                            Circle().fill(.orange).frame(width: 7, height: 7)
                                .accessibilityLabel("queued")
                        }
                    }
                }
                TableColumn("Category") { item in
                    Text(verbatim: CategoryMapCopy.rowLabel(item.row))
                }
            }
            .contextMenu(forSelectionType: String.self) { _ in
            } primaryAction: { keys in
                if let key = keys.first { model.send(CategoryMapUiEventOnTermClicked(key: key)) }
            }
            HStack(spacing: 0) {
                Button("Add Term", systemImage: "plus") {
                    model.send(CategoryMapUiEventOnAddTermClicked(categoryId: state.selectedSection?.category.id, text: ""))
                }
                Button("Remove Term", systemImage: "minus") {
                    guard let key = selectedTerm else { return }
                    model.send(CategoryMapUiEventOnTermClicked(key: key))
                    model.send(CategoryMapUiEventOnTermRemoveClicked.shared)
                }
                .disabled(selectedTerm == nil)
                Spacer()
            }
            .labelStyle(.iconOnly)
            .buttonStyle(.borderless)
            .padding(6)
        }
    }

    private var query: Binding<String> {
        Binding(
            get: { model.current.query },
            set: { model.send(CategoryMapUiEventOnQueryChanged(text: $0)) },
        )
    }

    private var section: Binding<String?> {
        Binding(
            get: { model.state.selectedSection?.category.id },
            set: { id in
                if let id { model.send(CategoryMapUiEventOnSectionSelected(categoryId: id)) }
            },
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

/// A term of the table with the line it is on.
private struct MacTermRow: Identifiable {
    let id: String
    let row: MapRow
    let term: MapTerm
}

/// The offline queue and the device-only note above the Mac's map.
private struct MapNoticeBar: View {
    let model: CategoryMapModel

    var body: some View {
        let state = model.state
        VStack(alignment: .leading, spacing: 6) {
            if state.showsOfflineNotice {
                Label(CategoryMapCopy.offlineQueue(Int(state.map.pendingCount)), systemImage: "icloud.slash")
            }
            if state.showsDeviceOnlyNotice {
                Text("Your map stays on this device only. To take it to your other devices, sign in with Google or Apple in your Profile.")
                    .foregroundStyle(.secondary)
            }
        }
        .font(.callout)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 12)
        .padding(.bottom, state.showsOfflineNotice || state.showsDeviceOnlyNotice ? 8 : 0)
    }
}
#endif
