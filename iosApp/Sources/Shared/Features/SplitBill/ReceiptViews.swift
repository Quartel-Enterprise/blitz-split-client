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

    /// The camera (a file panel on the Mac) while "Ler cupom fiscal" asks for it, once the form's sheet has settled
    /// (Início's shortcut opens it with the bill); closing it without a photo says so.
    func receiptCameraShown(isSettled: Bool) -> Binding<Bool> {
        Binding(
            get: { isSettled && self.state.showsReceiptCamera },
            set: { shown in
                if !shown, self.current.showsReceiptCamera { self.send(SplitBillUiEventOnReceiptCameraDismissed.shared) }
            },
        )
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

/// "Lido do cupom" and "14 itens" under the description once a receipt was read.
struct ReceiptReadChips: View {
    let receipt: BillReceipt

    var body: some View {
        HStack(spacing: 8) {
            Label("Read from the receipt", systemImage: "receipt")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(Palette.accent)
                .padding(.horizontal, 12)
                .frame(height: 30)
                .background(Palette.accent.opacity(0.12), in: .capsule)
            if receipt.itemCount > 0 {
                Text("\(Int(receipt.itemCount)) items")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 12)
                    .frame(height: 30)
                    .background(.fill.tertiary, in: .capsule)
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
                        Group {
                            if receipt.itemCount > 0 {
                                Text("· Read from the receipt, \(Int(receipt.itemCount)) items")
                            } else {
                                Text("· Read from the receipt")
                            }
                        }
                        .fontWeight(.semibold)
                        .foregroundStyle(Palette.accent)
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
