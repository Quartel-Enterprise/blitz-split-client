import Shared
import SwiftUI

/// The home's grid (5.6) on iPhone, iPad and Mac: the open groups as cards, as many columns as fit the width at the
/// size picked (an iPhone 3, 2 and 1; a Mac 5, 4 and 3), never a number picked. A card opens the group; its ⋯, its
/// long press (the context menu) and, on the Mac, its right click hold the same actions as a row's.
struct GroupsGrid: View {
    let groups: [GroupRow]
    let size: GroupGridSize
    let isOnline: Bool
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        let metrics = GroupCardMetrics(size)
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: metrics.minWidth), spacing: GroupCardMetrics.spacing, alignment: .top)],
            spacing: GroupCardMetrics.spacing,
        ) {
            ForEach(groups, id: \.id) { group in
                GroupGridCard(group: group, size: size, metrics: metrics, isOnline: isOnline, send: send)
            }
        }
    }
}

/// The canvas's sizes of each card, a little roomier on the Mac.
private struct GroupCardMetrics {
    let minWidth: CGFloat
    let avatar: CGFloat
    let padding: CGFloat
    let name: Font
    let amount: Font

    #if os(macOS)
    static let spacing: CGFloat = 12
    static let cornerRadius: CGFloat = 14
    #else
    static let spacing: CGFloat = 10
    static let cornerRadius: CGFloat = 22
    #endif

    init(_ size: GroupGridSize) {
        switch size {
        case .compact:
            #if os(macOS)
            minWidth = 150
            avatar = 28
            #else
            minWidth = 104
            avatar = 36
            #endif
            padding = 12
            name = .subheadline.weight(.semibold)
            amount = .subheadline.bold()
        case .large:
            #if os(macOS)
            minWidth = 280
            avatar = 44
            #else
            minWidth = 220
            avatar = 52
            #endif
            padding = 18
            name = .title3.weight(.semibold)
            amount = .title2.bold()
        default:
            #if os(macOS)
            minWidth = 200
            avatar = 36
            #else
            minWidth = 140
            avatar = 44
            #endif
            padding = 14
            name = .body.weight(.semibold)
            amount = .body.bold()
        }
    }
}

/// A card: the row's content stacked. The group's shape, its name in up to two lines (always room for two, so the
/// cards of a line match), the faces and the last activity (only the dot and "queued" in Compact; the Mac names the
/// members, as its table does), then the balance with its word.
private struct GroupGridCard: View {
    let group: GroupRow
    let size: GroupGridSize
    let metrics: GroupCardMetrics
    let isOnline: Bool
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        Button {
            send(GroupsUiEventOnGroupClicked(groupId: group.id))
        } label: {
            VStack(alignment: .leading, spacing: 8) {
                GroupAvatar(initials: group.initials, color: group.color, size: metrics.avatar)
                    .padding(.bottom, 4)
                Text(verbatim: group.name)
                    .font(metrics.name)
                    .lineLimit(2, reservesSpace: true)
                    .multilineTextAlignment(.leading)
                meta
                    .frame(minHeight: 20)
                Spacer(minLength: 0)
                balance
            }
            .padding(metrics.padding)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(Palette.card, in: .rect(cornerRadius: GroupCardMetrics.cornerRadius))
            .contentShape(.rect(cornerRadius: GroupCardMetrics.cornerRadius))
        }
        .buttonStyle(.plain)
        .overlay(alignment: .topTrailing) {
            GroupRowMenuButton(group: group, isOnline: isOnline, send: send)
                .padding(4)
        }
        .contextMenu {
            GroupRowActions(group: group, isOnline: isOnline, send: send)
        }
    }

    private var meta: some View {
        HStack(spacing: 6) {
            if size != .compact {
                #if os(macOS)
                Text(verbatim: GroupsCopy.memberNames(group))
                    .lineLimit(1)
                #else
                MemberFaces(group: group)
                #endif
            }
            if group.isPending {
                Circle()
                    .fill(.orange)
                    .frame(width: 8, height: 8)
                    .accessibilityHidden(true)
            }
            if size == .compact, group.isPending {
                Text("queued")
            }
            #if os(iOS)
            if size != .compact {
                Text(verbatim: GroupsCopy.detail(group))
                    .lineLimit(1)
            }
            #endif
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
    }

    @ViewBuilder private var balance: some View {
        if group.isSettled {
            Label("Settled up", systemImage: "checkmark")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.secondary)
        } else {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: group.currency.formatSigned(group.netMinor))
                    .font(metrics.amount)
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .foregroundStyle(Palette.balance(group.netMinor))
                Text(verbatim: GroupsCopy.balanceLabel(group.balanceKind))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

/// Up to three faces and "+N"; VoiceOver reads the member count instead.
struct MemberFaces: View {
    let group: GroupRow

    var body: some View {
        HStack(spacing: -6) {
            ForEach(Array(group.faces.enumerated()), id: \.offset) { _, face in
                AvatarPhoto(initial: face.initial, color: face.color, photoUrl: nil, size: 20)
            }
            if group.extraMemberCount > 0 {
                Text(verbatim: "+\(group.extraMemberCount)")
                    .font(.caption2.weight(.semibold))
                    .padding(.leading, 10)
            }
        }
        .accessibilityElement()
        .accessibilityLabel(Text(verbatim: GroupsCopy.members(group.memberCount)))
    }
}

/// The words and symbols of "View" and "Grid size", the same in the iPhone's menu and in the Mac's toolbar, slider and
/// View menu.
enum GroupsDisplayCopy {
    static func gridSizeName(_ size: GroupGridSize) -> LocalizedStringKey {
        switch size {
        case .compact: "Compact"
        case .large: "Large"
        default: "Standard"
        }
    }

    static let gridSizes: [GroupGridSize] = [.compact, .standard, .large]

    static func displayIcon(_ display: GroupsDisplay) -> String {
        display == .grid ? "square.grid.2x2" : "list.bullet"
    }
}

#if os(iOS)
/// "View" (5.5), the button of the "Open" header: a glass pull-down menu with List or Grid and, below, the grid's
/// size.
struct GroupsDisplayMenu: View {
    let state: GroupsUiState
    let send: (any GroupsUiEvent) -> Void

    var body: some View {
        Menu {
            Picker("View", selection: displayBinding) {
                Label("List", systemImage: GroupsDisplayCopy.displayIcon(.list)).tag(GroupsDisplay.list)
                Label("Grid", systemImage: GroupsDisplayCopy.displayIcon(.grid)).tag(GroupsDisplay.grid)
            }
            Section("Grid size") {
                Picker("Grid size", selection: gridSizeBinding) {
                    ForEach(GroupsDisplayCopy.gridSizes, id: \.self) { size in
                        Text(GroupsDisplayCopy.gridSizeName(size)).tag(size)
                    }
                }
                .disabled(!state.canPickGridSize)
            }
        } label: {
            Image(systemName: GroupsDisplayCopy.displayIcon(state.layout.display))
                .frame(width: 44, height: 44)
                .contentShape(.rect)
        }
        .accessibilityLabel(state.showsGrid ? Text("View: grid") : Text("View: list"))
    }

    private var displayBinding: Binding<GroupsDisplay> {
        Binding(
            get: { state.layout.display },
            set: { send(GroupsUiEventOnDisplayChanged(display: $0)) },
        )
    }

    private var gridSizeBinding: Binding<GroupGridSize> {
        Binding(
            get: { state.layout.gridSize },
            set: { send(GroupsUiEventOnGridSizeChanged(size: $0)) },
        )
    }
}
#endif

#if os(macOS)
extension FocusedValues {
    /// The groups of the window in front, for the View menu.
    @Entry var groupsModel: GroupsModel?
}

/// View › Grid Size: Compact, Standard or Large, as the slider under the grid; disabled while the list is picked or
/// the groups aren't in front.
struct GroupsCommands: Commands {
    @FocusedValue(\.groupsModel) private var model

    var body: some Commands {
        CommandGroup(after: .toolbar) {
            Picker("Grid Size", selection: gridSizeBinding) {
                ForEach(GroupsDisplayCopy.gridSizes, id: \.self) { size in
                    Text(GroupsDisplayCopy.gridSizeName(size)).tag(size)
                }
            }
            .disabled(model?.state.canPickGridSize != true)
        }
    }

    private var gridSizeBinding: Binding<GroupGridSize> {
        Binding(
            get: { model?.state.layout.gridSize ?? .standard },
            set: { model?.send(GroupsUiEventOnGridSizeChanged(size: $0)) },
        )
    }
}
#endif
