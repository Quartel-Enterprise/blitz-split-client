package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: the app's fixed categories (7.7), two levels, in the order "Todas as categorias" lists them. The key is what a
 * bill, a default split's exception and a term of the map store, so it never changes even if an entry is renamed. Each
 * UI names them; a subcategory without its own icon takes its parent's.
 */
enum class AppCategory(
    val key: String,
    private val ownIcon: CategoryIcon?,
    val parentKey: String?,
) {
    FOOD("food", CategoryIcon.FOOD, null),
    FOOD_RESTAURANT("food.restaurant", null, "food"),
    FOOD_MARKET("food.market", CategoryIcon.CART, "food"),
    FOOD_BAR("food.bar", CategoryIcon.GLASS, "food"),
    FOOD_DELIVERY("food.delivery", CategoryIcon.BAG, "food"),
    FOOD_BAKERY("food.bakery", null, "food"),
    HOME("home", CategoryIcon.HOME, null),
    HOME_RENT("home.rent", null, "home"),
    HOME_CONDO("home.condo", null, "home"),
    HOME_POWER("home.power", null, "home"),
    HOME_WATER("home.water", null, "home"),
    HOME_GAS("home.gas", null, "home"),
    HOME_INTERNET("home.internet", null, "home"),
    HOME_CLEANING("home.cleaning", null, "home"),
    TRANSPORT("transport", CategoryIcon.CAR, null),
    TRANSPORT_FUEL("transport.fuel", null, "transport"),
    TRANSPORT_RIDE("transport.ride", null, "transport"),
    TRANSPORT_PARKING("transport.parking", null, "transport"),
    TRANSPORT_TOLL("transport.toll", null, "transport"),
    TRANSPORT_TRANSIT("transport.transit", null, "transport"),
    TRAVEL("travel", CategoryIcon.PLANE, null),
    TRAVEL_LODGING("travel.lodging", CategoryIcon.HOME, "travel"),
    TRAVEL_FLIGHT("travel.flight", null, "travel"),
    TRAVEL_TOURS("travel.tours", CategoryIcon.SUN, "travel"),
    TRAVEL_CAR_RENTAL("travel.car_rental", CategoryIcon.CAR, "travel"),
    LEISURE("leisure", CategoryIcon.TICKET, null),
    LEISURE_TICKETS("leisure.tickets", null, "leisure"),
    LEISURE_PARTIES("leisure.parties", CategoryIcon.MUSIC, "leisure"),
    LEISURE_SPORTS("leisure.sports", CategoryIcon.BALL, "leisure"),
    LEISURE_GAMES("leisure.games", null, "leisure"),
    SUBSCRIPTIONS("subscriptions", CategoryIcon.REPEAT, null),
    SUBSCRIPTIONS_STREAMING("subscriptions.streaming", null, "subscriptions"),
    SUBSCRIPTIONS_MUSIC("subscriptions.music", CategoryIcon.MUSIC, "subscriptions"),
    SUBSCRIPTIONS_APPS("subscriptions.apps", null, "subscriptions"),
    SHOPPING("shopping", CategoryIcon.BAG, null),
    SHOPPING_CLOTHES("shopping.clothes", null, "shopping"),
    SHOPPING_GIFTS("shopping.gifts", CategoryIcon.GIFT, "shopping"),
    SHOPPING_ELECTRONICS("shopping.electronics", null, "shopping"),
    SHOPPING_HOME("shopping.home", CategoryIcon.HOME, "shopping"),
    HEALTH("health", CategoryIcon.HEART, null),
    HEALTH_PHARMACY("health.pharmacy", null, "health"),
    HEALTH_APPOINTMENTS("health.appointments", null, "health"),
    HEALTH_GYM("health.gym", CategoryIcon.GYM, "health"),
    EDUCATION("education", CategoryIcon.BOOK, null),
    EDUCATION_COURSES("education.courses", null, "education"),
    EDUCATION_BOOKS("education.books", null, "education"),
    EDUCATION_SUPPLIES("education.supplies", null, "education"),
    OTHER("other", CategoryIcon.DOTS, null),
    ;

    val isMain: Boolean
        get() = parentKey == null

    val parent: AppCategory?
        get() = parentKey?.let(::fromKeyOrNull)

    val icon: CategoryIcon
        get() = ownIcon ?: parent?.icon ?: CategoryIcon.DOTS

    /*
     * Why: each main category keeps one color of the avatar palette, in the pie of Estatísticas and in its row (6.6):
     * Alimentação blue, Lazer peach, Transporte green, Casa lilac, Assinaturas sand, the others cycling after them.
     */
    val color: AvatarColor
        get() = when (parent ?: this) {
            FOOD, SHOPPING -> AvatarColor.BLUE
            LEISURE, TRAVEL -> AvatarColor.PEACH
            TRANSPORT, HEALTH -> AvatarColor.GREEN
            HOME, EDUCATION -> AvatarColor.LILAC
            else -> AvatarColor.SAND
        }

    companion object {
        fun fromKeyOrNull(key: String?): AppCategory? = entries.firstOrNull { it.key == key }

        val mains: List<AppCategory>
            get() = entries.filter { it.isMain }
    }
}
