import Shared
import SwiftUI

/// The Groups and Search tabs' stack. On iPhone, iPad upright and the Mac, one NavigationStack pushes what the groups
/// open over them. On an iPad lying down (5.1, 5.2) the list keeps its column and the group's Expenses, Balances or
/// Members, the first entry over the groups in the shared back stack, roots a second stack beside it, which pushes
/// whatever the group opens; with no group open yet it holds the canvas' shapes. The list's stack stays the first
/// view either way, so turning the iPad keeps its scroll.
struct GroupsColumns<ListContent: View>: View {
    let model: GroupsModel
    /// The shared back stack above the groups, as this tab shows it.
    @Binding var path: [AppRoute]
    @ViewBuilder let list: () -> ListContent

    /// The whole tab, the column beside the list included, so showing it never changes what decides it.
    @State private var layout = ScreenLayout()

    private var showsDetail: Bool {
        layout.showsListDetailOnPad
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
        // The ViewModel then lets the list replace the group beside it and opens the search's first result there
        .onChange(of: showsDetail, initial: true) { _, isShown in
            model.send(GroupsUiEventOnDetailPaneChanged(isShown: isShown))
        }
    }

    /// The group's tab right above the groups, which roots the column beside the list.
    private var paneRoute: AppRoute? {
        switch path.first {
        case .groupDetail, .balances, .members: path.first
        default: nil
        }
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

    /// What the group beside the list pushed over itself; a back gesture there shortens the shared stack.
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
        } else {
            GroupPanePlaceholder()
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
