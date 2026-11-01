import Shared
import SwiftUI

/// The words of "Category map" (9.1, 9.2), composed from the state: the shared code hands over the terms and the
/// categories, and each platform writes its own sentences.
enum CategoryMapCopy {
    /// "Transport › Ride-hailing", or the main category's name.
    static func path(_ category: Shared.Category, in state: CategoryMapUiState) -> String {
        CategoryCopy.path(category, parent: category.parentId.flatMap { state.categories.findOrNull(id: $0) })
    }

    /// "7 terms", "No terms · Viagem Floripa's category".
    static func count(_ section: MapSection) -> String {
        let count = Int(section.termCount)
        let terms = switch count {
        case 0: String(localized: "No terms")
        case 1: String(localized: "1 term")
        default: String(localized: "\(count) terms")
        }
        guard let group = section.groupName else { return terms }
        return "\(terms) · \(String(localized: "\(group)'s category"))"
    }

    static func subcategories(_ section: MapSection) -> String {
        let count = Int(section.subcategoryCount)
        return count == 1 ? String(localized: "1 subcategory") : String(localized: "\(count) subcategories")
    }

    static func rowLabel(_ row: MapRow) -> String {
        let name = CategoryCopy.name(row.category)
        return row.isOnlyMain ? String(localized: "Only \(name)") : name
    }

    static func results(_ count: Int, query: String) -> String {
        count == 1 ? String(localized: "1 term with “\(query)”") : String(localized: "\(count) terms with “\(query)”")
    }

    static func offlineQueue(_ count: Int) -> String {
        count == 1
            ? String(localized: "Offline · 1 change queued")
            : String(localized: "Offline · \(count) changes queued")
    }

    static func restoreMessage(_ state: CategoryMapUiState) -> String {
        let body = String(localized: "The app's 60 terms come back as they came, including those you removed or moved.")
        let own = state.ownTerms
        switch own.count {
        case 0: return body
        case 1: return "\(body) \(String(localized: "The term you created, “\(own[0].term)”, stays."))"
        default: return "\(body) \(String(localized: "The \(own.count) terms you created stay."))"
        }
    }

    /// What "Word or phrase" says under the field (9.2a to 9.2d).
    static func termHelp(_ state: CategoryMapUiState, form: TermForm) -> String {
        if let error = state.termError {
            return error == .tooShort
                ? String(localized: "Use at least 2 letters.")
                : String(localized: "Use only letters, digits and spaces.")
        }
        if let duplicate = state.duplicateTerm, let category = state.duplicateCategory {
            let where_ = path(category, in: state)
            return String(localized: "“\(duplicate.term)” already picks \(where_). Each term has one category.")
        }
        if form.original?.isAppTerm == true {
            return String(localized: "An app term. Restore default brings it back as it came.")
        }
        return String(localized: "Matches the whole word, ignoring accents and case.")
    }

    static func categoryHelp(_ state: CategoryMapUiState, form: TermForm) -> String {
        if let category = state.duplicateCategory {
            return String(localized: "Moving takes the term out of \(CategoryCopy.name(category)).")
        }
        if form.isEditing { return String(localized: "Changing it here moves the term to another category.") }
        return String(localized: "It can be the category or a subcategory.")
    }

    /// "Example: “Pizzaria” → Food › Restaurant", "If moved" for a word another category had.
    static func example(_ state: CategoryMapUiState, form: TermForm) -> String? {
        let text = form.text.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty, let category = state.formCategory else { return nil }
        let label = state.duplicateTerm == nil ? String(localized: "Example") : String(localized: "If moved")
        return "\(label): “\(text.prefix(1).uppercased() + text.dropFirst())” → \(path(category, in: state))"
    }

    static func saveTitle(_ state: CategoryMapUiState) -> String {
        if state.duplicateTerm != nil, let category = state.formCategory {
            return String(localized: "Move to \(CategoryCopy.name(category))")
        }
        return String(localized: "Save")
    }

    static func option(_ category: Shared.Category, in state: CategoryMapUiState) -> String {
        let name = CategoryCopy.name(category)
        guard let group = state.groupNameOf(category: category) else { return name }
        return "\(name) · \(String(localized: "only in \(group)"))"
    }

    static func termLabel(_ term: MapTerm, path: String) -> String {
        let label = String(localized: "\(term.term), \(path). Edit")
        return term.isPending ? "\(label), \(String(localized: "queued"))" : label
    }
}
