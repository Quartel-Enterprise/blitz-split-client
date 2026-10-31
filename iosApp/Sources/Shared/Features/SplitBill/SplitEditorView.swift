import Shared
import SwiftUI

/// "Split": how the bill divides, by mode, over the bill. The bill's split changes only on "Done", which waits until
/// it adds up; "Back" leaves it as it was.
private struct SplitModePicker: View {
    let mode: SplitMode
    let send: (any SplitBillUiEvent) -> Void

    var body: some View {
        Picker("How to split", selection: selection) {
            ForEach(SplitBillCopy.modes, id: \.self) { mode in
                SplitBillCopy.modeLabel(mode).tag(mode)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
    }

    private var selection: Binding<SplitMode> {
        Binding(
            get: { mode },
            set: { send(SplitBillUiEventOnSplitModeClicked(mode: $0)) },
        )
    }
}

/// "$62.40 each" over "$312.00 among 5 people. Diego is out.", with the bar of what the parts cover by amount.
private struct SplitStatusView: View {
    let status: SplitBillCopy.Status

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(verbatim: status.title)
                .font(.headline)
                .monospacedDigit()
            if let progress = status.progress {
                ProgressView(value: progress)
                    .tint(status.isError ? Palette.negative : Palette.accent)
                    .accessibilityHidden(true)
            }
            if let detail = status.detail {
                Text(verbatim: detail)
                    .font(.subheadline)
                    .foregroundStyle(status.isError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.secondary))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .foregroundStyle(status.isError ? AnyShapeStyle(Palette.onErrorContainer) : AnyShapeStyle(.primary))
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.updatesFrequently)
    }
}

/// One person: the checkbox of Equal, the field of Amounts or %, or the stepper of Shares. Whoever is out of the
/// split is dimmed.
private struct SplitPersonRow: View {
    let row: SplitRow
    let editor: SplitEditor
    let model: SplitBillModel

    private var person: BillPerson {
        row.person
    }

    private var isOut: Bool {
        editor.excluded.contains { $0.id == person.id }
    }

    /// By amount the row stays lit: the field is where the person comes back in.
    private var isDimmed: Bool {
        isOut && editor.mode != .amount
    }

    var body: some View {
        if editor.mode == .equal {
            Button {
                model.send(SplitBillUiEventOnPersonToggled(memberId: person.id))
            } label: {
                content
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(row.isIncluded ? .isSelected : [])
        } else {
            content
        }
    }

    private var content: some View {
        let subtitle = SplitBillCopy.rowSubtitle(row, editor: editor, isOut: isOut)
        return HStack(spacing: 12) {
            if editor.mode == .equal {
                Image(systemName: row.isIncluded ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(checkboxStyle)
                    .accessibilityHidden(true)
            }
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 32)
                .opacity(isOut ? 0.5 : 1)
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: SplitBillCopy.name(person))
                    .foregroundStyle(isDimmed ? .secondary : .primary)
                Text(verbatim: subtitle.text)
                    .font(.footnote)
                    .foregroundStyle(subtitle.isError ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.secondary))
                    .monospacedDigit()
            }
            Spacer(minLength: 8)
            trailing
        }
        .contentShape(.rect)
    }

    private var checkboxStyle: AnyShapeStyle {
        if row.isIncluded { return AnyShapeStyle(Palette.accent) }
        return editor.includedCount == 0 ? AnyShapeStyle(Palette.negative) : AnyShapeStyle(.tertiary)
    }

    @ViewBuilder
    private var trailing: some View {
        switch editor.mode {
        case .amount:
            HStack(spacing: 4) {
                Text(verbatim: editor.currency.symbol)
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                valueField(prompt: editor.currency.zeroPlaceholder, width: 96)
            }
        case .percent:
            HStack(spacing: 4) {
                valueField(prompt: "0", width: 52)
                Text(verbatim: "%")
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            }
        case .shares:
            HStack(spacing: 8) {
                Text(verbatim: "\(row.parts)")
                    .monospacedDigit()
                    .frame(minWidth: 24, alignment: .trailing)
                Stepper {
                    Text("Shares of \(person.name)")
                } onIncrement: {
                    model.send(SplitBillUiEventOnPartsIncreased(memberId: person.id))
                } onDecrement: {
                    model.send(SplitBillUiEventOnPartsDecreased(memberId: person.id))
                }
                .labelsHidden()
            }
        default:
            Text(verbatim: row.shareMinor.map { editor.currency.format($0.int64Value) } ?? "—")
                .monospacedDigit()
                .foregroundStyle(.secondary)
        }
    }

    private func valueField(prompt: String, width: CGFloat) -> some View {
        let isError = editor.mode == .amount && !editor.addsUp && editor.withoutValue.contains { $0.id == person.id }
        return TextField(text: model.value(of: person.id), prompt: Text(verbatim: prompt)) {
            if editor.mode == .amount {
                Text("Part of \(person.name)")
            } else {
                Text("Percentage of \(person.name)")
            }
        }
        .multilineTextAlignment(.trailing)
        .monospacedDigit()
        #if os(iOS)
        .keyboardType(editor.mode == .amount ? .decimalPad : .numberPad)
        .padding(.horizontal, 10)
        .frame(width: width, height: 36)
        .background(.fill.tertiary, in: .rect(cornerRadius: 10))
        .overlay {
            if isError {
                RoundedRectangle(cornerRadius: 10).strokeBorder(Palette.negative, lineWidth: 2)
            }
        }
        #else
        .frame(width: width)
        .overlay {
            if isError {
                RoundedRectangle(cornerRadius: 5).strokeBorder(Palette.negative, lineWidth: 1.5)
            }
        }
        #endif
    }
}

/// "Who's in" with "Check all" or "Uncheck all" by Equal; the other modes only name their list.
private struct SplitListHeader: View {
    let editor: SplitEditor
    let send: (any SplitBillUiEvent) -> Void

    var body: some View {
        HStack {
            Text(SplitBillCopy.listLabel(editor.mode))
            Spacer()
            if editor.mode == .equal {
                Button(editor.isEveryoneIncluded ? "Uncheck all" : "Check all") {
                    send(SplitBillUiEventOnEveryoneToggled.shared)
                }
                .font(.footnote.weight(.semibold))
                .textCase(nil)
                .buttonStyle(.borderless)
            }
        }
    }
}

#if os(iOS)
/// Pushed inside the bill's sheet. Lying down and on iPad: the summary, the modes and the status on the left, the
/// people on the right.
struct PhoneSplitEditor: View {
    let model: SplitBillModel

    @State private var layout = ScreenLayout()

    var body: some View {
        Group {
            if let editor = model.state.splitEditor ?? model.shownEditor {
                if layout.usesTwoColumns {
                    HStack(alignment: .top, spacing: 0) {
                        Form { overview(editor) }
                        Form { people(editor) }
                    }
                } else {
                    Form {
                        overview(editor)
                        people(editor)
                    }
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .measuringScreenLayout($layout)
        .navigationTitle(Text(SplitBillCopy.editorTitle))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done", systemImage: "checkmark") {
                    model.send(SplitBillUiEventOnSplitDone.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(model.state.splitEditor?.addsUp != true)
            }
        }
    }

    @ViewBuilder
    private func overview(_ editor: SplitEditor) -> some View {
        Section {
            HStack(spacing: 12) {
                Image(systemName: "receipt")
                    .font(.title3)
                    .foregroundStyle(Palette.accent)
                    .frame(width: 44, height: 44)
                    .background(Palette.accent.opacity(0.12), in: .rect(cornerRadius: 13))
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: SplitBillCopy.paidBy(model.state.description_, state: model.state))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    Text(verbatim: editor.currency.format(editor.totalMinor))
                        .font(.largeTitle.bold())
                        .monospacedDigit()
                }
            }
            .accessibilityElement(children: .combine)
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4))
        Section {
            SplitModePicker(mode: editor.mode) { model.send($0) }
        }
        .listRowBackground(Color.clear)
        .listRowInsets(EdgeInsets())
        let status = SplitBillCopy.status(editor)
        Section {
            SplitStatusView(status: status)
                .listRowBackground(status.isError ? Palette.errorContainer : Palette.card)
        } footer: {
            Text(verbatim: SplitBillCopy.hint(editor))
        }
    }

    private func people(_ editor: SplitEditor) -> some View {
        Section {
            ForEach(editor.rows, id: \.person.id) { row in
                SplitPersonRow(row: row, editor: editor, model: model)
            }
        } header: {
            SplitListHeader(editor: editor) { model.send($0) }
        }
    }
}
#endif

#if os(macOS)
/// "Split Groceries": the sheet's second page, with Back and Done at the bottom.
struct MacSplitEditor: View {
    let model: SplitBillModel

    var body: some View {
        if let editor = model.state.splitEditor {
            let status = SplitBillCopy.status(editor)
            VStack(alignment: .leading, spacing: 14) {
                VStack(alignment: .leading, spacing: 2) {
                    Group {
                        let description = model.state.description_.trimmingCharacters(in: .whitespacesAndNewlines)
                        if description.isEmpty {
                            Text(SplitBillCopy.editorTitle)
                        } else {
                            Text("Split \(description)")
                        }
                    }
                    .font(.title2.bold())
                    .lineLimit(1)
                    Text(verbatim: SplitBillCopy.paidBy(editor.currency.format(editor.totalMinor), state: model.state))
                    .foregroundStyle(.secondary)
                }
                SplitModePicker(mode: editor.mode) { model.send($0) }
                SplitStatusView(status: status)
                    .padding(12)
                    .background(
                        status.isError ? AnyShapeStyle(Palette.errorContainer) : AnyShapeStyle(.fill.quaternary),
                        in: .rect(cornerRadius: 10),
                    )
                VStack(alignment: .leading, spacing: 6) {
                    SplitListHeader(editor: editor) { model.send($0) }
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    ScrollView {
                        VStack(spacing: 0) {
                            ForEach(Array(editor.rows.enumerated()), id: \.element.person.id) { index, row in
                                if index > 0 { Divider() }
                                SplitPersonRow(row: row, editor: editor, model: model)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                            }
                        }
                    }
                    .frame(maxHeight: 320)
                    .fixedSize(horizontal: false, vertical: true)
                    .background(.background, in: .rect(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(.separator))
                }
                Text(verbatim: SplitBillCopy.hint(editor))
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 8) {
                    Spacer()
                    Button("Back") {
                        model.send(SplitBillUiEventOnSplitDismissed.shared)
                    }
                    .keyboardShortcut(.cancelAction)
                    Button("Done") {
                        model.send(SplitBillUiEventOnSplitDone.shared)
                    }
                    .buttonStyle(.borderedProminent)
                    .keyboardShortcut(.defaultAction)
                    .disabled(!editor.addsUp)
                }
                .controlSize(.large)
            }
            .padding(20)
        }
    }
}
#endif
