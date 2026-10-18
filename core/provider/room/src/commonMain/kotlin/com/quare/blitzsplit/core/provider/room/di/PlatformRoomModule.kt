package com.quare.blitzsplit.core.provider.room.di

import org.koin.core.module.Module

/*
 * Why: each platform opens the database file its own way (a Context path, the app's Application
 * Support folder, OPFS through a Web Worker).
 */
internal expect val platformRoomModule: Module
