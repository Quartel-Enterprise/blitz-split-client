package com.quare.blitzsplit.feature.editprofile.di

import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.EditProfileViewModel
import com.quare.blitzsplit.feature.editprofile.presentation.viewmodel.ProfilePhotoViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val editProfileModule = module {
    factoryOf(::EditProfileViewModel)
    factoryOf(::ProfilePhotoViewModel)
}
