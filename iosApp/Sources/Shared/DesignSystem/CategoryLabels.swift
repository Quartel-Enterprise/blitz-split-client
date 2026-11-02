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
        case .food: String(localized: "Food", comment: "An app category")
        case .foodRestaurant: String(localized: "Restaurant", comment: "An app category")
        case .foodMarket: String(localized: "Groceries", comment: "An app category")
        case .foodBar: String(localized: "Bar", comment: "An app category")
        case .foodDelivery: String(localized: "Delivery", comment: "An app category")
        case .foodBakery: String(localized: "Bakery and coffee", comment: "An app category")
        case .home: String(localized: "Home", comment: "An app category")
        case .homeRent: String(localized: "Rent", comment: "An app category")
        case .homeCondo: String(localized: "Condo fees", comment: "An app category")
        case .homePower: String(localized: "Electricity", comment: "An app category")
        case .homeWater: String(localized: "Water", comment: "An app category")
        case .homeGas: String(localized: "Gas", comment: "An app category")
        case .homeInternet: String(localized: "Internet", comment: "An app category")
        case .homeCleaning: String(localized: "Cleaning", comment: "An app category")
        case .transport: String(localized: "Transport", comment: "An app category")
        case .transportFuel: String(localized: "Fuel", comment: "An app category")
        case .transportRide: String(localized: "Ride-hailing", comment: "An app category")
        case .transportParking: String(localized: "Parking", comment: "An app category")
        case .transportToll: String(localized: "Tolls", comment: "An app category")
        case .transportTransit: String(localized: "Bus and subway", comment: "An app category")
        case .travel: String(localized: "Travel", comment: "An app category")
        case .travelLodging: String(localized: "Lodging", comment: "An app category")
        case .travelFlight: String(localized: "Tickets and flights", comment: "An app category")
        case .travelTours: String(localized: "Tours", comment: "An app category")
        case .travelCarRental: String(localized: "Car rental", comment: "An app category")
        case .leisure: String(localized: "Leisure", comment: "An app category")
        case .leisureTickets: String(localized: "Event tickets", comment: "An app category")
        case .leisureParties: String(localized: "Parties", comment: "An app category")
        case .leisureSports: String(localized: "Sports", comment: "An app category")
        case .leisureGames: String(localized: "Games", comment: "An app category")
        case .subscriptions: String(localized: "Subscriptions", comment: "An app category")
        case .subscriptionsStreaming: String(localized: "Streaming", comment: "An app category")
        case .subscriptionsMusic: String(localized: "Music", comment: "An app category")
        case .subscriptionsApps: String(localized: "Apps", comment: "An app category")
        case .shopping: String(localized: "Shopping", comment: "An app category")
        case .shoppingClothes: String(localized: "Clothes", comment: "An app category")
        case .shoppingGifts: String(localized: "Gifts", comment: "An app category")
        case .shoppingElectronics: String(localized: "Electronics", comment: "An app category")
        case .shoppingHome: String(localized: "Home and decor", comment: "An app category")
        case .health: String(localized: "Health", comment: "An app category")
        case .healthPharmacy: String(localized: "Pharmacy", comment: "An app category")
        case .healthAppointments: String(localized: "Appointments", comment: "An app category")
        case .healthGym: String(localized: "Gym", comment: "An app category")
        case .education: String(localized: "Education", comment: "An app category")
        case .educationCourses: String(localized: "Courses", comment: "An app category")
        case .educationBooks: String(localized: "Books", comment: "An app category")
        case .educationSupplies: String(localized: "Supplies", comment: "An app category")
        default: String(localized: "Other (category)", defaultValue: "Other", comment: "An app category")
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
        case .food: String(localized: "Cutlery", comment: "An icon of a category")
        case .cart: String(localized: "Cart", comment: "An icon of a category")
        case .home: String(localized: "House", comment: "An icon of a category")
        case .car: String(localized: "Car", comment: "An icon of a category")
        case .plane: String(localized: "Plane", comment: "An icon of a category")
        case .ticket: String(localized: "Ticket", comment: "An icon of a category")
        case .repeat: String(localized: "Subscription", comment: "An icon of a category")
        case .bag: String(localized: "Bag", comment: "An icon of a category")
        case .heart: String(localized: "Heart", comment: "An icon of a category")
        case .book: String(localized: "Book", comment: "An icon of a category")
        case .sun: String(localized: "Sun", comment: "An icon of a category")
        case .ball: String(localized: "Ball", comment: "An icon of a category")
        case .paw: String(localized: "Paw", comment: "An icon of a category")
        case .gift: String(localized: "Gift", comment: "An icon of a category")
        case .glass: String(localized: "Glass", comment: "An icon of a category")
        case .music: String(localized: "Music", comment: "An icon of a category")
        case .gym: String(localized: "Dumbbell", comment: "An icon of a category")
        default: String(localized: "Other (icon)", defaultValue: "Other", comment: "An icon of a category")
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
