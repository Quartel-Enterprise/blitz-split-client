package com.quare.blitzsplit.feature.editprofile.di

import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val editProfileModule = module {
    viewModelOf(::EditProfileViewModel)
}
