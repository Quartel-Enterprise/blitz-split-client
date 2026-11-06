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
        case 0: String(localized: "No terms", bundle: .appLanguage)
        case 1: String(localized: "1 term", bundle: .appLanguage)
        default: String(localized: "\(count) terms", bundle: .appLanguage)
        }
        guard let group = section.groupName else { return terms }
        return "\(terms) · \(String(localized: "\(group)'s category", bundle: .appLanguage))"
    }

    static func subcategories(_ section: MapSection) -> String {
        let count = Int(section.subcategoryCount)
        return count == 1 ? String(localized: "1 subcategory", bundle: .appLanguage) : String(
            localized: "\(count) subcategories",
            bundle: .appLanguage,
        )
    }

    static func rowLabel(_ row: MapRow) -> String {
        let name = CategoryCopy.name(row.category)
        return row.isOnlyMain ? String(localized: "Only \(name)", bundle: .appLanguage) : name
    }

    static func results(_ count: Int, query: String) -> String {
        count == 1 ? String(localized: "1 term with “\(query)”", bundle: .appLanguage) : String(
            localized: "\(count) terms with “\(query)”",
            bundle: .appLanguage,
        )
    }

    static func offlineQueue(_ count: Int) -> String {
        count == 1
            ? String(localized: "Offline · 1 change queued", bundle: .appLanguage)
            : String(localized: "Offline · \(count) changes queued", bundle: .appLanguage)
    }

    static func restoreMessage(_ state: CategoryMapUiState) -> String {
        let body = String(
            localized: "The app's 60 terms come back as they came, including those you removed or moved.",
            bundle: .appLanguage,
        )
        let own = state.ownTerms
        switch own.count {
        case 0: return body
        case 1: return "\(body) \(String(localized: "The term you created, “\(own[0].term)”, stays.", bundle: .appLanguage))"
        default: return "\(body) \(String(localized: "The \(own.count) terms you created stay.", bundle: .appLanguage))"
        }
    }

    /// What "Word or phrase" says under the field (9.2a to 9.2d).
    static func termHelp(_ state: CategoryMapUiState, form: TermForm) -> String {
        if let error = state.termError {
            switch error {
            case .tooShort: return String(localized: "Use at least 2 letters.", bundle: .appLanguage)
            case .tooLong: return String(localized: "Use at most 30 letters.", bundle: .appLanguage)
            default: return String(localized: "Use only letters, digits and spaces.", bundle: .appLanguage)
            }
        }
        if let duplicate = state.duplicateTerm, let category = state.duplicateCategory {
            let where_ = path(category, in: state)
            return String(
                localized: "“\(duplicate.term)” already picks \(where_). Each term has one category.",
                bundle: .appLanguage,
            )
        }
        if form.original?.isAppTerm == true {
            return String(localized: "An app term. Restore default brings it back as it came.", bundle: .appLanguage)
        }
        return String(localized: "Matches the whole word, ignoring accents and case.", bundle: .appLanguage)
    }

    static func categoryHelp(_ state: CategoryMapUiState, form: TermForm) -> String {
        if let category = state.duplicateCategory {
            return String(
                localized: "Moving takes the term out of \(CategoryCopy.name(category)).",
                bundle: .appLanguage,
            )
        }
        if form.isEditing { return String(
            localized: "Changing it here moves the term to another category.",
            bundle: .appLanguage,
        ) }
        return String(localized: "It can be the category or a subcategory.", bundle: .appLanguage)
    }

    /// "Example: “Pizzaria” → Food › Restaurant", "If moved" for a word another category had.
    static func example(_ state: CategoryMapUiState, form: TermForm) -> String? {
        let text = form.text.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty, let category = state.formCategory else { return nil }
        let label = state.duplicateTerm == nil ? String(localized: "Example", bundle: .appLanguage) : String(
            localized: "If moved",
            bundle: .appLanguage,
        )
        return "\(label): “\(text.prefix(1).uppercased() + text.dropFirst())” → \(path(category, in: state))"
    }

    static func saveTitle(_ state: CategoryMapUiState) -> String {
        if state.duplicateTerm != nil, let category = state.formCategory {
            return String(localized: "Move to \(CategoryCopy.name(category))", bundle: .appLanguage)
        }
        return String(localized: "Save", bundle: .appLanguage)
    }

    static func option(_ category: Shared.Category, in state: CategoryMapUiState) -> String {
        let name = CategoryCopy.name(category)
        guard let group = state.groupNameOf(category: category) else { return name }
        return "\(name) · \(String(localized: "only in \(group)", bundle: .appLanguage))"
    }

    static func termLabel(_ term: MapTerm, path: String) -> String {
        let label = String(localized: "\(term.term), \(path). Edit", bundle: .appLanguage)
        return term.isPending ? "\(label), \(String(localized: "queued", bundle: .appLanguage))" : label
    }
}
