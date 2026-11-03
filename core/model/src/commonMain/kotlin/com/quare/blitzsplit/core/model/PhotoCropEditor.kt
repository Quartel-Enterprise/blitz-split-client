package com.quare.blitzsplit.core.model

/*
 * Why: "Ajustar foto", the editor a picked or taken photo opens in before it becomes the profile's or the group's:
 * the photo and where it sits in the frame. The screen that opened it picks the frame (a profile's circle, a
 * group's rounded square).
 */
data class PhotoCropEditor(
    val photo: CropPhoto,
    val crop: PhotoCrop,
)
