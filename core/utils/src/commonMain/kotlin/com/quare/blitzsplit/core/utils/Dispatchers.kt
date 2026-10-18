package com.quare.blitzsplit.core.utils

import kotlinx.coroutines.CoroutineDispatcher

// Why: the browser has no Dispatchers.IO, so each platform picks its own.
expect val ioDispatcher: CoroutineDispatcher
