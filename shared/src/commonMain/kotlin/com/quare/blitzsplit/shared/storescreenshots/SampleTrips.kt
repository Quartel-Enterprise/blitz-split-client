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

    /*
     * Why: the three groups' photos, files of scripts/store-screenshots/bank/groups without their extension. The App
     * Store's captures hand the app the same three files, listed in scripts/store-screenshots/assets.tsv since
     * neither the iOS UI test nor capture-mac.sh can read these names. The seed stops on one that list lacks.
     */
    const val TRIP_PHOTO = "trip-lisbon-tram"
    private const val ROOMMATES_PHOTO = "home-kitchen"
    private const val SOCCER_PHOTO = "event-soccer"
    private val italianDishes =
        listOf("Pizza margherita", "Burrata", "Risotto ai funghi", "Tiramisù", "Prosecco", "Espresso")
    private val copies = mapOf(
        "en" to SampleTripCopy(
            groupName = "Lisbon trip",
            people = listOf("Emma", "Liam", "Olivia", "Noah", "Ava"),
            dinner = "Dinner at the trattoria",
            apartment = "Apartment in Alfama",
            groceries = "Groceries",
            carRental = "Car rental",
            boatTour = "Boat tour",
            taxi = "Taxi from the airport",
            roommates = "Roommates",
            soccer = "Friday soccer",
            court = "Court booking",
            items = italianDishes,
        ),
        "pt" to SampleTripCopy(
            groupName = "Viagem a Lisboa",
            people = listOf("Ana", "Bruno", "Carla", "Diego", "Júlia"),
            dinner = "Jantar na trattoria",
            apartment = "Apê em Alfama",
            groceries = "Mercado",
            carRental = "Aluguel do carro",
            boatTour = "Passeio de barco",
            taxi = "Táxi do aeroporto",
            roommates = "República",
            soccer = "Futebol de sexta",
            court = "Aluguel da quadra",
            items = italianDishes,
        ),
        "pt-PT" to SampleTripCopy(
            groupName = "Viagem a Lisboa",
            people = listOf("Inês", "Tiago", "Beatriz", "Diogo", "Mariana"),
            dinner = "Jantar na trattoria",
            apartment = "Apartamento em Alfama",
            groceries = "Supermercado",
            carRental = "Aluguer do carro",
            boatTour = "Passeio de barco",
            taxi = "Táxi do aeroporto",
            roommates = "Colegas de casa",
            soccer = "Futebol à sexta",
            court = "Aluguer do campo",
            items = italianDishes,
        ),
        "es" to SampleTripCopy(
            groupName = "Viaje a Lisboa",
            people = listOf("Lucía", "Mateo", "Sofía", "Diego", "Valentina"),
            dinner = "Cena en la trattoria",
            apartment = "Departamento en Alfama",
            groceries = "Supermercado",
            carRental = "Alquiler de auto",
            boatTour = "Paseo en barco",
            taxi = "Taxi del aeropuerto",
            roommates = "Compañeros de piso",
            soccer = "Fútbol de los viernes",
            court = "Alquiler de la cancha",
            items = italianDishes,
        ),
        "es-ES" to SampleTripCopy(
            groupName = "Viaje a Lisboa",
            people = listOf("Lucía", "Hugo", "Paula", "Álvaro", "Carmen"),
            dinner = "Cena en la trattoria",
            apartment = "Apartamento en Alfama",
            groceries = "Supermercado",
            carRental = "Alquiler del coche",
            boatTour = "Paseo en barco",
            taxi = "Taxi del aeropuerto",
            roommates = "Compañeros de piso",
            soccer = "Fútbol de los viernes",
            court = "Alquiler del campo",
            items = italianDishes,
        ),
        "fr" to SampleTripCopy(
            groupName = "Voyage à Lisbonne",
            people = listOf("Léa", "Hugo", "Chloé", "Louis", "Manon"),
            dinner = "Dîner à la trattoria",
            apartment = "Appart à Alfama",
            groceries = "Courses",
            carRental = "Location de voiture",
            boatTour = "Balade en bateau",
            taxi = "Taxi depuis l'aéroport",
            roommates = "Colocation",
            soccer = "Foot du vendredi",
            court = "Location du terrain",
            items = italianDishes,
        ),
        "de" to SampleTripCopy(
            groupName = "Lissabon-Trip",
            people = listOf("Lena", "Jonas", "Mia", "Felix", "Hannah"),
            dinner = "Abendessen in der Trattoria",
            apartment = "Wohnung in Alfama",
            groceries = "Einkauf",
            carRental = "Mietwagen",
            boatTour = "Bootstour",
            taxi = "Taxi vom Flughafen",
            roommates = "WG",
            soccer = "Freitagskick",
            court = "Platzmiete",
            items = italianDishes,
        ),
        "it" to SampleTripCopy(
            groupName = "Viaggio a Lisbona",
            people = listOf("Giulia", "Luca", "Sofia", "Marco", "Chiara"),
            dinner = "Cena in trattoria",
            apartment = "Appartamento ad Alfama",
            groceries = "Spesa",
            carRental = "Noleggio auto",
            boatTour = "Giro in barca",
            taxi = "Taxi dall'aeroporto",
            roommates = "Coinquilini",
            soccer = "Calcetto del venerdì",
            court = "Affitto del campo",
            items = italianDishes,
        ),
        "pl" to SampleTripCopy(
            groupName = "Wyjazd do Lizbony",
            people = listOf("Zofia", "Jakub", "Maja", "Kacper", "Lena"),
            dinner = "Kolacja w trattorii",
            apartment = "Mieszkanie w Alfamie",
            groceries = "Zakupy",
            carRental = "Wynajem auta",
            boatTour = "Rejs łodzią",
            taxi = "Taksówka z lotniska",
            roommates = "Współlokatorzy",
            soccer = "Piłka w piątek",
            court = "Wynajem boiska",
            items = italianDishes,
        ),
        "nl" to SampleTripCopy(
            groupName = "Trip naar Lissabon",
            people = listOf("Emma", "Daan", "Sanne", "Lars", "Fleur"),
            dinner = "Diner in de trattoria",
            apartment = "Appartement in Alfama",
            groceries = "Boodschappen",
            carRental = "Huurauto",
            boatTour = "Boottocht",
            taxi = "Taxi vanaf het vliegveld",
            roommates = "Huisgenoten",
            soccer = "Vrijdagvoetbal",
            court = "Veldhuur",
            items = italianDishes,
        ),
        "tr" to SampleTripCopy(
            groupName = "Lizbon tatili",
            people = listOf("Elif", "Emre", "Zeynep", "Burak", "Ayşe"),
            dinner = "Trattoria'da akşam yemeği",
            apartment = "Alfama'daki daire",
            groceries = "Market alışverişi",
            carRental = "Araç kiralama",
            boatTour = "Tekne turu",
            taxi = "Havalimanından taksi",
            roommates = "Ev arkadaşları",
            soccer = "Cuma halı sahası",
            court = "Saha kirası",
            items = italianDishes,
        ),
        "id" to SampleTripCopy(
            groupName = "Liburan ke Lisbon",
            people = listOf("Putri", "Budi", "Sari", "Rizky", "Dewi"),
            dinner = "Makan malam di trattoria",
            apartment = "Apartemen di Alfama",
            groceries = "Belanja",
            carRental = "Sewa mobil",
            boatTour = "Tur perahu",
            taxi = "Taksi dari bandara",
            roommates = "Teman kos",
            soccer = "Futsal Jumat",
            court = "Sewa lapangan",
            items = italianDishes,
        ),
        "ru" to SampleTripCopy(
            groupName = "Поездка в Лиссабон",
            people = listOf("Аня", "Миша", "Катя", "Дима", "Оля"),
            dinner = "Ужин в траттории",
            apartment = "Квартира в Алфаме",
            groceries = "Продукты",
            carRental = "Аренда машины",
            boatTour = "Прогулка на лодке",
            taxi = "Такси из аэропорта",
            roommates = "Соседи по квартире",
            soccer = "Футбол по пятницам",
            court = "Аренда поля",
            items = italianDishes,
        ),
        "uk" to SampleTripCopy(
            groupName = "Подорож до Лісабона",
            people = listOf("Оксана", "Тарас", "Соломія", "Богдан", "Ірина"),
            dinner = "Вечеря в траторії",
            apartment = "Квартира в Алфамі",
            groceries = "Продукти",
            carRental = "Оренда авто",
            boatTour = "Прогулянка човном",
            taxi = "Таксі з аеропорту",
            roommates = "Сусіди по квартирі",
            soccer = "Футбол щоп'ятниці",
            court = "Оренда поля",
            items = italianDishes,
        ),
        "ja" to SampleTripCopy(
            groupName = "リスボン旅行",
            people = listOf("葵", "蓮", "陽菜", "大和", "結衣"),
            dinner = "トラットリアで夕食",
            apartment = "アルファマのアパート",
            groceries = "スーパーで買い出し",
            carRental = "レンタカー",
            boatTour = "ボートツアー",
            taxi = "空港からのタクシー",
            roommates = "シェアハウス",
            soccer = "金曜フットサル",
            court = "コート代",
            items = listOf("マルゲリータ", "ブッラータ", "きのこのリゾット", "ティラミス", "プロセッコ", "エスプレッソ"),
        ),
        "ko" to SampleTripCopy(
            groupName = "리스본 여행",
            people = listOf("지민", "서준", "하은", "도윤", "수아"),
            dinner = "트라토리아 저녁",
            apartment = "알파마 숙소",
            groceries = "마트 장보기",
            carRental = "렌터카",
            boatTour = "보트 투어",
            taxi = "공항 택시",
            roommates = "셰어하우스",
            soccer = "금요 풋살",
            court = "구장 대여",
            items = listOf("마르게리타 피자", "부라타", "버섯 리소토", "티라미수", "프로세코", "에스프레소"),
        ),
        "hi" to SampleTripCopy(
            groupName = "लिस्बन ट्रिप",
            people = listOf("आरव", "प्रिया", "रोहन", "अनन्या", "विहान"),
            dinner = "ट्रैटोरिया में डिनर",
            apartment = "अल्फ़ामा का अपार्टमेंट",
            groceries = "किराने का सामान",
            carRental = "किराए की कार",
            boatTour = "बोट टूर",
            taxi = "एयरपोर्ट टैक्सी",
            roommates = "रूममेट्स का घर",
            soccer = "शुक्रवार का फ़ुटबॉल",
            court = "कोर्ट का किराया",
            items = listOf("मार्गेरिटा पिज़्ज़ा", "बुराटा", "मशरूम रिसोट्टो", "तिरामिसु", "प्रोसेको", "एस्प्रेसो"),
        ),
        "ar" to SampleTripCopy(
            groupName = "رحلة لشبونة",
            people = listOf("ليلى", "عمر", "نور", "يوسف", "سارة"),
            dinner = "عشاء في التراتوريا",
            apartment = "شقة في ألفاما",
            groceries = "مشتريات السوبرماركت",
            carRental = "استئجار سيارة",
            boatTour = "جولة بالقارب",
            taxi = "تاكسي المطار",
            roommates = "السكن المشترك",
            soccer = "كرة القدم يوم الجمعة",
            court = "حجز الملعب",
            items = listOf("بيتزا مارغريتا", "بوراتا", "ريزوتو بالفطر", "تيراميسو", "بروسيكو", "إسبريسو"),
        ),
        "he" to SampleTripCopy(
            groupName = "טיול לליסבון",
            people = listOf("נועה", "איתי", "מאיה", "יונתן", "שירה"),
            dinner = "ארוחת ערב בטרטוריה",
            apartment = "דירה באלפמה",
            groceries = "קניות בסופר",
            carRental = "השכרת רכב",
            boatTour = "שיט בנהר",
            taxi = "מונית לשדה התעופה",
            roommates = "הדירה המשותפת",
            soccer = "כדורגל ביום שישי",
            court = "השכרת מגרש",
            items = listOf("פיצה מרגריטה", "בוראטה", "ריזוטו פטריות", "טירמיסו", "פרוסקו", "אספרסו"),
        ),
    )
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
        "he" to GroupCurrency.ILS,
    )

    fun avatarColorOf(person: Int): AvatarColor = avatarColors[person % avatarColors.size]

    fun tripOf(language: String): SampleTrip {
        val copy = copies[language] ?: copies.getValue("en")
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
            GroupCurrency.ILS -> 3.7 to 100L
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
