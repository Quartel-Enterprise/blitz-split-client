import Shared
import SwiftUI

/// "Ler cupom fiscal" (7.1e to 7.1g) on Rachar conta: what the form shows of the receipt, the same on every Apple
/// platform but its place, which each form picks.
extension SplitBillModel {
    /// The photo taken or picked; a photo that can't be read goes as empty bytes, which read as a receipt that
    /// couldn't be read.
    func receiptPicked(_ data: Data?) {
        send(SplitBillUiEventOnReceiptPhotoPicked(photo: BlitzSplitApple.shared.toByteArray(data: data ?? Data())))
    }

    /// "Scan receipt" asks where the photo comes from (beta feedback, part 2, item 7), once the form's sheet has
    /// settled (Início's shortcut opens it with the bill). Closing it without a pick says so, a beat later: a button
    /// of the dialog may close it before its own action has picked.
    func receiptSourceChoiceShown(isSettled: Bool) -> Binding<Bool> {
        Binding(
            get: { isSettled && self.state.showsReceiptSourceChoice },
            set: { shown in
                guard !shown else { return }
                DispatchQueue.main.async {
                    if self.current.showsReceiptSourceChoice { self.send(SplitBillUiEventOnReceiptPickerDismissed.shared) }
                }
            },
        )
    }

    /// The camera, Photos or the Mac's file panel, once picked on that choice; closing it without a photo says so.
    func receiptSourceShown(_ source: ReceiptPhotoSource, isSettled: Bool) -> Binding<Bool> {
        Binding(
            get: { isSettled && self.state.shownReceiptSource == source },
            set: { shown in
                if !shown, self.current.shownReceiptSource == source {
                    self.send(SplitBillUiEventOnReceiptPickerDismissed.shared)
                }
            },
        )
    }

    func pickReceiptSource(_ source: ReceiptPhotoSource) {
        send(SplitBillUiEventOnReceiptSourcePicked(source: source))
    }
}

extension View {
    /// "Scan receipt"'s choice: "Take photo" (where there is a camera) or "Choose from Photos", and on the Mac "Choose
    /// File…" too, as a bill's "Add" offers them. On iPhone and iPad it points at "Scan receipt" while that button is
    /// on screen (`ReceiptSection`), else at the form.
    func receiptSourceDialog(model: SplitBillModel, isPresented: Binding<Bool>) -> some View {
        confirmationDialog(receiptSourceTitle, isPresented: isPresented, titleVisibility: .visible) {
            #if os(iOS)
            Button("Take photo") { model.pickReceiptSource(.camera) }
            #else
            Button("Choose File…") { model.pickReceiptSource(.files) }
            #endif
            Button("Choose from Photos") { model.pickReceiptSource(.photos) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Fills in the amount, date and description from the photo.")
        }
    }

    /// The form's side of the choice: the dialog while no "Scan receipt" button holds it, Photos once picked, and
    /// straight to Photos where there is no camera to choose, the Mac's file panel being its own.
    func receiptSourceChoice(model: SplitBillModel, isSettled: Bool) -> some View {
        receiptSourceDialog(
            model: model,
            isPresented: model.receiptSourceChoiceShown(
                isSettled: isSettled && ReceiptSource.hasChoice && model.receiptChoiceAnchors == 0,
            ),
        )
        .onChange(of: isSettled && model.state.showsReceiptSourceChoice, initial: true) { _, shows in
            if shows, !ReceiptSource.hasChoice { model.send(SplitBillUiEventOnReceiptChoiceSkipped(source: .photos)) }
        }
        .photoLibraryPicker(isPresented: model.receiptSourceShown(.photos, isSettled: isSettled)) { data in
            model.receiptPicked(data)
        }
    }

    private var receiptSourceTitle: Text {
        #if os(macOS)
        Text("Scan Receipt")
        #else
        Text("Scan receipt")
        #endif
    }
}

enum ReceiptSource {
    /// A device without a camera (or one restricted) has nothing to choose: Photos opens at once.
    static var hasChoice: Bool {
        #if os(iOS)
        CameraPicker.isAvailable
        #else
        true
        #endif
    }
}

/// Why the receipt read nothing (7.1g); the fields stay as they were, to fill in by hand. "Tirar outra foto" for a
/// photo nothing could be read from, "Tentar de novo" sends the same photo when the reader didn't answer or the
/// connection dropped, and the day's limit offers neither.
struct ReceiptFailureText: View {
    let model: SplitBillModel
    let error: ReceiptError

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(message)
                .fixedSize(horizontal: false, vertical: true)
            if model.state.offersReceiptRetake {
                Button(retakeLabel) {
                    model.send(SplitBillUiEventOnScanReceiptClicked.shared)
                }
                .disabled(!model.state.canScanReceipt)
            } else if model.state.offersReceiptRetry {
                Button("Try again") {
                    model.send(SplitBillUiEventOnReceiptRetryClicked.shared)
                }
                .disabled(!model.state.canScanReceipt)
            }
        }
    }

    /// The Mac's banner heads the text with it.
    static func title(of error: ReceiptError) -> LocalizedStringKey {
        switch error {
        case .readerUnavailable: "The receipt reader didn't answer"
        case .limitReached: "Today's reading limit"
        case .offline: "No connection"
        default: "Couldn't read the receipt"
        }
    }

    /// The Mac's banner already heads it with the title, so its text goes on from there.
    private var message: LocalizedStringKey {
        #if os(macOS)
        detail
        #else
        switch error {
        case .readerUnavailable:
            "The receipt reader didn't answer. It isn't your photo: try again in a moment, or fill it in by hand."
        case .limitReached:
            "You've read 30 receipts in the last 24 hours. Fill it in by hand, or read it again tomorrow."
        case .offline:
            "No connection. The photo didn't reach the reader: try again once you're back online, or fill it in by hand."
        default:
            "Couldn't read the receipt. Try again with more light and the whole receipt in the photo, or fill it in by hand."
        }
        #endif
    }

    private var detail: LocalizedStringKey {
        switch error {
        case .readerUnavailable: "It isn't your photo. Try again in a moment, or fill it in by hand."
        case .limitReached: "You've read 30 receipts in the last 24 hours. Fill it in by hand, or read it again tomorrow."
        case .offline: "The photo didn't reach the reader. Try again once you're back online, or fill it in by hand."
        default: "Try again with more light and the whole receipt in the photo, or fill it in by hand."
        }
    }

    private var retakeLabel: LocalizedStringKey {
        #if os(macOS)
        "Choose Another Photo…"
        #else
        "Take another photo"
        #endif
    }
}

/// "Lido do cupom" and "14 itens" under the description once a receipt was read; the items open Dividir on "Itens".
struct ReceiptReadChips: View {
    let receipt: BillReceipt
    let onItemsTapped: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Label("Read from the receipt", systemImage: "receipt")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.accent)
                .padding(.horizontal, 12)
                .frame(height: 30)
                .background(Palette.accent.opacity(0.12), in: .capsule)
            if receipt.itemCount > 0 {
                Button(action: onItemsTapped) {
                    HStack(spacing: 2) {
                        Text("\(Int(receipt.itemCount)) items")
                        Image(systemName: "chevron.right")
                            .font(.caption2.weight(.semibold))
                            .accessibilityHidden(true)
                    }
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 12)
                    .padding(.trailing, 10)
                    .frame(height: 30)
                    .background(.fill.tertiary, in: .capsule)
                    .contentShape(.capsule)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

/// The board's small receipt, a few printed lines on a slip.
struct ReceiptThumbnail: View {
    var width: CGFloat = 32
    var height: CGFloat = 40

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Capsule().frame(height: 2)
            Capsule().frame(width: (width - 12) * 0.7, height: 2)
            Capsule().frame(height: 2)
        }
        .foregroundStyle(.secondary)
        .padding(.horizontal, 6)
        .padding(.vertical, 7)
        .frame(width: width, height: height, alignment: .top)
        .background(.fill.tertiary, in: .rect(cornerRadius: 6))
        .accessibilityHidden(true)
    }
}

#if os(iOS)
/// Under "Detalhes": "Ler cupom fiscal" until a receipt is picked (where the board offers it), then "Foto do cupom",
/// which the bill attaches once saved, with × to take it off.
struct ReceiptSection: View {
    let model: SplitBillModel
    let isSettled: Bool
    var showsScanButton = true

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        if let receipt = state.receipt, receipt.hasPhoto {
            Section {
                HStack(spacing: 12) {
                    ReceiptThumbnail()
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Receipt photo")
                        Group {
                            if receipt.status == .reading {
                                Text("Reading the receipt…")
                            } else {
                                Text("Everyone in the group sees it")
                            }
                        }
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    }
                    .accessibilityElement(children: .combine)
                    Spacer(minLength: 8)
                    if receipt.status == .reading {
                        ProgressView()
                    }
                    Button {
                        model.send(SplitBillUiEventOnReceiptRemoved.shared)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Remove the receipt photo"))
                }
            }
        } else if state.showsScanReceipt, showsScanButton {
            Section {
                Button {
                    model.send(SplitBillUiEventOnScanReceiptClicked.shared)
                } label: {
                    Label("Scan receipt", systemImage: "doc.viewfinder")
                }
                .disabled(!state.canScanReceipt)
                // Why: the choice points at the button that asked for it, while the button is on screen.
                .receiptSourceDialog(
                    model: model,
                    isPresented: model.receiptSourceChoiceShown(isSettled: isSettled && ReceiptSource.hasChoice),
                )
                .onAppear { model.receiptChoiceAnchors += 1 }
                .onDisappear { model.receiptChoiceAnchors -= 1 }
            } footer: {
                if state.isOnline {
                    Text("Fills in the amount, date and description from the photo.")
                } else {
                    Text("Needs internet. Fill it in by hand or scan the receipt once you reconnect.")
                }
            }
        }
    }
}
#endif

#if os(macOS)
/// The form's "Cupom:" row: "Ler Cupom Fiscal…" until a receipt is picked, then "Foto do cupom anexada", read or
/// being read, with "Remover" (7.1e).
struct MacReceiptRow: View {
    let model: SplitBillModel

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        if let receipt = state.receipt, receipt.hasPhoto {
            LabeledContent("Receipt:") {
                HStack(spacing: 8) {
                    ReceiptThumbnail(width: 22, height: 28)
                    Text("Receipt photo attached")
                    switch receipt.status {
                    case .reading:
                        ProgressView()
                            .controlSize(.small)
                        Text("Reading…")
                            .foregroundStyle(.secondary)
                    case .read:
                        Text("· Read from the receipt")
                            .fontWeight(.semibold)
                            .foregroundStyle(Palette.accent)
                        if receipt.itemCount > 0 {
                            Button("\(Int(receipt.itemCount)) items") {
                                model.send(SplitBillUiEventOnReceiptItemsClicked.shared)
                            }
                            .buttonStyle(.link)
                            // Why: two links side by side, the items and "Remover", read apart.
                            Text(verbatim: "·")
                                .foregroundStyle(.secondary)
                        }
                    default:
                        EmptyView()
                    }
                    Button("Remove") {
                        model.send(SplitBillUiEventOnReceiptRemoved.shared)
                    }
                    .buttonStyle(.link)
                }
            }
        } else if state.showsScanReceipt {
            LabeledContent("Receipt:") {
                HStack(spacing: 10) {
                    Button("Scan Receipt…") {
                        model.send(SplitBillUiEventOnScanReceiptClicked.shared)
                    }
                    .disabled(!state.canScanReceipt)
                    Group {
                        if state.isOnline {
                            Text("Fills in the amount, date and description from the photo.")
                        } else {
                            Text("Needs internet. Fill it in by hand or scan the receipt once you reconnect.")
                        }
                    }
                    .font(.callout)
                    .foregroundStyle(.secondary)
                }
            }
        }
    }
}
#endif
