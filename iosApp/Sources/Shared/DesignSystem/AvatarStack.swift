import Shared
import SwiftUI

/// One face of a stack, whoever it stands for: a member, a payer, someone in a split.
struct StackFace {
    let initials: String
    let color: AvatarColor
    let photoUrl: String?
}

/// Every stack of faces in the app, as Android and the web draw it, by the shared `FaceStack` rule: `.compact` in a
/// list's row or a form's field (two bubbles: both faces of two people, the first face and "+N" of three or more),
/// `.roomy` on a screen's own header or body (five: everyone up to five, four faces and "+N" of six or more). Each bubble is ringed in
/// the surface behind it so its neighbor's edge reads, and keeps its circle whatever room the row leaves. VoiceOver
/// skips it; the row around it says who.
struct AvatarStack: View {
    let faces: [StackFace]
    let extraCount: Int
    let size: CGFloat
    var ring: Color = Palette.card
    var ringWidth: CGFloat = 2

    /// The faces and the "+N" a ViewModel already picked.
    init(faces: [StackFace], extraCount: Int, size: CGFloat, ring: Color = Palette.card, ringWidth: CGFloat = 2) {
        self.faces = faces
        self.extraCount = extraCount
        self.size = size
        self.ring = ring
        self.ringWidth = ringWidth
    }

    /// Everyone at once, cut to the bubbles the place has room for.
    init(people: [StackFace], stack: FaceStack, size: CGFloat, ring: Color = Palette.card, ringWidth: CGFloat = 2) {
        let count = Int32(people.count)
        let shown = Int(stack.faceCountOf(peopleCount: count))
        self.init(
            faces: Array(people.prefix(shown)),
            extraCount: Int(stack.extraCountOf(peopleCount: count)),
            size: size,
            ring: ring,
            ringWidth: ringWidth,
        )
    }

    var body: some View {
        HStack(spacing: -(size + ringWidth * 2) * 0.3) {
            ForEach(Array(faces.enumerated()), id: \.offset) { _, face in
                AvatarPhoto(initial: face.initials, color: face.color, photoUrl: face.photoUrl, size: size)
                    .padding(ringWidth)
                    .background(ring, in: .circle)
                    .fixedSize()
            }
            if extraCount > 0 {
                Text(verbatim: "+\(extraCount)")
                    .font(.system(size: size * 0.4, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .fixedSize()
                    .padding(.horizontal, 3)
                    .frame(minWidth: size, minHeight: size, maxHeight: size)
                    .background(.fill.secondary, in: .capsule)
                    .padding(ringWidth)
                    .background(ring, in: .capsule)
                    .fixedSize()
            }
        }
        .accessibilityHidden(true)
    }
}

extension StackFace {
    init(_ face: MemberFace) {
        self.init(initials: face.initial, color: face.color, photoUrl: face.photoUrl)
    }

    init(_ person: BillPerson) {
        self.init(initials: person.initials, color: person.avatarColor, photoUrl: person.photoUrl)
    }
}
