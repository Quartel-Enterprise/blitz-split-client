package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: the color a category shows, one of the twelve named ones or a custom one from someone's palette. Its code is what
 * Room and the server store (`group_categories.color`, `app_category_color`): a named color's key ("teal") or a custom
 * color's hex ("#3A7BD5"). A client older than custom colors reads a hex as none picked, the category's default.
 */
sealed interface CategoryTint {
    val code: String

    // Why: what Analytics gets: the named color's key, never a person's own hex.
    val analyticsCode: String

    data class Named(
        val color: CategoryColor,
    ) : CategoryTint {
        override val code: String
            get() = color.code

        override val analyticsCode: String
            get() = color.code
    }

    data class Custom(
        val color: CustomColor,
    ) : CategoryTint {
        override val code: String
            get() = color.hex

        override val analyticsCode: String
            get() = CUSTOM_ANALYTICS_CODE
    }

    companion object {
        private const val CUSTOM_ANALYTICS_CODE = "custom"

        // Why: a code this version doesn't know reads as none picked, so the category takes its default.
        fun fromCodeOrNull(code: String?): CategoryTint? = CategoryColor.fromCodeOrNull(code)?.let(::Named)
            ?: CustomColor.fromHexOrNull(code)?.takeIf { code?.startsWith("#") == true }?.let(::Custom)
    }
}

val CategoryColor.tint: CategoryTint
    get() = CategoryTint.Named(this)

val CustomColor.tint: CategoryTint
    get() = CategoryTint.Custom(this)
