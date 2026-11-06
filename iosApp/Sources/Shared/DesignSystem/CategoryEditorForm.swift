import Shared
import SwiftUI

/// What "New category" (7.8) says, the same from a bill's Category and from the map (9.4).
enum CategoryEditorCopy {
    /// "Já existe Churrasco em Alimentação", "Criada por Ana · usada em 3 contas" or the name's rules.
    static func nameHelp(_ editor: CategoryEditor) -> String {
        let name = editor.form.name.trimmingCharacters(in: .whitespaces)
        if editor.isNameTaken {
            if let parent = editor.parent {
                return String(
                    localized: "\(name) already exists in \(CategoryCopy.name(parent)). Pick another name.",
                    bundle: .appLanguage,
                )
            }
            return String(localized: "\(name) already exists. Pick another name.", bundle: .appLanguage)
        }
        if editor.form.isEditing {
            // Int, not KotlinInt's Int32, so the key is the catalog's "%lld" with its plural.
            let used = editor.usage.map { String(
                localized: "used in \(Int(truncating: $0)) bills",
                bundle: .appLanguage,
            ) }
            let creator = editor.creatorName.map { String(localized: "Created by \($0)", bundle: .appLanguage) }
            return [creator, used].compactMap(\.self).joined(separator: " · ")
        }
        return String(localized: "Up to 30 letters. It can't repeat inside the same category.", bundle: .appLanguage)
    }

    static func parentHelp(_ editor: CategoryEditor) -> String {
        if editor.isParentLocked { return String(
            localized: "It has subcategories, so it stays a main category.",
            bundle: .appLanguage,
        ) }
        if let parent = editor.parent {
            return String(
                localized: "A subcategory uses the icon of \(CategoryCopy.name(parent)).",
                bundle: .appLanguage,
            )
        }
        return String(localized: "Pick a category to create a subcategory.", bundle: .appLanguage)
    }

    static func groupNote(_ editor: CategoryEditor) -> String {
        if editor.form.isEditing, let usage = editor.usage {
            let name = editor.form.name.trimmingCharacters(in: .whitespaces)
            return String(
                localized: "Changing the name or the place counts for all of \(editor.groupName) and for the \(Int(truncating: usage)) bills that already use \(name).",
                bundle: .appLanguage,
            )
        }
        return String(
            localized: "It belongs to all of \(editor.groupName): anyone in the group can use, edit and delete it.",
            bundle: .appLanguage,
        )
    }

    static func formTitle(_ form: CategoryForm) -> String {
        if form.isEditing { return String(localized: "Edit category", bundle: .appLanguage) }
        return form.isSubcategory ? String(localized: "New subcategory", bundle: .appLanguage) : String(
            localized: "New category",
            bundle: .appLanguage,
        )
    }

    static func deletionMessage(_ deletion: CategoryDeletion, groupName: String) -> String {
        let name = CategoryCopy.name(deletion.category)
        if deletion.billCount == 0 { return String(
            localized: "The category goes for all of \(groupName).",
            bundle: .appLanguage,
        ) }
        return String(
            localized: "The \(Int(deletion.billCount)) bills with \(name) move to \(CategoryCopy.name(deletion.movesTo)). The category goes for all of \(groupName).",
            bundle: .appLanguage,
        )
    }
}

/// What "New category" sends back, to a bill's model or the map's.
struct CategoryEditorActions {
    let name: Binding<String>
    let parent: Binding<String?>
    let pickIcon: (CategoryIcon) -> Void
    let save: () -> Void
    let dismiss: () -> Void
    let askToDelete: () -> Void
}

/// "New category", "New subcategory" or "Edit category" (7.8), pushed over a bill's "Category" or opened over the map
/// (9.4): the name and where it goes, the icon of a main category, what it looks like on bills and who it is for.
/// `groupField` is the map's "Group", above the name. `isSheet` is for a form opened as its own sheet, which needs a
/// way out on iOS where a pushed one has the back button.
struct CategoryEditorForm<GroupField: View>: View {
    let editor: CategoryEditor?
    let actions: CategoryEditorActions
    var isSheet = false
    @ViewBuilder let groupField: () -> GroupField

    private let iconColumns = [GridItem(.adaptive(minimum: 44), spacing: 8)]

    var body: some View {
        Form {
            if let editor {
                let form = editor.form
                groupField()
                Section {
                    TextField("Name", text: actions.name.firstLetterCapitalized)
                        .capitalizesFirstLetter()
                } header: {
                    Text("Name")
                } footer: {
                    Text(verbatim: CategoryEditorCopy.nameHelp(editor))
                        .foregroundStyle(editor.isNameTaken ? Palette.negative : .secondary)
                }
                Section {
                    Picker("Inside", selection: actions.parent) {
                        Text("None: main category").tag(String?.none)
                        ForEach(editor.parents, id: \.id) { option in
                            Text(verbatim: CategoryCopy.name(option)).tag(String?.some(option.id))
                        }
                    }
                    .disabled(editor.isParentLocked)
                } footer: {
                    Text(verbatim: CategoryEditorCopy.parentHelp(editor))
                }
                if !form.isSubcategory {
                    Section("Icon") {
                        LazyVGrid(columns: iconColumns, spacing: 8) {
                            ForEach(CategoryIcon.entries, id: \.self) { icon in
                                iconButton(icon, isSelected: icon == form.icon)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                Section {
                    LabeledContent("How it shows on bills") {
                        Text(verbatim: preview(editor))
                    }
                } footer: {
                    VStack(alignment: .leading, spacing: 6) {
                        if editor.showsOfflineNotice {
                            Label("Offline: the category can already be used on this \(DeviceName.current) and reaches the group once back online.", systemImage: "icloud.slash")
                        }
                        Text(verbatim: CategoryEditorCopy.groupNote(editor))
                    }
                }
                if form.isEditing {
                    Section {
                        Button("Delete category", role: .destructive, action: actions.askToDelete)
                            .disabled(!editor.canDelete)
                    }
                }
            }
        }
        .navigationTitle(editor.map { CategoryEditorCopy.formTitle($0.form) } ?? "")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel", action: actions.dismiss)
            }
            #else
            if isSheet {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", systemImage: "xmark", role: .cancel, action: actions.dismiss)
                }
            }
            #endif
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", systemImage: "checkmark", action: actions.save)
                    .buttonStyle(.glassProminent)
                    .prominentLabel()
                    .disabled(editor?.canSave != true)
            }
        }
    }

    private func iconButton(_ icon: CategoryIcon, isSelected: Bool) -> some View {
        Button {
            actions.pickIcon(icon)
        } label: {
            Image(systemName: CategoryCopy.symbol(icon))
                .font(.title3)
                .frame(width: 44, height: 44)
                .foregroundStyle(isSelected ? Palette.onAccent : Palette.accent)
                .background(isSelected ? Palette.accent : Color.secondary.opacity(0.12), in: .rect(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(CategoryCopy.iconLabel(icon))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func preview(_ editor: CategoryEditor) -> String {
        let name = editor.form.name.trimmingCharacters(in: .whitespaces)
        guard let parent = editor.parent else { return name }
        return "\(CategoryCopy.name(parent)) › \(name)"
    }
}

extension CategoryEditorForm where GroupField == EmptyView {
    init(editor: CategoryEditor?, actions: CategoryEditorActions) {
        self.init(editor: editor, actions: actions) { EmptyView() }
    }
}

extension View {
    /// "Delete “Churrasco”?" (7.8f), saying where its bills go. `isShown`'s setter dismisses it while not deleting.
    func categoryDeletionAlert(
        _ deletion: CategoryDeletion?,
        groupName: String,
        isShown: Binding<Bool>,
        onConfirm: @escaping () -> Void,
    ) -> some View {
        alert(
            deletion.map { String(
                localized: "Delete “\(CategoryCopy.name($0.category))”?",
                bundle: .appLanguage,
            ) } ?? "",
            isPresented: isShown,
        ) {
            Button("Delete", role: .destructive, action: onConfirm)
            Button("Cancel", role: .cancel) {}
        } message: {
            if let deletion {
                Text(verbatim: CategoryEditorCopy.deletionMessage(deletion, groupName: groupName))
            }
        }
    }
}
