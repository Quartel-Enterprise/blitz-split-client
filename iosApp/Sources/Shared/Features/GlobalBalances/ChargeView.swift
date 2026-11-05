import Observation
import Shared
import SwiftUI

@Observable
final class ChargeModel {
    private(set) var state: ChargeUiState
    private(set) var copiedCount = 0

    @ObservationIgnored private let holder: ViewModelHolder<ChargeViewModel>
    @ObservationIgnored private let watches = WatchBag()

    init(route: RouteCharge) {
        holder = BlitzSplitApple.shared.createChargeViewModel(route: route)
        let viewModel = holder.viewModel
        state = viewModel.uiState.value as! ChargeUiState
        watches.add(viewModel.stateWatcher.observe { [weak self] state in
            self?.state = state
        })
        watches.add(viewModel.actionWatcher.observe { [weak self] action in
            switch action {
            case let copy as ChargeUiActionCopyMessage:
                Pasteboard.copy(copy.message)
                self?.copiedCount += 1
            // Send is a ShareLink: a share sheet can't be opened from code
            default: break
            }
        })
    }

    /// The state as the ViewModel holds it right now: the message field reads what was typed back at once.
    var current: ChargeUiState {
        (holder.viewModel.uiState.value as? ChargeUiState) ?? state
    }

    func send(_ event: any ChargeUiEvent) {
        holder.viewModel.onEvent(event: event)
    }

    isolated deinit {
        holder.clear()
    }
}

/// Request: a message for whoever owes you, which moves no money and sends nothing by itself. Send opens the share
/// sheet (the Share menu on the Mac) and Copy puts the text on the pasteboard. Its own route, a sheet over the screen
/// that opened it (Balances, a contact's profile): about 540 pt in the middle with an iPad's room, the whole screen on
/// an iPhone lying down (8.2), split in two there: whom and the message on the left, the Pix key and the buttons on the
/// right.
struct ChargeView: View {
    let route: RouteCharge

    /// Made once the screen appears: SwiftUI rebuilds this view's value whenever its parent redraws.
    @State private var model: ChargeModel?

    @Environment(\.windowLayout) private var window

    var body: some View {
        Group {
            if let model, let charge = model.state.charge {
                ChargeContent(model: model, charge: charge)
            } else {
                // The person's message waits for Room, read in a moment
                Palette.groupedBackground.ignoresSafeArea()
            }
        }
        // No title until Room says whom: a nil name would read "Former member" for a moment
        .navigationTitle(model?.state.charge.map { Text("Request from \(PaymentCopy.name($0.name))") } ?? Text(verbatim: ""))
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model?.send(ChargeUiEventOnDismissed.shared)
                }
            }
        }
        .glassToast([
            GlassToastMessage(text: "Copied", trigger: model?.copiedCount ?? 0),
        ])
        #if os(iOS)
        // An iPad's room centres a 540 pt form sheet, which a medium detent would pin to the bottom; an iPhone lying
        // down has a compact height, where the sheet takes the whole screen whatever the detents
        .presentationDetents(window.isLarge ? [.large] : [.medium, .large])
        .formSheetSizing()
        #else
        .frame(width: 480, height: 460)
        #endif
        .onAppear {
            if model == nil { model = ChargeModel(route: route) }
        }
    }
}

private struct ChargeContent: View {
    let model: ChargeModel
    let charge: ChargeDraft

    @Environment(\.windowLayout) private var window
    @State private var layout = ScreenLayout()

    private var name: String {
        PaymentCopy.name(charge.name)
    }

    private var message: String {
        GlobalBalancesCopy.message(model.current.charge ?? charge, pixKey: model.state.pixKey)
    }

    private var messageBinding: Binding<String> {
        Binding(
            get: { message },
            set: { model.send(ChargeUiEventOnMessageChanged(message: $0)) },
        )
    }

    private var pixBinding: Binding<Bool> {
        Binding(
            get: { charge.includesPixKey },
            set: { model.send(ChargeUiEventOnPixKeyToggled(isIncluded: $0)) },
        )
    }

    /// The sheet's own size: the whole screen on an iPhone lying down splits in two; a form sheet keeps one column.
    /// Until the sheet has measured itself, the window it opens over decides, so it never opens in one column first.
    private var usesTwoColumns: Bool {
        #if os(iOS)
        let sheet = layout.size == .zero ? window : layout
        return sheet.usesTwoColumns && !sheet.isLarge
        #else
        false
        #endif
    }

    var body: some View {
        Group {
            if usesTwoColumns {
                TwoColumnsLayout(spacing: 0) {
                    Form {
                        whomSection
                        messageSection(includesPixKey: false)
                    }
                } second: {
                    Form {
                        if let pixKey = model.state.pixKey {
                            Section {
                                pixToggle(pixKey)
                            }
                        }
                        Section {
                            buttons
                                .listRowInsets(EdgeInsets())
                                .listRowBackground(Color.clear)
                        }
                    }
                }
            } else {
                Form {
                    whomSection
                    messageSection(includesPixKey: true)
                }
                .safeAreaInset(edge: .bottom) {
                    buttons
                        .padding(.horizontal, 20)
                        .padding(.bottom, 8)
                }
            }
        }
        .measuringScreenLayout($layout)
        .formStyle(.grouped)
    }

    /// Whom: the face and what they owe from which groups, with why a guest's message says more.
    private var whomSection: some View {
        Section {
            HStack(spacing: 12) {
                AvatarPhoto(initial: charge.initials, color: charge.color ?? .blue, photoUrl: charge.photoUrl, size: 36)
                    .accessibilityHidden(true)
                Text("\(name) owes you \(Text(verbatim: charge.currency.format(charge.amountMinor)).foregroundStyle(Palette.positive).bold()) · \(GlobalBalancesCopy.groupsText(names: charge.groupNames))")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            if charge.isGuest {
                Label {
                    Text("\(name) is a guest without the app. They only learn about it from this message, so it already says what went into the bill.")
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "info.circle")
                }
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
        }
    }

    /// The message, with the Pix key under it in one column, as before; two columns move the key beside it.
    private func messageSection(includesPixKey: Bool) -> some View {
        Section {
            TextField("Message", text: messageBinding, axis: .vertical)
                .lineLimit(3...8)
            if includesPixKey, let pixKey = model.state.pixKey {
                pixToggle(pixKey)
            }
        }
    }

    private func pixToggle(_ pixKey: PaymentKey) -> some View {
        Toggle(isOn: pixBinding) {
            VStack(alignment: .leading, spacing: 2) {
                Text("Include my Pix key")
                Text(verbatim: GlobalBalancesCopy.pixKey(pixKey))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .disabled(!charge.canTogglePixKey)
    }

    private var buttons: some View {
        HStack(spacing: 10) {
            Button {
                model.send(ChargeUiEventOnCopyClicked(message: message))
            } label: {
                Text("Copy message")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            ShareLink(item: message) {
                // A list row would drop the icon
                Label("Send", systemImage: "square.and.arrow.up")
                    .labelStyle(.titleAndIcon)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .prominentLabel()
            // The share sheet opens by itself; the tap still goes to the ViewModel, which records the charge in Activity
            .simultaneousGesture(TapGesture().onEnded {
                model.send(ChargeUiEventOnShareClicked(message: message))
            })
        }
        .controlSize(.large)
        .buttonBorderShape(.capsule)
    }
}
