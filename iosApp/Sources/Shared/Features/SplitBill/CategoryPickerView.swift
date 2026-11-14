import Shared
import SwiftUI

/// The words of "Category" (7.7) and "New category" (7.8), composed from the state: the shared code hands over the
/// categories and what chose the bill's, and each platform writes its own sentences.
enum CategoryPickerCopy {
    static func path(_ category: Shared.Category, in state: SplitBillUiState) -> String {
        CategoryCopy.path(category, parent: category.parentId.flatMap { state.categories.findOrNull(id: $0) })
    }

    /// The line under the description, what chose the bill's category (7.1, 7.1l to 7.1n). The Mac clicks the icon
    /// (its menu beside the description) where a touch screen taps it.
    static func hint(_ state: SplitBillUiState) -> String {
        let path = CategoryCopy.path(state.category, parent: state.categoryParent)
        let term = state.categoryMatches.first?.term ?? ""
        switch state.categoryHint {
        case .byTerm:
            #if os(macOS)
            return String(
                localized: "\(path) by the word “\(term)” in your map. Click the icon to change it.",
                bundle: .appLanguage,
            )
            #else
            return String(
                localized: "\(path) by the word “\(term)” in your map. Tap the icon to change it.",
                bundle: .appLanguage,
            )
            #endif
        case .byReceipt:
            #if os(macOS)
            return String(localized: "\(path) from the receipt. Click the icon to change it.", bundle: .appLanguage)
            #else
            return String(localized: "\(path) from the receipt. Tap the icon to change it.", bundle: .appLanguage)
            #endif
        case .byLongestTerm:
            return String(
                localized: "\(state.categoryMatches.count) words of the map matched. “\(term)” wins, the longest: \(path).",
                bundle: .appLanguage,
            )
        case .noTerm:
            #if os(macOS)
            return String(
                localized: "No word of your map. The bill stays in Other; click the icon to pick one.",
                bundle: .appLanguage,
            )
            #else
            return String(
                localized: "No word of your map. The bill stays in Other; tap the icon to pick one.",
                bundle: .appLanguage,
            )
            #endif
        case .changed:
            let mapPath = state.mapCategory.map { self.path($0, in: state) } ?? ""
            return String(
                localized: "You picked \(path). By the map it would be \(mapPath) (“\(term)”). On saving, we ask whether the map learns.",
                bundle: .appLanguage,
            )
        default:
            #if os(macOS)
            return String(localized: "Category: \(path). Click the icon to change it.", bundle: .appLanguage)
            #else
            return String(localized: "Category: \(path). Tap the icon to change it.", bundle: .appLanguage)
            #endif
        }
    }

    static func reason(_ reason: CategorySuggestionReason) -> String? {
        switch reason {
        case .description_: String(localized: "From the description", bundle: .appLanguage)
        case .mostUsed: String(localized: "Most used in the group", bundle: .appLanguage)
        case .group: String(localized: "From the group", bundle: .appLanguage)
        case .receipt: String(localized: "From the receipt", bundle: .appLanguage)
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
                return String(localized: "Created by \(creator.name) · from the group", bundle: .appLanguage)
            }
            return String(localized: "From the group", bundle: .appLanguage)
        }
        if branch.subcategories.isEmpty { return String(localized: "No subcategories", bundle: .appLanguage) }
        return String(localized: "\(branch.subcategories.count) subcategories", bundle: .appLanguage)
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
            Section {
                CategoryMapCardButton(model: model)
            }
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
            SubcategoryLabel(title: String(
                localized: "Only \(CategoryCopy.name(main))",
                bundle: .appLanguage,
            ), isSelected: state.category.id == main.id)
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
                .prominentLabel()
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
                                .map { String(localized: "in \(CategoryCopy.name($0))", bundle: .appLanguage) },
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

/// "New category", "New subcategory" or "Edit category" (7.8), pushed over "Category", as the map opens it too.
struct CategoryFormView: View {
    let model: SplitBillModel

    var body: some View {
        CategoryEditorForm(
            editor: model.state.categoryEditor,
            actions: CategoryEditorActions(
                name: Binding(
                    get: { model.current.categoryForm?.name ?? "" },
                    set: { model.send(SplitBillUiEventOnCategoryNameChanged(text: $0)) },
                ),
                parent: Binding(
                    get: { model.state.categoryForm?.parentId },
                    set: { model.send(SplitBillUiEventOnCategoryParentPicked(parentId: $0)) },
                ),
                pickIcon: { model.send(SplitBillUiEventOnCategoryIconPicked(icon: $0)) },
                pickColor: { model.send(SplitBillUiEventOnCategoryColorPicked(color: $0)) },
                customColors: model.state.customColors,
                sendCustomColor: { model.send(SplitBillUiEventOnCustomColor(event: $0)) },
                save: { model.send(SplitBillUiEventOnCategoryFormSaveClicked.shared) },
                dismiss: { model.send(SplitBillUiEventOnCategoryFormDismissed.shared) },
                askToDelete: { model.send(SplitBillUiEventOnCategoryDeleteClicked.shared) },
            ),
        )
        .categoryDeletionAlert(
            model.state.categoryDeletion,
            groupName: model.state.groupName,
            isShown: Binding(
                get: { model.state.categoryDeletion != nil },
                set: { shown in
                    if !shown, model.current.categoryDeletion?.isDeleting == false {
                        model.send(SplitBillUiEventOnCategoryDeleteDismissed.shared)
                    }
                },
            ),
            onConfirm: { model.send(SplitBillUiEventOnCategoryDeleteConfirmed.shared) },
        )
    }
}

/// "Category map" between Suggested and All categories (9.0): which word chose the bill's category, and the way to the
/// words that choose it. It opens over the bill's form, which keeps what was typed.
private struct CategoryMapCardButton: View {
    let model: SplitBillModel

    var body: some View {
        let state = model.state
        Button {
            model.send(SplitBillUiEventOnCategoryDismissed.shared)
            model.send(SplitBillUiEventOnCategoryMapClicked.shared)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "tag")
                    .foregroundStyle(Palette.accent)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Category map")
                        .font(.headline)
                    if let match = state.categoryMatches.first, let category = state.mapCategory {
                        Text("\(CategoryCopy.name(category)) came from the word “\(match.term)”. See and edit the words that pick the category.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    } else {
                        Text("See and edit the words that pick a bill's category from its title.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Image(systemName: "chevron.forward")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}
