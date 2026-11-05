import Observation
import Shared
import SwiftUI

@Observable
final class SearchModel: SearchSource {
    private(set) var state: SearchUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createSearchViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! SearchUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    var search: GroupsSearch { state.search }

    var currentSearch: GroupsSearch {
        ((holder.viewModel.uiState.value as? SearchUiState) ?? state).search
    }

    func sendSearch(_ event: any SearchUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Search" (5.2) over Home, Balances or Contacts (`Route.Search`): the same search as Groups' own. The Mac opens it
/// as a sheet with the field, the segments and the results, Esc or Cancel closing it; an iPhone and an iPad search in
/// their search tab, so a stack restored with it on top draws the tab's screen. What it finds opens in its place.
struct SearchView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: SearchModel?

    var body: some View {
        Group {
            if let model {
                #if os(iOS)
                GroupsSearchView(model: model)
                #else
                MacSearchSheet(model: model)
                #endif
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .onAppear {
            if model == nil { model = SearchModel() }
        }
    }
}

#if os(macOS)
/// The Mac's sheet: the field and All / Groups / People / Expenses above the results, as its toolbar field draws them
/// over Groups.
private struct MacSearchSheet: View {
    let model: SearchModel
    @FocusState private var isFieldFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            VStack(spacing: 10) {
                TextField("Groups, people and expenses", text: GroupsSearchCopy.queryBinding(model))
                    .textFieldStyle(.roundedBorder)
                    .focused($isFieldFocused)
                Picker("Search in", selection: GroupsSearchCopy.scopeBinding(model)) {
                    ForEach(GroupsSearchCopy.scopes, id: \.self) { scope in
                        Text(GroupsSearchCopy.scopeTitle(scope)).tag(scope)
                    }
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }
            .padding(16)
            Divider()
            GroupsSearchResultsList(model: model)
        }
        .frame(width: 560, height: 520)
        .navigationTitle("Search")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    model.sendSearch(SearchUiEventOnClosed.shared)
                }
            }
        }
        .onAppear { isFieldFocused = true }
    }
}
#endif
