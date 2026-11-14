import Shared
import SwiftUI

/// The colors a category may take (7.7): the twelve named ones, then the person's own (a long press, or a right click
/// on the Mac, offers to delete one), then "+", which opens "New color" (Pierre's beta feedback of 2026-11-14).
/// `categoryKey` is the app category "Change color of Food" is open for, nil in a form. The sheet and the questions
/// about the person's colors hang on the grid on screen.
struct CategoryColorGrid: View {
    let selected: CategoryTint
    let customColors: CustomColorState
    let pick: (CategoryTint) -> Void
    let send: (CustomColorEvent) -> Void
    var categoryKey: String?

    private let columns = [GridItem(.adaptive(minimum: 44), spacing: 8)]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(CategoryColor.entries, id: \.self) { color in
                    swatch(color.tint)
                }
                ForEach(customColors.customSwatchesFor(selected: selected), id: \.hex) { color in
                    swatch(color.tint)
                        .contextMenu {
                            if customColors.isDeletable(color: color) {
                                Button("Delete", systemImage: "trash", role: .destructive) {
                                    send(CustomColorEventOnLongPressed(color: color))
                                }
                            }
                        }
                }
                if customColors.canAdd {
                    Button {
                        send(CustomColorEventOnAddClicked(current: selected, categoryKey: categoryKey))
                    } label: {
                        Image(systemName: "plus")
                            .font(.body.bold())
                            .frame(width: 36, height: 36)
                            .overlay { Circle().strokeBorder(.secondary, lineWidth: 1.5) }
                            .frame(width: 44, height: 44)
                            .contentShape(.circle)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Add color"))
                }
            }
            if let note {
                Text(note)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .customColorDialogs(customColors, send: send)
    }

    /// How to delete one of the person's colors, said where the hand is: a long press, a right click on the Mac.
    private var note: LocalizedStringKey? {
        #if os(macOS)
        if !customColors.canAdd { return "Your palette is full: right-click one of your colors to delete it." }
        return customColors.palette.isEmpty ? nil : "Right-click one of your colors to delete it."
        #else
        if !customColors.canAdd { return "Your palette is full: long press one of your colors to delete it." }
        return customColors.palette.isEmpty ? nil : "Long press one of your colors to delete it."
        #endif
    }

    private func swatch(_ tint: CategoryTint) -> some View {
        let isSelected = tint.isSame(as: selected)
        return Button {
            pick(tint)
        } label: {
            Circle()
                .fill(tint.container)
                .frame(width: 36, height: 36)
                .overlay {
                    if isSelected {
                        Image(systemName: "checkmark").font(.footnote.bold()).foregroundStyle(tint.content)
                    }
                }
                .overlay { Circle().strokeBorder(isSelected ? Palette.accent : .clear, lineWidth: 3) }
                .frame(width: 44, height: 44)
                .contentShape(.circle)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tint.label)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

extension View {
    /// "New color", then "Delete this color?" or "This color is in use", over the grid that opened them.
    func customColorDialogs(_ state: CustomColorState, send: @escaping (CustomColorEvent) -> Void) -> some View {
        let deletion = state.deletion
        return sheet(isPresented: Binding(
            get: { state.draft != nil },
            set: { shown in if !shown { send(CustomColorEventOnDraftDismissed.shared) } },
        )) {
            if let draft = state.draft {
                CustomColorSheet(draft: draft, send: send)
            }
        }
        .alert(
            deletion?.usedBy == nil ? String(localized: "Delete this color?", bundle: .appLanguage)
                : String(localized: "This color is in use", bundle: .appLanguage),
            isPresented: Binding(
                get: { deletion != nil },
                // Why: every button of the alert sends its own event; the binding's close would send a second one.
                set: { _ in },
            ),
            presenting: deletion,
        ) { deletion in
            if deletion.usedBy == nil {
                Button("Delete", role: .destructive) { send(CustomColorEventOnDeleteConfirmed.shared) }
                Button("Cancel", role: .cancel) { send(CustomColorEventOnDeletionDismissed.shared) }
            } else {
                Button("OK", role: .cancel) { send(CustomColorEventOnDeletionDismissed.shared) }
            }
        } message: { deletion in
            if let usedBy = deletion.usedBy {
                Text("\(CategoryCopy.name(usedBy)) uses this color. Change its color first, then delete it.")
            } else {
                Text("It leaves your palette on all your devices.")
            }
        }
    }
}
