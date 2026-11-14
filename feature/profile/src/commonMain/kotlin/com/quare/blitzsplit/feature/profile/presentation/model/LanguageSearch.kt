package com.quare.blitzsplit.feature.profile.presentation.model

import com.quare.blitzsplit.core.model.AppLanguage
import com.quare.blitzsplit.core.utils.TextFolding

// Why: Profile › Language's search, folded as the other searches ("alemao" finds "Alemão").
internal object LanguageSearch {
    fun filter(
        options: List<AppLanguage>,
        query: String,
        namesOf: (AppLanguage) -> List<String>,
    ): List<AppLanguage> {
        val folded = TextFolding.toFolded(query.trim())
        if (folded.isEmpty()) return options
        return options.filter { language ->
            val code = language.tag?.substringBefore('-')
            code == folded || namesOf(language).any { name -> folded in TextFolding.toFolded(name) }
        }
    }
}
