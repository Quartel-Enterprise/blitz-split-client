import SwiftUI

/// The zoom from a group's row or card into the group's screen it pushes, and back into it (Pierre's beta feedback of
/// 2026-11-12: the group's photo and name carry over from the list into the group). Only where the group is pushed on
/// a tab's stack: an iPhone, and an iPad with no room for the group beside the list. Where the group opens beside the
/// list (an iPad lying down) nothing is pushed, and the Mac has no navigation transitions, so neither zooms. The zoom
/// falls back to a crossfade with Reduce Motion on, as the system draws it.
///
/// A group zooms only out of the row that was just tapped: the same group opened another way (from a bill, from
/// Activity) while its row sits under it in the stack pushes as usual, rather than flying out of a hidden row.
@MainActor
final class GroupZoom {
    /// How long after a tap on a row the group's screen it pushes still zooms out of it.
    private static let armedFor: Duration = .seconds(1)

    private var armedGroupId: String?
    private var armedAt: ContinuousClock.Instant?

    /// A row of `groupId` was tapped: the group's screen pushed right after zooms out of it.
    func arm(_ groupId: String) {
        armedGroupId = groupId
        armedAt = .now
    }

    /// Whether the group's screen being pushed now opens from its row.
    func isArmed(for groupId: String) -> Bool {
        guard armedGroupId == groupId, let armedAt else { return false }
        return armedAt.duration(to: .now) < Self.armedFor
    }

    func disarm() {
        armedGroupId = nil
        armedAt = nil
    }
}

/// A tab's stack: the namespace its group rows and the group's screens it pushes share, with the row tapped last.
struct GroupZoomScope {
    let namespace: Namespace.ID
    let zoom: GroupZoom
}

extension EnvironmentValues {
    /// Set on the tabs whose rows open a group (Home, Groups, Search), nil elsewhere, where nothing zooms.
    @Entry var groupZoomScope: GroupZoomScope?
}

extension View {
    /// A tab's stack whose group rows zoom into the group's screen they push.
    func groupZoomScope() -> some View {
        modifier(GroupZoomScopeModifier())
    }

    /// A group's row or card the group's screen zooms out of, clipped to its `cornerRadius` while it does.
    func groupZoomSource(groupId: String, cornerRadius: CGFloat = 0) -> some View {
        modifier(GroupZoomSourceModifier(groupId: groupId, cornerRadius: cornerRadius))
    }

    /// The group's screen, which zooms out of the row of the group that pushed it.
    func groupZoomDestination(groupId: String) -> some View {
        modifier(GroupZoomDestinationModifier(groupId: groupId))
    }
}

private struct GroupZoomScopeModifier: ViewModifier {
    @Namespace private var namespace
    @State private var zoom = GroupZoom()

    func body(content: Content) -> some View {
        #if os(iOS)
        content.environment(\.groupZoomScope, GroupZoomScope(namespace: namespace, zoom: zoom))
        #else
        content
        #endif
    }
}

private struct GroupZoomSourceModifier: ViewModifier {
    let groupId: String
    let cornerRadius: CGFloat

    @Environment(\.groupZoomScope) private var scope
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList

    func body(content: Content) -> some View {
        #if os(iOS)
        if let scope, !showsGroupBesideList {
            content.matchedTransitionSource(id: groupId, in: scope.namespace) { source in
                source.clipShape(.rect(cornerRadius: cornerRadius, style: .continuous))
            }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

private struct GroupZoomDestinationModifier: ViewModifier {
    let groupId: String

    @Environment(\.groupZoomScope) private var scope
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList
    /// Decided once, as the screen is pushed: the row's tap is forgotten right after.
    @State private var zooms: Bool?

    func body(content: Content) -> some View {
        #if os(iOS)
        let zooms = zooms ?? (!showsGroupBesideList && scope?.zoom.isArmed(for: groupId) == true)
        Group {
            if zooms, let scope {
                content.navigationTransition(.zoom(sourceID: groupId, in: scope.namespace))
            } else {
                content
            }
        }
        .onAppear {
            guard self.zooms == nil else { return }
            self.zooms = zooms
            scope?.zoom.disarm()
        }
        #else
        content
        #endif
    }
}
