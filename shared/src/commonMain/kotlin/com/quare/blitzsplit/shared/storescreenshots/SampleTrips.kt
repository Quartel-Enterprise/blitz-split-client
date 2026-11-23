package com.quare.blitzsplit.shared.storescreenshots

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillItem
import com.quare.blitzsplit.core.expenses.domain.model.BillItems
import com.quare.blitzsplit.core.expenses.domain.model.BillItemsSplit
import com.quare.blitzsplit.core.expenses.domain.model.ItemSplitMode
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.math.roundToLong
import kotlin.native.HiddenFromObjC

/*
 * Why: one trip per language of the listing, its amounts priced in that language's currency at a round rate and
 * rounded to what a menu there shows, so no screenshot reads "¥2,397" or "Rp 255.873". The prices are in US cents.
 */
@OptIn(ExperimentalObjCRefinement::class)
@HiddenFromObjC
object SampleTrips {
    // Why: what both stores draw the people and the other groups in, and the made-up invite code their QR codes hold.
    val avatarColors: List<AvatarColor> =
        listOf(AvatarColor.BLUE, AvatarColor.PEACH, AvatarColor.GREEN, AvatarColor.LILAC, AvatarColor.SAND)
    val sideGroupColors: List<AvatarColor> = listOf(AvatarColor.BLUE, AvatarColor.GREEN)
    const val INVITE_CODE = "Lx7Kp2Qa"

    // Why: the person this device's user owes on the trip, and the made-up key "Pagar Carla" shows in reais.
    const val CREDITOR = 2
    const val PIX_KEY = "7d9f0335-8dcc-4054-9bf9-0dbd61d36906"

    /*
     * Why: the three groups' photos, files of scripts/store-screenshots/bank/groups without their extension. The App
     * Store's captures hand the app the same three files, listed in scripts/store-screenshots/assets.tsv since
     * neither the iOS UI test nor capture-mac.sh can read these names. The seed stops on one that list lacks.
     */
    const val TRIP_PHOTO = "trip-lisbon-tram"
    private const val ROOMMATES_PHOTO = "home-kitchen"
    private const val SOCCER_PHOTO = "event-soccer"
    private val currencies = mapOf(
        "en" to GroupCurrency.USD,
        "pt" to GroupCurrency.BRL,
        "pt-PT" to GroupCurrency.EUR,
        "es" to GroupCurrency.EUR,
        "es-ES" to GroupCurrency.EUR,
        "fr" to GroupCurrency.EUR,
        "de" to GroupCurrency.EUR,
        "it" to GroupCurrency.EUR,
        "pl" to GroupCurrency.PLN,
        "nl" to GroupCurrency.EUR,
        "tr" to GroupCurrency.TRY,
        "id" to GroupCurrency.IDR,
        "ru" to GroupCurrency.RUB,
        "uk" to GroupCurrency.UAH,
        "ja" to GroupCurrency.JPY,
        "ko" to GroupCurrency.KRW,
        "hi" to GroupCurrency.INR,
        "ar" to GroupCurrency.SAR,
        "zh" to GroupCurrency.CNY,
        "he" to GroupCurrency.ILS,
        "th" to GroupCurrency.THB,
        "vi" to GroupCurrency.VND,
        "zh-Hant" to GroupCurrency.TWD,
        "sv" to GroupCurrency.SEK,
        "cs" to GroupCurrency.CZK,
        "ro" to GroupCurrency.RON,
        "bn" to GroupCurrency.BDT,
        "ms" to GroupCurrency.MYR,
        "el" to GroupCurrency.EUR,
        "hr" to GroupCurrency.EUR,
        "hu" to GroupCurrency.HUF,
        "ur" to GroupCurrency.PKR,
        "fil" to GroupCurrency.PHP,
        "sr" to GroupCurrency.RSD,
        "sk" to GroupCurrency.EUR,
        "fa" to GroupCurrency.IRR,
        "ta" to GroupCurrency.INR,
        "nb" to GroupCurrency.NOK,
        "te" to GroupCurrency.INR,
        "bg" to GroupCurrency.EUR,
        "mr" to GroupCurrency.INR,
        "ca" to GroupCurrency.EUR,
        "da" to GroupCurrency.DKK,
        "fi" to GroupCurrency.EUR,
        "sw" to GroupCurrency.KES,
    )

    fun avatarColorOf(person: Int): AvatarColor = avatarColors[person % avatarColors.size]

    fun tripOf(language: String): SampleTrip {
        val copy = SampleTripCopies.byLanguage[language] ?: SampleTripCopies.byLanguage.getValue("en")
        val currency = currencies[language] ?: GroupCurrency.USD
        val foreignCurrency = if (currency == GroupCurrency.EUR) GroupCurrency.USD else GroupCurrency.EUR
        val price = { usdCents: Long -> priceOf(usdCents, currency) }
        val dinnerItems = dinnerItemsOf(copy.items, price)
        val dinnerTotal = BillItemsSplit.breakdownOf(dinnerItems, listOf(ME)).totalMinor
        return SampleTrip(
            groupName = copy.groupName,
            currency = currency,
            foreignCurrency = foreignCurrency,
            people = copy.people,
            bills = listOf(
                createEqualBill(
                    copy.taxi,
                    price(TAXI_CENTS),
                    currency,
                    payer = 4,
                    AppCategory.TRANSPORT_RIDE,
                    daysAgo = 4,
                    10 * 60,
                ),
                createEqualBill(
                    copy.apartment,
                    price(APARTMENT_CENTS),
                    currency,
                    payer = 2,
                    AppCategory.TRAVEL_LODGING,
                    daysAgo = 4,
                    14 * 60,
                ),
                createEqualBill(
                    copy.carRental,
                    price(CAR_CENTS),
                    currency,
                    payer = 0,
                    AppCategory.TRAVEL_CAR_RENTAL,
                    daysAgo = 3,
                    9 * 60,
                ),
                createEqualBill(
                    copy.groceries,
                    price(GROCERIES_CENTS),
                    currency,
                    payer = 3,
                    AppCategory.FOOD_MARKET,
                    daysAgo = 3,
                    18 * 60 + 30,
                ),
                SampleBill(
                    description = copy.boatTour,
                    totalMinor = BOAT_TOUR_CENTS,
                    currency = foreignCurrency,
                    payers = listOf(SamplePayer(1, BOAT_TOUR_CENTS / 2), SamplePayer(3, BOAT_TOUR_CENTS / 2)),
                    categoryKey = AppCategory.TRAVEL_TOURS.key,
                    daysAgo = 1,
                    minuteOfDay = 11 * 60,
                    items = null,
                ),
                SampleBill(
                    description = copy.dinner,
                    totalMinor = dinnerTotal,
                    currency = currency,
                    payers = listOf(SamplePayer(0, dinnerTotal)),
                    categoryKey = AppCategory.FOOD_RESTAURANT.key,
                    daysAgo = 1,
                    minuteOfDay = 21 * 60,
                    items = dinnerItems,
                ),
            ),
            payment = SamplePayment(
                fromPerson = 1,
                toPerson = 0,
                amountMinor = price(PAYMENT_CENTS),
                method = if (currency == GroupCurrency.BRL) SettlementMethod.PIX else SettlementMethod.TRANSFER,
                daysAgo = 0,
                minuteOfDay = 10 * 60,
            ),
            sideGroups = listOf(
                SampleSideGroup(
                    name = copy.roommates,
                    type = GroupType.HOME,
                    photo = ROOMMATES_PHOTO,
                    people = listOf(0, 2, 3),
                    billDescription = copy.groceries,
                    billTotalMinor = price(ROOMMATES_GROCERIES_CENTS),
                    billPayer = 0,
                    categoryKey = AppCategory.FOOD_MARKET.key,
                    daysAgo = 6,
                ),
                SampleSideGroup(
                    name = copy.soccer,
                    type = GroupType.EVENT,
                    photo = SOCCER_PHOTO,
                    people = listOf(0, 1, 3, 4),
                    billDescription = copy.court,
                    billTotalMinor = price(COURT_CENTS),
                    billPayer = 1,
                    categoryKey = AppCategory.LEISURE_SPORTS.key,
                    daysAgo = 8,
                ),
            ),
        )
    }

    private fun createEqualBill(
        description: String,
        totalMinor: Long,
        currency: GroupCurrency,
        payer: Int,
        category: AppCategory,
        daysAgo: Int,
        minuteOfDay: Int,
    ): SampleBill = SampleBill(
        description = description,
        totalMinor = totalMinor,
        currency = currency,
        payers = listOf(SamplePayer(payer, totalMinor)),
        categoryKey = category.key,
        daysAgo = daysAgo,
        minuteOfDay = minuteOfDay,
        items = null,
    )

    // Why: who ate what at the trattoria; the ids are people's indexes, which the seed maps to the members' ids.
    private fun dinnerItemsOf(
        names: List<String>,
        price: (Long) -> Long,
    ): BillItems {
        val eaters = listOf(
            listOf(1, 3),
            listOf(0, 2),
            listOf(0, 4),
            listOf(0, 1, 2),
            listOf(0, 1, 2, 3, 4),
            listOf(1, 2, 3, 4),
        )
        val quantities = listOf(2L, 1L, 2L, 3L, 1L, 4L)
        val unitCents = listOf(1_600L, 1_400L, 1_900L, 800L, 3_600L, 350L)
        return BillItems(
            items = names.indices.map { index ->
                BillItem(
                    id = "item-$index",
                    name = names[index],
                    quantity = quantities[index],
                    unitPriceMinor = price(unitCents[index]),
                    mode = ItemSplitMode.EQUAL,
                    entries = eaters[index].map {
                        SplitEntry(
                            memberId = it.toString(),
                            value = 1,
                        )
                    },
                )
            },
            servicePercent = SERVICE_PERCENT,
            couvertMinor = 0,
            couvertMemberIds = emptyList(),
            extraMinor = 0,
            isFromReceipt = true,
        )
    }

    private fun priceOf(
        usdCents: Long,
        currency: GroupCurrency,
    ): Long {
        val (rate, step) = when (currency) {
            GroupCurrency.BRL -> 5.0 to 10L
            GroupCurrency.PLN -> 4.0 to 50L
            GroupCurrency.TRY -> 40.0 to 500L
            GroupCurrency.IDR -> 16_000.0 to 1_000L
            GroupCurrency.RUB -> 90.0 to 1_000L
            GroupCurrency.UAH -> 40.0 to 500L
            GroupCurrency.JPY -> 150.0 to 10L
            GroupCurrency.KRW -> 1_400.0 to 1_000L
            GroupCurrency.INR -> 85.0 to 1_000L
            GroupCurrency.SAR -> 3.75 to 100L
            GroupCurrency.CNY -> 7.0 to 100L
            GroupCurrency.ILS -> 3.7 to 100L
            GroupCurrency.THB -> 35.0 to 100L
            GroupCurrency.VND -> 25_000.0 to 1_000L
            GroupCurrency.TWD -> 32.0 to 10L
            GroupCurrency.SEK -> 10.0 to 100L
            GroupCurrency.CZK -> 23.0 to 100L
            GroupCurrency.RON -> 4.6 to 50L
            GroupCurrency.BDT -> 120.0 to 1_000L
            GroupCurrency.MYR -> 4.5 to 50L
            GroupCurrency.HUF -> 360.0 to 100L
            GroupCurrency.PKR -> 280.0 to 1_000L
            GroupCurrency.PHP -> 56.0 to 500L
            GroupCurrency.RSD -> 117.0 to 1_000L
            GroupCurrency.IRR -> 1_000_000.0 to 100_000L
            GroupCurrency.NOK -> 11.0 to 100L
            GroupCurrency.KES -> 129.0 to 1_000L
            GroupCurrency.DKK -> 7.0 to 100L
            else -> 1.0 to 50L
        }
        val minor = usdCents / CENTS_PER_DOLLAR * rate * currency.minorPerMajor
        return (minor / step).roundToLong() * step
    }

    const val ME = "0"
    private const val CENTS_PER_DOLLAR = 100.0
    private const val SERVICE_PERCENT = 10L
    private const val TAXI_CENTS = 3_800L
    private const val APARTMENT_CENTS = 64_000L
    private const val CAR_CENTS = 21_000L
    private const val GROCERIES_CENTS = 8_420L
    private const val BOAT_TOUR_CENTS = 12_000L
    private const val PAYMENT_CENTS = 6_000L
    private const val ROOMMATES_GROCERIES_CENTS = 9_600L
    private const val COURT_CENTS = 8_000L
}
