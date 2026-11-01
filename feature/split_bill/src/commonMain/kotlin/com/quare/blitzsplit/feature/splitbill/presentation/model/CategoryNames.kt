package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.utils.TextFold

/*
 * Why: "Buscar categoria" and "Não pode repetir dentro da mesma categoria" need the app categories' names, which each UI
 * writes in its language. These are only what they are matched by, in both languages the app speaks, never shown.
 */
internal object CategoryNames {
    // Why: by any word of a name starting with what is typed, ignoring case and accents ("alug" finds "Aluguel").
    fun matches(
        category: Category,
        query: String,
    ): Boolean {
        val folded = TextFold.toFolded(query.trim())
        return namesOf(category).any { name ->
            val words = TextFold.toFolded(name).split(' ', '-', '&', ',').filter { it.isNotEmpty() }
            words.any { it.startsWith(folded) } || TextFold.toFolded(name).startsWith(folded)
        }
    }

    fun isNamed(
        category: Category,
        name: String,
    ): Boolean {
        val folded = TextFold.toFolded(name.trim())
        return namesOf(category).any { TextFold.toFolded(it) == folded }
    }

    private fun namesOf(category: Category): List<String> = category.app?.let(::appNamesOf) ?: listOfNotNull(category.name)

    private fun appNamesOf(category: AppCategory): List<String> = when (category) {
        AppCategory.FOOD -> listOf("Alimentação", "Food")
        AppCategory.FOOD_RESTAURANT -> listOf("Restaurante", "Restaurant")
        AppCategory.FOOD_MARKET -> listOf("Mercado", "Groceries")
        AppCategory.FOOD_BAR -> listOf("Bar", "Bar")
        AppCategory.FOOD_DELIVERY -> listOf("Delivery", "Delivery")
        AppCategory.FOOD_BAKERY -> listOf("Padaria e café", "Bakery and coffee")
        AppCategory.HOME -> listOf("Casa", "Home")
        AppCategory.HOME_RENT -> listOf("Aluguel", "Rent")
        AppCategory.HOME_CONDO -> listOf("Condomínio", "Condo fees")
        AppCategory.HOME_POWER -> listOf("Luz", "Electricity")
        AppCategory.HOME_WATER -> listOf("Água", "Water")
        AppCategory.HOME_GAS -> listOf("Gás", "Gas")
        AppCategory.HOME_INTERNET -> listOf("Internet", "Internet")
        AppCategory.HOME_CLEANING -> listOf("Limpeza", "Cleaning")
        AppCategory.TRANSPORT -> listOf("Transporte", "Transport")
        AppCategory.TRANSPORT_FUEL -> listOf("Combustível", "Fuel")
        AppCategory.TRANSPORT_RIDE -> listOf("Corrida por app", "Ride-hailing")
        AppCategory.TRANSPORT_PARKING -> listOf("Estacionamento", "Parking")
        AppCategory.TRANSPORT_TOLL -> listOf("Pedágio", "Tolls")
        AppCategory.TRANSPORT_TRANSIT -> listOf("Ônibus e metrô", "Bus and subway")
        AppCategory.TRAVEL -> listOf("Viagem", "Travel")
        AppCategory.TRAVEL_LODGING -> listOf("Hospedagem", "Lodging")
        AppCategory.TRAVEL_FLIGHT -> listOf("Passagem", "Tickets and flights")
        AppCategory.TRAVEL_TOURS -> listOf("Passeios", "Tours")
        AppCategory.TRAVEL_CAR_RENTAL -> listOf("Aluguel de carro", "Car rental")
        AppCategory.LEISURE -> listOf("Lazer", "Leisure")
        AppCategory.LEISURE_TICKETS -> listOf("Ingressos", "Event tickets")
        AppCategory.LEISURE_PARTIES -> listOf("Festas", "Parties")
        AppCategory.LEISURE_SPORTS -> listOf("Esportes", "Sports")
        AppCategory.LEISURE_GAMES -> listOf("Jogos", "Games")
        AppCategory.SUBSCRIPTIONS -> listOf("Assinaturas", "Subscriptions")
        AppCategory.SUBSCRIPTIONS_STREAMING -> listOf("Streaming", "Streaming")
        AppCategory.SUBSCRIPTIONS_MUSIC -> listOf("Música", "Music")
        AppCategory.SUBSCRIPTIONS_APPS -> listOf("Apps", "Apps")
        AppCategory.SHOPPING -> listOf("Compras", "Shopping")
        AppCategory.SHOPPING_CLOTHES -> listOf("Roupas", "Clothes")
        AppCategory.SHOPPING_GIFTS -> listOf("Presentes", "Gifts")
        AppCategory.SHOPPING_ELECTRONICS -> listOf("Eletrônicos", "Electronics")
        AppCategory.SHOPPING_HOME -> listOf("Casa e decoração", "Home and decor")
        AppCategory.HEALTH -> listOf("Saúde", "Health")
        AppCategory.HEALTH_PHARMACY -> listOf("Farmácia", "Pharmacy")
        AppCategory.HEALTH_APPOINTMENTS -> listOf("Consultas", "Appointments")
        AppCategory.HEALTH_GYM -> listOf("Academia", "Gym")
        AppCategory.EDUCATION -> listOf("Educação", "Education")
        AppCategory.EDUCATION_COURSES -> listOf("Cursos", "Courses")
        AppCategory.EDUCATION_BOOKS -> listOf("Livros", "Books")
        AppCategory.EDUCATION_SUPPLIES -> listOf("Material", "Supplies")
        AppCategory.OTHER -> listOf("Outros", "Other")
    }
}
