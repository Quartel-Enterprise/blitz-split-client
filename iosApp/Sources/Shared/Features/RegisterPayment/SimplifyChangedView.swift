import Shared
import SwiftUI

/// 13.5c's banner: who changed "Simplify debts" just now and that the pair owes nothing anymore, then what to do if
/// the money already went.
private struct SimplifyChangedBanner: View {
    let change: SimplifyChangedNotice
    let to: PaymentPerson?

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SimplifyDebtsTile(size: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: SimplifyDebtsCopy.changed(change, to: to))
                    .font(.subheadline.weight(.semibold))
                Text("If you already sent the money, mark it as paid anyway: the app redoes the math.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 20))
        .accessibilityElement(children: .combine)
    }
}

/// Whom the payment was for, dimmed, with what was owed struck through: "Pay Bruno · Floripa trip · what you owed".
private struct SimplifyChangedPayee: View {
    let change: SimplifyChangedNotice
    let to: PaymentPerson?
    let currency: GroupCurrency
    let size: CGFloat

    private var name: String {
        PaymentCopy.name(to?.name)
    }

    private var amount: String {
        currency.format(change.previousDebtMinor)
    }

    var body: some View {
        HStack(spacing: 14) {
            if let to {
                AvatarPhoto(initial: to.initials, color: to.avatarColor, photoUrl: to.photoUrl, size: size)
                    .opacity(0.6)
                    .accessibilityHidden(true)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("Pay \(name)")
                    .font(.title3.bold())
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: String(
                    localized: "\(change.groupName) · what you owed",
                    bundle: .appLanguage,
                    comment: "13.5c: where the old debt came from",
                ))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Text(verbatim: amount)
                .font(.title3.bold())
                .monospacedDigit()
                .strikethrough()
                .foregroundStyle(.secondary)
                .accessibilityLabel(Text("was \(amount)", comment: "13.5c: the amount owed before, struck through"))
        }
        // No opacity over the row: its secondary lines and the strikethrough already say it is the old debt, and a
        // dimmed secondary read at under 2:1
        .accessibilityElement(children: .combine)
    }
}

#if os(iOS)
/// 13.5c in Record payment's sheet, in the place of the form or "Pay Ana": the banner, the payee as it was, "I paid
/// anyway" (the payment as it was owed) and "Close".
struct PhoneSimplifyChanged: View {
    let model: RegisterPaymentModel
    let change: SimplifyChangedNotice

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                SimplifyChangedBanner(change: change, to: state.to)
                SimplifyChangedPayee(change: change, to: state.to, currency: state.currency, size: 48)
            }
            .padding(20)
            .frame(maxWidth: ScreenLayout.twoColumnsMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .scrollBounceBehavior(.basedOnSize)
        .background(Palette.groupedBackground)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 4) {
                PrimaryButton(title: "I paid anyway", isEnabled: state.canPayAnyway, isLoading: state.isSaving) {
                    model.send(RegisterPaymentUiEventOnPaidAnywayClicked.shared)
                }
                Button("Close") {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 44)
                .disabled(state.isSaving)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close", systemImage: "xmark", role: .cancel) {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .disabled(state.isSaving)
            }
        }
    }
}
#endif

#if os(macOS)
/// 13.5c in the Mac's 440 pt sheet: the banner, the payee as it was, and Close and "I paid anyway".
struct MacSimplifyChanged: View {
    let model: RegisterPaymentModel
    let change: SimplifyChangedNotice

    private var state: RegisterPaymentUiState {
        model.state
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SimplifyChangedBanner(change: change, to: state.to)
            SimplifyChangedPayee(change: change, to: state.to, currency: state.currency, size: 36)
            HStack(spacing: 8) {
                Spacer()
                Button("Close") {
                    model.send(RegisterPaymentUiEventOnCloseClicked.shared)
                }
                .keyboardShortcut(.cancelAction)
                .disabled(state.isSaving)
                Button("I paid anyway") {
                    model.send(RegisterPaymentUiEventOnPaidAnywayClicked.shared)
                }
                .buttonStyle(.borderedProminent)
                .prominentLabel()
                .keyboardShortcut(.defaultAction)
                .disabled(!state.canPayAnyway)
            }
        }
        .padding(20)
        .frame(width: 440)
    }
}
#endif
