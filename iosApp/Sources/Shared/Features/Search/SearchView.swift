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

/// "Search" (5.2) over Home, Balances or Contacts (`Route.Search`): the same search as Groups' own. An iPhone, an iPad
/// and the Mac search in their search tab, so a stack restored with it on top draws the tab's screen. What it finds
/// opens in its place.
struct SearchView: View {
    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: SearchModel?

    var body: some View {
        Group {
            if let model {
                GroupsSearchView(model: model)
            } else {
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        .onAppear {
            if model == nil { model = SearchModel() }
        }
    }
}
