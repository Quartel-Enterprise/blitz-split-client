import Shared
import SwiftUI

/// What the app's categories and their icons are called, and how they look: the shared code hands over the category
/// and each platform names it. A group's own category is named by whoever created it.
enum CategoryCopy {
    static func name(_ category: Shared.Category) -> String {
        guard let app = category.app else { return category.name ?? "" }
        return name(app)
    }

    /// "Alimentação › Mercado", or just the name of a main category.
    static func path(_ category: Shared.Category, parent: Shared.Category?) -> String {
        guard let parent else { return name(category) }
        return "\(name(parent)) › \(name(category))"
    }

    // swiftlint:disable:next cyclomatic_complexity function_body_length
    static func name(_ category: AppCategory) -> String {
        switch category {
        case .food: String(localized: "Food", bundle: .appLanguage, comment: "An app category")
        case .foodRestaurant: String(localized: "Restaurant", bundle: .appLanguage, comment: "An app category")
        case .foodMarket: String(localized: "Groceries", bundle: .appLanguage, comment: "An app category")
        case .foodBar: String(localized: "Bar", bundle: .appLanguage, comment: "An app category")
        case .foodDelivery: String(localized: "Delivery", bundle: .appLanguage, comment: "An app category")
        case .foodBakery: String(localized: "Bakery and coffee", bundle: .appLanguage, comment: "An app category")
        case .home: String(localized: "Home", bundle: .appLanguage, comment: "An app category")
        case .homeRent: String(localized: "Rent", bundle: .appLanguage, comment: "An app category")
        case .homeCondo: String(localized: "Condo fees", bundle: .appLanguage, comment: "An app category")
        case .homePower: String(localized: "Electricity", bundle: .appLanguage, comment: "An app category")
        case .homeWater: String(localized: "Water", bundle: .appLanguage, comment: "An app category")
        case .homeGas: String(localized: "Gas", bundle: .appLanguage, comment: "An app category")
        case .homeInternet: String(localized: "Internet", bundle: .appLanguage, comment: "An app category")
        case .homeCleaning: String(localized: "Cleaning", bundle: .appLanguage, comment: "An app category")
        case .transport: String(localized: "Transport", bundle: .appLanguage, comment: "An app category")
        case .transportFuel: String(localized: "Fuel", bundle: .appLanguage, comment: "An app category")
        case .transportRide: String(localized: "Ride-hailing", bundle: .appLanguage, comment: "An app category")
        case .transportParking: String(localized: "Parking", bundle: .appLanguage, comment: "An app category")
        case .transportToll: String(localized: "Tolls", bundle: .appLanguage, comment: "An app category")
        case .transportTransit: String(localized: "Bus and subway", bundle: .appLanguage, comment: "An app category")
        case .travel: String(localized: "Travel", bundle: .appLanguage, comment: "An app category")
        case .travelLodging: String(localized: "Lodging", bundle: .appLanguage, comment: "An app category")
        case .travelFlight: String(localized: "Tickets and flights", bundle: .appLanguage, comment: "An app category")
        case .travelTours: String(localized: "Tours", bundle: .appLanguage, comment: "An app category")
        case .travelCarRental: String(localized: "Car rental", bundle: .appLanguage, comment: "An app category")
        case .leisure: String(localized: "Leisure", bundle: .appLanguage, comment: "An app category")
        case .leisureTickets: String(localized: "Event tickets", bundle: .appLanguage, comment: "An app category")
        case .leisureParties: String(localized: "Parties", bundle: .appLanguage, comment: "An app category")
        case .leisureSports: String(localized: "Sports", bundle: .appLanguage, comment: "An app category")
        case .leisureGames: String(localized: "Games", bundle: .appLanguage, comment: "An app category")
        case .subscriptions: String(localized: "Subscriptions", bundle: .appLanguage, comment: "An app category")
        case .subscriptionsStreaming: String(localized: "Streaming", bundle: .appLanguage, comment: "An app category")
        case .subscriptionsMusic: String(localized: "Music", bundle: .appLanguage, comment: "An app category")
        case .subscriptionsApps: String(localized: "Apps", bundle: .appLanguage, comment: "An app category")
        case .shopping: String(localized: "Shopping", bundle: .appLanguage, comment: "An app category")
        case .shoppingClothes: String(localized: "Clothes", bundle: .appLanguage, comment: "An app category")
        case .shoppingGifts: String(localized: "Gifts", bundle: .appLanguage, comment: "An app category")
        case .shoppingElectronics: String(localized: "Electronics", bundle: .appLanguage, comment: "An app category")
        case .shoppingHome: String(localized: "Home and decor", bundle: .appLanguage, comment: "An app category")
        case .health: String(localized: "Health", bundle: .appLanguage, comment: "An app category")
        case .healthPharmacy: String(localized: "Pharmacy", bundle: .appLanguage, comment: "An app category")
        case .healthAppointments: String(localized: "Appointments", bundle: .appLanguage, comment: "An app category")
        case .healthGym: String(localized: "Gym", bundle: .appLanguage, comment: "An app category")
        case .education: String(localized: "Education", bundle: .appLanguage, comment: "An app category")
        case .educationCourses: String(localized: "Courses", bundle: .appLanguage, comment: "An app category")
        case .educationBooks: String(localized: "Books", bundle: .appLanguage, comment: "An app category")
        case .educationSupplies: String(localized: "Supplies", bundle: .appLanguage, comment: "An app category")
        default: String(
            localized: "Other (category)",
            defaultValue: "Other",
            bundle: .appLanguage,
            comment: "An app category",
        )
        }
    }

    /// The SF Symbol of each CategoryIcon, the same pictures the other platforms draw.
    static func symbol(_ icon: CategoryIcon) -> String {
        switch icon {
        case .food: "fork.knife"
        case .cart: "cart"
        case .home: "house"
        case .car: "car"
        case .plane: "airplane"
        case .ticket: "ticket"
        case .repeat: "arrow.triangle.2.circlepath"
        case .bag: "bag"
        case .heart: "heart"
        case .book: "book"
        case .sun: "sun.max"
        case .ball: "soccerball"
        case .paw: "pawprint"
        case .gift: "gift"
        case .glass: "wineglass"
        case .music: "music.note"
        case .gym: "dumbbell"
        default: "ellipsis"
        }
    }

    /// "Talheres", "Pata": what VoiceOver says for each icon of "New category".
    static func iconLabel(_ icon: CategoryIcon) -> String {
        switch icon {
        case .food: String(localized: "Cutlery", bundle: .appLanguage, comment: "An icon of a category")
        case .cart: String(localized: "Cart", bundle: .appLanguage, comment: "An icon of a category")
        case .home: String(localized: "House", bundle: .appLanguage, comment: "An icon of a category")
        case .car: String(localized: "Car", bundle: .appLanguage, comment: "An icon of a category")
        case .plane: String(localized: "Plane", bundle: .appLanguage, comment: "An icon of a category")
        case .ticket: String(localized: "Ticket", bundle: .appLanguage, comment: "An icon of a category")
        case .repeat: String(localized: "Subscription", bundle: .appLanguage, comment: "An icon of a category")
        case .bag: String(localized: "Bag", bundle: .appLanguage, comment: "An icon of a category")
        case .heart: String(localized: "Heart", bundle: .appLanguage, comment: "An icon of a category")
        case .book: String(localized: "Book", bundle: .appLanguage, comment: "An icon of a category")
        case .sun: String(localized: "Sun", bundle: .appLanguage, comment: "An icon of a category")
        case .ball: String(localized: "Ball", bundle: .appLanguage, comment: "An icon of a category")
        case .paw: String(localized: "Paw", bundle: .appLanguage, comment: "An icon of a category")
        case .gift: String(localized: "Gift", bundle: .appLanguage, comment: "An icon of a category")
        case .glass: String(localized: "Glass", bundle: .appLanguage, comment: "An icon of a category")
        case .music: String(localized: "Music", bundle: .appLanguage, comment: "An icon of a category")
        case .gym: String(localized: "Dumbbell", bundle: .appLanguage, comment: "An icon of a category")
        default: String(
            localized: "Other (icon)",
            defaultValue: "Other",
            bundle: .appLanguage,
            comment: "An icon of a category",
        )
        }
    }
}

/// A category's icon on its main category's color, the same in a bill's row, the picker and the pie.
struct CategoryBadge: View {
    let category: Shared.Category
    var size: CGFloat = 40

    var body: some View {
        Image(systemName: CategoryCopy.symbol(category.icon))
            .font(.system(size: size * 0.45, weight: .medium))
            .foregroundStyle(category.color.content)
            .frame(width: size, height: size)
            .background(category.color.container, in: .rect(cornerRadius: size * 0.3))
            .accessibilityHidden(true)
    }
}
