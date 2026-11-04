import Shared
import SwiftUI

/// "New category", "New subcategory" or "Edit category" from the map (9.4): a bill's Category form, with "Group"
/// above the name while a new one may go in more than one group, and "Delete “X”?" over it.
struct MapCategoryFormView: View {
    let model: CategoryMapModel

    var body: some View {
        CategoryEditorForm(
            editor: model.state.categoryEditor,
            actions: CategoryEditorActions(
                name: Binding(
                    get: { model.current.categoryForm?.name ?? "" },
                    set: { model.send(CategoryMapUiEventOnCategoryNameChanged(text: $0)) },
                ),
                parent: Binding(
                    get: { model.state.categoryForm?.parentId },
                    set: { model.send(CategoryMapUiEventOnCategoryParentPicked(parentId: $0)) },
                ),
                pickIcon: { model.send(CategoryMapUiEventOnCategoryIconPicked(icon: $0)) },
                save: { model.send(CategoryMapUiEventOnCategoryFormSaveClicked.shared) },
                dismiss: { model.send(CategoryMapUiEventOnCategoryFormDismissed.shared) },
                askToDelete: { model.send(CategoryMapUiEventOnCategoryDeleteClicked.shared) },
            ),
        ) {
            if model.state.canPickCategoryGroup {
                Section {
                    Picker("Group", selection: group) {
                        ForEach(model.state.groups, id: \.id) { group in
                            Text(verbatim: group.name).tag(group.id)
                        }
                    }
                }
            }
        }
        .categoryDeletionAlert(
            model.state.categoryDeletion,
            groupName: model.state.categoryEditor?.groupName ?? "",
            isShown: Binding(
                get: { model.state.categoryDeletion != nil },
                set: { shown in
                    if !shown, model.current.categoryDeletion?.isDeleting == false {
                        model.send(CategoryMapUiEventOnCategoryDeleteDismissed.shared)
                    }
                },
            ),
            onConfirm: { model.send(CategoryMapUiEventOnCategoryDeleteConfirmed.shared) },
        )
    }

    private var group: Binding<String> {
        Binding(
            get: { model.state.categoryForm?.groupId ?? "" },
            set: { model.send(CategoryMapUiEventOnCategoryGroupPicked(groupId: $0)) },
        )
    }
}

extension View {
    /// The map's category form as a sheet: a form sheet with an iPad's room, a sheet on the Mac's Settings window.
    func mapCategorySheet(model: CategoryMapModel) -> some View {
        sheet(
            isPresented: Binding(
                get: { model.state.categoryForm != nil },
                set: { shown in
                    if !shown, model.current.categoryForm != nil {
                        model.send(CategoryMapUiEventOnCategoryFormDismissed.shared)
                    }
                },
            ),
        ) {
            NavigationStack { MapCategoryFormView(model: model) }
            #if os(macOS)
                .frame(width: 520, height: 600)
            #else
                .formSheetSizing()
            #endif
        }
        .glassToast("Couldn't save the category. Try again.", trigger: model.categorySaveErrorCount)
        .glassToast("Couldn't delete the category. Try again.", trigger: model.categoryDeleteErrorCount)
    }
}

/// "Edit “Churrasco”" on a group's category (9.4), which the app's categories don't have.
struct EditCategoryButton: View {
    let category: Shared.Category
    let model: CategoryMapModel

    var body: some View {
        if model.state.canCreateCategory, category.isGroupCategory {
            Button(String(localized: "Edit “\(CategoryCopy.name(category))”"), systemImage: "pencil") {
                model.send(CategoryMapUiEventOnEditCategoryClicked(categoryId: category.id))
            }
        }
    }
}

/// "New subcategory in Transport" (9.4), in every main category but Other.
struct NewSubcategoryButton: View {
    let section: MapSection
    let model: CategoryMapModel

    var body: some View {
        if model.state.canCreateCategory, !section.category.isOther {
            Button("New subcategory in \(CategoryCopy.name(section.category))", systemImage: "folder.badge.plus") {
                model.send(CategoryMapUiEventOnNewCategoryClicked(parentId: section.category.id))
            }
        }
    }
}
