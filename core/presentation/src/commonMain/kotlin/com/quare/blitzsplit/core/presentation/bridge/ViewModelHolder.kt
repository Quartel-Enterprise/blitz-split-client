package com.quare.blitzsplit.core.presentation.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/*
 * Why: outside Compose nothing owns a ViewModelStore, so SwiftUI and React keep one per screen and
 * clear it when the screen goes away, which cancels viewModelScope like Android does.
 */
class ViewModelHolder<VM : ViewModel>(
    kClass: KClass<VM>,
    create: () -> VM,
) {
    private val store = ViewModelStore()

    val viewModel: VM = ViewModelProvider
        .create(
            store = store,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T = create() as T
            },
        ).get(kClass)

    fun clear() {
        store.clear()
    }
}
