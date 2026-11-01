import Shared
import SwiftUI

/// The Groups and Search tabs' stack. On iPhone, iPad upright and the Mac, one NavigationStack pushes what the groups
/// open over them. Lying down with an iPad's room (5.1, 5.2: an iPad, an iPhone Duo open) the list keeps its column
/// while nothing but the group's Expenses, Balances or Members is above the groups in the shared back stack: the first
/// of them roots a second stack beside it, and with no group open yet it holds the canvas' shapes. Anything else the
/// group or the list opens (Statistics, the profile, a joined invite) covers the whole tab, as upright and as
/// Android's scene does, since the ViewModel ignores the list's taps under it; a sheet keeps the columns under it. The
/// home with no group at all is one pane on an iPad until the first group exists, while an iPhone Duo open keeps the
/// fold's other side for the group, saying so (decision 5, each size's board). The list's stack stays the first view
/// either way, so turning the iPad keeps its scroll.
struct GroupsColumns<ListContent: View>: View {
    let model: GroupsModel
    /// The shared back stack above the groups, as this tab shows it.
    @Binding var path: [AppRoute]
    /// This tab is the one in front, the only one that tells the ViewModel whether the column beside the list shows.
    let isInFront: Bool
    @ViewBuilder let list: () -> ListContent

    /// The whole tab, the column beside the list included, so showing it never changes what decides it.
    @State private var layout = ScreenLayout()
    /// The iPhone Duo's fold, whose halves the empty home keeps (iOS 27.1).
    @State private var division: FoldDivision?

    /// The column beside the list: lying down with an iPad's room (`showsListDetail`), with only the group's tabs
    /// above the groups (sheets aside, which the path leaves out).
    private var showsDetail: Bool {
        layout.showsListDetail && path.allSatisfy(Self.isGroupTab) && !showsEmptyHomeAlone
    }

    /// No group yet and no fold down the window: the empty home takes the whole tab, as the iPad's board draws it.
    private var showsEmptyHomeAlone: Bool {
        model.state.showsEmptyState && division?.isVertical != true
    }

    /// What this tab tells the ViewModel, nil while another tab is in front or before the first layout, whose zero
    /// size would say there is no column for a moment.
    private var reportedDetail: Bool? {
        guard isInFront, layout.size != .zero else { return nil }
        return showsDetail
    }

    var body: some View {
        HStack(spacing: 0) {
            NavigationStack(path: listPath) {
                list()
                    .navigationDestination(for: AppRoute.self) { MainTabView.destination($0) }
            }
            .frame(width: showsDetail ? ScreenLayout.listColumnWidth : nil)
            if showsDetail {
                Divider()
                    .ignoresSafeArea()
                NavigationStack(path: panePath) {
                    paneRoot
                        .navigationDestination(for: AppRoute.self) { MainTabView.destination($0) }
                }
            }
        }
        .environment(\.showsGroupBesideList, showsDetail)
        .measuringScreenLayout($layout)
        .readingFoldDivision($division)
        // The ViewModel then lets the list replace the group beside it and opens the search's first result there
        .onChange(of: reportedDetail, initial: true) { _, isShown in
            guard let isShown else { return }
            model.send(GroupsUiEventOnDetailPaneChanged(isShown: isShown))
        }
    }

    /// A group's Expenses, Balances or Members, the only screens the column beside the list draws.
    private static func isGroupTab(_ route: AppRoute) -> Bool {
        switch route {
        case .groupDetail, .balances, .members: true
        default: false
        }
    }

    /// The group's tab right above the groups, which roots the column beside the list.
    private var paneRoute: AppRoute? {
        path.first.flatMap { Self.isGroupTab($0) ? $0 : nil }
    }

    /// Beside the detail the list pushes nothing; on its own it pushes the whole path, as before.
    private var listPath: Binding<[AppRoute]> {
        Binding(
            get: { showsDetail ? [] : path },
            set: { newValue in
                guard !showsDetail else { return }
                path = newValue
            },
        )
    }

    /// Another group's tab stacked over the first one (a bill saved in another group); a back gesture there shortens
    /// the shared stack.
    private var panePath: Binding<[AppRoute]> {
        Binding(
            get: { Array(path.dropFirst(paneRoute == nil ? 0 : 1)) },
            set: { newValue in
                path = (paneRoute.map { [$0] } ?? []) + newValue
            },
        )
    }

    @ViewBuilder
    private var paneRoot: some View {
        if let paneRoute {
            MainTabView.destination(paneRoute)
                .id(paneRoute)
        } else if model.state.showsEmptyState {
            NoGroupOpenPlaceholder()
        } else {
            GroupPanePlaceholder()
        }
    }
}

/// The fold's other side on an iPhone Duo open while there is no group at all: where the group will show.
private struct NoGroupOpenPlaceholder: View {
    var body: some View {
        ZStack {
            Palette.groupedBackground
                .ignoresSafeArea()
            VStack(spacing: 10) {
                Text("No group open")
                    .font(.title3.weight(.semibold))
                Text("Once you create or join a group, its expenses and balances show on this side of the fold.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: 300)
            }
            .multilineTextAlignment(.center)
            .padding(24)
        }
    }
}

/// Before a group is picked, or once it closed, the column beside the list holds the canvas' shapes, with no words.
private struct GroupPanePlaceholder: View {
    var body: some View {
        ZStack {
            Palette.groupedBackground
                .ignoresSafeArea()
            ZStack {
                Circle()
                    .fill(Palette.pixTile)
                    .frame(width: 110, height: 110)
                    .offset(x: -40, y: 10)
                RoundedRectangle(cornerRadius: 28)
                    .fill(Palette.errorContainer)
                    .frame(width: 88, height: 88)
                    .rotationEffect(.degrees(14))
                    .offset(x: 46, y: -24)
                Capsule()
                    .fill(Palette.infoContainer)
                    .frame(width: 96, height: 56)
                    .offset(x: 30, y: 58)
            }
            .accessibilityHidden(true)
        }
    }
}
