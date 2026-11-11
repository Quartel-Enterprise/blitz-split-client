package com.quare.blitzsplit.core.utils

// Why: the web sets the page's lang to the language its UI speaks; a worker, which has no page, uses the browser's.
actual fun String.toAppLanguageUppercase(): String {
    val language: String? = js("typeof document !== 'undefined' ? document.documentElement.lang : undefined")
    val text = asDynamic()
    return (if (language.isNullOrEmpty()) text.toLocaleUpperCase() else text.toLocaleUpperCase(language)) as String
}
