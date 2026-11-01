import Shared
import SwiftUI

/// "Exceptions by category" (6.7f): each category that splits its own way, or "None", and "Add exception".
struct DefaultSplitExceptions: View {
    let model: DefaultSplitModel

    private var state: DefaultSplitUiState {
        model.state
    }

    var body: some View {
        Section {
            if state.exceptionRows.isEmpty {
                Text("None. Every category follows the split above.")
                    .foregroundStyle(.secondary)
            }
            ForEach(state.exceptionRows, id: \.exception.categoryId) { row in
                Button {
                    model.send(DefaultSplitUiEventOnExceptionClicked(categoryId: row.exception.categoryId))
                } label: {
                    HStack(spacing: 12) {
                        CategoryBadge(category: row.category, size: 36)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(verbatim: CategoryCopy.name(row.category))
                            Text(verbatim: ExceptionCopy.summary(row, state: state))
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .contentShape(.rect)
                }
                .buttonStyle(.plain)
            }
            Button("Add exception", systemImage: "plus") {
                model.send(DefaultSplitUiEventOnAddExceptionClicked.shared)
            }
        } header: {
            Text("Exceptions by category")
        }
    }
}

enum ExceptionCopy {
    /// "Equally among everyone", "Equally among 2", "Shares, 2 to 1", "Percentage, 60% · 40%".
    static func summary(_ row: ExceptionRow, state: DefaultSplitUiState) -> String {
        let values = row.valuesOf(memberIds: state.people.map(\.id)).map(\.int64Value)
        switch row.exception.mode {
        case .shares:
            let text = values.count == 2
                ? String(localized: "\(values[0]) to \(values[1])")
                : values.map { String($0) }.joined(separator: " · ")
            return String(localized: "Shares, \(text)")
        case .percent:
            return String(localized: "Percentage, \(values.map { "\($0)%" }.joined(separator: " · "))")
        default:
            if row.includedCount >= row.totalCount { return String(localized: "Equally among everyone") }
            return String(localized: "Equally among \(Int(row.includedCount))")
        }
    }

    static func modeLabel(_ mode: DefaultSplitMode) -> String {
        switch mode {
        case .shares: String(localized: "Shares")
        case .percent: String(localized: "Percentage")
        default: String(localized: "Equal")
        }
    }

    /// What is missing, else "On a R$ 100,00 bill in Groceries: Pierre R$ 50,00 · Ana R$ 50,00".
    static func status(_ state: DefaultSplitUiState, editor: ExceptionEditor) -> String {
        guard let category = state.exceptionCategory else { return String(localized: "Pick a category for the exception.") }
        let name = CategoryCopy.name(category)
        if state.isExceptionCategoryTaken { return String(localized: "\(name) already has an exception.") }
        if editor.showsPercentError {
            let sum = Int(editor.percentSum)
            let remaining = Int(editor.remainingPercent)
            if remaining > 0 { return String(localized: "It adds up to \(sum)%. \(remaining)% missing.") }
            return String(localized: "It adds up to \(sum)%. \(-remaining)% too much.")
        }
        let parts = editor.figures.rows
            .compactMap { row -> String? in
                guard let minor = row.exampleMinor?.int64Value, minor > 0 else { return nil }
                let who = row.person.isMe ? String(localized: "You") : row.person.name
                return "\(who) \(state.currency.format(minor))"
            }
            .joined(separator: " · ")
        let total = state.currency.format(editor.figures.exampleTotalMinor)
        return String(localized: "On a \(total) bill in \(name): \(parts)")
    }
}

/// "Exception by category", a sheet over "Default split": one category, its own way and who is in it, saved only on
/// "Save exception".
struct ExceptionSheet: View {
    let model: DefaultSplitModel

    private var state: DefaultSplitUiState {
        model.state
    }

    var body: some View {
        NavigationStack {
            Form {
                if let editor = state.exceptionEditor {
                    Section {
                        ChipsRow(categories: state.exceptionMains, selectedId: editor.categoryId, openId: editor.openMainId, model: model)
                        if !state.exceptionSubcategories.isEmpty {
                            ChipsRow(categories: state.exceptionSubcategories, selectedId: editor.categoryId, openId: nil, model: model)
                        }
                    } header: {
                        Text("Category")
                    } footer: {
                        Text("Bills of this category split another way.")
                    }
                    Section("How to split") {
                        Picker("How to split", selection: mode) {
                            ForEach(state.exceptionModes, id: \.self) { option in
                                Text(verbatim: ExceptionCopy.modeLabel(option)).tag(option)
                            }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                    }
                    Section {
                        ForEach(editor.people, id: \.id) { person in
                            personRow(person, mode: editor.mode)
                        }
                    } footer: {
                        Text(verbatim: ExceptionCopy.status(state, editor: editor))
                            .foregroundStyle(editor.showsPercentError || state.isExceptionCategoryTaken ? Palette.negative : .secondary)
                    }
                    if editor.editedCategoryId != nil {
                        Section {
                            Button("Remove exception", role: .destructive) {
                                model.send(DefaultSplitUiEventOnExceptionRemoveClicked.shared)
                            }
                        }
                    }
                }
            }
            .formStyle(.grouped)
            .navigationTitle("Exception by category")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark", role: .cancel) {
                        model.send(DefaultSplitUiEventOnExceptionDismissed.shared)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save exception", systemImage: "checkmark") {
                        model.send(DefaultSplitUiEventOnExceptionSaveClicked.shared)
                    }
                    .buttonStyle(.glassProminent)
                    .disabled(!state.canSaveException)
                }
            }
        }
        #if os(macOS)
        .frame(width: 520, height: 560)
        #endif
    }

    @ViewBuilder
    private func personRow(_ person: DefaultSplitPerson, mode: DefaultSplitMode) -> some View {
        if mode == .equal {
            Button {
                model.send(DefaultSplitUiEventOnExceptionPersonToggled(memberId: person.id))
            } label: {
                personLine(person, mode: mode)
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(person.isIncluded ? .isSelected : [])
        } else {
            personLine(person, mode: mode)
        }
    }

    @ViewBuilder
    private func personLine(_ person: DefaultSplitPerson, mode: DefaultSplitMode) -> some View {
        HStack(spacing: 12) {
            if mode == .equal {
                Image(systemName: person.isIncluded ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(person.isIncluded ? Palette.accent : .secondary)
                    .accessibilityHidden(true)
            }
            AvatarPhoto(initial: person.initials, color: person.avatarColor, photoUrl: nil, size: 32)
                .accessibilityHidden(true)
            Text(verbatim: person.isMe ? String(localized: "You") : person.name)
            Spacer()
            switch mode {
            case .shares:
                Text(verbatim: "\(person.parts)")
                    .monospacedDigit()
                Stepper {
                    Text("Shares of \(person.name)")
                } onIncrement: {
                    model.send(DefaultSplitUiEventOnExceptionPartsIncreased(memberId: person.id))
                } onDecrement: {
                    model.send(DefaultSplitUiEventOnExceptionPartsDecreased(memberId: person.id))
                }
                .labelsHidden()
            case .percent:
                TextField(text: percent(of: person.id), prompt: Text(verbatim: "0")) {
                    Text("Percentage of \(person.name)")
                }
                #if os(iOS)
                .keyboardType(.numberPad)
                #endif
                .inlineValueFieldStyle(width: 52, isError: model.state.exceptionEditor?.showsPercentError == true)
                Text(verbatim: "%")
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
            default:
                EmptyView()
            }
        }
        .contentShape(.rect)
    }

    private var mode: Binding<DefaultSplitMode> {
        Binding(
            get: { model.state.exceptionEditor?.mode ?? .equal },
            set: { model.send(DefaultSplitUiEventOnExceptionModeSelected(mode: $0)) },
        )
    }

    private func percent(of memberId: String) -> Binding<String> {
        Binding(
            get: { model.current.exceptionEditor?.people.first { $0.id == memberId }?.percentText ?? "" },
            set: { model.send(DefaultSplitUiEventOnExceptionPercentChanged(memberId: memberId, text: $0)) },
        )
    }
}

/// The categories an exception may take, as chips that wrap; a main one shows its subcategories too.
private struct ChipsRow: View {
    let categories: [Shared.Category]
    let selectedId: String?
    let openId: String?
    let model: DefaultSplitModel

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories, id: \.id) { category in
                    let isSelected = category.id == selectedId
                    Button {
                        model.send(DefaultSplitUiEventOnExceptionCategoryPicked(categoryId: category.id))
                    } label: {
                        Label {
                            Text(verbatim: CategoryCopy.name(category))
                        } icon: {
                            if isSelected { Image(systemName: "checkmark") }
                        }
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(isSelected || category.id == openId ? Palette.accent.opacity(0.16) : Color.clear, in: .rect(cornerRadius: 8))
                        .overlay {
                            if !(isSelected || category.id == openId) {
                                RoundedRectangle(cornerRadius: 8).stroke(.secondary.opacity(0.4))
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
            .padding(.vertical, 2)
        }
    }
}
