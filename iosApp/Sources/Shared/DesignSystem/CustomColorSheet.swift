import Shared
import SwiftUI

/// "New color", which "+" opens: a gradient to pick from (saturation and brightness in the square, the hue below it),
/// or the color typed as a hex or an RGB code, each following the others. The shared model holds the draft, so the
/// fields and the gradient never disagree; "Add" waits for whole codes.
struct CustomColorSheet: View {
    let draft: ColorDraft
    /// The draft as the model holds it right now, which the fields read so fast typing never sees a stale value.
    let current: () -> ColorDraft?
    let send: (CustomColorEvent) -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    shadeSquare
                    hueBar
                }
                Section {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(draft.color.raw)
                            .frame(width: 36, height: 36)
                            .overlay { Circle().strokeBorder(.separator) }
                            .accessibilityHidden(true)
                        TextField("Hex code", text: Binding(
                            get: { current()?.hexText ?? draft.hexText },
                            set: { send(CustomColorEventOnHexChanged(text: $0)) },
                        ))
                        .autocorrectionDisabled()
                        #if os(iOS)
                            .textInputAutocapitalization(.characters)
                        #endif
                    }
                    HStack {
                        channelField("Red", channel: .red) { $0.redText }
                        channelField("Green", channel: .green) { $0.greenText }
                        channelField("Blue", channel: .blue) { $0.blueText }
                    }
                } footer: {
                    if draft.isHexInvalid {
                        Text("Enter a code like #3A7BD5").foregroundStyle(Palette.negative)
                    } else if draft.isRgbInvalid {
                        Text("Each value goes from 0 to 255").foregroundStyle(Palette.negative)
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle("New color")
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { send(CustomColorEventOnDraftDismissed.shared) }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Add") { send(CustomColorEventOnSaveClicked.shared) }
                            .disabled(!draft.canSave)
                    }
                }
        }
        #if os(macOS)
        .frame(minWidth: 360, minHeight: 460)
        #endif
        .presentationDetents([.large])
    }

    /// Saturation left to right, brightness bottom to top, on the hue picked below.
    private var shadeSquare: some View {
        GeometryReader { proxy in
            let size = proxy.size
            ZStack {
                Rectangle().fill(CustomColor.companion.fromHsv(hue: draft.hue, saturation: 1, value: 1).raw)
                Rectangle().fill(LinearGradient(colors: [.white, .white.opacity(0)], startPoint: .leading, endPoint: .trailing))
                Rectangle().fill(LinearGradient(colors: [.black.opacity(0), .black], startPoint: .top, endPoint: .bottom))
                Circle()
                    .strokeBorder(.white, lineWidth: 3)
                    .background(Circle().strokeBorder(.black.opacity(0.6), lineWidth: 1).padding(-1))
                    .frame(width: 22, height: 22)
                    .position(x: draft.saturation * size.width, y: (1 - draft.value) * size.height)
            }
            .clipShape(.rect(cornerRadius: 12))
            .contentShape(.rect)
            .gesture(DragGesture(minimumDistance: 0).onChanged { drag in
                guard size.width > 0, size.height > 0 else { return }
                send(CustomColorEventOnHsvChanged(
                    hue: draft.hue,
                    saturation: min(max(drag.location.x / size.width, 0), 1),
                    value: min(max(1 - drag.location.y / size.height, 0), 1),
                ))
            })
        }
        .frame(height: 180)
        .accessibilityElement()
        .accessibilityLabel(Text("Saturation and brightness"))
        .accessibilityValue(Text(verbatim: "\(Int((draft.saturation * 100).rounded()))%, \(Int((draft.value * 100).rounded()))%"))
    }

    private var hueBar: some View {
        GeometryReader { proxy in
            let width = proxy.size.width
            ZStack(alignment: .leading) {
                Capsule().fill(LinearGradient(
                    colors: (0 ... 6).map { CustomColor.companion.fromHsv(hue: Double($0) * 60, saturation: 1, value: 1).raw },
                    startPoint: .leading,
                    endPoint: .trailing,
                ))
                Circle()
                    .strokeBorder(.white, lineWidth: 3)
                    .frame(width: 24, height: 24)
                    .position(x: min(max(draft.hue / 360 * width, 12), width - 12), y: 14)
            }
            .contentShape(.rect)
            .gesture(DragGesture(minimumDistance: 0).onChanged { drag in
                guard width > 0 else { return }
                send(CustomColorEventOnHsvChanged(
                    hue: min(max(drag.location.x / width * 360, 0), 360),
                    saturation: draft.saturation,
                    value: draft.value,
                ))
            })
        }
        .frame(height: 28)
        .accessibilityElement()
        .accessibilityLabel(Text("Hue"))
        .accessibilityValue(Text(verbatim: "\(Int(draft.hue.rounded()))°"))
        .accessibilityAdjustableAction { direction in
            let step = direction == .increment ? 10.0 : -10.0
            send(CustomColorEventOnHsvChanged(
                hue: min(max(draft.hue + step, 0), 360),
                saturation: draft.saturation,
                value: draft.value,
            ))
        }
    }

    private func channelField(
        _ title: LocalizedStringKey,
        channel: ColorDraft.ColorChannel,
        text: @escaping (ColorDraft) -> String,
    ) -> some View {
        TextField(title, text: Binding(
            get: { text(current() ?? draft) },
            set: { send(CustomColorEventOnChannelChanged(channel: channel, text: $0)) },
        ))
        #if os(iOS)
        .keyboardType(.numberPad)
        #endif
        .multilineTextAlignment(.center)
        .accessibilityLabel(Text(title))
    }
}
