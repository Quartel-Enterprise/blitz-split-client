#if os(macOS)
import Observation
import Shared
import SwiftUI

/// The open groups of the Mac's sidebar, "Open" under the home's tabs while Groups or a group's screen is in front.
@Observable
final class HomeSidebarModel {
    private(set) var state: HomeSidebarUiState

    @ObservationIgnored private let holder = BlitzSplitApple.shared.createHomeSidebarViewModel()
    @ObservationIgnored private let watches = WatchBag()

    init() {
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! HomeSidebarUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
    }

    /// Opens the group over Groups, as its row there does, in the place of whatever Groups opened.
    func openGroup(_ groupId: String) {
        holder.viewModel.onEvent(event: HomeSidebarUiEventOnGroupClicked(groupId: groupId))
    }

    isolated deinit {
        holder.clear()
    }
}

/// "Open" under the sidebar's tabs: each open group's shape, name and your net in it, the group in front marked.
struct SidebarOpenGroups: View {
    let groups: [GroupRow]
    let selectedId: String?
    let onOpen: (String) -> Void

    var body: some View {
        if !groups.isEmpty {
            VStack(alignment: .leading, spacing: 2) {
                Text("Open")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .padding(.horizontal, 8)
                    .padding(.bottom, 4)
                    .accessibilityAddTraits(.isHeader)
                // Many open groups scroll, so the footer never squeezes the tabs out of a short window
                ScrollView {
                    VStack(alignment: .leading, spacing: Self.rowSpacing) {
                        ForEach(groups, id: \.id) { group in
                            row(group)
                        }
                    }
                }
                .scrollBounceBehavior(.basedOnSize)
                .frame(height: min(CGFloat(groups.count) * (Self.rowHeight + Self.rowSpacing), Self.maxListHeight))
            }
            .padding(.top, 12)
        }
    }

    private func row(_ group: GroupRow) -> some View {
        let isSelected = group.id == selectedId
        return Button {
            onOpen(group.id)
        } label: {
            HStack(spacing: 8) {
                GroupAvatar(initials: group.initials, color: group.color, photoUrl: group.photoUrl, size: 20)
                    .accessibilityHidden(true)
                Text(verbatim: group.name)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(verbatim: Self.net(group))
                    .font(.caption.weight(.medium))
                    .monospacedDigit()
                    .foregroundStyle(Palette.balance(group.netMinor))
            }
            .padding(.horizontal, 8)
            .frame(height: Self.rowHeight)
            .contentShape(.rect)
            .background(isSelected ? AnyShapeStyle(.fill.secondary) : AnyShapeStyle(.clear), in: .rect(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private static let rowHeight: CGFloat = 28
    private static let rowSpacing: CGFloat = 2
    /// About eight rows, then the list scrolls.
    private static let maxListHeight: CGFloat = 240

    /// "+214.90", "−98.00" or "0.00": the sidebar's narrow column leaves out the group's currency symbol.
    private static func net(_ group: GroupRow) -> String {
        let amount = group.currency.formatPlain(abs(group.netMinor))
        if group.netMinor > 0 { return "+\(amount)" }
        if group.netMinor < 0 { return "−\(amount)" }
        return amount
    }
}
#endif
