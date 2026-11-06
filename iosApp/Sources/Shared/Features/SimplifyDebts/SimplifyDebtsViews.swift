import Shared
import SwiftUI

/// The feature's symbol on a tile, as Edit group's row and the Balances card draw it: the accent, or muted while the
/// switch can't be used.
struct SimplifyDebtsTile: View {
    var isEnabled = true
    var size: CGFloat = 30

    var body: some View {
        Image(systemName: SimplifyDebtsCopy.icon)
            .font(.system(size: size * 0.5, weight: .semibold))
            .foregroundStyle(isEnabled ? Palette.onAccent : .secondary)
            .frame(width: size, height: size)
            .background(isEnabled ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.fill.tertiary), in: .rect(cornerRadius: size * 0.27))
            .accessibilityHidden(true)
    }
}

// MARK: - The conflict (13.1e)

/// "Ana turned it off after you. The last change counts: Simplify debts is off." with "See in Activity", in a glass
/// banner at the bottom, gone by itself after a while.
private struct SimplifyConflictBanner: ViewModifier {
    let notice: SimplifyDebtsConflictNotice?
    let onActivity: () -> Void
    let onDismiss: () -> Void

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .bottom) {
                if let notice {
                    HStack(spacing: 12) {
                        Text(verbatim: SimplifyDebtsCopy.conflict(notice))
                            .font(.subheadline)
                            .fixedSize(horizontal: false, vertical: true)
                        Spacer(minLength: 0)
                        Button("See in Activity", action: onActivity)
                            .font(.subheadline.weight(.semibold))
                            .buttonStyle(.bordered)
                            .buttonBorderShape(.capsule)
                    }
                    .padding(.leading, 16)
                    .padding(.trailing, 12)
                    .padding(.vertical, 12)
                    .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 26))
                    .padding(.horizontal, 12)
                    .padding(.bottom, 12)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .accessibilityElement(children: .contain)
                }
            }
            .animation(.default, value: notice)
            .task(id: notice) {
                guard notice != nil else { return }
                try? await Task.sleep(for: .seconds(8))
                guard !Task.isCancelled else { return }
                onDismiss()
            }
    }
}

extension View {
    /// The banner of 13.1e while `notice` is set; `onDismiss` once it went away by itself.
    func simplifyConflictBanner(
        _ notice: SimplifyDebtsConflictNotice?,
        onActivity: @escaping () -> Void,
        onDismiss: @escaping () -> Void,
    ) -> some View {
        modifier(SimplifyConflictBanner(notice: notice, onActivity: onActivity, onDismiss: onDismiss))
    }
}

// MARK: - The confirmation (13.3)

/// "Simplify debts?" (13.3, 13.3c) or "Turn off Simplify debts?" (13.3b): the counts, what it does, the payments
/// queued here (13.3d), what changes for each person and who learns of it. A sheet over Edit group on iPhone and iPad;
/// on the Mac it takes the place of Edit group's form in the same sheet.
struct SimplifyDebtsConfirmationView: View {
    let confirmation: SimplifyDebtsConfirmation
    let currency: GroupCurrency
    let isChanging: Bool
    let onConfirm: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        #if os(macOS)
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                content
                    .padding(20)
            }
            Divider()
            HStack(spacing: 8) {
                Spacer()
                Button(SimplifyDebtsCopy.cancel(confirmation), action: onDismiss)
                    .keyboardShortcut(.cancelAction)
                    .disabled(isChanging)
                Button(action: onConfirm) {
                    if isChanging {
                        ProgressView().controlSize(.small)
                    } else {
                        Text(SimplifyDebtsCopy.confirm(confirmation))
                    }
                }
                .buttonStyle(.borderedProminent)
                .prominentLabel()
                .keyboardShortcut(.defaultAction)
                .disabled(isChanging)
            }
            .padding(16)
        }
        #else
        ScrollView {
            content
                .padding(.horizontal, 20)
                .padding(.top, 24)
                .padding(.bottom, 12)
        }
        .scrollBounceBehavior(.basedOnSize)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 4) {
                PrimaryButton(title: SimplifyDebtsCopy.confirm(confirmation), isEnabled: !isChanging, isLoading: isChanging, action: onConfirm)
                Button(SimplifyDebtsCopy.cancel(confirmation), action: onDismiss)
                    .font(.headline)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .disabled(isChanging)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 8)
        }
        .background(Palette.groupedBackground)
        #endif
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(SimplifyDebtsCopy.title(confirmation))
                .font(.title2.bold())
                .accessibilityAddTraits(.isHeader)
            counts
            Text(verbatim: SimplifyDebtsCopy.body(confirmation))
                .fixedSize(horizontal: false, vertical: true)
            if let queued = SimplifyDebtsCopy.queued(confirmation.queuedPayments, currency: currency) {
                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    PendingDot()
                    Text(verbatim: queued)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .font(.footnote)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Palette.card, in: .rect(cornerRadius: 18))
            }
            if !confirmation.people.isEmpty {
                VStack(alignment: .leading, spacing: 6) {
                    Text("What changes")
                        .font(.footnote)
                        .textCase(.uppercase)
                        .foregroundStyle(.secondary)
                        .accessibilityAddTraits(.isHeader)
                    VStack(spacing: 0) {
                        ForEach(Array(confirmation.people.enumerated()), id: \.element.memberId) { index, person in
                            SimplifyPersonRow(change: person, currency: currency)
                            if index < confirmation.people.count - 1 {
                                Divider()
                                    .padding(.leading, 52)
                            }
                        }
                    }
                    .background(Palette.card, in: .rect(cornerRadius: 22))
                }
            }
            Text(verbatim: SimplifyDebtsCopy.foot(confirmation))
                .font(.footnote)
                .foregroundStyle(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// "7 payments → 5 payments", read as one sentence.
    private var counts: some View {
        HStack(spacing: 14) {
            count(confirmation.paymentsBefore, isAfter: false)
            Image(systemName: "arrow.right")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Palette.accent)
            count(confirmation.paymentsAfter, isAfter: true)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity)
        .background(Palette.card, in: .rect(cornerRadius: 22))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: SimplifyDebtsCopy.countsLabel(confirmation)))
    }

    private func count(_ value: Int32, isAfter: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text(verbatim: "\(value)")
                .font(.largeTitle.bold())
                .monospacedDigit()
            Text(verbatim: SimplifyDebtsCopy.paymentsWord(value))
                .font(.subheadline)
        }
        .foregroundStyle(isAfter ? AnyShapeStyle(Palette.accent) : AnyShapeStyle(.secondary))
    }
}

/// One person of "What changes": the face and "**Ana:** Pays $203.80 to Bruno."
private struct SimplifyPersonRow: View {
    let change: SimplifyPersonChange
    let currency: GroupCurrency

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            AvatarPhoto(initial: change.initials, color: change.avatarColor, photoUrl: change.photoUrl, size: 28)
                .padding(.top, 1)
                .accessibilityHidden(true)
            Text("\(Text(verbatim: SimplifyDebtsCopy.personName(change)).fontWeight(.semibold)): \(SimplifyDebtsCopy.personText(change, currency: currency))")
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 9)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - The group's Balances (13.4)

/// "Simplified debts · 5 payments instead of 7…" with "How it works", at the top of the pairs: a list row on iPhone and
/// iPad, the strip above the pairs on the Mac.
struct SimplifiedDebtsCard: View {
    let simplification: DebtSimplificationSummary
    let onInfo: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SimplifyDebtsTile(size: 32)
            VStack(alignment: .leading, spacing: 3) {
                Text("Simplified debts")
                    .font(.headline)
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: SimplifyDebtsCopy.card(simplification))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                Button("How it works", action: onInfo)
                    .font(.subheadline.weight(.semibold))
                    .buttonStyle(.borderless)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
    }
}

/// The icon a pair "Simplify debts" made carries next to its sentence.
struct SimplifiedPairIcon: View {
    var body: some View {
        Image(systemName: SimplifyDebtsCopy.icon)
            .font(.caption.weight(.semibold))
            .foregroundStyle(Palette.accent)
            .accessibilityLabel(Text("New pair from simplifying"))
    }
}

/// "Simplified debts" on a group's line of "By group" (13.6).
struct SimplifiedDebtsLabel: View {
    var body: some View {
        Label {
            Text("Simplified debts")
        } icon: {
            Image(systemName: SimplifyDebtsCopy.icon)
        }
        .labelStyle(.titleAndIcon)
        .font(.caption2.weight(.semibold))
        .foregroundStyle(Palette.accent)
        .padding(.horizontal, 6)
        .padding(.vertical, 2)
        .background(Palette.accent.opacity(0.12), in: .capsule)
    }
}
