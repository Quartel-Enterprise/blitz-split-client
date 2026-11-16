import SwiftUI

/// The zoom from a person's face into the profile it pushes, Profile or a contact's, and back into it (Pierre's beta
/// feedback of 2026-11-15: the profile photo carries over wherever a profile opens). It follows the group's zoom
/// (`GroupZoom`): only where the profile is pushed on a tab's stack over the face, never beside a list (an iPad lying
/// down draws the group or the person in the column beside it) nor on the Mac, which has no navigation transitions.
/// Reduce Motion turns it into the system's crossfade.
///
/// A profile zooms only out of the face just tapped: a bill shows its payer twice, and a profile opened another way
/// (a search hit, a link) while a face of the same person sits under it pushes as usual.
@MainActor
final class ProfileZoom {
    /// How long after a tap on a face the profile it pushes still zooms out of it.
    private static let armedFor: Duration = .seconds(1)

    private var armedFaceId: String?
    private var armedAt: ContinuousClock.Instant?

    /// The face `faceId` was tapped: the profile pushed right after zooms out of it.
    func arm(_ faceId: String) {
        armedFaceId = faceId
        armedAt = .now
    }

    /// The face the profile being pushed now opens from, if one was tapped in the last second.
    var armedFace: String? {
        guard let armedFaceId, let armedAt, armedAt.duration(to: .now) < Self.armedFor else { return nil }
        return armedFaceId
    }

    func disarm() {
        armedFaceId = nil
        armedAt = nil
    }
}

/// A tab's stack: the namespace its faces and the profiles they push share, with the face tapped last.
struct ProfileZoomScope {
    let namespace: Namespace.ID
    let zoom: ProfileZoom
}

extension EnvironmentValues {
    /// Set on every tab's stack (iOS and iPadOS only), nil elsewhere, where nothing zooms.
    @Entry var profileZoomScope: ProfileZoomScope?
}

extension View {
    /// A tab's stack whose faces zoom into the profiles they push.
    func profileZoomScope() -> some View {
        modifier(ProfileZoomScopeModifier())
    }

    /// A face a profile zooms out of: `faceId` names it apart from the stack's other faces, the same id its tap arms;
    /// nil where nothing zooms.
    func profileZoomSource(faceId: String?) -> some View {
        modifier(ProfileZoomSourceModifier(faceId: faceId))
    }

    /// Profile or a contact's profile, which zooms out of the face just tapped.
    func profileZoomDestination() -> some View {
        modifier(ProfileZoomDestinationModifier())
    }

    /// `opensProfile` whose tap first arms the zoom out of the face `faceId`.
    func opensProfile(_ action: (() -> Void)?, zoomingFrom faceId: String) -> some View {
        modifier(ProfileZoomArmingModifier(action: action, faceId: faceId))
    }
}

private struct ProfileZoomScopeModifier: ViewModifier {
    @Namespace private var namespace
    @State private var zoom = ProfileZoom()

    func body(content: Content) -> some View {
        #if os(iOS)
        content.environment(\.profileZoomScope, ProfileZoomScope(namespace: namespace, zoom: zoom))
        #else
        content
        #endif
    }
}

private struct ProfileZoomSourceModifier: ViewModifier {
    let faceId: String?

    @Environment(\.profileZoomScope) private var scope
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList
    /// Half the face's shorter side, the radius that draws it as its circle, less half a point so rounding never pushes
    /// it past half the side, which hides the face.
    @State private var radius: CGFloat = 0

    func body(content: Content) -> some View {
        #if os(iOS)
        if let faceId, let scope, !showsGroupBesideList {
            // The zoom clips only to a rounded rectangle, so one of half the face's side draws it as its circle. A
            // radius past that hides the face altogether, outside the zoom too
            content
                .onGeometryChange(for: CGFloat.self) { max(min($0.size.width, $0.size.height) / 2 - 0.5, 0) } action: {
                    radius = $0
                }
                .matchedTransitionSource(id: faceId, in: scope.namespace) { source in
                    source.clipShape(.rect(cornerRadius: radius, style: .circular))
                }
        } else {
            content
        }
        #else
        content
        #endif
    }
}

private struct ProfileZoomArmingModifier: ViewModifier {
    let action: (() -> Void)?
    let faceId: String

    @Environment(\.profileZoomScope) private var scope

    func body(content: Content) -> some View {
        content.opensProfile(action.map { action in
            {
                scope?.zoom.arm(faceId)
                action()
            }
        })
    }
}

private struct ProfileZoomDestinationModifier: ViewModifier {
    @Environment(\.profileZoomScope) private var scope
    @Environment(\.showsGroupBesideList) private var showsGroupBesideList
    /// Decided once, as the profile is pushed: the face's tap is forgotten right after.
    @State private var faceId: String??

    func body(content: Content) -> some View {
        #if os(iOS)
        let faceId = faceId ?? (showsGroupBesideList ? nil : scope?.zoom.armedFace)
        Group {
            if let faceId, let scope {
                content.navigationTransition(.zoom(sourceID: faceId, in: scope.namespace))
            } else {
                content
            }
        }
        .onAppear {
            guard self.faceId == nil else { return }
            self.faceId = .some(faceId)
            scope?.zoom.disarm()
        }
        #else
        content
        #endif
    }
}
