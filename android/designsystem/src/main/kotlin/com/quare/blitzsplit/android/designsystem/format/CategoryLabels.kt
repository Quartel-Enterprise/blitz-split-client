package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon

// Why: what a category is called: the app's in the device's language, the group's as someone named it.
@Composable
fun categoryName(category: Category): String = category.app?.let { appCategoryName(it) } ?: category.name.orEmpty()

// Why: "Alimentação › Mercado", or just "Alimentação" for a main category.
@Composable
fun categoryPath(
    category: Category,
    parent: Category?,
): String = if (parent == null) {
    categoryName(category)
} else {
    stringResource(R.string.designsystem_category_path, categoryName(parent), categoryName(category))
}

@Composable
fun appCategoryName(category: AppCategory): String = stringResource(
    when (category) {
        AppCategory.FOOD -> R.string.designsystem_category_food
        AppCategory.FOOD_RESTAURANT -> R.string.designsystem_category_food_restaurant
        AppCategory.FOOD_MARKET -> R.string.designsystem_category_food_market
        AppCategory.FOOD_BAR -> R.string.designsystem_category_food_bar
        AppCategory.FOOD_DELIVERY -> R.string.designsystem_category_food_delivery
        AppCategory.FOOD_BAKERY -> R.string.designsystem_category_food_bakery
        AppCategory.HOME -> R.string.designsystem_category_home
        AppCategory.HOME_RENT -> R.string.designsystem_category_home_rent
        AppCategory.HOME_CONDO -> R.string.designsystem_category_home_condo
        AppCategory.HOME_POWER -> R.string.designsystem_category_home_power
        AppCategory.HOME_WATER -> R.string.designsystem_category_home_water
        AppCategory.HOME_GAS -> R.string.designsystem_category_home_gas
        AppCategory.HOME_INTERNET -> R.string.designsystem_category_home_internet
        AppCategory.HOME_CLEANING -> R.string.designsystem_category_home_cleaning
        AppCategory.TRANSPORT -> R.string.designsystem_category_transport
        AppCategory.TRANSPORT_FUEL -> R.string.designsystem_category_transport_fuel
        AppCategory.TRANSPORT_RIDE -> R.string.designsystem_category_transport_ride
        AppCategory.TRANSPORT_PARKING -> R.string.designsystem_category_transport_parking
        AppCategory.TRANSPORT_TOLL -> R.string.designsystem_category_transport_toll
        AppCategory.TRANSPORT_TRANSIT -> R.string.designsystem_category_transport_transit
        AppCategory.TRAVEL -> R.string.designsystem_category_travel
        AppCategory.TRAVEL_LODGING -> R.string.designsystem_category_travel_lodging
        AppCategory.TRAVEL_FLIGHT -> R.string.designsystem_category_travel_flight
        AppCategory.TRAVEL_TOURS -> R.string.designsystem_category_travel_tours
        AppCategory.TRAVEL_CAR_RENTAL -> R.string.designsystem_category_travel_car_rental
        AppCategory.LEISURE -> R.string.designsystem_category_leisure
        AppCategory.LEISURE_TICKETS -> R.string.designsystem_category_leisure_tickets
        AppCategory.LEISURE_PARTIES -> R.string.designsystem_category_leisure_parties
        AppCategory.LEISURE_SPORTS -> R.string.designsystem_category_leisure_sports
        AppCategory.LEISURE_GAMES -> R.string.designsystem_category_leisure_games
        AppCategory.SUBSCRIPTIONS -> R.string.designsystem_category_subscriptions
        AppCategory.SUBSCRIPTIONS_STREAMING -> R.string.designsystem_category_subscriptions_streaming
        AppCategory.SUBSCRIPTIONS_MUSIC -> R.string.designsystem_category_subscriptions_music
        AppCategory.SUBSCRIPTIONS_APPS -> R.string.designsystem_category_subscriptions_apps
        AppCategory.SHOPPING -> R.string.designsystem_category_shopping
        AppCategory.SHOPPING_CLOTHES -> R.string.designsystem_category_shopping_clothes
        AppCategory.SHOPPING_GIFTS -> R.string.designsystem_category_shopping_gifts
        AppCategory.SHOPPING_ELECTRONICS -> R.string.designsystem_category_shopping_electronics
        AppCategory.SHOPPING_HOME -> R.string.designsystem_category_shopping_home
        AppCategory.HEALTH -> R.string.designsystem_category_health
        AppCategory.HEALTH_PHARMACY -> R.string.designsystem_category_health_pharmacy
        AppCategory.HEALTH_APPOINTMENTS -> R.string.designsystem_category_health_appointments
        AppCategory.HEALTH_GYM -> R.string.designsystem_category_health_gym
        AppCategory.EDUCATION -> R.string.designsystem_category_education
        AppCategory.EDUCATION_COURSES -> R.string.designsystem_category_education_courses
        AppCategory.EDUCATION_BOOKS -> R.string.designsystem_category_education_books
        AppCategory.EDUCATION_SUPPLIES -> R.string.designsystem_category_education_supplies
        AppCategory.OTHER -> R.string.designsystem_category_other
    },
)

// Why: "Talheres", "Pata": what TalkBack says for each icon of "Nova categoria".
@Composable
fun categoryIconLabel(icon: CategoryIcon): String = stringResource(
    when (icon) {
        CategoryIcon.FOOD -> R.string.designsystem_category_icon_food
        CategoryIcon.CART -> R.string.designsystem_category_icon_cart
        CategoryIcon.HOME -> R.string.designsystem_category_icon_home
        CategoryIcon.CAR -> R.string.designsystem_category_icon_car
        CategoryIcon.PLANE -> R.string.designsystem_category_icon_plane
        CategoryIcon.TICKET -> R.string.designsystem_category_icon_ticket
        CategoryIcon.REPEAT -> R.string.designsystem_category_icon_repeat
        CategoryIcon.BAG -> R.string.designsystem_category_icon_bag
        CategoryIcon.HEART -> R.string.designsystem_category_icon_heart
        CategoryIcon.BOOK -> R.string.designsystem_category_icon_book
        CategoryIcon.SUN -> R.string.designsystem_category_icon_sun
        CategoryIcon.BALL -> R.string.designsystem_category_icon_ball
        CategoryIcon.PAW -> R.string.designsystem_category_icon_paw
        CategoryIcon.GIFT -> R.string.designsystem_category_icon_gift
        CategoryIcon.GLASS -> R.string.designsystem_category_icon_glass
        CategoryIcon.MUSIC -> R.string.designsystem_category_icon_music
        CategoryIcon.GYM -> R.string.designsystem_category_icon_gym
        CategoryIcon.DOTS -> R.string.designsystem_category_icon_dots
    },
)

val CategoryIcon.imageVector: ImageVector
    get() = when (this) {
        CategoryIcon.FOOD -> Icons.Outlined.Restaurant
        CategoryIcon.CART -> Icons.Outlined.ShoppingCart
        CategoryIcon.HOME -> Icons.Outlined.Home
        CategoryIcon.CAR -> Icons.Outlined.DirectionsCar
        CategoryIcon.PLANE -> Icons.Outlined.Flight
        CategoryIcon.TICKET -> Icons.Outlined.ConfirmationNumber
        CategoryIcon.REPEAT -> Icons.Outlined.Autorenew
        CategoryIcon.BAG -> Icons.Outlined.ShoppingBag
        CategoryIcon.HEART -> Icons.Outlined.FavoriteBorder
        CategoryIcon.BOOK -> Icons.AutoMirrored.Outlined.MenuBook
        CategoryIcon.SUN -> Icons.Outlined.WbSunny
        CategoryIcon.BALL -> Icons.Outlined.SportsSoccer
        CategoryIcon.PAW -> Icons.Outlined.Pets
        CategoryIcon.GIFT -> Icons.Outlined.CardGiftcard
        CategoryIcon.GLASS -> Icons.Outlined.LocalBar
        CategoryIcon.MUSIC -> Icons.Outlined.MusicNote
        CategoryIcon.GYM -> Icons.Outlined.FitnessCenter
        CategoryIcon.DOTS -> Icons.Outlined.MoreHoriz
    }
