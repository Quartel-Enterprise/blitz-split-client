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
            isSheet: true,
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
    /// The map's forms in one sheet: "New term" and "Edit term" (9.2), and the category's form (9.4), which a new
    /// category swaps for "New term" in place, since SwiftUI can't dismiss one sheet and present another at once. A form
    /// sheet with an iPad's room, a sheet on the Mac's Settings window.
    func mapFormSheet(model: CategoryMapModel) -> some View {
        sheet(
            isPresented: Binding(
                get: { model.state.categoryForm != nil || model.state.termForm != nil },
                set: { shown in
                    guard !shown else { return }
                    if model.current.categoryForm != nil {
                        model.send(CategoryMapUiEventOnCategoryFormDismissed.shared)
                    } else if model.current.termForm != nil {
                        model.send(CategoryMapUiEventOnTermDismissed.shared)
                    }
                },
            ),
        ) {
            let isCategory = model.state.categoryForm != nil
            NavigationStack {
                if isCategory {
                    MapCategoryFormView(model: model)
                } else {
                    TermFormView(model: model)
                }
            }
            // Why: the ViewModel keeps a form open while it saves, so a swipe then would leave the state and the sheet apart.
            .interactiveDismissDisabled(
                model.state.categoryForm?.isSaving == true || model.state.termForm?.isSaving == true,
            )
            // Why: in the sheet, as a failed save or delete keeps the form open over the map, which would hide it.
            .glassToast([
                GlassToastMessage(text: "Couldn't save the category. Try again.", trigger: model.categorySaveErrorCount),
                GlassToastMessage(text: "Couldn't delete the category. Try again.", trigger: model.categoryDeleteErrorCount),
            ])
            #if os(macOS)
            .frame(width: isCategory ? 520 : 460, height: isCategory ? 600 : 420)
            #else
            .formSheetSizing()
            #endif
        }
    }
}

/// "Edit “Churrasco”" on a group's category (9.4), which the app's categories don't have.
struct EditCategoryButton: View {
    let category: Shared.Category
    let model: CategoryMapModel

    var body: some View {
        if model.state.canCreateCategory, category.isGroupCategory {
            Button(String(
                localized: "Edit “\(CategoryCopy.name(category))”",
                bundle: .appLanguage,
            ), systemImage: "pencil") {
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
