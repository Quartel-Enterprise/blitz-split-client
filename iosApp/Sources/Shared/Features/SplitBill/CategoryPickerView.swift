import Shared
import SwiftUI

/// The words of "Category" (7.7) and "New category" (7.8), composed from the state: the shared code hands over the
/// categories and what chose the bill's, and each platform writes its own sentences.
enum CategoryPickerCopy {
    static func path(_ category: Shared.Category, in state: SplitBillUiState) -> String {
        CategoryCopy.path(category, parent: category.parentId.flatMap { state.categories.findOrNull(id: $0) })
    }

    /// The line under the description, what chose the bill's category (7.1, 7.1l to 7.1n).
    static func hint(_ state: SplitBillUiState) -> String {
        let path = CategoryCopy.path(state.category, parent: state.categoryParent)
        let term = state.categoryMatches.first?.term ?? ""
        switch state.categoryHint {
        case .byTerm:
            return String(localized: "\(path) by the word “\(term)” in your map. Tap the icon to change it.")
        case .byLongestTerm:
            return String(localized: "\(state.categoryMatches.count) words of the map matched. “\(term)” wins, the longest: \(path).")
        case .noTerm:
            return String(localized: "No word of your map. The bill stays in Other; tap the icon to pick one.")
        case .changed:
            let mapPath = state.mapCategory.map { self.path($0, in: state) } ?? ""
            return String(localized: "You picked \(path). By the map it would be \(mapPath) (“\(term)”).")
        default:
            return String(localized: "Category: \(path). Tap the icon to change it.")
        }
    }

    static func reason(_ reason: CategorySuggestionReason) -> String? {
        switch reason {
        case .description_: String(localized: "From the description")
        case .mostUsed: String(localized: "Most used in the group")
        case .group: String(localized: "From the group")
        default: nil
        }
    }

    /// The bill's subcategory under its main one, "Created by Carla · from the group" for the group's, or how many.
    static func branchSubtitle(_ branch: CategoryBranch, state: SplitBillUiState) -> String {
        let category = branch.category
        if let selected = branch.subcategories.first(where: { $0.id == state.category.id }) {
            return CategoryCopy.name(selected)
        }
        if category.isGroupCategory {
            if let creator = state.people.first(where: { $0.id == category.createdByMemberId }) {
                return String(localized: "Created by \(creator.name) · from the group")
            }
            return String(localized: "From the group")
        }
        if branch.subcategories.isEmpty { return String(localized: "No subcategories") }
        return String(localized: "\(branch.subcategories.count) subcategories")
    }

    /// "Já existe Churrasco em Alimentação", "Criada por Ana · usada em 3 contas" or the name's rules.
    static func nameHelp(_ form: CategoryForm, state: SplitBillUiState) -> String {
        let name = form.name.trimmingCharacters(in: .whitespaces)
        let parent = form.parentId.flatMap { state.categories.findOrNull(id: $0) }
        if state.isCategoryNameTaken {
            if let parent {
                return String(localized: "\(name) already exists in \(CategoryCopy.name(parent)). Pick another name.")
            }
            return String(localized: "\(name) already exists. Pick another name.")
        }
        if form.isEditing {
            let creator = state.categories.findOrNull(id: form.categoryId)
                .flatMap { category in state.people.first { $0.id == category.createdByMemberId } }
            let used = String(localized: "used in \(Int(state.categoryFormUsage)) bills")
            guard let creator else { return used }
            return "\(String(localized: "Created by \(creator.name)")) · \(used)"
        }
        return String(localized: "Up to 30 letters. It can't repeat inside the same category.")
    }

    static func parentHelp(_ form: CategoryForm, state: SplitBillUiState) -> String {
        if state.isCategoryParentLocked { return String(localized: "It has subcategories, so it stays a main category.") }
        if let parent = form.parentId.flatMap({ state.categories.findOrNull(id: $0) }) {
            return String(localized: "A subcategory uses the icon of \(CategoryCopy.name(parent)).")
        }
        return String(localized: "Pick a category to create a subcategory.")
    }

    static func groupNote(_ form: CategoryForm, state: SplitBillUiState) -> String {
        if form.isEditing {
            let name = form.name.trimmingCharacters(in: .whitespaces)
            return String(localized: "Changing the name or the place counts for all of \(state.groupName) and for the \(Int(state.categoryFormUsage)) bills that already use \(name).")
        }
        return String(localized: "It belongs to all of \(state.groupName): anyone in the group can use, edit and delete it.")
    }

    static func formTitle(_ form: CategoryForm) -> String {
        if form.isEditing { return String(localized: "Edit category") }
        return form.isSubcategory ? String(localized: "New subcategory") : String(localized: "New category")
    }

    static func deletionMessage(_ deletion: CategoryDeletion, state: SplitBillUiState) -> String {
        let name = CategoryCopy.name(deletion.category)
        if deletion.billCount == 0 { return String(localized: "The category goes for all of \(state.groupName).") }
        return String(localized: "The \(Int(deletion.billCount)) bills with \(name) move to \(CategoryCopy.name(deletion.movesTo)). The category goes for all of \(state.groupName).")
    }
}

/// "Category" (7.7), pushed over "Split a bill" on iPhone and iPad: the search, "Suggested" and "All categories",
/// each main one opening its subcategories; lying down the open one's subcategories sit beside the list. A group's own
/// category has a context menu to edit it.
struct CategoryPickerView: View {
    let model: SplitBillModel

    @State private var layout = ScreenLayout()

    private var state: SplitBillUiState {
        model.state
    }

    var body: some View {
        Group {
            if layout.usesTwoColumns, !state.isSearchingCategories, let branch = state.openCategoryBranch {
                HStack(alignment: .top, spacing: 0) {
                    List { listSections(showsInline: false) }
                    List { sideSection(branch) }
                }
            } else {
                List { listSections(showsInline: true) }
            }
        }
        .measuringScreenLayout($layout)
        .searchable(text: query, prompt: Text("Search categories"))
        .navigationTitle("Category")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("New category", systemImage: "plus") {
                    model.send(SplitBillUiEventOnNewCategoryClicked(parentId: nil, name: ""))
                }
            }
        }
        .navigationDestination(isPresented: formShown) {
            CategoryFormView(model: model)
        }
    }

    @ViewBuilder
    private func listSections(showsInline: Bool) -> some View {
        if state.isSearchingCategories {
            searchSection
        } else {
            suggestionsSection
            Section("All categories") {
                ForEach(state.categories.branches, id: \.category.id) { branch in
                    branchRow(branch, showsInline: showsInline)
                }
            }
        }
    }

    @ViewBuilder
    private var suggestionsSection: some View {
        if !state.categorySuggestions.isEmpty {
            Section {
                ForEach(state.categorySuggestions, id: \.category.id) { suggestion in
                    Button {
                        model.send(SplitBillUiEventOnCategoryPicked(categoryId: suggestion.category.id))
                    } label: {
                        CategoryRowLabel(
                            category: suggestion.category,
                            title: CategoryCopy.name(suggestion.category),
                            subtitle: CategoryPickerCopy.reason(suggestion.reason),
                            isSelected: suggestion.isSelected,
                        )
                    }
                    .buttonStyle(.plain)
                }
            } header: {
                Text("Suggested")
            } footer: {
                if let mostUsed = state.mostUsedCategory {
                    Text("\(CategoryCopy.name(mostUsed)) is the most used in \(state.groupName).")
                }
            }
        }
    }

    @ViewBuilder
    private func branchRow(_ branch: CategoryBranch, showsInline: Bool) -> some View {
        let category = branch.category
        let isOpen = state.categoryPicker?.openMainId == category.id
        Button {
            model.send(SplitBillUiEventOnCategoryMainToggled(categoryId: category.id))
        } label: {
            HStack {
                CategoryRowLabel(
                    category: category,
                    title: CategoryCopy.name(category),
                    subtitle: CategoryPickerCopy.branchSubtitle(branch, state: state),
                    isSelected: false,
                )
                if !category.isOther {
                    Image(systemName: isOpen ? "chevron.up" : "chevron.down")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOpen ? .isSelected : [])
        .categoryEditMenu(category, model: model)
        if isOpen, showsInline {
            subcategoryRows(branch)
        }
    }

    @ViewBuilder
    private func subcategoryRows(_ branch: CategoryBranch) -> some View {
        let main = branch.category
        Button {
            model.send(SplitBillUiEventOnCategoryPicked(categoryId: main.id))
        } label: {
            SubcategoryLabel(title: String(localized: "Only \(CategoryCopy.name(main))"), isSelected: state.category.id == main.id)
        }
        .buttonStyle(.plain)
        ForEach(branch.subcategories, id: \.id) { sub in
            Button {
                model.send(SplitBillUiEventOnCategoryPicked(categoryId: sub.id))
            } label: {
                SubcategoryLabel(title: CategoryCopy.name(sub), isSelected: state.category.id == sub.id, isGroupCategory: sub.isGroupCategory)
            }
            .buttonStyle(.plain)
            .categoryEditMenu(sub, model: model)
        }
        Button {
            model.send(SplitBillUiEventOnNewCategoryClicked(parentId: main.id, name: ""))
        } label: {
            Label("New in \(CategoryCopy.name(main))", systemImage: "plus")
                .padding(.leading, 24)
        }
    }

    private func sideSection(_ branch: CategoryBranch) -> some View {
        Section {
            subcategoryRows(branch)
        } header: {
            HStack(spacing: 12) {
                CategoryBadge(category: branch.category, size: 32)
                VStack(alignment: .leading, spacing: 2) {
                    Text(verbatim: CategoryCopy.name(branch.category))
                        .font(.headline)
                    Text("Pick a subcategory or keep just \(CategoryCopy.name(branch.category))")
                        .font(.footnote)
                }
            }
            .textCase(nil)
        } footer: {
            Text("Touch and hold one of the group's categories to edit or delete it.")
        }
    }

    @ViewBuilder
    private var searchSection: some View {
        let query = state.categoryPicker?.query.trimmingCharacters(in: .whitespaces) ?? ""
        if state.showsNoCategoryFound {
            ContentUnavailableView {
                Label("No category with “\(query)”", systemImage: "magnifyingglass")
            } description: {
                Text("Create one: everyone in \(state.groupName) can use it.")
            } actions: {
                Button("Create “\(query)”") {
                    model.send(SplitBillUiEventOnNewCategoryClicked(parentId: nil, name: query))
                }
                .buttonStyle(.borderedProminent)
            }
        } else {
            Section("\(state.categoryResults.count) results") {
                ForEach(state.categoryResults, id: \.id) { category in
                    Button {
                        model.send(SplitBillUiEventOnCategoryPicked(categoryId: category.id))
                    } label: {
                        CategoryRowLabel(
                            category: category,
                            title: CategoryCopy.name(category),
                            subtitle: category.parentId
                                .flatMap { state.categories.findOrNull(id: $0) }
                                .map { String(localized: "in \(CategoryCopy.name($0))") },
                            isSelected: category.id == state.category.id,
                        )
                    }
                    .buttonStyle(.plain)
                    .categoryEditMenu(category, model: model)
                }
                Button("Create category “\(query)”", systemImage: "plus") {
                    model.send(SplitBillUiEventOnNewCategoryClicked(parentId: nil, name: query))
                }
            }
        }
    }

    private var query: Binding<String> {
        Binding(
            get: { model.current.categoryPicker?.query ?? "" },
            set: { model.send(SplitBillUiEventOnCategoryQueryChanged(text: $0)) },
        )
    }

    private var formShown: Binding<Bool> {
        Binding(
            get: { model.state.categoryForm != nil },
            set: { shown in
                if !shown, model.current.categoryForm != nil { model.send(SplitBillUiEventOnCategoryFormDismissed.shared) }
            },
        )
    }
}

/// A category's badge, its name and what is under it, with a check for the one the bill has.
struct CategoryRowLabel: View {
    let category: Shared.Category
    let title: String
    let subtitle: String?
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            CategoryBadge(category: category, size: 36)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(verbatim: title)
                    if category.isGroupCategory {
                        Image(systemName: "person.2")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .accessibilityLabel("From the group")
                    }
                }
                if let subtitle {
                    Text(verbatim: subtitle)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

private struct SubcategoryLabel: View {
    let title: String
    let isSelected: Bool
    var isGroupCategory = false

    var body: some View {
        HStack(spacing: 8) {
            Text(verbatim: title)
            if isGroupCategory {
                Image(systemName: "person.2")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .accessibilityLabel("From the group")
            }
            Spacer()
            if isSelected {
                Image(systemName: "checkmark")
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
            }
        }
        .padding(.leading, 24)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

extension View {
    /// "Edit" on a group's own category: a context menu (a long press on iPhone and iPad, a right click on the Mac).
    @ViewBuilder
    func categoryEditMenu(_ category: Shared.Category, model: SplitBillModel) -> some View {
        if category.isGroupCategory {
            contextMenu {
                Button("Edit", systemImage: "pencil") {
                    model.send(SplitBillUiEventOnEditCategoryClicked(categoryId: category.id))
                }
            }
        } else {
            self
        }
    }
}

/// "New category", "New subcategory" or "Edit category" (7.8), pushed over "Category": the name and where it goes,
/// the icon of a main category, what it looks like on bills and who it is for.
struct CategoryFormView: View {
    let model: SplitBillModel

    private var state: SplitBillUiState {
        model.state
    }

    private let iconColumns = [GridItem(.adaptive(minimum: 44), spacing: 8)]

    var body: some View {
        Form {
            if let form = state.categoryForm {
                Section {
                    TextField("Name", text: name)
                    #if os(iOS)
                        .textInputAutocapitalization(.sentences)
                    #endif
                } header: {
                    Text("Name")
                } footer: {
                    Text(verbatim: CategoryPickerCopy.nameHelp(form, state: state))
                        .foregroundStyle(state.isCategoryNameTaken ? Palette.negative : .secondary)
                }
                Section {
                    Picker("Inside", selection: parent) {
                        Text("None: main category").tag(String?.none)
                        ForEach(state.categoryParents, id: \.id) { option in
                            Text(verbatim: CategoryCopy.name(option)).tag(String?.some(option.id))
                        }
                    }
                    .disabled(state.isCategoryParentLocked)
                } footer: {
                    Text(verbatim: CategoryPickerCopy.parentHelp(form, state: state))
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
                        Text(verbatim: preview(form))
                    }
                } footer: {
                    VStack(alignment: .leading, spacing: 6) {
                        if state.showsOfflineNotice {
                            Label("Offline: the category can already be used on this \(DeviceName.current) and reaches the group once back online.", systemImage: "icloud.slash")
                        }
                        Text(verbatim: CategoryPickerCopy.groupNote(form, state: state))
                    }
                }
                if form.isEditing {
                    Section {
                        Button("Delete category", role: .destructive) {
                            model.send(SplitBillUiEventOnCategoryDeleteClicked.shared)
                        }
                    }
                }
            }
        }
        .navigationTitle(state.categoryForm.map(CategoryPickerCopy.formTitle) ?? "")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .toolbar {
            #if os(macOS)
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") {
                    model.send(SplitBillUiEventOnCategoryFormDismissed.shared)
                }
            }
            #endif
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", systemImage: "checkmark") {
                    model.send(SplitBillUiEventOnCategoryFormSaveClicked.shared)
                }
                .buttonStyle(.glassProminent)
                .disabled(!state.canSaveCategory)
            }
        }
        .categoryDeletionAlert(model: model)
    }

    private func iconButton(_ icon: CategoryIcon, isSelected: Bool) -> some View {
        Button {
            model.send(SplitBillUiEventOnCategoryIconPicked(icon: icon))
        } label: {
            Image(systemName: CategoryCopy.symbol(icon))
                .font(.title3)
                .frame(width: 44, height: 44)
                .foregroundStyle(isSelected ? Color.white : Palette.accent)
                .background(isSelected ? Palette.accent : Color.secondary.opacity(0.12), in: .rect(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(CategoryCopy.iconLabel(icon))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func preview(_ form: CategoryForm) -> String {
        let name = form.name.trimmingCharacters(in: .whitespaces)
        guard let parent = form.parentId.flatMap({ state.categories.findOrNull(id: $0) }) else { return name }
        return "\(CategoryCopy.name(parent)) › \(name)"
    }

    private var name: Binding<String> {
        Binding(
            get: { model.current.categoryForm?.name ?? "" },
            set: { model.send(SplitBillUiEventOnCategoryNameChanged(text: $0)) },
        )
    }

    private var parent: Binding<String?> {
        Binding(
            get: { model.state.categoryForm?.parentId },
            set: { model.send(SplitBillUiEventOnCategoryParentPicked(parentId: $0)) },
        )
    }
}

extension View {
    /// "Delete “Churrasco”?" (7.8f), saying where its bills go.
    func categoryDeletionAlert(model: SplitBillModel) -> some View {
        let deletion = model.state.categoryDeletion
        return alert(
            deletion.map { String(localized: "Delete “\(CategoryCopy.name($0.category))”?") } ?? "",
            isPresented: Binding(
                get: { model.state.categoryDeletion != nil },
                set: { shown in
                    if !shown, model.current.categoryDeletion?.isDeleting == false {
                        model.send(SplitBillUiEventOnCategoryDeleteDismissed.shared)
                    }
                },
            ),
        ) {
            Button("Delete", role: .destructive) {
                model.send(SplitBillUiEventOnCategoryDeleteConfirmed.shared)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            if let deletion {
                Text(verbatim: CategoryPickerCopy.deletionMessage(deletion, state: model.state))
            }
        }
    }
}
