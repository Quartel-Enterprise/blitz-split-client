package com.quare.blitzsplit.feature.editprofile.presentation.model

// Why: not NameError, which profile setup exports to Swift already and a second one would be renamed NameError_.
enum class EditProfileNameError {
    TOO_SHORT,
    TOO_LONG,
}
